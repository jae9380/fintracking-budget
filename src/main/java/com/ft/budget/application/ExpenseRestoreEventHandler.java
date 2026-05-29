package com.ft.budget.application;

import com.ft.budget.application.port.MonthlyExpenseRepository;
import com.ft.common.event.TransactionDeletedEvent;
import com.ft.common.kafka.EventHandler;
import com.ft.common.kafka.KafkaTopic;
import com.ft.common.metric.annotation.MonitoredKafka;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseRestoreEventHandler implements EventHandler<TransactionDeletedEvent> {

    private final MonthlyExpenseRepository monthlyExpenseRepository;

    @MonitoredKafka(topic = KafkaTopic.TRANSACTION_DELETED, action = "consume")
    @KafkaListener(topics = KafkaTopic.TRANSACTION_DELETED, groupId = "budget-service")
    @Transactional
    @Override
    public void handle(TransactionDeletedEvent event) {
        if (!"EXPENSE".equals(event.type()) || event.categoryId() == null) {
            return;
        }

        String yearMonthStr = YearMonth.from(event.transactedAt()).toString();

        monthlyExpenseRepository
                .findByUserIdAndCategoryIdAndYearMonth(event.userId(), event.categoryId(), yearMonthStr)
                .ifPresent(monthlyExpense -> {
                    monthlyExpense.subtract(event.amount());
                    monthlyExpenseRepository.save(monthlyExpense);
                    log.info("[ExpenseRestore] 월간 지출 차감 — userId={}, categoryId={}, yearMonth={}, amount={}",
                            event.userId(), event.categoryId(), yearMonthStr, event.amount());
                });
    }
}
