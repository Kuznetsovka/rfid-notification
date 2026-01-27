package ru.kuznetsovka.logger.dto;

import java.time.LocalDateTime;

public class ClientInfo {
        String ipAddress;
        LocalDateTime connectedAt;
        LocalDateTime lastActivity;
        int bytesReceived;
        
        public ClientInfo(String ipAddress) {
            this.ipAddress = ipAddress;
            this.connectedAt = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
            this.bytesReceived = 0;
        }
        
        public void updateActivity(int bytes) {
            this.lastActivity = LocalDateTime.now();
            this.bytesReceived += bytes;
        }
    }