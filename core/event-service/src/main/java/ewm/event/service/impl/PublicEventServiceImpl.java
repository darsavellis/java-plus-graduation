package ewm.event.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.dto.PublicEventParam;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.QEvent;
import ewm.event.repository.EventRepository;
import ewm.event.service.PublicEventService;
import ewm.exception.BadRequestException;
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
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.AnalyzerGrpcClient;
import ru.practicum.ewm.stats.client.CollectorGrpcClient;
import ru.practicum.ewm.stats.grpc.RecommendedEventProto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicEventServiceImpl implements PublicEventService {
    final EventRepository eventRepository;
    final RequestClient requestClient;
    final EventMapper eventMapper;
    final UserMapper userMapper;
    final JPAQueryFactory jpaQueryFactory;
    final CategoryClient categoryClient;
    final UserClient userClient;
    final CollectorGrpcClient collectorGrpcClient;
    final AnalyzerGrpcClient analyzerGrpcClient;

    @Override
    public List<EventShortDto> getAllBy(PublicEventParam eventParam, Pageable pageRequest) {
        BooleanBuilder eventQueryExpression = buildExpression(eventParam);

        List<Event> events = getEvents(pageRequest, eventQueryExpression);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(eventIds);

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
    public EventFullDto getBy(long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with Id =" + eventId + " not found"));
        CategoryDto categoryDto = categoryClient.findBy(event.getCategoryId());
        UserShortDto userShortDto = userMapper.toUserShortDto(userClient.findBy(event.getInitiatorId()));
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, categoryDto, userShortDto);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Event id = " + eventId + " is not published");
        }

        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(Collections.singletonList(eventId));
        long confirmedRequests = confirmedRequestsMap.getOrDefault(eventId, 0L);
        eventFullDto.setConfirmedRequests(confirmedRequests);
        return eventFullDto;
    }

    @Override
    public EventFullDto getBy(long eventId, long userId) {
        EventFullDto eventFullDto = getBy(eventId);
        collectorGrpcClient.collectUserActions(userId, eventId, ActionType.ACTION_VIEW);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getRecommendations(long userId) {
        List<RecommendedEventProto> recommendedEvents = analyzerGrpcClient
            .getRecommendationsForUser(userId, 10)
            .toList();

        Map<Long, Double> ratingMap = recommendedEvents.stream()
            .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

        List<Event> events = eventRepository.findAllByIdIn(
            recommendedEvents.stream().map(RecommendedEventProto::getEventId).collect(Collectors.toSet()));

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsMap(eventIds);

        List<Long> categoryIds = events.stream().map(Event::getCategoryId).toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.findAllByIds(categoryIds).stream()
            .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        return events.stream().map(event -> {
            EventShortDto shortDto = eventMapper.toEventShortDto(event, categoryDtoMap.get(event.getCategoryId()));
            shortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(shortDto.getId(), 0L));
            shortDto.setRating(ratingMap.get(shortDto.getId()));
            return shortDto;
        }).toList();
    }


    @Override
    public void like(long userId, long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with Id =" + eventId + " not found");
        }

        boolean hasParticipated = requestClient.hasUserParticipated(userId, eventId);
        if (!hasParticipated) {
            throw new BadRequestException("User can like only events they attended");
        }

        collectorGrpcClient.collectUserActions(userId, eventId, ActionType.ACTION_LIKE);
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
