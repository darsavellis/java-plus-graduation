package ewm.request.mapper;

import ewm.event.model.Event;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {Event.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RequestMapper {
    @Mapping(target = "requester", source = "participationRequest.requesterId")
    @Mapping(target = "event", source = "participationRequest.event.id")
    ParticipationRequestDto toParticipantRequestDto(ParticipationRequest participationRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "requesterId", source = "requesterId")
    ParticipationRequest toParticipationRequest(Event event, Long requesterId);
}
