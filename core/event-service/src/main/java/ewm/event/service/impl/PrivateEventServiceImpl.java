package ewm.event.service.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.NewEventDto;
import ewm.event.dto.UpdateEventUserRequest;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.QEvent;
import ewm.event.repository.EventRepository;
import ewm.event.service.PrivateEventService;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.exception.PermissionException;
import ewm.interaction.api.client.CategoryClient;
import ewm.interaction.api.client.RequestClient;
import ewm.interaction.api.client.UserClient;
import ewm.interaction.api.dto.CategoryDto;
import ewm.interaction.api.dto.UserShortDto;
import ewm.interaction.api.mappers.UserMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrivateEventServiceImpl implements PrivateEventService {
    final CategoryClient categoryClient;
    final EventRepository eventRepository;
    final UserMapper userMapper;
    final EventMapper eventMapper;
    final JPAQueryFactory jpaQueryFactory;
    final UserClient userClient;
    final RequestClient requestClient;

    @Override
    public List<EventShortDto> getAllBy(long userId, Pageable pageRequest) {
        BooleanExpression booleanExpression = QEvent.event.initiatorId.eq(userId);

        List<Event> events = getEvents(pageRequest, booleanExpression);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(userId, eventIds);

        Set<String> uris = events.stream()
            .map(event -> "/events/" + event.getId()).collect(Collectors.toSet());

        LocalDateTime start = events
            .stream()
            .min(Comparator.comparing(Event::getEventDate))
            .orElseThrow(() -> new NotFoundException("Даты не заданы"))
            .getEventDate();

        List<Long> categoryIds = events.stream().map(Event::getCategoryId).toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.findAllByIds(categoryIds).stream()
            .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        return events.stream().map(event -> {
            EventShortDto shortDto = eventMapper.toEventShortDto(event, categoryDtoMap.get(event.getCategoryId()));
            shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
            return shortDto;
        }).toList();
    }

    @Override
    @Transactional
    public EventFullDto create(long userId, NewEventDto newEventDto) {
        UserShortDto initiator = userMapper.toUserShortDto(userClient.findBy(userId));
        CategoryDto categoryDto = categoryClient.findBy(newEventDto.getCategory());
        Event event = eventMapper.toEvent(newEventDto, initiator.getId(), categoryDto);
        event = eventRepository.save(event);
        return eventMapper.toEventFullDto(event, categoryDto, initiator);
    }

    @Override
    public EventFullDto getBy(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        CategoryDto categoryDto = categoryClient.findBy(event.getCategoryId());
        UserShortDto userShortDto = userMapper.toUserShortDto(userClient.findBy(userId));
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, categoryDto, userShortDto);

        if (eventFullDto.getInitiator().getId() != userId) {
            throw new PermissionException("Доступ запрещен");
        }
        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto updateBy(long userId, long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Событие с с id = " + eventId + " не найдено"));

        if (event.getInitiatorId() != userId) {
            throw new PermissionException("Доступ запрещен");
        }
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя отменить событие с состоянием");
        }

        CategoryDto categoryDto = categoryClient.findBy(event.getCategoryId());
        UserShortDto userShortDto = userMapper.toUserShortDto(userClient.findBy(userId));
        Event updatedEvent = eventMapper.toUpdatedEvent(event, updateEventUserRequest, categoryDto);
        return eventMapper.toEventFullDto(updatedEvent, categoryDto, userShortDto);
    }

    List<Event> getEvents(Pageable pageRequest, BooleanExpression eventQueryExpression) {
        return jpaQueryFactory
            .selectFrom(QEvent.event)
            .where(eventQueryExpression)
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .stream()
            .toList();
    }
}
