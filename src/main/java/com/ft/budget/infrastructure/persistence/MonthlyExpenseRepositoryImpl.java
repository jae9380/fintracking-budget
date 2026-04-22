package com.ft.budget.infrastructure.persistence;

import com.ft.budget.application.port.MonthlyExpenseRepository;
import com.ft.budget.domain.MonthlyExpense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MonthlyExpenseRepositoryImpl implements MonthlyExpenseRepository {

    private final JpaMonthlyExpenseRepository jpaRepository;

    @Override
    public Optional<MonthlyExpense> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth) {
        return jpaRepository.findByUserIdAndCategoryIdAndYearMonth(userId, categoryId, yearMonth);
    }

    @Override
    public MonthlyExpense save(MonthlyExpense monthlyExpense) {
        return jpaRepository.save(monthlyExpense);
    }
}
