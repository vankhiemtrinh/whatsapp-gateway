package de.nailsbeauty.whatsappgateway.mapper;

import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.dto.request.CreateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.request.UpdateStudioConfigRequest;
import de.nailsbeauty.whatsappgateway.dto.response.StudioConfigResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct-Mapper zwischen {@link StudioConfig} und ihren DTOs.
 */
@Mapper(componentModel = "spring")
public interface StudioConfigMapper {

    /**
     * Mappt eine Entity auf das Response-DTO (ohne Access-Token).
     *
     * @param entity die Entity, niemals {@code null}
     * @return Response-DTO
     */
    StudioConfigResponse toResponse(StudioConfig entity);

    /**
     * Mappt eine Liste von Entities auf Response-DTOs.
     *
     * @param entities die Entities
     * @return Liste der Response-DTOs
     */
    List<StudioConfigResponse> toResponseList(List<StudioConfig> entities);

    /**
     * Erstellt aus einem Create-Request eine neue Entity.
     *
     * @param request der Create-Request
     * @return neue, noch nicht persistierte Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StudioConfig toEntity(CreateStudioConfigRequest request);

    /**
     * Aktualisiert eine bestehende Entity in-place aus einem Update-Request.
     *
     * <p>{@code null}-Felder des Requests lassen den bestehenden Wert unveraendert; die fachlichen
     * Schluessel {@code studioId}/{@code phoneNumberId} sowie technische Felder werden nie gemappt.
     *
     * @param request der Update-Request
     * @param entity  die zu aktualisierende Entity (Zielobjekt)
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studioId", ignore = true)
    @Mapping(target = "phoneNumberId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateStudioConfigRequest request, @MappingTarget StudioConfig entity);
}
