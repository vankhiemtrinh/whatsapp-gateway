package de.nailsbeauty.whatsappgateway.whatsapp;

import de.nailsbeauty.whatsappgateway.config.WhatsAppProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests fuer die HMAC-SHA256-Signaturpruefung des Webhooks.
 */
class SignatureVerifierTest {

    private static final String APP_SECRET = "test-app-secret";
    private static final String BODY = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

    private final SignatureVerifier verifier = new SignatureVerifier(
            new WhatsAppProperties("verify", APP_SECRET,
                    new WhatsAppProperties.Graph("v21.0", "https://graph.facebook.com")));

    @Test
    void acceptsValidSignature() throws Exception {
        var header = "sha256=" + hmacHex(APP_SECRET, BODY);
        assertThat(verifier.isValid(BODY, header)).isTrue();
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        var header = "sha256=" + hmacHex(APP_SECRET, BODY);
        assertThat(verifier.isValid(BODY + "tampered", header)).isFalse();
    }

    @Test
    void rejectsWrongSecret() throws Exception {
        var header = "sha256=" + hmacHex("other-secret", BODY);
        assertThat(verifier.isValid(BODY, header)).isFalse();
    }

    @Test
    void rejectsMissingHeader() {
        assertThat(verifier.isValid(BODY, null)).isFalse();
    }

    @Test
    void rejectsHeaderWithoutPrefix() throws Exception {
        assertThat(verifier.isValid(BODY, hmacHex(APP_SECRET, BODY))).isFalse();
    }

    private static String hmacHex(String secret, String body) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
