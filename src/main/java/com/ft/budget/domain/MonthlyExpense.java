package com.ft.budget.domain;

import com.ft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "monthly_expenses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "categoryId", "yearMonth"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyExpense extends BaseEntity {

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
    private BigDecimal totalAmount;

    private MonthlyExpense(Long userId, Long categoryId, String yearMonth, BigDecimal totalAmount) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.yearMonth = yearMonth;
        this.totalAmount = totalAmount;
    }

    public static MonthlyExpense of(Long userId, Long categoryId, String yearMonth) {
        return new MonthlyExpense(userId, categoryId, yearMonth, BigDecimal.ZERO);
    }

    public void add(BigDecimal amount) {
        this.totalAmount = this.totalAmount.add(amount);
    }
}
