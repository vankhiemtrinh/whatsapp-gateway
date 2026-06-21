package de.nailsbeauty.whatsappgateway.service;

import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import java.net.URI;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Leitet eine eingehende WhatsApp-Nachricht an die Backend-Instanz des jeweiligen
 * Studios weiter.
 *
 * <p>Zielpfad ist {@code POST {backendBaseUrl}/api/public/whatsapp/gateway/inbound}
 * mit einem flachen, <strong>normalisierten</strong> JSON-Body (das Backend muss die
 * rohe Meta-Payload nicht kennen). Authentifiziert wird per Header
 * {@code X-Gateway-Secret} ({@link StudioConfig#getForwardSecret()}, identisch zum
 * {@code app.whatsapp.gateway.inbound-secret} des Backends).
 *
 * <p><strong>Best-Effort:</strong> Fehler werden geloggt, aber nicht erneut
 * versucht. Da das Gateway den Webhook gegenueber Meta bereits mit {@code 200}
 * quittiert hat, loest Meta keine Retransmission aus. Eine fehlgeschlagene
 * Weiterleitung ist fuer den Stempel-Flow unkritisch: Der store-globale Token
 * rotiert ohnehin, der Kunde kann erneut scannen. Secrets werden nie geloggt.
 */
@Slf4j
@Component
public class BackendForwarder {

    /** Header, ueber den das geteilte Geheimnis an das Backend uebergeben wird. */
    public static final String SECRET_HEADER = "X-Gateway-Secret";

    /** Pfad des Inbound-Receivers im Studio-Backend. */
    private static final String INBOUND_PATH = "/api/public/whatsapp/gateway/inbound";

    private final RestClient restClient;

    /**
     * Erstellt den Forwarder.
     *
     * @param backendRestClient {@link RestClient} mit knappen Timeouts fuer
     *                          Backend-Aufrufe
     */
    public BackendForwarder(@Qualifier("backendRestClient") RestClient backendRestClient) {
        this.restClient = backendRestClient;
    }

    /**
     * Leitet eine eingehende Nachricht an das Studio-Backend weiter. Fehlt die
     * Backend-URL oder das Geheimnis, wird ohne Weiterleitung zurueckgekehrt.
     *
     * @param studio    Studio-Konfiguration (liefert Backend-URL, Geheimnis, IDs)
     * @param from      Absender-Rufnummer (WhatsApp wa_id)
     * @param messageId WhatsApp-Message-ID (wamid) fuer die Idempotenz im Backend
     * @param type      Nachrichtentyp (z. B. {@code text})
     * @param text      Nachrichtentext (darf {@code null} sein)
     * @param timestamp Meta-Zeitstempel des Events (Unix-Sekunden) oder {@code null}
     */
    public void forwardMessage(StudioConfig studio, String from, String messageId,
                               String type, String text, String timestamp) {
        var baseUrl = trimTrailingSlash(studio.getBackendBaseUrl());
        if (baseUrl == null || baseUrl.isBlank()) {
            log.debug("Studio {} ohne backendBaseUrl — Forwarding uebersprungen", studio.getStudioId());
            return;
        }
        var secret = studio.getForwardSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Studio {} hat backendBaseUrl, aber kein forwardSecret — Forwarding "
                + "uebersprungen", studio.getStudioId());
            return;
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("studioId", studio.getStudioId());
        payload.put("phoneNumberId", studio.getPhoneNumberId());
        payload.put("from", from);
        payload.put("messageId", messageId);
        payload.put("type", type);
        payload.put("text", text);
        payload.put("timestamp", timestamp);

        try {
            restClient.post()
                .uri(URI.create(baseUrl + INBOUND_PATH))
                .header(SECRET_HEADER, secret)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
            log.info("Inbound-Event an Backend weitergeleitet: studioId={}, messageId={}",
                studio.getStudioId(), messageId);
        } catch (RestClientResponseException e) {
            log.error("Forwarding an Backend (studioId={}) fehlgeschlagen: HTTP {}",
                studio.getStudioId(), e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("Forwarding an Backend (studioId={}) fehlgeschlagen: {}",
                studio.getStudioId(), e.getMessage());
        }
    }

    /**
     * Entfernt einen abschliessenden Slash der Basis-URL, damit die
     * Pfad-Konkatenierung keinen Doppel-Slash erzeugt.
     *
     * @param url Basis-URL (darf {@code null} sein)
     * @return URL ohne Trailing-Slash oder {@code null}
     */
    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
