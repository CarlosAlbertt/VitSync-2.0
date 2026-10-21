package com.vitsync.common.error;

import com.vitsync.common.web.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Locale;

/**
 * Fábrica de respuestas de error con formato RFC 9457 ({@code application/problem+json}).
 *
 * <p>Un ProblemDetail es el JSON estándar de error: type, title, status, detail, instance.
 * Aquí se le añaden dos campos propios de VitSync:
 * <ul>
 *   <li>{@code code}: el nombre del ErrorCode, para que el front decida por él.
 *   <li>{@code traceId}: el id de la petición, para encontrarla en los logs.
 * </ul>
 *
 * <p>Clase final con constructor privado: solo tiene métodos estáticos, no se instancia.
 * Es package-private (sin public) porque solo la usa el paquete {@code common.error}.
 */
final class ProblemDetails {

    // Cada error tiene una URI que lo identifica, p. ej. .../errors/resource-not-found
    private static final String TYPE_BASE = "https://api.vitsync.es/errors/";

    private ProblemDetails() {}

    // Construye un ProblemDetail completo a partir de un ErrorCode de nuestro catálogo.
    static ProblemDetail of (ErrorCode code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.httpStatus(),
                code.detail());
        pd.setTitle(code.title());
        return stamp(pd, code.name());

    }
            // Añade type, code y traceId a un ProblemDetail que ya existe. Se separa de of()
            // porque también se usa con los ProblemDetail que crea el propio Spring
            // (404, 405, JSON ilegible...), para que TODOS los errores tengan la misma forma.
            static ProblemDetail stamp(ProblemDetail pd, String code){
                // RESOURCE_NOT_FOUND -> resource-not-found
                pd.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_','-')));

                pd.setProperty("code", code);
                // MDC: mapa de contexto del log, por hilo. TraceIdFilter guardó ahí el id.
                pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
                return pd;
    }

}
