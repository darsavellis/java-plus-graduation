package ewm.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", path = "/admin/events", fallback = MainClientFallback.class)
public interface MainClient {
    @GetMapping("/{categoryId}")
    boolean existsByCategoryId(@PathVariable("categoryId") long categoryId);
}
