package ewm.interaction.api.client;

import ewm.interaction.api.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public List<UserDto> findAllBy(List<Long> ids, int from, int size) {
        return List.of();
    }

    @Override
    public void deleteBy(Long userId) {

    }

    @Override
    public UserDto findBy(Long userId) {
        return null;
    }
}
