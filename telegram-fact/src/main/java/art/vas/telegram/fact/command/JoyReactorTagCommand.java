package art.vas.telegram.fact.command;

import art.vas.telegram.fact.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Base64Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
@RequiredArgsConstructor
public class JoyReactorTagCommand implements CallBackMessageCommando<SendMediaGroup> {
    private final char separator = '/';
    private final JoyReactorFeedCommand command;
    public final Map<Long, String> nextMsg = new HashMap<>();

    @Override
    public String getCommandLine() {
        return "/joytags";
    }

    @Override
    public String getCallBack() {
        return "Выберите тег";
    }

    @Override
    public SendMediaGroup alternative(CallbackQuery message) {
        nextMsg.clear();
        Long chatId = message.getMessage().getChatId();
        String data = new String(Base64.decodeBase64(message.getData()));
        data = "tag/" + UriUtils.encode(data, UTF_8);
        SendMediaGroup sendMediaGroup = command.getSendMediaGroup(chatId, data);
        sendMediaGroup.setReplyToMessageId(message.getMessage().getMessageId());
        return sendMediaGroup;
    }

    @Override
    @SneakyThrows
    public SendMessage answer(Message message) {
        command.nextMsg.clear();

        Long chatId = message.getChatId();
        String next = StringUtils.defaultString(nextMsg.remove(chatId));

        Document doc = Jsoup.connect(JoyReactorFeedCommand.url + "tags/subscribers" + next).get();
        Elements elements = doc.select("strong");
        List<String> links = elements.stream().map(s -> s.select("a").attr("href"))
                .map(s -> StringUtils.substringAfter(s, "/tag/"))
                .filter(s -> !StringUtils.contains(s, separator))
                .filter(StringUtils::isNotEmpty).toList();

        SendMessage sendMessage = new SendMessage(chatId.toString(), getCallBack());

        List<List<String>> partition = ListUtils.partition(links, 3);

        List<List<InlineKeyboardButton>> list = partition.stream().map(t -> t.stream()
                .map(s -> UriUtils.decode(s, UTF_8))
                .map(s -> {
                    InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                    String encode = Base64Util.encode(s);
                    if (StringUtils.length(encode) > 64) return null;
                    inlineKeyboardButton.setCallbackData(encode);
                    inlineKeyboardButton.setText(Utils.toCyrillic(s));
                    return inlineKeyboardButton;
                }).filter(Objects::nonNull).collect(Collectors.toList())).collect(Collectors.toList());

        sendMessage.setReplyMarkup(new InlineKeyboardMarkup(list));

        String href = doc.select("a[class=next]").attr("href");
        nextMsg.put(chatId, separator + StringUtils.substringAfterLast(href, separator));

        return sendMessage;
    }

    @Override
    public String getAnswer() {
        return null;
    }
}