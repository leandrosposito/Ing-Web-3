package ar.edu.iua.iw3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Manda el mail de la alarma despues del commit (nunca sale el mail de una alarma que no se guardo) y en otro hilo
 * (@Async), asi un SMTP lento no demora la respuesta al sistema de carga. Si falla, solo se registra en el log:
 * la alarma ya esta guardada y se ve en la pantalla.
 */
@Component
public class NotificadorAlarmas {

    private static final Logger log = LoggerFactory.getLogger(NotificadorAlarmas.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public NotificadorAlarmas(JavaMailSender mailSender,
                              @Value("${app.alarma.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Async
    @TransactionalEventListener
    public void alCrearseAlarma(AlarmaCreada evento) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(remitente);
        mail.setTo(evento.destinatarios().toArray(new String[0]));
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
