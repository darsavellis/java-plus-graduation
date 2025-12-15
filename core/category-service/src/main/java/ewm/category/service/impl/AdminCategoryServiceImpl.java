package ewm.category.service.impl;

import ewm.category.client.MainClient;
import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;
import ewm.category.exception.ConflictException;
import ewm.category.exception.NotFoundException;
import ewm.category.mapper.CategoryMapper;
import ewm.category.model.Category;
import ewm.category.repository.CategoryRepository;
import ewm.category.service.AdminCategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCategoryServiceImpl implements AdminCategoryService {
    final CategoryRepository categoryRepository;
    final CategoryMapper categoryMapper;
    final MainClient mainClient;

    @Override
    public CategoryDto create(NewCategoryDto categoryDto) {
        Category category = categoryRepository.save(categoryMapper.toCategory(categoryDto));
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public void deleteBy(long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Категории с id = " + id + " не существует"));

        if (mainClient.existsByCategoryId(id)) {
            throw new ConflictException("Обьект имеет зависимость с событием");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryDto updateBy(long id, NewCategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        category.setName(categoryDto.getName());

        return categoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDto> findAllByIds(List<Long> categoryIds) {
        return categoryRepository.findAllById
            (categoryIds).stream().map(categoryMapper::toCategoryDto).toList();
    }
}

