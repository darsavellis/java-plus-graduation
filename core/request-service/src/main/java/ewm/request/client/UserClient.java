package ewm.request.client;

import ewm.request.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping
    List<UserDto> findAllBy(@RequestParam(required = false) List<Long> ids,
                            @RequestParam(defaultValue = "0") int from,
                            @RequestParam(defaultValue = "10") int size);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBy(@PathVariable Long userId);

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    UserDto findBy(@PathVariable Long userId);
}
