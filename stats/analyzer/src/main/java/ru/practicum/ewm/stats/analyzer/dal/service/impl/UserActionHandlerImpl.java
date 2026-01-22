package ru.practicum.ewm.stats.analyzer.dal.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.dal.mapper.UserActionMapper;
import ru.practicum.ewm.stats.analyzer.dal.repository.UserActionRepository;
import ru.practicum.ewm.stats.analyzer.dal.service.UserActionHandler;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandlerImpl implements UserActionHandler {
    final UserActionMapper userActionMapper;
    final UserActionRepository userActionRepository;

    @Override
    @Transactional
    public void handle(UserActionAvro userActionAvro) {
        log.debug("Handling UserActionAvro: {}", userActionAvro);
        try {
            userActionRepository.findByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId())
                .map(exist -> {
                    double weight = userActionMapper.getActionWeight(userActionAvro.getActionType());
                    double oldWeight = exist.getWeight();
                    log.trace("Found existing action for userId={}, eventId={}, oldWeight={}, newWeight={}",
                        exist.getUserId(), exist.getEventId(), oldWeight, weight);

                    if (weight > oldWeight) {
                        exist.setWeight(weight);
                        log.info("Updated weight for userId={}, eventId={} from {} to {}", exist.getUserId(),
                            exist.getEventId(), oldWeight, weight);
                    } else {
                        log.debug("No update needed for userId={}, eventId={} (newWeight <= oldWeight)",
                            exist.getUserId(), exist.getEventId());
                    }
                    return userActionRepository.save(exist);
                })
                .orElseGet(() -> {
                    log.info("Saving new user action for userId={}, eventId={}", userActionAvro.getUserId(),
                        userActionAvro.getEventId());
                    return userActionRepository.save(userActionMapper.mapToUserAction(userActionAvro));
                });
        } catch (Exception e) {
            log.error("Error handling UserActionAvro: {}", userActionAvro, e);
            throw e;
        }
    }
}
