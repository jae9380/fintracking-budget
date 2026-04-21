package com.ft.budget.infrastructure.persistence;

import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.domain.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BudgetRepositoryImpl implements BudgetRepository {

    private final JpaBudgetRepository jpaBudgetRepository;

    @Override
    public Budget save(Budget budget) {
        return jpaBudgetRepository.save(budget);
    }

    @Override
    public Optional<Budget> findById(Long id) {
        return jpaBudgetRepository.findById(id);
    }

    @Override
    public Optional<Budget> findByUserIdAndCategoryIdAndYearMonth(Long userId, Long categoryId, String yearMonth) {
        return jpaBudgetRepository.findByUserIdAndCategoryIdAndYearMonth(userId, categoryId, yearMonth);
    }

    @Override
    public List<Budget> findAllByUserIdAndYearMonth(Long userId, String yearMonth) {
        return jpaBudgetRepository.findAllByUserIdAndYearMonth(userId, yearMonth);
    }

    @Override
    public void delete(Budget budget) {
        jpaBudgetRepository.delete(budget);
    }
}
