package com.vitsync.common.error;

/**
 * Excepción para reglas de negocio incumplidas (p. ej. "el hueco ya está ocupado").
 *
 * <p>Se lanza desde los servicios con un {@link ErrorCode}, y el
 * {@code GlobalExceptionHandler} la convierte en la respuesta HTTP adecuada.
 * Lo que ve el cliente sale del ErrorCode; el mensaje y la causa se quedan en los logs.
 *
 * <p>Extiende RuntimeException (no comprobada) para no obligar a declarar
 * {@code throws} en cada método por el que pasa.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    // Caso normal: solo se indica qué regla se ha incumplido.
    public BusinessException(ErrorCode errorCode){
        super(errorCode.name());   // mensaje interno = nombre del código, útil en logs
        this.errorCode = errorCode;
    }

    // Cuando la regla se detecta a partir de otra excepción (p. ej. la violación de un
    // índice único en la BD), se guarda como causa para no perder la traza original.
    public BusinessException(ErrorCode errorCode, Throwable  cause){
        super(errorCode.name(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode(){return errorCode; }
}
