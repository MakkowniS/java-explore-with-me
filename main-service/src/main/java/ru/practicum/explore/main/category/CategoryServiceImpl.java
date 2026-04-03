package ru.practicum.explore.main.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.category.dto.CategoryDto;
import ru.practicum.explore.main.category.dto.NewCategoryDto;
import ru.practicum.explore.main.category.model.Category;
import ru.practicum.explore.main.error.model.exception.ConflictException;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.event.EventRepository;
import ru.practicum.explore.main.event.model.Event;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    //Admin
    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        return CategoryMapper.mapToCategoryDto(
                categoryRepository.save(CategoryMapper.mapToCategory(newCategoryDto))
        );
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
        category.setName(newCategoryDto.getName());
        return CategoryMapper.mapToCategoryDto(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category with id=" + categoryId + " was not found");
        }
        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Category with id=" + categoryId + " cannot be deleted.");
        }

        categoryRepository.deleteById(categoryId);
    }

    //Public
    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        PageRequest pageRequest = PageRequest.of(from, size);
        return categoryRepository.findAll(pageRequest).stream()
                .map(CategoryMapper::mapToCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
        return CategoryMapper.mapToCategoryDto(category);
    }
}
