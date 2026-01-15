package ewm.interaction.api.client;

import ewm.interaction.api.dto.CategoryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryClientFallback implements CategoryClient {
    @Override
    public CategoryDto findBy(long catId) {
        return null;
    }

    @Override
    public List<CategoryDto> findAllByIds(List<Long> categoryIds) {
        return List.of();
    }
}
