package ewm.event.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.client.StatRestClientImpl;
import ewm.dto.ViewStatsDto;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.PublicEventParam;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.QEvent;
import ewm.event.repository.EventRepository;
import ewm.event.service.PublicEventService;
import ewm.exception.NotFoundException;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicEventServiceImpl implements PublicEventService {
    final EventRepository eventRepository;
    final RequestClient requestClient;
    final StatRestClientImpl statRestClient;
    final EventMapper eventMapper;
    final UserMapper userMapper;
    final JPAQueryFactory jpaQueryFactory;
    final CategoryClient categoryClient;
    final UserClient userClient;

    @Override
    public List<EventShortDto> getAllBy(PublicEventParam eventParam, Pageable pageRequest) {
        BooleanBuilder eventQueryExpression = buildExpression(eventParam);

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

        return events.stream().map(event -> {
            EventShortDto shortDto = eventMapper.toEventShortDto(event, categoryDtoMap.get(event.getCategoryId()));
            shortDto.setViews(viewMap.getOrDefault("/events/" + shortDto.getId(), 0L));
            shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
            return shortDto;
        }).toList();
    }

    @Override
    public EventFullDto getBy(long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Мероприятие с Id =" + eventId + " не найдено"));
        CategoryDto categoryDto = categoryClient.findBy(event.getCategoryId());
        UserShortDto userShortDto = userMapper.toUserShortDto(userClient.findBy(event.getInitiatorId()));
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, categoryDto, userShortDto);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие id = " + eventId + " не опубликовано");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusYears(10);

        statRestClient.stats(start, now, List.of("/events/" + eventId), true)
            .forEach(viewStatsDto -> eventFullDto.setViews(viewStatsDto.getHits()));
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(Collections.singletonList(eventId));
        long confirmedRequests = confirmedRequestsMap.getOrDefault(eventId, 0L);
        eventFullDto.setConfirmedRequests(confirmedRequests);
        return eventFullDto;
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

    BooleanBuilder buildExpression(PublicEventParam eventParam) {
        BooleanBuilder eventQueryExpression = new BooleanBuilder();

        eventQueryExpression.and(QEvent.event.state.eq(EventState.PUBLISHED));
        Optional.ofNullable(eventParam.getRangeStart())
            .ifPresent(rangeStart -> eventQueryExpression.and(QEvent.event.eventDate.after(rangeStart)));
        Optional.ofNullable(eventParam.getRangeEnd())
            .ifPresent(rangeEnd -> eventQueryExpression.and(QEvent.event.eventDate.before(eventParam.getRangeEnd())));
        Optional.ofNullable(eventParam.getPaid())
            .ifPresent(paid -> eventQueryExpression.and(QEvent.event.paid.eq(paid)));
        Optional.ofNullable(eventParam.getCategories())
            .filter(category -> !category.isEmpty())
            .ifPresent(category -> eventQueryExpression.and(QEvent.event.categoryId.in(category)));
        Optional.ofNullable(eventParam.getText())
            .filter(text -> !text.isEmpty()).ifPresent(text -> {
                eventQueryExpression.and(QEvent.event.annotation.containsIgnoreCase(text));
                eventQueryExpression.or(QEvent.event.description.containsIgnoreCase(text));
            });
        return eventQueryExpression;
    }
}
