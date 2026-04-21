package com.ft.budget.domain.handler;

import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;

import java.math.BigDecimal;
import java.util.List;

public class Exceeded100Handler extends BudgetAlertHandler {

    @Override
    protected boolean shouldAlert(Budget budget, BigDecimal spent, List<AlertType> sentAlerts) {
        return budget.isExceeded(spent)
                && !sentAlerts.contains(AlertType.EXCEEDED_100);
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.EXCEEDED_100;
    }
}
