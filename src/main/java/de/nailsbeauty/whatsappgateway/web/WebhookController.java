package de.nailsbeauty.whatsappgateway.web;

import de.nailsbeauty.whatsappgateway.config.WhatsAppProperties;
import de.nailsbeauty.whatsappgateway.service.WebhookProcessingService;
import de.nailsbeauty.whatsappgateway.whatsapp.SignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Oeffentlicher Webhook-Endpunkt fuer die WhatsApp Cloud API.
 *
 * <p>Eine zentrale Callback-URL fuer alle Studios; das Routing erfolgt anhand der
 * {@code phone_number_id} im Payload. POSTs werden via HMAC-Signatur abgesichert und
 * asynchron verarbeitet.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WhatsAppProperties properties;
    private final SignatureVerifier signatureVerifier;
    private final WebhookProcessingService processingService;

    /**
     * Webhook-Verifizierung durch Meta.
     *
     * <p>Gibt bei korrektem {@code hub.verify_token} den {@code hub.challenge} als Klartext zurueck.
     *
     * @param mode        Wert von {@code hub.mode} (erwartet {@code subscribe})
     * @param verifyToken Wert von {@code hub.verify_token}
     * @param challenge   Wert von {@code hub.challenge}, der zurueckgegeben werden muss
     * @return {@code 200} mit dem Challenge bei Erfolg, sonst {@code 403}
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && properties.verifyToken().equals(verifyToken)) {
            log.info("Webhook-Verifizierung erfolgreich");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Webhook-Verifizierung fehlgeschlagen (mode={})", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Empfaengt Webhook-Events (Nachrichten und Status-Updates).
     *
     * <p>Prueft die HMAC-Signatur ueber den Raw-Body, antwortet sofort mit {@code 200} und stoesst
     * die Verarbeitung asynchron an, um Meta-Retries (Duplikate) zu vermeiden.
     *
     * @param signature Wert des Headers {@code X-Hub-Signature-256}
     * @param rawBody   exakter Roh-Body des Requests
     * @return {@code 200} bei gueltiger Signatur, sonst {@code 401}
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) String rawBody) {
        var body = rawBody == null ? "" : rawBody;
        if (!signatureVerifier.isValid(body, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        processingService.process(body);
        return ResponseEntity.ok().build();
    }
}
