package com.ft.budget.infrastructure.persistence;

import com.ft.budget.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaBudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth);
    List<Budget> findAllByUserIdAndYearMonth(Long userId, String yearMonth);
}
