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

        var studioId = studio.get().getStudioId();
        processMessages(value.path("messages"), phoneNumberId, studioId);
        processStatuses(value.path("statuses"), phoneNumberId, studioId);
    }

    /** Verbucht eingehende Nachrichten idempotent. */
    private void processMessages(JsonNode messages, String phoneNumberId, String studioId) {
        if (!messages.isArray()) {
            return;
        }
        for (var message : messages) {
            var metaId = message.path("id").asText(null);
            var from = message.path("from").asText("?");
            var type = message.path("type").asText("?");
            var isNew = inboundEventService.recordIfNew(
                    metaId, phoneNumberId, EventType.MESSAGE, safeJson(message));
            if (isNew) {
                log.info("Nachricht empfangen: studioId={}, from={}, type={}, messageId={}",
                        studioId, from, type, metaId);
            }
        }
    }

    /** Verbucht Status-Updates (sent/delivered/read/failed) idempotent. */
    private void processStatuses(JsonNode statuses, String phoneNumberId, String studioId) {
        if (!statuses.isArray()) {
            return;
        }
        for (var status : statuses) {
            var metaId = status.path("id").asText(null);
            var statusValue = status.path("status").asText("?");
            var isNew = inboundEventService.recordIfNew(
                    metaId, phoneNumberId, EventType.STATUS, safeJson(status));
            if (isNew) {
                log.info("Status-Update: studioId={}, status={}, messageId={}",
                        studioId, statusValue, metaId);
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
