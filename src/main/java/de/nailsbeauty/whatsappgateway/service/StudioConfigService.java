package de.nailsbeauty.whatsappgateway.service;

import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.dto.request.CreateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.request.UpdateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.response.StudioConfigResponse;
import de.nailsbeauty.whatsappgateway.exception.ConflictException;
import de.nailsbeauty.whatsappgateway.exception.NotFoundException;
import de.nailsbeauty.whatsappgateway.mapper.StudioConfigMapper;
import de.nailsbeauty.whatsappgateway.repository.StudioConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business-Logik fuer die Verwaltung der Studio-/WABA-Konfigurationen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioConfigService {

    private final StudioConfigRepository repository;
    private final StudioConfigMapper mapper;

    /**
     * Liefert alle Studio-Konfigurationen.
     *
     * @return Liste aller Konfigurationen als Response-DTOs
     */
    @Transactional(readOnly = true)
    public List<StudioConfigResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    /**
     * Laedt eine Studio-Konfiguration anhand der fachlichen Studio-Kennung.
     *
     * @param studioId fachliche Studio-Kennung
     * @return Response-DTO der Konfiguration
     * @throws NotFoundException wenn keine Konfiguration existiert
     */
    @Transactional(readOnly = true)
    public StudioConfigResponse findByStudioId(String studioId) {
        return mapper.toResponse(getEntity(studioId));
    }

    /**
     * Legt eine neue Studio-Konfiguration an.
     *
     * @param request Create-Request
     * @return Response-DTO der angelegten Konfiguration
     * @throws ConflictException wenn {@code studioId} oder {@code phoneNumberId} bereits existieren
     */
    @Transactional
    public StudioConfigResponse create(CreateStudioConfigRequest request) {
        if (repository.existsByStudioId(request.studioId())) {
            throw new ConflictException("Studio existiert bereits: " + request.studioId());
        }
        if (repository.existsByPhoneNumberId(request.phoneNumberId())) {
            throw new ConflictException("phoneNumberId bereits belegt: " + request.phoneNumberId());
        }
        var saved = repository.save(mapper.toEntity(request));
        log.info("Studio-Konfiguration angelegt: studioId={}, phoneNumberId={}",
                saved.getStudioId(), saved.getPhoneNumberId());
        return mapper.toResponse(saved);
    }

    /**
     * Aktualisiert eine bestehende Studio-Konfiguration.
     *
     * @param studioId fachliche Studio-Kennung
     * @param request  Update-Request ({@code null}-Felder bleiben unveraendert)
     * @return Response-DTO der aktualisierten Konfiguration
     * @throws NotFoundException wenn keine Konfiguration existiert
     */
    @Transactional
    public StudioConfigResponse update(String studioId, UpdateStudioConfigRequest request) {
        var entity = getEntity(studioId);
        mapper.updateEntity(request, entity);
        log.info("Studio-Konfiguration aktualisiert: studioId={}", studioId);
        return mapper.toResponse(entity);
    }

    /**
     * Loescht eine Studio-Konfiguration.
     *
     * @param studioId fachliche Studio-Kennung
     * @throws NotFoundException wenn keine Konfiguration existiert
     */
    @Transactional
    public void delete(String studioId) {
        var entity = getEntity(studioId);
        repository.delete(entity);
        log.info("Studio-Konfiguration geloescht: studioId={}", studioId);
    }

    /**
     * Laedt die Entity einer Studio-Konfiguration (intern, fuer andere Services).
     *
     * @param studioId fachliche Studio-Kennung
     * @return die Entity
     * @throws NotFoundException wenn keine Konfiguration existiert
     */
    @Transactional(readOnly = true)
    public StudioConfig getEntity(String studioId) {
        return repository.findByStudioId(studioId)
                .orElseThrow(() -> new NotFoundException("Studio nicht gefunden: " + studioId));
    }
}
