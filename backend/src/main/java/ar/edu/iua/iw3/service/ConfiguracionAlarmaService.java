package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.ConfiguracionAlarmaRequest;
import ar.edu.iua.iw3.model.ConfiguracionAlarma;
import ar.edu.iua.iw3.repository.ConfiguracionAlarmaRepository;
import ar.edu.iua.iw3.security.UsuarioActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Umbral y destinatarios, en la base para cambiarlos sin reiniciar. La primera vez salen de application.properties. */
@Service
public class ConfiguracionAlarmaService {

    private final ConfiguracionAlarmaRepository repository;
    private final double umbralInicial;
    private final String destinatariosIniciales;

    public ConfiguracionAlarmaService(ConfiguracionAlarmaRepository repository,
                                      @Value("${app.alarma.temperatura-umbral}") double umbralInicial,
                                      @Value("${app.alarma.destinatarios}") String destinatariosIniciales) {
        this.repository = repository;
        this.umbralInicial = umbralInicial;
        this.destinatariosIniciales = destinatariosIniciales;
    }

    @Transactional
    public ConfiguracionAlarma obtener() {
        return repository.findById(ConfiguracionAlarma.ID)
                .orElseGet(() -> repository.save(new ConfiguracionAlarma(umbralInicial, destinatariosIniciales)));
    }

    @Transactional
    public ConfiguracionAlarma actualizar(ConfiguracionAlarmaRequest req) {
        ConfiguracionAlarma config = obtener();
        config.setTemperaturaUmbral(req.temperaturaUmbral());
        config.setDestinatarios(String.join(",", req.destinatarios().stream().map(String::trim).toList()));
        config.setActualizadoPor(UsuarioActual.nombre());
        config.setFechaActualizacion(LocalDateTime.now());
        return config;
    }
}
