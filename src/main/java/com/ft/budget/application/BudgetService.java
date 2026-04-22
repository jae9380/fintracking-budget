package com.ft.budget.application;

import com.ft.budget.application.dto.BudgetResult;
import com.ft.budget.application.dto.CreateBudgetCommand;
import com.ft.budget.application.port.BudgetAlertRepository;
import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.application.port.MonthlyExpenseRepository;
import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;
import com.ft.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static com.ft.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;

    // 예산 생성
    @Transactional
    public BudgetResult create(Long userId, CreateBudgetCommand command) {
        budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
                        userId, command.categoryId(), command.yearMonth().toString())
                .ifPresent(b -> { throw new CustomException(BUDGET_DUPLICATE); });

        Budget budget = Budget.create(userId, command.categoryId(), command.yearMonth(), command.amount());
        Budget saved = budgetRepository.save(budget);

        return BudgetResult.of(saved, BigDecimal.ZERO, List.of());
    }

    // 월별 예산 목록 조회
    @Transactional(readOnly = true)
    public List<BudgetResult> findAll(Long userId, YearMonth yearMonth) {
        return budgetRepository.findAllByUserIdAndYearMonth(userId, yearMonth.toString())
                .stream()
                .map(budget -> buildResult(budget))
                .toList();
    }

    // 예산 단건 조회
    @Transactional(readOnly = true)
    public BudgetResult findById(Long userId, Long budgetId) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        return buildResult(budget);
    }

    // 예산 금액 수정
    @Transactional
    public BudgetResult updateAmount(Long userId, Long budgetId, BigDecimal amount) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        budget.updateAmount(amount);
        return buildResult(budget);
    }

    // 예산 삭제
    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        budgetRepository.delete(budget);
    }

    private BudgetResult buildResult(Budget budget) {
        BigDecimal spent = monthlyExpenseRepository
                .findByUserIdAndCategoryIdAndYearMonth(
                        budget.getUserId(), budget.getCategoryId(), budget.getYearMonth())
                .map(e -> e.getTotalAmount())
                .orElse(BigDecimal.ZERO);
        List<AlertType> sentAlerts = budgetAlertRepository.findSentAlertTypesByBudgetId(budget.getId());
        return BudgetResult.of(budget, spent, sentAlerts);
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new CustomException(BUDGET_NOT_FOUND));
    }
}
