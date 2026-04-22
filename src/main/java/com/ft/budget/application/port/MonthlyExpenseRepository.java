package com.ft.budget.application.port;

import com.ft.budget.domain.MonthlyExpense;

import java.util.Optional;

public interface MonthlyExpenseRepository {

    Optional<MonthlyExpense> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth);

    MonthlyExpense save(MonthlyExpense monthlyExpense);
}
