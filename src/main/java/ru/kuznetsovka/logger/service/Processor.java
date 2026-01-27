package ru.kuznetsovka.logger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.kuznetsovka.logger.dto.Pass;
import ru.kuznetsovka.logger.dto.RfidInfo;

@Slf4j
@Service
public class Processor {
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public void processNotification(String message, String clientIp) {
        // Ваша бизнес-логика обработки уведомлений
        log.debug("🔄 Обработка уведомления от {}: {}", clientIp, message);

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final RfidInfo rfidInfo = mapper.readValue(message, RfidInfo.class);
            logNotification(rfidInfo);
        } catch (Exception e) {
            log.error("❌ Ошибка обработки сообщения от {}: {}", clientIp, e.getMessage());
        }
    }

    private void logNotification(final RfidInfo rfidInfo) {
        final String timestamp = LocalDateTime.now().format(timeFormatter);
        final String pass = Pass.valueOf(rfidInfo.getAntenna());
        log.info("📨 Время: [{}] | Направление: {} | RFID: {} | Владелец: {}",
                timestamp, pass, rfidInfo.getRfid(), null);
    }
}
