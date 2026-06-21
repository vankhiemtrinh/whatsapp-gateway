package de.nailsbeauty.whatsappgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nailsbeauty.whatsappgateway.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Schuetzt die {@code /api/**}-Verwaltungs-Endpoints ueber einen statischen Admin-API-Key.
 *
 * <p>Erwartet den Header {@code X-Admin-Api-Key}. Stimmt er nicht (zeitkonstanter Vergleich)
 * oder fehlt er, wird der Request mit {@code 401} abgewiesen. Der Filter wird nur fuer
 * {@code /api/*} registriert ({@link SecurityConfig}); {@code /webhook} und {@code /actuator}
 * bleiben oeffentlich.
 */
@Slf4j
@RequiredArgsConstructor
public class AdminApiKeyFilter extends OncePerRequestFilter {

    /** Header-Name, ueber den der Admin-API-Key uebergeben wird. */
    public static final String API_KEY_HEADER = "X-Admin-Api-Key";

    private final String expectedApiKey;
    private final ObjectMapper objectMapper;

    /**
     * Prueft den Admin-API-Key und gibt den Request bei Erfolg an die Kette weiter.
     *
     * @param request     der eingehende HTTP-Request
     * @param response    die HTTP-Response
     * @param filterChain die Filter-Kette
     * @throws ServletException bei Servlet-Fehlern
     * @throws IOException      bei I/O-Fehlern
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !constantTimeEquals(provided, expectedApiKey)) {
            log.warn("Abgewiesener Admin-Zugriff auf {} {} (fehlender/ungueltiger API-Key)",
                    request.getMethod(), request.getRequestURI());
            writeUnauthorized(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Schreibt eine einheitliche {@link ErrorResponse} mit Status 401. */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        var body = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Fehlender oder ungueltiger Admin-API-Key",
                request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /** Zeitkonstanter Vergleich, um Timing-Angriffe auf den API-Key zu erschweren. */
    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
