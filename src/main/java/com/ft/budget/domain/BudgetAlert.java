package com.ft.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Column(nullable = false)
    private LocalDateTime alertedAt;

    private BudgetAlert(Budget budget, AlertType alertType) {
        this.budget = budget;
        this.alertType = alertType;
        this.alertedAt = LocalDateTime.now();
    }

    public static BudgetAlert of(Budget budget, AlertType alertType) {
        return new BudgetAlert(budget, alertType);
    }
}
