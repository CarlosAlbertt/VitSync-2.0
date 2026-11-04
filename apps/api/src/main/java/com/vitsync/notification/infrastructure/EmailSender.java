package com.vitsync.notification.infrastructure;

/**
 * Envía un email ya renderizado. Interfaz para poder simularlo en los tests sin SMTP.
 *
 * <p>Lanza una excepción si el envío falla: el worker la captura y programa el reintento.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
