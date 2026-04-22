package com.ft.budget.application;

import com.ft.budget.application.port.BudgetAlertRepository;
import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.application.port.MonthlyExpenseRepository;
import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;
import com.ft.budget.domain.BudgetAlert;
import com.ft.budget.domain.MonthlyExpense;
import com.ft.budget.domain.handler.BudgetAlertHandler;
import com.ft.budget.domain.handler.Exceeded100Handler;
import com.ft.budget.domain.handler.Warning50Handler;
import com.ft.budget.domain.handler.Warning80Handler;
import com.ft.common.event.BudgetAlertEvent;
import com.ft.common.event.TransactionCreatedEvent;
import com.ft.common.kafka.EventHandler;
import com.ft.common.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertEventListener implements EventHandler<TransactionCreatedEvent> {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final MonthlyExpenseRepository monthlyExpenseRepository;
    private final BudgetAlertEventPublisher alertEventPublisher;

    @KafkaListener(topics = KafkaTopic.TRANSACTION_CREATED, groupId = "budget-service")
    @Transactional
    @Override
    public void handle(TransactionCreatedEvent event) {
        if (!"EXPENSE".equals(event.type()) || event.categoryId() == null) {
            return;
        }

        YearMonth yearMonth = YearMonth.from(event.transactedAt());
        String yearMonthStr = yearMonth.toString();

        MonthlyExpense monthlyExpense = monthlyExpenseRepository
                .findByUserIdAndCategoryIdAndYearMonth(event.userId(), event.categoryId(), yearMonthStr)
                .orElseGet(() -> MonthlyExpense.of(event.userId(), event.categoryId(), yearMonthStr));

        monthlyExpense.add(event.amount());
        MonthlyExpense saved = monthlyExpenseRepository.save(monthlyExpense);

        budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
                        event.userId(), event.categoryId(), yearMonthStr)
                .ifPresent(budget -> checkAndPublishAlerts(budget, saved.getTotalAmount()));
    }

    private void checkAndPublishAlerts(Budget budget, BigDecimal spent) {
        List<AlertType> sentAlerts = budgetAlertRepository.findSentAlertTypesByBudgetId(budget.getId());

        BudgetAlertHandler chain = new Warning50Handler();
        chain.setNext(new Warning80Handler()).setNext(new Exceeded100Handler());

        List<AlertType> newAlerts = new ArrayList<>();
        chain.handle(budget, spent, sentAlerts, newAlerts);

        newAlerts.forEach(alertType -> {
            budgetAlertRepository.save(BudgetAlert.of(budget, alertType));

            alertEventPublisher.publish(new BudgetAlertEvent(
                    UUID.randomUUID().toString(),
                    budget.getUserId(),
                    budget.getId(),
                    budget.getCategoryId(),
                    alertType.name(),
                    spent,
                    budget.getAmount()
            ));

            log.info("[BudgetAlert] 예산 알림 발행 — userId={}, budgetId={}, alertType={}, spent={}, limit={}",
                    budget.getUserId(), budget.getId(), alertType, spent, budget.getAmount());
        });
    }
}
