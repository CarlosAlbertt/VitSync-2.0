package com.vitsync.iam.application;

import java.time.LocalDate;

/**
 * Datos de entrada del caso de uso "registrar paciente".
 *
 * <p>Es un tipo propio de la capa {@code application}, distinto del DTO HTTP: si mañana el
 * registro llega también por otra vía (una importación, un admin), el servicio no cambia.
 */
public record RegisterPatientCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String nif,
        LocalDate birthDate,
        String phone) {

    // Sin la contraseña ni datos personales: el toString() de un record los imprimiría todos.
    @Override
    public String toString() {
        return "RegisterPatientCommand[...]";
    }
}
