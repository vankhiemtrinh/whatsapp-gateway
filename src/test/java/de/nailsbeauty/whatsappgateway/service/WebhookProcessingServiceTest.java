package de.nailsbeauty.whatsappgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nailsbeauty.whatsappgateway.domain.EventType;
import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.repository.StudioConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests fuer das Routing und die Verarbeitung im {@link WebhookProcessingService}.
 */
@ExtendWith(MockitoExtension.class)
class WebhookProcessingServiceTest {

    @Mock
    private StudioConfigRepository studioConfigRepository;

    @Mock
    private InboundEventService inboundEventService;

    @Mock
    private BackendForwarder backendForwarder;

    private WebhookProcessingService service;

    @BeforeEach
    void setUp() {
        service = new WebhookProcessingService(
                studioConfigRepository, inboundEventService, backendForwarder, new ObjectMapper());
    }

    @Test
    void routesIncomingMessageToStudioAndRecordsEvent() {
        var studio = new StudioConfig();
        studio.setStudioId("studio-1");
        studio.setPhoneNumberId("PN-123");
        when(studioConfigRepository.findByPhoneNumberId("PN-123")).thenReturn(Optional.of(studio));

        service.process("""
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-123" },
                    "messages": [{ "id": "wamid.ABC", "from": "49170", "type": "text" }]
                  }
                }]
              }]
            }
            """);

        verify(inboundEventService)
                .recordIfNew(eq("wamid.ABC"), eq("PN-123"), eq(EventType.MESSAGE), any());
    }

    @Test
    void forwardsNewTextMessageToBackend() {
        var studio = new StudioConfig();
        studio.setStudioId("studio-1");
        studio.setPhoneNumberId("PN-123");
        when(studioConfigRepository.findByPhoneNumberId("PN-123")).thenReturn(Optional.of(studio));
        when(inboundEventService.recordIfNew(eq("wamid.ABC"), eq("PN-123"),
                eq(EventType.MESSAGE), any())).thenReturn(true);

        service.process("""
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-123" },
                    "messages": [{ "id": "wamid.ABC", "from": "49170", "type": "text",
                                   "timestamp": "1718900000",
                                   "text": { "body": "STAMP-XYZ" } }]
                  }
                }]
              }]
            }
            """);

        verify(backendForwarder).forwardMessage(
                eq(studio), eq("49170"), eq("wamid.ABC"), eq("text"), eq("STAMP-XYZ"), eq("1718900000"));
    }

    @Test
    void doesNotForwardNonTextMessage() {
        var studio = new StudioConfig();
        studio.setStudioId("studio-1");
        studio.setPhoneNumberId("PN-123");
        when(studioConfigRepository.findByPhoneNumberId("PN-123")).thenReturn(Optional.of(studio));
        when(inboundEventService.recordIfNew(eq("wamid.IMG"), eq("PN-123"),
                eq(EventType.MESSAGE), any())).thenReturn(true);

        service.process("""
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-123" },
                    "messages": [{ "id": "wamid.IMG", "from": "49170", "type": "image" }]
                  }
                }]
              }]
            }
            """);

        verify(backendForwarder, never()).forwardMessage(any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotForwardDuplicateMessage() {
        var studio = new StudioConfig();
        studio.setStudioId("studio-1");
        studio.setPhoneNumberId("PN-123");
        when(studioConfigRepository.findByPhoneNumberId("PN-123")).thenReturn(Optional.of(studio));
        when(inboundEventService.recordIfNew(eq("wamid.DUP"), eq("PN-123"),
                eq(EventType.MESSAGE), any())).thenReturn(false);

        service.process("""
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-123" },
                    "messages": [{ "id": "wamid.DUP", "from": "49170", "type": "text",
                                   "text": { "body": "STAMP-XYZ" } }]
                  }
                }]
              }]
            }
            """);

        verify(backendForwarder, never()).forwardMessage(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordsStatusUpdate() {
        var studio = new StudioConfig();
        studio.setStudioId("studio-1");
        studio.setPhoneNumberId("PN-123");
        when(studioConfigRepository.findByPhoneNumberId("PN-123")).thenReturn(Optional.of(studio));

        service.process("""
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-123" },
                    "statuses": [{ "id": "wamid.STAT", "status": "delivered" }]
                  }
                }]
              }]
            }
            """);

        verify(inboundEventService)
                .recordIfNew(eq("wamid.STAT"), eq("PN-123"), eq(EventType.STATUS), any());
    }

    @Test
    void ignoresUnknownPhoneNumberId() {
        when(studioConfigRepository.findByPhoneNumberId("PN-UNKNOWN")).thenReturn(Optional.empty());

        service.process("""
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "metadata": { "phone_number_id": "PN-UNKNOWN" },
                    "messages": [{ "id": "wamid.X", "from": "49170", "type": "text" }]
                  }
                }]
              }]
            }
            """);

        verify(inboundEventService, never()).recordIfNew(any(), any(), any(), any());
    }

    @Test
    void handlesMalformedPayloadGracefully() {
        service.process("not-json");
        verify(inboundEventService, never()).recordIfNew(any(), any(), any(), any());
    }
}
