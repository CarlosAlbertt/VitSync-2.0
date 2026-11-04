package com.vitsync.common.error;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores de la API.
 *
 * <p>Cada constante es un error que el cliente puede recibir. Su nombre (p. ej.
 * {@code VALIDATION_FAILED}) viaja en el campo {@code code} de la respuesta y forma parte
 * del contrato: el frontend decide qué hacer mirando el {@code code}, nunca el texto.
 *
 * <p>Aquí solo van los errores transversales. Los de cada funcionalidad
 * (p. ej. {@code APPOINTMENT_SLOT_TAKEN}) se añaden cuando se construye esa funcionalidad.
 */
public enum ErrorCode {

    // En un enum las constantes van SIEMPRE primero. Cada una llama al constructor de abajo
    // con: estado HTTP, título corto y detalle legible para el usuario.
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request", "The request could not be understood"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required", "A valid access token is required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied", "You are not allowed to perform this action"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found", "The requested resource does not exist"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred"),

    // ----- Identidad (iam) -----
    // 409 sin decir si choca el email o el NIF: decir cuál permitiría comprobar, campo a
    // campo, si una persona concreta tiene cuenta (enumeración).
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Account already exists",
            "An account with these details already exists"),
    PASSWORD_BREACHED(HttpStatus.BAD_REQUEST, "Password breached",
            "This password has appeared in a data breach; choose a different one"),
    // Mismo error para código incorrecto, caducado, ya usado o email desconocido: distinguirlos
    // diría qué cuentas existen.
    INVALID_OR_EXPIRED_CODE(HttpStatus.BAD_REQUEST, "Invalid code",
            "The code is invalid or has expired"),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts",
            "Too many failed attempts; request a new code"),
    // Mismo error si el email no existe o la contraseña es incorrecta (ADR-0004).
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials",
            "The email or password is incorrect"),
    // Solo se devuelven tras comprobar la contraseña: ya no sirven para enumerar cuentas.
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Account not verified",
            "Verify your email before logging in; we have sent you a code"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account disabled",
            "This account cannot log in");


    // final: cada constante fija sus valores al crearse y ya no cambian.
    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

    // Constructor de enum: siempre privado (aunque no se escriba). Solo lo usan las
    // constantes de arriba; nadie puede hacer new ErrorCode(...).
    ErrorCode(HttpStatus httpStatus, String title, String detail){
        this.httpStatus = httpStatus;
        this.title = title;
        this.detail = detail;
    }

    // Accesores al estilo record (httpStatus() en vez de getHttpStatus()).
    public HttpStatus httpStatus(){return httpStatus;}
    public String title(){ return title;}
    public String detail() {return detail; }


}
