package ru.practicum.explore.main.category;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explore.main.category.model.Category;

public interface CategoryRepository extends JpaRepository<Category,Long> {
}
