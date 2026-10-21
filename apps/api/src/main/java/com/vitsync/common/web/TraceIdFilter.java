package com.vitsync.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Da a cada petición un identificador único (traceId) para poder seguirla.
 *
 * <p>El mismo id aparece en tres sitios: la cabecera {@code X-Request-Id} de la respuesta,
 * cada línea de log de esa petición y el campo {@code traceId} de los errores. Si un
 * usuario reporta un fallo con ese id, se encuentra en los logs al momento.
 *
 * <p>Un filtro se ejecuta antes de que la petición llegue a ningún controlador.
 * {@code OncePerRequestFilter} garantiza que corre una sola vez por petición.
 */
@Component   // Spring lo detecta y lo registra en la cadena de filtros automáticamente
@Order(Ordered.HIGHEST_PRECEDENCE) // antes que la seguridad: los 401 también llevan traza
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "traceId";
    // Solo letras, números y guiones, entre 8 y 64 caracteres. Si se aceptara cualquier
    // valor del cliente, podría meter saltos de línea y falsear los logs (log injection).
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Si el cliente (o un proxy) ya trae un id válido, se respeta para no romper la
        // trazabilidad entre sistemas. Si no, se generan 16 caracteres hexadecimales.
        String incoming = request.getHeader(HEADER);
        String traceId = incoming != null && VALID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // MDC: mapa ligado al hilo actual. Lo que se guarda aquí lo puede imprimir el
        // patrón de log (ver logging.pattern.correlation en application.properties).
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try{
            // Deja pasar la petición al resto de filtros y al controlador.
            chain.doFilter(request, response);
        } finally {
            // Imprescindible: los hilos del servidor se reutilizan. Sin esto, la siguiente
            // petición atendida por este hilo heredaría un traceId ajeno.
            MDC.remove(MDC_KEY);
        }
    }
}
