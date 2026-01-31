package ru.kuznetsovka.logger.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class NotificationService {

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.chat_ids}")
    private String[] chatIds;

    @Value("${telegram.bot.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    public void sendAlert(String message) {
        if (!enabled || botToken.isEmpty() || chatIds.length < 1) {
            log.warn("Telegram уведомления отключены или не настроены");
            return;
        }

        try {
            // Форматируем для Telegram Markdown
            String escapedMessage = message
                .replace(".", "\\.")
                .replace("-", "\\-")
                .replace("(", "\\(")
                .replace(")", "\\)");

            String url = TELEGRAM_API + botToken + "/sendMessage";
            for (final String chatId : chatIds) {
                if (chatId.isEmpty()) {
                    continue;
                }
                Map<String, Object> request = new HashMap<>();
                request.put("chat_id", chatId);
                request.put("text", escapedMessage);
                request.put("parse_mode", "MarkdownV2");
                request.put("disable_notification", false);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Уведомление отправлено в Telegram");
                } else {
                    log.error("❌ Ошибка отправки в Telegram: {}", response.getBody());
                }
            }
        } catch (Exception e) {
            log.error("❌ Ошибка отправки Telegram уведомления", e);
        }
    }
}