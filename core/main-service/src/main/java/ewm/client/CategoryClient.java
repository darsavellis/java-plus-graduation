package ewm.client;

import ewm.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "category-service", path = "/")
public interface CategoryClient {
    @GetMapping("/categories/{catId}")
    CategoryDto findBy(@PathVariable("catId") long catId);

    @GetMapping("/admin/categories")
    List<CategoryDto> findAllByIds(@RequestParam(name = "ids") List<Long> categoryIds);
}
