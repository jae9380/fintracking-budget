package com.ft.budget.domain.handler;

import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;

import java.math.BigDecimal;
import java.util.List;

public abstract class BudgetAlertHandler {

    private BudgetAlertHandler next;

    public BudgetAlertHandler setNext(BudgetAlertHandler next) {
        this.next = next;
        return next;
    }

    // 체인 실행 — 조건 충족 시 alertType 추가 후 다음 핸들러로 전달
    public void handle(Budget budget, BigDecimal spent, List<AlertType> sentAlerts, List<AlertType> result) {
        if (shouldAlert(budget, spent, sentAlerts)) {
            result.add(getAlertType());
        }
        if (next != null) {
            next.handle(budget, spent, sentAlerts, result);
        }
    }

    protected abstract boolean shouldAlert(Budget budget, BigDecimal spent, List<AlertType> sentAlerts);

    protected abstract AlertType getAlertType();
}
