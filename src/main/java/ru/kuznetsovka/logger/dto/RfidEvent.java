package ru.kuznetsovka.logger.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RfidEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String rfid;
    private int antenna;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private int notificationCount;
    private boolean notified;
}