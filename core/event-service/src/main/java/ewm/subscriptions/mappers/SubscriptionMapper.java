package ewm.subscriptions.mappers;

import ewm.subscriptions.dto.SubscriptionDto;
import ewm.subscriptions.model.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionDto toSubscriptionShortDto(Subscription subscription);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscription.followerId", source = "followerId")
    @Mapping(target = "subscription.followingId", source = "followingId")
    Subscription toSubscription(@MappingTarget Subscription subscription, Long followerId, Long followingId);
}
