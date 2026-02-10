package ru.kuznetsovka.logger.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSocketServer {

    @Value("${notification.socket.port:52000}")
    private int port;

    @Value("${notification.socket.bind-address:0.0.0.0}")
    private String bindAddress;

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private volatile Socket activeClientSocket = null; // Храним активное подключение
    private final Processor processor;
    private final SecureServer secureServer;
    private final NotificationService notificationService;
    private Thread acceptorThread;

    @EventListener(ApplicationReadyEvent.class)
    public void startSocketServer() {
        log.info("⏳ Запуск сокет-сервера для одного подключения на {}:{}", bindAddress, port);

        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName(bindAddress)); // backlog=1
            serverSocket.setSoTimeout(5000);

            isRunning = true;
            log.info("✅ Сокет-сервер запущен. Ожидаю подключение контроллера...");
            // Запускаем в отдельном потоке
            if (acceptorThread == null || !acceptorThread.isAlive()) {
                acceptorThread = new Thread(this::acceptSingleConnection, "Socket-Acceptor");
                acceptorThread.setDaemon(true); // Делаем демоном для автоматического завершения
                acceptorThread.start();
            }

        } catch (IOException e) {
            stopSocketServer();
            log.error("❌ Ошибка запуска сервера", e);
        }
    }

    private void acceptSingleConnection() {
        while (isRunning) {
            try {
                log.debug("👂 Ожидаю подключения...");
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();

                boolean isAllowedSocket = secureServer.secure(clientSocket);
                if (!isAllowedSocket) {
                    notificationService.sendAlert("Несанкционированный ip: " + clientIp);
                    continue;
                }
                // Принимаем новое подключение
                activeClientSocket = clientSocket;
                activeClientSocket.setSoTimeout(30000);

                log.debug("✅ Подключение установлено: {}", clientIp);
                log.debug("📡 Готов к приему уведомлений...");

                // Обрабатываем это подключение
                handleSingleClient(clientSocket, clientIp);

            } catch (final SocketTimeoutException e) {
                // Таймаут accept - нормально, продолжаем цикл
            } catch (IOException e) {
                if (isRunning) {
                    stopSocketServer();
                    log.error("Ошибка accept", e);
                }
            }
        }
    }

    private void handleSingleClient(Socket clientSocket, String clientIp) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {

            char[] buffer = new char[4096];

            while (isRunning && clientSocket.isConnected() && !clientSocket.isClosed()) {
                try {
                    int bytesRead = reader.read(buffer);
                    if (bytesRead == -1) {
                        log.debug("📤 Клиент закрыл соединение");
                        break;
                    }

                    String message = new String(buffer, 0, bytesRead).trim();
                    if (!message.isEmpty()) {
                        processor.processNotification(message, clientIp);
                    }

                } catch (SocketTimeoutException e) {
                    // Таймаут чтения - проверяем соединение
                    if (!clientSocket.isConnected() || clientSocket.isClosed()) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            log.error("💥 Ошибка чтения данных", e);
        } finally {
            closeClientSocket(clientSocket);

            // Если это было активное подключение - очищаем
            if (clientSocket == activeClientSocket) {
                activeClientSocket = null;
                log.debug("🔄 Готов к новому подключению");
            }
        }
    }

    private void closeClientSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Не удалось закрыть сокет");
        }
    }

    @PreDestroy
    public void stopSocketServer() {
        log.info("🛑 Останавливаю сервер...");
        isRunning = false;

        if (activeClientSocket != null) {
            closeClientSocket(activeClientSocket);
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Ошибка закрытия серверного сокета", e);
            }
        }

        if (acceptorThread != null && acceptorThread.isAlive()) {
            acceptorThread.interrupt();
            try {
                acceptorThread.join(5000); // Ждем завершения потока
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("✅ Сервер остановлен");
    }

    // Метод для проверки статуса (можно использовать для health check)
    public boolean isClientConnected() {
        return activeClientSocket != null
                && activeClientSocket.isConnected()
                && !activeClientSocket.isClosed();
    }

    public String getClientIp() {
        return activeClientSocket != null ?
                activeClientSocket.getInetAddress().getHostAddress() : "нет подключения";
    }
}