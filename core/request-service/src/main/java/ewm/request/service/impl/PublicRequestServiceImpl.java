package ewm.request.service.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.request.client.EventClient;
import ewm.request.client.UserClient;
import ewm.request.dto.EventFullDto;
import ewm.request.dto.EventState;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.dto.UserDto;
import ewm.request.exception.ConflictException;
import ewm.request.exception.NotFoundException;
import ewm.request.exception.PermissionException;
import ewm.request.mapper.RequestMapper;
import ewm.request.model.ParticipationRequest;
import ewm.request.model.QParticipationRequest;
import ewm.request.model.RequestStatus;
import ewm.request.repository.RequestRepository;
import ewm.request.service.PublicRequestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.ActionType;
import ru.practicum.ewm.stats.client.CollectorGrpcClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicRequestServiceImpl implements PublicRequestService {
    final RequestRepository requestRepository;
    final EventClient eventClient;
    final RequestMapper requestMapper;
    final UserClient userClient;
    final JPAQueryFactory jpaQueryFactory;
    final CollectorGrpcClient collectorGrpcClient;

    @Override
    public List<ParticipationRequestDto> getSentBy(long userId) {
        return requestRepository.findAllByRequesterId(userId)
            .stream().map(requestMapper::toParticipantRequestDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto send(long userId, long eventId) {
        UserDto requester = userClient.findBy(userId);
        EventFullDto eventFullDto = eventClient.getByInternal(eventId);

        if (requester.getId().equals(eventFullDto.getInitiator().getId())) {
            throw new ConflictException("Нельзя делать запрос на свое событие");
        }
        if (!eventFullDto.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Заявка должна быть в состоянии PUBLISHED");
        }

        long confirmedRequests = requestRepository.countAllByEventIdAndStatusIs(eventId, RequestStatus.CONFIRMED);

        if (eventFullDto.getParticipantLimit() != 0 && eventFullDto.getParticipantLimit() == confirmedRequests) {
            throw new ConflictException("Лимит запросов исчерпан");
        }
        ParticipationRequest request = requestMapper.toParticipationRequest(eventFullDto.getId(), requester.getId());

        if (!eventFullDto.isRequestModeration() || eventFullDto.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }
        request = requestRepository.save(request);

        collectorGrpcClient.collectUserActions(userId, eventId, ActionType.ACTION_REGISTER);

        return requestMapper.toParticipantRequestDto(request);
    }

    @Override
    public ParticipationRequestDto cancel(long requestId, long userId) {
        ParticipationRequest participationRequest = requestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        if (userId != participationRequest.getRequesterId()) {
            throw new PermissionException("Доступ запрещен. Отменять может только владелец");
        }

        if (participationRequest.getStatus().equals(RequestStatus.PENDING)) {
            participationRequest.setStatus(RequestStatus.CANCELED);
        }

        return requestMapper.toParticipantRequestDto(participationRequest);
    }

    public Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        QParticipationRequest qRequest = QParticipationRequest.participationRequest;

        return jpaQueryFactory
            .select(qRequest.eventId.as("eventId"), qRequest.count().as("confirmedRequests"))
            .from(qRequest)
            .where(qRequest.eventId.in(eventIds).and(qRequest.status.eq(RequestStatus.CONFIRMED)))
            .groupBy(qRequest.eventId)
            .fetch()
            .stream()
            .collect(Collectors.toMap(
                tuple -> tuple.get(0, Long.class),
                tuple -> Optional.ofNullable(tuple.get(1, Long.class)).orElse(0L))
            );
    }

    @Override
    public boolean hasUserParticipated(long userId, long eventId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId, RequestStatus.CONFIRMED);
    }
}
