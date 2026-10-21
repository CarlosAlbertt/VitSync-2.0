package com.vitsync.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;

/**
 * Punto único donde cualquier excepción se convierte en respuesta HTTP.
 *
 * <p>{@code @RestControllerAdvice}: Spring aplica esta clase a TODOS los controladores.
 * Cuando un controlador (o algo por debajo) lanza una excepción, Spring busca aquí el
 * método {@code @ExceptionHandler} más específico para ese tipo y devuelve lo que retorne.
 *
 * <p>Extiende {@link ResponseEntityExceptionHandler}, que ya sabe responder a las
 * excepciones propias de Spring MVC (404, 405, 415, JSON ilegible...). Sobrescribimos
 * algunos de sus métodos para dar a esos errores nuestro formato.
 *
 * <p>Regla de seguridad: el mensaje de una excepción NUNCA llega al cliente (fuga V15 de
 * v1). El cliente solo ve el texto del ErrorCode; el detalle técnico va a los logs.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler
{

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Reglas de negocio: cada BusinessException trae su ErrorCode, que decide estado y texto.
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> onBusiness(BusinessException ex) {
        return respond(ErrorCode.valueOf(ex.getErrorCode().name()));
    }

    // 401: no hay usuario autenticado (o el token no es válido).
    // Llega aquí desde SecurityConfig, que delega en este handler.
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> onUnauthenticated(AuthenticationException ex){
        return respond(ErrorCode.UNAUTHENTICATED);
    }

    // 403: hay usuario, pero no tiene permiso (p. ej. @PreAuthorize("hasRole('ADMIN')")).
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> onAccessDenied(AccessDeniedException ex){
        return respond(ErrorCode.ACCESS_DENIED);
    }

    // Red de seguridad: cualquier excepción no prevista. Se registra entera en el log
    // (con su traza) y al cliente solo le llega un 500 genérico.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> onUnexpected(Exception ex){
        logger.error("Unhandled exception", ex);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    // 400 por @Valid: Spring lanza MethodArgumentNotValidException cuando el cuerpo de la
    // petición no cumple las anotaciones (@NotBlank, @Email...). Se devuelve la lista de
    // campos que fallan, p. ej. "errors": [{"field": "name", "message": "must not be blank"}]
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ProblemDetails.of(ErrorCode.VALIDATION_FAILED);
        pd.setProperty("errors", ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> Map.of("field", e.getField(), "message",
                        String.valueOf(e.getDefaultMessage())))
                .toList());

        // Se delega en el método común de la clase padre para que monte la respuesta.
        return handleExceptionInternal(ex,pd,headers, status, request);
    }

    // Los errores que resuelve el propio framework (404, 405, JSON ilegible...) pasan por aquí:
    // se les añade code y traceId para que todo error tenga la misma forma.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd
                && (pd.getProperties() == null || !pd.getProperties().containsKey("code"))) {
            ProblemDetails.stamp(pd, codeFor(status));
        }
        return response;
    }

    // Traduce un estado HTTP de los errores del framework a nuestro "code".
    private static String codeFor(HttpStatusCode status){
        return switch (status.value()){
            case 400 -> ErrorCode.MALFORMED_REQUEST.name();
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND.name();
            // Resto (405, 415...): el nombre del estado, p. ej. METHOD_NOT_ALLOWED
            default -> HttpStatus.valueOf(status.value()).name();
        };
    }

    // Atajo: ErrorCode -> respuesta HTTP con su estado y el ProblemDetail como cuerpo.
    // Spring pone solo el Content-Type application/problem+json al ver un ProblemDetail.
    private static ResponseEntity<ProblemDetail> respond(ErrorCode code){
        return
                ResponseEntity.status(code.httpStatus()).body(ProblemDetails.of(code));
    }
}
