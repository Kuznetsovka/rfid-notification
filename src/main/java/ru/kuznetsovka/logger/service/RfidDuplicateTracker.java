package ru.kuznetsovka.logger.service;

import com.google.common.cache.CacheBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.kuznetsovka.logger.dto.RfidEvent;

@RequiredArgsConstructor
@Component
@Slf4j
public class RfidDuplicateTracker {
    
    @Value("${notification.rfid.duplicate-timeout-minutes:2}")
    private int duplicateTimeoutMinutes;
    
    @Value("${notification.rfid.check-interval-seconds:10}")
    private int checkIntervalSeconds;

    @Value("${notification.rfid.count-duplicate:10}")
    private int countDuplicate;


    private final ConcurrentMap<Object, Object> rfidEvents = CacheBuilder
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()
            .asMap();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final NotificationService notificationService;
    
    @PostConstruct
    public void init() {
        // Запускаем периодическую проверку
        scheduler.scheduleAtFixedRate(
            this::checkDuplicates,
            checkIntervalSeconds,
            checkIntervalSeconds,
            TimeUnit.SECONDS
        );
        log.info("✅ Трекер дубликатов запущен. Таймаут: {} мин, проверка каждые {} сек", 
                duplicateTimeoutMinutes, checkIntervalSeconds);
    }
    
    public void processRfid(String rfid, int antenna) {
        String key = rfid + "_" + antenna;
        LocalDateTime now = LocalDateTime.now();
        
        rfidEvents.compute(key, (k, obj) -> {
            RfidEvent existingEvent = (RfidEvent) obj;
            if (existingEvent == null) {
                // Новая метка
                log.debug("📌 Новая метка: RFID={}, Антенна={}", rfid, antenna);
                return new RfidEvent(rfid, antenna, now, now, 1, false);
            } else {
                // Обновляем существующую
                existingEvent.setLastSeen(now);
                existingEvent.setNotificationCount(existingEvent.getNotificationCount() + 1);
                log.debug("🔄 Обновлена метка: RFID={}, Антенна={}, Счетчик={}", 
                         rfid, antenna, existingEvent.getNotificationCount());
                return existingEvent;
            }
        });
    }
    
    private void checkDuplicates() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.minusMinutes(duplicateTimeoutMinutes);

            rfidEvents.entrySet().removeIf(entry -> {
                RfidEvent event = (RfidEvent) entry.getValue();

                // Проверяем дубликаты
                if (!event.isNotified() &&
                        event.getFirstSeen().isBefore(threshold) &&
                        event.getNotificationCount() >= countDuplicate) {

                    // Отправляем уведомление
                    sendNotification(event);
                    event.setNotified(true);

                    log.warn("🚨 ДУБЛИКАТ: Метка {} на антенне {} висит более {} минут. Счетчик: {}",
                            event.getRfid(), event.getAntenna(), duplicateTimeoutMinutes,
                            event.getNotificationCount());
                }

                return false;
            });
        } catch (final Exception e) {
            log.error(e.getMessage());
        }
    }
    
    public void sendNotification(RfidEvent event) {
        String message = String.format(
            "🚨 *ДУБЛИКАТ RFID*\n" +
            "• *Метка:* `%s`\n" +
            "• *Антенна:* %d\n" +
            "• *Время обнаружения:* %s\n" +
            "• *Последний раз:* %s\n" +
            "• *Количество срабатываний:* %d\n" +
            "• *Длительность:* больше %d минут",
            event.getRfid(),
            event.getAntenna(),
            event.getFirstSeen().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            event.getLastSeen().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            event.getNotificationCount(),
            duplicateTimeoutMinutes
        );
        
        notificationService.sendAlert(message);
    }
    
    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public HashMap<Object, Object> getActiveEvents() {
        return new HashMap<>(rfidEvents);
    }
}