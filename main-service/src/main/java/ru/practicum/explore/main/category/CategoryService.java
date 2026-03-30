package ru.practicum.explore.main.category;

import ru.practicum.explore.main.category.dto.CategoryDto;
import ru.practicum.explore.main.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    //Admin
    CategoryDto addCategory(NewCategoryDto newCategoryDto);
    CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto);
    void deleteCategory(Long categoryId);

    //Public
    List<CategoryDto> getCategories(int from, int size);
    CategoryDto getCategoryById(Long categoryId);

}
