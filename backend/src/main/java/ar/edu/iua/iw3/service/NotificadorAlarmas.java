package ar.edu.iua.iw3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Manda el mail de la alarma. Se ejecuta DESPUES del commit: asi nunca sale un mail de una alarma
 * que despues no se pudo guardar. Si el mail falla, solo se registra en el log: la alarma ya esta
 * guardada y se ve en la pantalla de alarmas, y no se pierde el dato de carga.
 */
@Component
public class NotificadorAlarmas {

    private static final Logger log = LoggerFactory.getLogger(NotificadorAlarmas.class);

    private final JavaMailSender mailSender;
    private final String[] destinatarios;
    private final String remitente;

    public NotificadorAlarmas(JavaMailSender mailSender,
                              @Value("${app.alarma.destinatarios}") String[] destinatarios,
                              @Value("${app.alarma.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.destinatarios = destinatarios;
        this.remitente = remitente;
    }

    @TransactionalEventListener
    public void alCrearseAlarma(AlarmaCreada evento) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(remitente);
        mail.setTo(destinatarios);
        mail.setSubject("Alarma de temperatura - Orden " + evento.numeroOrden());
        mail.setText("""
                La orden %d registro una temperatura de %.1f grados, por encima del umbral de %.1f.

                Ingresar al sistema para aceptar la alarma (alarma n° %d).
                No se enviaran mas mails de esta orden hasta que se acepte."""
                .formatted(evento.numeroOrden(), evento.temperatura(), evento.umbral(), evento.alarmaId()));
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            log.error("No se pudo enviar el mail de la alarma {}: {}", evento.alarmaId(), e.getMessage());
        }
    }
}
