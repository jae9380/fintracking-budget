package com.ft.budget.domain.handler;

import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;

import java.math.BigDecimal;
import java.util.List;

public class Warning80Handler extends BudgetAlertHandler {

    @Override
    protected boolean shouldAlert(Budget budget, BigDecimal spent, List<AlertType> sentAlerts) {
        BigDecimal usageRate = budget.calculateUsageRate(spent);
        return usageRate.compareTo(new BigDecimal("80")) >= 0
                && !sentAlerts.contains(AlertType.WARNING_80);
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.WARNING_80;
    }
}
