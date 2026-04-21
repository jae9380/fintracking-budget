package com.ft.budget.presentation.dto;

import com.ft.budget.application.dto.CreateBudgetCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CreateBudgetRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotBlank(message = "연월은 필수입니다.")
        String yearMonth,   // "2026-04" 형식

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        BigDecimal amount
) {
    public CreateBudgetCommand toCommand() {
        return new CreateBudgetCommand(categoryId, YearMonth.parse(yearMonth), amount);
    }
}
