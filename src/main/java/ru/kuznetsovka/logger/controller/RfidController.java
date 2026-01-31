package ru.kuznetsovka.logger.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsovka.logger.dto.RfidEvent;
import ru.kuznetsovka.logger.service.NotificationService;
import ru.kuznetsovka.logger.service.RfidDuplicateTracker;

@RestController
@RequestMapping("/api/rfid")
@RequiredArgsConstructor
public class RfidController {
    
    private final RfidDuplicateTracker tracker;
    private final NotificationService notificationService;
    
    @GetMapping("/active")
    public ResponseEntity<ArrayList<Object>> getActiveRfids() {
        return ResponseEntity.ok(
                new ArrayList<>(tracker.getActiveEvents().values())
        );
    }
    
    @GetMapping("/test-alert/{rfid}")
    public ResponseEntity<String> testAlert(@PathVariable("rfid") String rfid) {
        RfidEvent testEvent = new RfidEvent(
            rfid, 
            1,
            LocalDateTime.now().minusMinutes(6),
            LocalDateTime.now(),
            30,
            false
        );
        tracker.sendNotification(testEvent);
        return ResponseEntity.ok("Тестовое уведомление отправлено");
    }
}