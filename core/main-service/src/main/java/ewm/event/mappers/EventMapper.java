package ewm.event.mappers;

import ewm.dto.CategoryDto;
import ewm.event.dto.*;
import ewm.event.model.Event;
import ewm.user.mappers.UserMapper;
import ewm.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {UserMapper.class, StateActionMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "id", ignore = true)
    Event toEvent(NewEventDto newEventDto, User initiator, CategoryDto categoryDto);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "categoryDto")
    EventFullDto toEventFullDto(Event event, CategoryDto categoryDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "state", source = "updateEventUserRequest.stateAction")
    Event toUpdatedEvent(@MappingTarget Event event, UpdateEventUserRequest updateEventUserRequest, CategoryDto categoryDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "categoryId", source = "categoryDto.id")
    @Mapping(target = "state", source = "updateEventAdminRequest.stateAction")
    Event toUpdatedEvent(@MappingTarget Event event, UpdateEventAdminRequest updateEventAdminRequest, CategoryDto categoryDto);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "category", source = "categoryDto")
    EventShortDto toEventShortDto(Event event, CategoryDto categoryDto);
}
