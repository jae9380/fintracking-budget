package com.ft.budget.application.port;

import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.BudgetAlert;

import java.util.List;

public interface BudgetAlertRepository {
    BudgetAlert save(BudgetAlert alert);
    List<AlertType> findSentAlertTypesByBudgetId(Long budgetId);
}
