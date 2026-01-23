package ewm.event.mapper;

import ewm.event.dto.*;
import ewm.event.model.Event;
import ewm.interaction.api.dto.CategoryDto;
import ewm.interaction.api.dto.UserShortDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {StateActionMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "initiatorId", source = "initiatorId")
    @Mapping(target = "id", ignore = true)
    Event toEvent(NewEventDto newEventDto, Long initiatorId, CategoryDto categoryDto);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "categoryDto")
    @Mapping(target = "initiator", source = "userShortDto")
    EventFullDto toEventFullDto(Event event, CategoryDto categoryDto, UserShortDto userShortDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "state", source = "updateEventUserRequest.stateAction")
    Event toUpdatedEvent(@MappingTarget Event event, UpdateEventUserRequest updateEventUserRequest, CategoryDto categoryDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "state", source = "updateEventAdminRequest.stateAction")
    Event toUpdatedEvent(@MappingTarget Event event, UpdateEventAdminRequest updateEventAdminRequest, CategoryDto categoryDto);

    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "categoryDto")
    EventShortDto toEventShortDto(Event event, CategoryDto categoryDto);
}
