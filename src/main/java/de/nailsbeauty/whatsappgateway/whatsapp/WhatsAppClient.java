package de.nailsbeauty.whatsappgateway.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import de.nailsbeauty.whatsappgateway.config.WhatsAppProperties;
import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.exception.WhatsAppApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schmaler HTTP-Client fuer ausgehende Aufrufe an die Meta Graph API (Cloud API).
 *
 * <p>Baut pro Aufruf die studio-spezifische URL und setzt das Bearer-Token des jeweiligen Studios.
 * Tokens werden nie geloggt.
 */
@Slf4j
@Component
public class WhatsAppClient {

    private final RestClient restClient;
    private final WhatsAppProperties properties;

    /**
     * Erstellt den Client.
     *
     * @param whatsAppRestClient vorkonfigurierter {@link RestClient}
     * @param properties         Graph-API-Konfiguration (Version, Basis-URL)
     */
    public WhatsAppClient(RestClient whatsAppRestClient, WhatsAppProperties properties) {
        this.restClient = whatsAppRestClient;
        this.properties = properties;
    }

    /**
     * Sendet eine Textnachricht ueber die Cloud API.
     *
     * @param studio Studio-Konfiguration (liefert {@code phoneNumberId} und Token)
     * @param to     Empfaenger-Telefonnummer
     * @param text   Nachrichtentext
     * @return die von Meta vergebene Message-ID
     * @throws WhatsAppApiException wenn der Graph-API-Aufruf fehlschlaegt
     */
    public String sendText(StudioConfig studio, String to, String text) {
        var payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", to,
                "type", "text",
                "text", Map.of("preview_url", false, "body", text));
        var response = post(studio, "/messages", payload);
        return extractMessageId(response);
    }

    /**
     * Sendet eine Template-Nachricht ueber die Cloud API.
     *
     * <p>Sind {@code bodyParams} gesetzt, werden sie als {@code components[0]} vom Typ
     * {@code body} mit positionsbezogenen Text-Parametern an die Graph API uebergeben
     * (Reihenfolge muss zur Template-Definition passen). Andernfalls wird das Template
     * ohne Parameter gesendet.
     *
     * @param studio       Studio-Konfiguration
     * @param to           Empfaenger-Telefonnummer
     * @param templateName Name des genehmigten Templates
     * @param languageCode Sprachcode des Templates
     * @param bodyParams   positionsbezogene Body-Parameter ({@code null}/leer = ohne Parameter)
     * @return die von Meta vergebene Message-ID
     * @throws WhatsAppApiException wenn der Graph-API-Aufruf fehlschlaegt
     */
    public String sendTemplate(StudioConfig studio, String to, String templateName,
                               String languageCode, List<String> bodyParams) {
        var template = new LinkedHashMap<String, Object>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode));
        if (bodyParams != null && !bodyParams.isEmpty()) {
            var parameters = new ArrayList<Map<String, Object>>();
            for (var param : bodyParams) {
                parameters.add(Map.of("type", "text", "text", param != null ? param : ""));
            }
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", template);
        var response = post(studio, "/messages", payload);
        return extractMessageId(response);
    }

    /**
     * Abonniert die App auf die WABA des Studios ({@code POST /{waba-id}/subscribed_apps}).
     *
     * @param studio Studio-Konfiguration (liefert {@code wabaId} und Token)
     * @return {@code true}, wenn Meta die Subscription bestaetigt
     * @throws WhatsAppApiException wenn der Graph-API-Aufruf fehlschlaegt
     */
    public boolean subscribeApp(StudioConfig studio) {
        var url = baseGraphUrl() + "/" + studio.getWabaId() + "/subscribed_apps";
        try {
            var response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, bearer(studio))
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.path("success").asBoolean(false);
        } catch (RestClientResponseException e) {
            log.error("subscribed_apps fuer WABA {} fehlgeschlagen: HTTP {}",
                    studio.getWabaId(), e.getStatusCode().value());
            throw new WhatsAppApiException(
                    "subscribed_apps fehlgeschlagen (HTTP %d)".formatted(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.error("subscribed_apps fuer WABA {} fehlgeschlagen: {}", studio.getWabaId(), e.getMessage());
            throw new WhatsAppApiException("subscribed_apps fehlgeschlagen", e);
        }
    }

    /** Fuehrt einen POST gegen den message-spezifischen Endpunkt des Studios aus. */
    private JsonNode post(StudioConfig studio, String pathSuffix, Map<String, Object> payload) {
        var url = baseGraphUrl() + "/" + studio.getPhoneNumberId() + pathSuffix;
        try {
            return restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, bearer(studio))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.error("Graph-API-Aufruf fuer Studio {} fehlgeschlagen: HTTP {} — {}",
                    studio.getStudioId(), e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new WhatsAppApiException(
                    "WhatsApp-Versand fehlgeschlagen (HTTP %d)".formatted(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            log.error("Graph-API-Aufruf fuer Studio {} fehlgeschlagen: {}", studio.getStudioId(), e.getMessage());
            throw new WhatsAppApiException("WhatsApp-Versand fehlgeschlagen", e);
        }
    }

    /** Liest die erste Message-ID aus der Graph-API-Antwort null-sicher aus. */
    private String extractMessageId(JsonNode response) {
        if (response == null) {
            throw new WhatsAppApiException("Leere Antwort der Graph API");
        }
        var messages = response.path("messages");
        if (!messages.isArray() || messages.isEmpty()) {
            throw new WhatsAppApiException("Graph-API-Antwort enthaelt keine Message-ID");
        }
        return messages.get(0).path("id").asText(null);
    }

    /** {@code https://graph.facebook.com/v21.0} (ohne Trailing-Slash). */
    private String baseGraphUrl() {
        var base = properties.graph().baseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + properties.graph().apiVersion();
    }

    /** Baut den Bearer-Header-Wert aus dem (entschluesselten) Studio-Token. */
    private String bearer(StudioConfig studio) {
        return "Bearer " + studio.getAccessToken();
    }
}
