package art.vas.telegram.fact.command;

import art.vas.telegram.fact.config.JsonConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.HOST;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@Component
@RequiredArgsConstructor
public class ChatGptCommando implements SimpleMessageCommando {
    private final ObjectMapper objectMapper;
    public static final String openAi = "https://proxy.usagepanda.com/v1/chat/completions";

    @Value("${open.ai.token}")
    String openAiToken;

    @Value("${panda.proxy.token}")
    String pandaProxyToken;

    @Override
    public List<String> getCommandLines() {
        return Arrays.asList("/bot", "/robot", "/бот", "/gpt");
    }

    @Override
    public SendMessage answer(Message message) {
        String chatId = message.getChatId().toString();

        String text = "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}";
        String format = String.format(text, message.getText());
//        JsonNode body = objectMapper.valueToTree(format);

        HttpEntity<?> entity = new HttpEntity<>(format,
                toMultiValueMap(Map.of(AUTHORIZATION, singletonList(openAiToken),
                        "x-usagepanda-api-key", singletonList(pandaProxyToken),
                        HOST, singletonList("usagepanda.com"),
                        CONTENT_TYPE, singletonList(MediaType.APPLICATION_JSON))));
        ResponseEntity<JsonNode> forEntity = JsonConfig.restTemplate.exchange(openAi, HttpMethod.POST, entity, JsonNode.class);

        return new SendMessage(chatId, forEntity.getBody().findValue("content").asText());
    }
}
