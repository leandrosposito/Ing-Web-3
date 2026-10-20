package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.ConfiguracionAlarma;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ConfiguracionAlarmaResponse(
        double temperaturaUmbral,
        List<String> destinatarios,
        String actualizadoPor,
        LocalDateTime fechaActualizacion) {

    public static ConfiguracionAlarmaResponse desde(ConfiguracionAlarma c) {
        return new ConfiguracionAlarmaResponse(c.getTemperaturaUmbral(),
                Arrays.asList(c.getDestinatarios().split(",")), c.getActualizadoPor(), c.getFechaActualizacion());
    }
}
