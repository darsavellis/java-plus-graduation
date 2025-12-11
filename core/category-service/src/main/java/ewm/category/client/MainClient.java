package ewm.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "main-service", path = "/admin/events")
public interface MainClient {
    @GetMapping("/{categoryId}")
    boolean existsByCategoryId(@PathVariable("categoryId") long categoryId);
}
