package ewm.request.service;

import ewm.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface PublicRequestService {
    List<ParticipationRequestDto> getSentBy(long userId);

    ParticipationRequestDto send(long userId, long eventId);

    ParticipationRequestDto cancel(long requestId, long userId);

    Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds);
}
