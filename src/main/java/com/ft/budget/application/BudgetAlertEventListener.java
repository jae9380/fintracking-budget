package com.ft.budget.application;

import com.ft.budget.application.port.BudgetAlertRepository;
import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;
import com.ft.budget.domain.BudgetAlert;
import com.ft.budget.domain.handler.BudgetAlertHandler;
import com.ft.budget.domain.handler.Exceeded100Handler;
import com.ft.budget.domain.handler.Warning50Handler;
import com.ft.budget.domain.handler.Warning80Handler;
// TODO: [Kafka 도입 시] 아래 두 import 제거
// transaction-service의 클래스를 budget-service가 직접 참조 — MSA 원칙 위반
// TransactionCreatedEvent는 Kafka 토픽 메시지로 수신, TransactionRepository는 직접 접근 불가
import com.ft.transaction.application.event.TransactionCreatedEvent;
import com.ft.transaction.application.port.TransactionRepository;
import com.ft.transaction.domain.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
// TODO: [Kafka 도입 시] 아래 두 import 제거 — @KafkaListener로 교체
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetAlertEventListener {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    // TODO: [Kafka 도입 시] TransactionRepository 제거
    // transaction-service DB에 직접 접근 — MSA 원칙 위반
    // 지출 합계(spent)는 TransactionCreatedEvent 메시지에 포함하거나
    // budget-service 자체 집계 테이블로 관리하는 방향으로 전환
    private final TransactionRepository transactionRepository;

    // TODO: [Kafka 도입 시] @TransactionalEventListener → @KafkaListener로 교체
    // @KafkaListener(topics = "transaction.created", groupId = "budget-service")
    // public void onTransactionCreated(TransactionCreatedEvent event) { ... }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (event.type() != TransactionType.EXPENSE || event.categoryId() == null) {
            return;
        }

        YearMonth yearMonth = YearMonth.from(event.transactionDate());

        budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
                        event.userId(), event.categoryId(), yearMonth.toString())
                .ifPresent(budget -> checkAndLogAlerts(budget, yearMonth));
    }

    private void checkAndLogAlerts(Budget budget, YearMonth yearMonth) {
        // TODO: [Kafka 도입 시] transactionRepository 직접 호출 제거
        // 지출 합계를 Kafka 이벤트 메시지에 포함하거나 budget-service 내부 집계로 대체
        BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryIdAndYearMonth(
                budget.getUserId(), budget.getCategoryId(),
                yearMonth.getYear(), yearMonth.getMonthValue());

        List<AlertType> sentAlerts = budgetAlertRepository.findSentAlertTypesByBudgetId(budget.getId());

        BudgetAlertHandler chain = new Warning50Handler();
        chain.setNext(new Warning80Handler()).setNext(new Exceeded100Handler());

        List<AlertType> newAlerts = new ArrayList<>();
        chain.handle(budget, spent, sentAlerts, newAlerts);

        newAlerts.forEach(alertType -> {
            budgetAlertRepository.save(BudgetAlert.of(budget, alertType));
            log.info("[BudgetAlert] 예산 알림 발생 — userId={}, budgetId={}, alertType={}, spent={}, limit={}",
                    budget.getUserId(), budget.getId(), alertType, spent, budget.getAmount());
        });
    }
}
