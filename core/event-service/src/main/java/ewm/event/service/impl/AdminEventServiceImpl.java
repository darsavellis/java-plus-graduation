package ewm.event.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.client.StatRestClient;
import ewm.dto.ViewStatsDto;
import ewm.event.dto.AdminEventParam;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventAdminRequest;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.QEvent;
import ewm.event.repository.EventRepository;
import ewm.event.service.AdminEventService;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.interaction.api.client.CategoryClient;
import ewm.interaction.api.client.RequestClient;
import ewm.interaction.api.client.UserClient;
import ewm.interaction.api.dto.CategoryDto;
import ewm.interaction.api.dto.UserDto;
import ewm.interaction.api.dto.UserShortDto;
import ewm.interaction.api.mappers.UserMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminEventServiceImpl implements AdminEventService {
    final EventRepository eventRepository;
    final EventMapper eventMapper;
    final UserMapper userMapper;
    final JPAQueryFactory jpaQueryFactory;
    final StatRestClient statRestClient;
    final CategoryClient categoryClient;
    final UserClient userClient;
    final RequestClient requestClient;

    @Override
    public List<EventFullDto> getAllBy(AdminEventParam eventParam, Pageable pageRequest) {
        BooleanBuilder eventQueryExpression = buildBooleanExpression(eventParam);

        List<Event> events = getEvents(pageRequest, eventQueryExpression);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(eventIds);

        Set<String> uris = events.stream()
            .map(event -> "/events/" + event.getId()).collect(Collectors.toSet());

        LocalDateTime start = events
            .stream()
            .min(Comparator.comparing(Event::getEventDate))
            .orElseThrow(() -> new NotFoundException("Даты не заданы"))
            .getEventDate();

        Map<String, Long> viewMap = statRestClient
            .stats(start, LocalDateTime.now(), uris.stream().toList(), false).stream()
            .collect(Collectors.groupingBy(ViewStatsDto::getUri, Collectors.summingLong(ViewStatsDto::getHits)));

        List<Long> categoryIds = events.stream().map(Event::getCategoryId).toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.findAllByIds(categoryIds).stream()
            .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        List<Long> initiators = events.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserShortDto> userShortDtoMap = userClient.findAllBy(initiators, 0, initiators.size()).stream()
            .collect(Collectors.toMap(UserDto::getId, userMapper::toUserShortDto));

        return events.stream().map(event -> {
            CategoryDto categoryDto = categoryDtoMap.get(event.getCategoryId());
            UserShortDto userShortDto = userShortDtoMap.get(event.getInitiatorId());
            EventFullDto fullDto = eventMapper.toEventFullDto(event, categoryDto, userShortDto);
            fullDto.setViews(viewMap.getOrDefault("/events/" + fullDto.getId(), 0L));
            fullDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(fullDto.getId(), 0L));
            return fullDto;
        }).toList();

    }

    @Override
    @Transactional
    public EventFullDto updateBy(long eventId, UpdateEventAdminRequest updateEventUserRequest) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Событие с с id = " + eventId + " не найдено"));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие" + event.getId() + "уже опубликовано");
        }
        if (event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Нельзя опубликовать отмененное событие");
        }

        CategoryDto categoryDto = categoryClient.findBy(event.getCategoryId());
        UserShortDto userShortDto = userMapper.toUserShortDto(userClient.findBy(event.getInitiatorId()));
        event = eventRepository.save(eventMapper.toUpdatedEvent(event, updateEventUserRequest, categoryDto));
        return eventMapper.toEventFullDto(event, categoryDto, userShortDto);
    }

    @Override
    public boolean existsByCategoryId(long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    List<Event> getEvents(Pageable pageRequest, BooleanBuilder eventQueryExpression) {
        return jpaQueryFactory
            .selectFrom(QEvent.event)
            .where(eventQueryExpression)
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .stream()
            .toList();
    }

    BooleanBuilder buildBooleanExpression(AdminEventParam eventParam) {
        BooleanBuilder eventQueryExpression = new BooleanBuilder();

        QEvent qEvent = QEvent.event;
        Optional.ofNullable(eventParam.getUsers())
            .ifPresent(userIds -> eventQueryExpression.and(qEvent.initiatorId.in(userIds)));
        Optional.ofNullable(eventParam.getStates())
            .ifPresent(userStates -> eventQueryExpression.and(qEvent.state.in(userStates)));
        Optional.ofNullable(eventParam.getCategories())
            .ifPresent(categoryIds -> eventQueryExpression.and(qEvent.categoryId.in(categoryIds)));
        Optional.ofNullable(eventParam.getRangeStart())
            .ifPresent(rangeStart -> eventQueryExpression.and(qEvent.eventDate.after(rangeStart)));
        Optional.ofNullable(eventParam.getRangeEnd())
            .ifPresent(rangeEnd -> eventQueryExpression.and(qEvent.eventDate.before(rangeEnd)));
        return eventQueryExpression;
    }
}
