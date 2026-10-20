package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.ConfiguracionAlarmaRequest;
import ar.edu.iua.iw3.model.ConfiguracionAlarma;
import ar.edu.iua.iw3.repository.ConfiguracionAlarmaRepository;
import ar.edu.iua.iw3.security.UsuarioActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Umbral y destinatarios de las alarmas, guardados en la base para poder cambiarlos sin reiniciar.
 * La primera vez (tabla vacia) se crea con los valores de application.properties / variables de entorno.
 */
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

    /** Devuelve la configuracion vigente; si todavia no existe la crea con los valores iniciales. */
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
