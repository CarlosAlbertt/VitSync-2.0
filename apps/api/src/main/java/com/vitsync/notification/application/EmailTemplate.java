package com.vitsync.notification.application;

import java.util.Map;

/**
 * Los emails transaccionales que conoce el sistema. El nombre se guarda en
 * {@code outbox_messages.type}.
 *
 * <p>Plantillas en texto plano y en español (la interfaz va en español, CONTRIBUTING §3). Sin
 * HTML a propósito: menos superficie de inyección y se leen en cualquier cliente. Las
 * variables se sustituyen con {@code {nombre}}.
 */
public enum EmailTemplate {

    VERIFY_EMAIL(
            "Tu código de verificación de VitSync",
            """
            Hola, {firstName}:

            Tu código para verificar el email de tu cuenta de VitSync es:

                {code}

            Caduca en {minutes} minutos. Si no has creado una cuenta, ignora este mensaje.
            """),

    PASSWORD_RESET(
            "Recuperación de contraseña de VitSync",
            """
            Hola, {firstName}:

            Tu código para elegir una contraseña nueva en VitSync es:

                {code}

            Caduca en {minutes} minutos. Si no lo has pedido tú, ignora este mensaje: tu
            contraseña actual sigue siendo válida.
            """);

    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String subject() {
        return subject;
    }

    /** Sustituye las variables. Una variable sin valor se queda tal cual: es un bug visible. */
    public String render(Map<String, String> variables) {
        String out = body;
        for (Map.Entry<String, String> v : variables.entrySet()) {
            out = out.replace("{" + v.getKey() + "}", v.getValue());
        }
        return out;
    }
}
