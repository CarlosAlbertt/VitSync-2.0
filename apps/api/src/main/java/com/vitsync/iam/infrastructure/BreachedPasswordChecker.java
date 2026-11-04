package com.vitsync.iam.infrastructure;

/**
 * ¿Aparece esta contraseña en filtraciones conocidas?
 *
 * <p>Es una interfaz para poder cambiar la implementación sin tocar el servicio: la real
 * consulta HaveIBeenPwned ({@link PwnedPasswordsClient}); en los tests se simula
 * (decisión 6 del plan del Sprint 1).
 *
 * <p>Contrato: <b>nunca lanza</b>. Si el servicio externo no responde, devuelve
 * {@code false} y deja un aviso en el log: el registro no puede depender de un tercero.
 */
public interface BreachedPasswordChecker {

    boolean isBreached(String password);
}
