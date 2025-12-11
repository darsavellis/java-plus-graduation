package ewm.client;

import ewm.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "admin-category-service", path = "/admin/categories", url = "category-service")
public interface AdminCategoryClient {
    @GetMapping
    List<CategoryDto> findAllByIds(@RequestParam(name = "ids") List<Long> categoryIds);
}
