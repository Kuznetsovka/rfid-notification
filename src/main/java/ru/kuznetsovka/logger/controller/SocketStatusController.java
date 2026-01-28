package ru.kuznetsovka.logger.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kuznetsovka.logger.service.NotificationSocketServer;

@RestController
@RequestMapping("/api/socket-status")
@RequiredArgsConstructor
public class SocketStatusController {
    
    private final NotificationSocketServer socketServer;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", true);
        status.put("clientConnected", socketServer.isClientConnected());
        status.put("clientIp", socketServer.getClientIp());
        status.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }
}