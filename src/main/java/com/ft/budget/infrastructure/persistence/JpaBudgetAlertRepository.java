package com.ft.budget.infrastructure.persistence;

import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaBudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {

    @Query("SELECT ba.alertType FROM BudgetAlert ba WHERE ba.budget.id = :budgetId")
    List<AlertType> findAlertTypesByBudgetId(@Param("budgetId") Long budgetId);
}
