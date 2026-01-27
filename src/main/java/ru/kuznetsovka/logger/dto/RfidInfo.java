package ru.kuznetsovka.logger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class RfidInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("SN")
    private String sn;

    @JsonProperty("RTC")
    private String rtc;

    @JsonProperty("ANT")
    private int antenna;

    @JsonProperty("EPC")
    private String epc;

    @JsonProperty("EPCLEN")
    private int epcLength;

    @JsonProperty("RSSI")
    private int rssi;

    @JsonProperty("BANK")
    private int bank;

    @JsonProperty("DATA")
    private String rfid;

    @JsonProperty("DATALEN")
    private int dataLength;

}
