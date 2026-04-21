package com.ft.budget.presentation.dto;

import com.ft.budget.application.dto.BudgetResult;
import com.ft.budget.domain.AlertType;

import java.math.BigDecimal;
import java.util.List;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String yearMonth,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal usageRate,
        List<AlertType> sentAlerts
) {
    public static BudgetResponse from(BudgetResult result) {
        return new BudgetResponse(
                result.id(),
                result.categoryId(),
                result.yearMonth(),
                result.amount(),
                result.spent(),
                result.usageRate(),
                result.sentAlerts()
        );
    }
}
