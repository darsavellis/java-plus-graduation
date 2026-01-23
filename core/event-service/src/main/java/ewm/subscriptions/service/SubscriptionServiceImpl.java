package ewm.subscriptions.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ewm.exception.ConflictException;
import ewm.exception.ValidationException;
import ewm.interaction.api.client.UserClient;
import ewm.interaction.api.dto.UserDto;
import ewm.interaction.api.dto.UserShortDto;
import ewm.interaction.api.mappers.UserMapper;
import ewm.subscriptions.dto.SubscriptionDto;
import ewm.subscriptions.mappers.SubscriptionMapper;
import ewm.subscriptions.model.QSubscription;
import ewm.subscriptions.model.Subscription;
import ewm.subscriptions.repository.SubscriptionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionServiceImpl implements SubscriptionService {
    final SubscriptionRepository subscriptionRepository;
    final SubscriptionMapper subscriptionMapper;
    final UserMapper userMapper;
    final JPAQueryFactory jpaQueryFactory;
    final QSubscription qSubscription = QSubscription.subscription;
    final UserClient userClient;

    @Override
    public Set<UserShortDto> findFollowing(long userId, Pageable page) {
        log.info("Getting subscriptions for current user with id = {}", userId);
        List<Long> followingIds = jpaQueryFactory
            .selectFrom(qSubscription)
            .where(qSubscription.followerId.eq(userId))
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .stream()
            .map(Subscription::getFollowingId)
            .toList();
        log.info("Received {} subscriptions for user with id = {}", followingIds.size(), userId);

        return userClient.findAllBy(followingIds, 0, followingIds.size()).stream()
            .map(userMapper::toUserShortDto).collect(Collectors.toSet());
    }

    @Override
    public Set<UserShortDto> findFollowers(long userId, Pageable page) {
        log.info("Getting followers for current user with id = {}", userId);
        List<Long> followerIds = jpaQueryFactory
            .selectFrom(qSubscription)
            .where(qSubscription.followingId.eq(userId))
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .stream()
            .map(Subscription::getFollowerId)
            .collect(Collectors.toList());
        log.info("Received {} followers for user with id = {}", followerIds.size(), userId);

        return userClient.findAllBy(followerIds, 0, followerIds.size()).stream()
            .map(userMapper::toUserShortDto).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public SubscriptionDto follow(long userId, long followingId) {
        log.info("User with id = {} attempts to follow user with id = {}", userId, followingId);

        if (userId == followingId) {
            throw new ConflictException("User with id = " + userId + " cannot follow themselves");
        }

        UserDto follower = userClient.findBy(userId);
        UserDto following = userClient.findBy(followingId);

        Subscription subscription = subscriptionRepository.save(
            subscriptionMapper.toSubscription(new Subscription(), follower.getId(), following.getId())
        );
        log.info("User with id = {} successfully followed user with id = {}", userId, followingId);
        return subscriptionMapper.toSubscriptionShortDto(subscription);
    }

    @Override
    @Transactional
    public void unfollow(long userId, long followingId) {
        log.info("User with id = {} attempts to unfollow user with id = {}", userId, followingId);
        int deleteCount = subscriptionRepository.deleteByFollowingId(followingId);
        if (deleteCount == 0) {
            throw new ValidationException("Invalid parameters provided");
        }
        log.info("User with id = {} successfully unfollowed user with id = {}", userId, followingId);
    }
}
