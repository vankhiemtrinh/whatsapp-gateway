package de.nailsbeauty.whatsappgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nailsbeauty.whatsappgateway.domain.EventType;
import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.repository.StudioConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Verarbeitet eingehende Webhook-Payloads asynchron.
 *
 * <p>Parst die Struktur {@code entry[].changes[].value} null-sicher, loest ueber
 * {@code metadata.phone_number_id} das Studio auf und verbucht Nachrichten/Status-Updates
 * idempotent. Fehler einzelner Events brechen den Batch nicht ab.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessingService {

    private final StudioConfigRepository studioConfigRepository;
    private final InboundEventService inboundEventService;
    private final BackendForwarder backendForwarder;
    private final ObjectMapper objectMapper;

    /**
     * Verarbeitet den Roh-Body eines Webhook-POSTs asynchron.
     *
     * <p>Wird vom Controller nach erfolgreicher Signaturpruefung aufgerufen, nachdem dieser bereits
     * mit {@code 200} geantwortet hat.
     *
     * @param rawBody exakter Roh-Body des Webhook-Requests
     */
    @Async("webhookExecutor")
    public void process(String rawBody) {
        try {
            var root = objectMapper.readTree(rawBody);
            for (var entry : root.path("entry")) {
                for (var change : entry.path("changes")) {
                    processChange(change.path("value"));
                }
            }
        } catch (Exception e) {
            log.error("Fehler beim Parsen eines Webhook-Payloads: {}", e.getMessage());
        }
    }

    /** Verarbeitet einen einzelnen {@code changes[].value}-Block. */
    private void processChange(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return;
        }
        var phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
        if (phoneNumberId == null) {
            log.warn("Webhook-Event ohne phone_number_id — ignoriert");
            return;
        }

        Optional<StudioConfig> studio = studioConfigRepository.findByPhoneNumberId(phoneNumberId);
        if (studio.isEmpty()) {
            log.warn("Unbekannte phone_number_id={} — Event wird ignoriert", phoneNumberId);
            return;
        }

        processMessages(value.path("messages"), studio.get());
        processStatuses(value.path("statuses"), studio.get());
    }

    /**
     * Verbucht eingehende Nachrichten idempotent und leitet neue Text-Nachrichten
     * an das Studio-Backend weiter.
     */
    private void processMessages(JsonNode messages, StudioConfig studio) {
        if (!messages.isArray()) {
            return;
        }
        for (var message : messages) {
            var metaId = message.path("id").asText(null);
            var from = message.path("from").asText("?");
            var type = message.path("type").asText("?");
            var isNew = inboundEventService.recordIfNew(
                    metaId, studio.getPhoneNumberId(), EventType.MESSAGE, safeJson(message));
            if (!isNew) {
                continue;
            }
            log.info("Nachricht empfangen: studioId={}, from={}, type={}, messageId={}",
                    studio.getStudioId(), from, type, metaId);
            // Nur Text-Nachrichten sind fuer das Stempel-System relevant — andere
            // Typen (Bilder/Audio/...) werden verbucht, aber nicht weitergeleitet.
            if ("text".equals(type)) {
                var text = message.path("text").path("body").asText(null);
                var timestamp = message.path("timestamp").asText(null);
                backendForwarder.forwardMessage(studio, from, metaId, type, text, timestamp);
            }
        }
    }

    /** Verbucht Status-Updates (sent/delivered/read/failed) idempotent. */
    private void processStatuses(JsonNode statuses, StudioConfig studio) {
        if (!statuses.isArray()) {
            return;
        }
        for (var status : statuses) {
            var metaId = status.path("id").asText(null);
            var statusValue = status.path("status").asText("?");
            var isNew = inboundEventService.recordIfNew(
                    metaId, studio.getPhoneNumberId(), EventType.STATUS, safeJson(status));
            if (isNew) {
                log.info("Status-Update: studioId={}, status={}, messageId={}",
                        studio.getStudioId(), statusValue, metaId);
            }
        }
    }

    /** Serialisiert einen Teilbaum als JSON-String; bei Fehler {@code null}. */
    private String safeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Konnte Event-Payload nicht serialisieren: {}", e.getMessage());
            return null;
        }
    }
}
