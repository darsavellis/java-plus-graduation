package ewm.request.mapper;

import ewm.request.dto.ParticipationRequestDto;
import ewm.request.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RequestMapper {
    @Mapping(target = "requester", source = "participationRequest.requesterId")
    @Mapping(target = "event", source = "participationRequest.eventId")
    ParticipationRequestDto toParticipantRequestDto(ParticipationRequest participationRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "requesterId", source = "requesterId")
    ParticipationRequest toParticipationRequest(Long eventId, Long requesterId);
}
