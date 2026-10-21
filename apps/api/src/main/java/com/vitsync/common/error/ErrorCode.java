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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred");


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
