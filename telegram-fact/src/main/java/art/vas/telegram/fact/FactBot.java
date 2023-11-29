package art.vas.telegram.fact;

import art.vas.telegram.fact.command.CallBackMessageCommando;
import art.vas.telegram.fact.command.Commando;
import art.vas.telegram.fact.command.ReduplicatorCommand;
import art.vas.telegram.fact.service.UserService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.CastUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class FactBot extends TelegramLongPollingBot {
    private final String name;
    private final Map<String, Commando<?>> map = new CaseInsensitiveMap<>();
    private final Map<String, CallBackMessageCommando> calBackMap = new HashMap<>();
    private final ReduplicatorCommand all;
    private final UserService userService;

    @PostConstruct
    public void after() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
    }

    public FactBot(@Value("${tg.token}") String botToken,
                   @Value("${tg.name}") String tgName,
                   List<Commando<?>> commands,
                   UserService userService) {
        super(botToken);
        this.name = tgName;
        this.userService = userService;
        for (Commando<?> command : commands) {
            for (String commandLine : command.getCommandLines()) {
                if (command instanceof CallBackMessageCommando call) {
                    this.calBackMap.put(call.getCallBack(), call);
                }
                this.map.put(commandLine, command);
            }
        }
        this.all = CastUtils.cast(map.get(StringUtils.EMPTY));
    }

    @Override
    public void onUpdateReceived(Update update) {
        userService.log(update);
        System.out.println(update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            //String[] split = StringUtils.split(update.getMessage().getText(), key);
            Commando<?> commando = map.get(update.getMessage().getText());
            if (Objects.isNull(commando)) {
                commando = all;
            }
            commando.takeIt(update, this);
        }
        if (update.hasCallbackQuery()) {
            String text = update.getCallbackQuery().getMessage().getText();
            CallBackMessageCommando<?> commando = calBackMap.get(text);
            commando.alternative(update.getCallbackQuery(), this);
        }
    }

    @Override
    public String getBotUsername() {
        return name;
    }
}
