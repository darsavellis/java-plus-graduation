package ewm.request.service;

import ewm.request.dto.EventRequestStatusUpdateRequest;
import ewm.request.dto.EventRequestStatusUpdateResult;
import ewm.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface PrivateRequestService {
    List<ParticipationRequestDto> getReceivedBy(long userId, long eventId);

    EventRequestStatusUpdateResult update(long userId, long eventId, EventRequestStatusUpdateRequest updateRequest);

    Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds);
}
