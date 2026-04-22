package com.ft.budget.infrastructure.persistence;

import com.ft.budget.domain.MonthlyExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaMonthlyExpenseRepository extends JpaRepository<MonthlyExpense, Long> {

    Optional<MonthlyExpense> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth);
}
