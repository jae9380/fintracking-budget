package com.ft.budget.domain;

import com.ft.common.entity.BaseEntity;
import com.ft.common.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

import static com.ft.common.exception.ErrorCode.*;

@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String yearMonth;  // "2026-04" 형식

    @Column(nullable = false)
    private BigDecimal amount;

    private Budget(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal amount) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.yearMonth = yearMonth.toString();
        this.amount = amount;
    }

    public static Budget create(Long userId, Long categoryId, YearMonth yearMonth, BigDecimal amount) {
        validateAmount(amount);
        return new Budget(userId, categoryId, yearMonth, amount);
    }

    // 소유자 검증
    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomException(BUDGET_NO_ACCESS);
        }
    }

    // 예산 금액 수정
    public void updateAmount(BigDecimal amount) {
        validateAmount(amount);
        this.amount = amount;
    }

    // 사용률 계산 (%)
    public BigDecimal calculateUsageRate(BigDecimal spent) {
        if (spent == null || spent.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(BUDGET_EXPENSE_INVALID_AMOUNT);
        }
        return spent
                .multiply(new BigDecimal("100"))
                .divide(amount, 2, RoundingMode.HALF_UP);
    }

    // 초과 여부
    public boolean isExceeded(BigDecimal spent) {
        return spent.compareTo(amount) >= 0;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(BUDGET_INVALID_AMOUNT);
        }
    }
}
