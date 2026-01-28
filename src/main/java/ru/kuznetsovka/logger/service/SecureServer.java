package ru.kuznetsovka.logger.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecureServer {
    
    @Value("${notification.socket.allowed-ip:}")
    private String allowedIp;
    
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private final Map<String, Integer> connectionAttempts = new ConcurrentHashMap<>();
    
    public boolean secure(final Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        
        try {
            
            // 1. Проверка попыток подключения (защита от brute force)
            if (isBlockedIp(clientIp)) {
                log.warn("IP заблокирован: {}", clientIp);
                sendResponse(clientSocket, "ERROR: IP blocked");
                return false;
            }
            
            // 2. Проверка разрешенных IP
            if (!isIpAllowed(clientIp)) {
                log.warn("Неразрешенный IP: {}", clientIp);
                recordFailedAttempt(clientIp);
                sendResponse(clientSocket, "ERROR: IP not allowed");
                return false;
            }
            
            // 3. Все проверки пройдены
            currentConnections.incrementAndGet();
            log.info("✅ Принято защищенное подключение от {}", clientIp);
            return true;
        } catch (Exception e) {
            log.error("Ошибка обработки клиента", e);
            return false;
        } finally {
            currentConnections.decrementAndGet();
        }
    }
    
    private boolean isBlockedIp(String ip) {
        Integer attempts = connectionAttempts.get(ip);
        return attempts != null && attempts > 5; // Блокировка после 5 неудач
    }
    
    private void recordFailedAttempt(String ip) {
        connectionAttempts.merge(ip, 1, Integer::sum);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                connectionAttempts.remove(ip);
            }
        }, 15 * 60 * 1000);
    }
    
    private void sendResponse(Socket socket, String message) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write((message + "\n").getBytes());
        out.flush();
        socket.close();
    }

    private boolean isIpAllowed(String ip) {
        return allowedIp.equals(ip);
    }
}