package com.ft.budget.application.port;

import com.ft.budget.domain.Budget;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository {
    Budget save(Budget budget);
    Optional<Budget> findById(Long id);
    Optional<Budget> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth);
    List<Budget> findAllByUserIdAndYearMonth(Long userId, String yearMonth);
    void delete(Budget budget);
}
