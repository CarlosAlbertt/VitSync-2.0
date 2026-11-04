package com.vitsync.common.crypto;

/**
 * Fallo al cifrar, descifrar o validar las claves.
 *
 * <p>Es no comprobada (RuntimeException) porque quien la recibe casi nunca puede
 * arreglarla: un cifrado manipulado o una clave que falta son errores de sistema, no de
 * negocio. Llega al {@code GlobalExceptionHandler} como un 500 genérico.
 *
 * <p>Regla: el mensaje NUNCA contiene el valor en claro, el cifrado ni la clave. Solo dice
 * qué ha fallado ("versión desconocida", "autenticación fallida"), porque los mensajes de
 * excepción acaban en los logs.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
