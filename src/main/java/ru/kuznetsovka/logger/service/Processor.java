package ru.kuznetsovka.logger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.kuznetsovka.logger.dto.RfidInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class Processor {

    private static final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RfidDuplicateTracker duplicateTracker;
    private final NotificationService notificationService;

    public void processNotification(String message, String clientIp) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            RfidInfo rfidInfo = mapper.readValue(message, RfidInfo.class);

            // Логируем
            logNotification(rfidInfo);

            // Отслеживаем дубликаты
            duplicateTracker.processRfid(rfidInfo.getRfid(), rfidInfo.getAntenna());

        } catch (final Exception e) {
            // Отправляем уведомление об ошибке
            String errorMsg = String.format(
                    "⚠️ *Ошибка обработки RFID*\\n" +
                            "• *Сообщение:* `%s`\\n" +
                            "• *IP клиента:* %s\\n" +
                            "• *Ошибка:* %s",
                    message.length() > 50 ? message.substring(0, 50) + "..." : message,
                    clientIp,
                    e.getMessage()
            );
            log.error("❌ Ошибка обработки сообщения: {} error {}", errorMsg, e.getMessage());
        }
    }

    private void logNotification(RfidInfo rfidInfo) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        log.info("📨 [{}] Антенна: {} | RFID: {}",
                timestamp, rfidInfo.getAntenna(), rfidInfo.getRfid());
    }
}