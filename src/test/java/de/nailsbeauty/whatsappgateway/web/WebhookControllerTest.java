package de.nailsbeauty.whatsappgateway.web;

import de.nailsbeauty.whatsappgateway.config.WhatsAppProperties;
import de.nailsbeauty.whatsappgateway.service.WebhookProcessingService;
import de.nailsbeauty.whatsappgateway.whatsapp.SignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-Tests fuer den {@link WebhookController}: Verifizierung, Signaturpruefung, Routing-Anstoss.
 */
class WebhookControllerTest {

    private static final String VERIFY_TOKEN = "my-verify-token";
    private static final String APP_SECRET = "my-app-secret";

    private MockMvc mockMvc;
    private WebhookProcessingService processingService;

    @BeforeEach
    void setUp() {
        var properties = new WhatsAppProperties(VERIFY_TOKEN, APP_SECRET,
                new WhatsAppProperties.Graph("v21.0", "https://graph.facebook.com"));
        var verifier = new SignatureVerifier(properties);
        processingService = mock(WebhookProcessingService.class);
        var controller = new WebhookController(properties, verifier, processingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void verifyReturnsChallengeForCorrectToken() throws Exception {
        mockMvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", VERIFY_TOKEN)
                        .param("hub.challenge", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("12345"));
    }

    @Test
    void verifyReturns403ForWrongToken() throws Exception {
        mockMvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiveAccepts200AndProcessesValidSignature() throws Exception {
        var body = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
        mockMvc.perform(post("/webhook")
                        .header("X-Hub-Signature-256", "sha256=" + hmacHex(APP_SECRET, body))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
        verify(processingService).process(body);
    }

    @Test
    void receiveReturns401ForInvalidSignature() throws Exception {
        var body = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
        mockMvc.perform(post("/webhook")
                        .header("X-Hub-Signature-256", "sha256=deadbeef")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
        verify(processingService, never()).process(body);
    }

    private static String hmacHex(String secret, String body) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
