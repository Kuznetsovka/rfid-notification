package ru.kuznetsovka.logger.dto;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Pass {
    INPUT(1, "Въезд"), OUTPUT(2, "Выезд");

    private final int antenna;
    private final String desc;

    public static String valueOf(int antenna) {
        return Arrays.stream(Pass.values())
                .filter(pass -> antenna == pass.antenna)
                .map(pass -> pass.desc)
                .findAny()
                .orElse("Не определено");
    }
}
