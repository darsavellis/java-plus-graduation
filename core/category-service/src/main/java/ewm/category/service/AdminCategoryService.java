package ewm.category.service;

import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;

import java.util.List;

public interface AdminCategoryService {
    CategoryDto create(NewCategoryDto categoryDto);

    void deleteBy(long id);

    CategoryDto updateBy(long id, NewCategoryDto newCategoryDto);

    List<CategoryDto> findAllByIds(List<Long> categoryIds);
}
