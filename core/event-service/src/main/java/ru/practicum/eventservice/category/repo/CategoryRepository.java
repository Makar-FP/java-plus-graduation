package ru.practicum.eventservice.category.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.eventservice.category.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
