package de.nailsbeauty.whatsappgateway.whatsapp;

import de.nailsbeauty.whatsappgateway.config.WhatsAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifiziert die Authentizitaet eingehender Webhook-POSTs via HMAC-SHA256 ueber den Raw-Body.
 *
 * <p>Meta signiert den Body mit dem App-Secret und liefert das Ergebnis im Header
 * {@code X-Hub-Signature-256} im Format {@code sha256=<hex>}. Der Vergleich erfolgt zeitkonstant.
 */
@Slf4j
@Component
public class SignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final byte[] appSecret;

    /**
     * Initialisiert den Verifier mit dem App-Secret aus der Konfiguration.
     *
     * @param properties WhatsApp-Konfiguration mit dem App-Secret
     */
    public SignatureVerifier(WhatsAppProperties properties) {
        this.appSecret = properties.appSecret().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Prueft, ob die mitgelieferte Signatur zum Raw-Body passt.
     *
     * @param rawBody         exakter Roh-Body des Requests
     * @param signatureHeader Wert des Headers {@code X-Hub-Signature-256}, darf {@code null} sein
     * @return {@code true}, wenn die Signatur gueltig ist
     */
    public boolean isValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook ohne gueltigen X-Hub-Signature-256-Header abgelehnt");
            return false;
        }
        var provided = signatureHeader.substring(SIGNATURE_PREFIX.length());
        var expected = computeHmacHex(rawBody == null ? "" : rawBody);

        var providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        var expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        var matches = MessageDigest.isEqual(expectedBytes, providedBytes);
        if (!matches) {
            log.warn("Webhook mit ungueltiger Signatur abgelehnt");
        }
        return matches;
    }

    /** Berechnet den HMAC-SHA256 ueber den Body und gibt ihn als Lowercase-Hex zurueck. */
    private String computeHmacHex(String rawBody) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret, HMAC_ALGORITHM));
            var digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            // Kein Secret loggen.
            throw new IllegalStateException("HMAC-Berechnung fehlgeschlagen", e);
        }
    }
}
