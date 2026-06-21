package de.nailsbeauty.whatsappgateway.web;

import de.nailsbeauty.whatsappgateway.dto.request.CreateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.request.UpdateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.response.StudioConfigResponse;
import de.nailsbeauty.whatsappgateway.service.StudioConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Admin-geschuetzte CRUD-Endpoints fuer die Studio-/WABA-Konfiguration.
 *
 * <p>Alle Routen liegen unter {@code /api} und sind durch den Admin-API-Key abgesichert.
 */
@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioConfigController {

    private final StudioConfigService service;

    /**
     * Liefert alle Studio-Konfigurationen.
     *
     * @return Liste aller Konfigurationen
     */
    @GetMapping
    public List<StudioConfigResponse> list() {
        return service.findAll();
    }

    /**
     * Liefert eine einzelne Studio-Konfiguration.
     *
     * @param studioId fachliche Studio-Kennung
     * @return die Konfiguration
     */
    @GetMapping("/{studioId}")
    public StudioConfigResponse get(@PathVariable String studioId) {
        return service.findByStudioId(studioId);
    }

    /**
     * Legt eine neue Studio-Konfiguration an.
     *
     * @param request validierter Create-Request
     * @return {@code 201} mit Location-Header und der angelegten Konfiguration
     */
    @PostMapping
    public ResponseEntity<StudioConfigResponse> create(@Valid @RequestBody CreateStudioConfigRequest request) {
        var created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/studios/" + created.studioId()))
                .body(created);
    }

    /**
     * Aktualisiert eine bestehende Studio-Konfiguration.
     *
     * @param studioId fachliche Studio-Kennung
     * @param request  validierter Update-Request
     * @return die aktualisierte Konfiguration
     */
    @PutMapping("/{studioId}")
    public StudioConfigResponse update(@PathVariable String studioId,
                                       @Valid @RequestBody UpdateStudioConfigRequest request) {
        return service.update(studioId, request);
    }

    /**
     * Loescht eine Studio-Konfiguration.
     *
     * @param studioId fachliche Studio-Kennung
     */
    @DeleteMapping("/{studioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String studioId) {
        service.delete(studioId);
    }
}
