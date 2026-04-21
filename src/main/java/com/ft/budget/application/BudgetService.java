package com.ft.budget.application;

import com.ft.budget.application.dto.BudgetResult;
import com.ft.budget.application.dto.CreateBudgetCommand;
import com.ft.budget.application.port.BudgetAlertRepository;
import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.domain.AlertType;
import com.ft.budget.domain.Budget;
import com.ft.budget.domain.BudgetAlert;
import com.ft.budget.domain.handler.BudgetAlertHandler;
import com.ft.budget.domain.handler.Exceeded100Handler;
import com.ft.budget.domain.handler.Warning50Handler;
import com.ft.budget.domain.handler.Warning80Handler;
import com.ft.common.exception.CustomException;
// TODO: [Kafka 도입 시] 아래 두 import 제거
// notification-service를 budget-service가 직접 호출 — MSA 원칙 위반
// 알림 발송은 Kafka 토픽으로 BudgetAlertEvent를 발행하고 notification-service가 구독하는 방식으로 전환
import com.ft.notification.application.NotificationService;
import com.ft.notification.domain.NotificationType;
// TODO: [Kafka 도입 시] 아래 import 제거
// transaction-service DB에 직접 접근 — MSA 원칙 위반
// 지출 합계 조회는 BudgetAlertEventListener로 이전하거나 budget-service 내부 집계로 대체
import com.ft.transaction.application.port.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.ft.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    // TODO: [Kafka 도입 시] TransactionRepository 제거 — transaction-service DB 직접 접근
    private final TransactionRepository transactionRepository;
    // TODO: [Kafka 도입 시] NotificationService 직접 호출 제거
    // KafkaTemplate으로 "budget.alert" 토픽에 BudgetAlertEvent 발행
    // notification-service가 해당 토픽을 구독하여 알림 처리
    private final NotificationService notificationService;

    // 예산 생성
    @Transactional
    public BudgetResult create(Long userId, CreateBudgetCommand command) {
        budgetRepository.findByUserIdAndCategoryIdAndYearMonth(
                        userId, command.categoryId(), command.yearMonth().toString())
                .ifPresent(b -> { throw new CustomException(BUDGET_DUPLICATE); });

        Budget budget = Budget.create(userId, command.categoryId(), command.yearMonth(), command.amount());
        Budget saved = budgetRepository.save(budget);

        return BudgetResult.of(saved, BigDecimal.ZERO, List.of());
    }

    // 월별 예산 목록 조회
    @Transactional(readOnly = true)
    public List<BudgetResult> findAll(Long userId, YearMonth yearMonth) {
        return budgetRepository.findAllByUserIdAndYearMonth(userId, yearMonth.toString())
                .stream()
                .map(budget -> buildResult(budget))
                .toList();
    }

    // 예산 단건 조회
    @Transactional(readOnly = true)
    public BudgetResult findById(Long userId, Long budgetId) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        return buildResult(budget);
    }

    // 예산 금액 수정
    @Transactional
    public BudgetResult updateAmount(Long userId, Long budgetId, BigDecimal amount) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        budget.updateAmount(amount);
        return buildResult(budget);
    }

    // 예산 삭제
    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);
        budgetRepository.delete(budget);
    }

    // 알림 체크 (Chain of Responsibility 실행)
    @Transactional
    public List<AlertType> checkAlerts(Long userId, Long budgetId) {
        Budget budget = getBudget(budgetId);
        budget.validateOwner(userId);

        YearMonth yearMonth = YearMonth.parse(budget.getYearMonth());
        BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryIdAndYearMonth(
                userId, budget.getCategoryId(), yearMonth.getYear(), yearMonth.getMonthValue());

        List<AlertType> sentAlerts = budgetAlertRepository.findSentAlertTypesByBudgetId(budgetId);

        // 체인 구성: 50% → 80% → 100%
        BudgetAlertHandler chain = new Warning50Handler();
        chain.setNext(new Warning80Handler()).setNext(new Exceeded100Handler());

        List<AlertType> newAlerts = new ArrayList<>();
        chain.handle(budget, spent, sentAlerts, newAlerts);

        // 새로 발생한 알림 저장 + Observer 브로드캐스트
        newAlerts.forEach(alertType -> {
            budgetAlertRepository.save(BudgetAlert.of(budget, alertType));
            // TODO: [Kafka 도입 시] notificationService.send() 직접 호출 제거
            // kafkaTemplate.send("budget.alert", new BudgetAlertEvent(
            //         userId, toNotificationType(alertType), buildTitle(alertType), buildMessage(alertType, budget)
            // ));
            notificationService.send(
                    userId,
                    toNotificationType(alertType),
                    buildTitle(alertType),
                    buildMessage(alertType, budget)
            );
        });

        return newAlerts;
    }

    private BudgetResult buildResult(Budget budget) {
        YearMonth yearMonth = YearMonth.parse(budget.getYearMonth());
        BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryIdAndYearMonth(
                budget.getUserId(), budget.getCategoryId(),
                yearMonth.getYear(), yearMonth.getMonthValue());
        List<AlertType> sentAlerts = budgetAlertRepository.findSentAlertTypesByBudgetId(budget.getId());
        return BudgetResult.of(budget, spent, sentAlerts);
    }

    private Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new CustomException(BUDGET_NOT_FOUND));
    }

    private NotificationType toNotificationType(AlertType alertType) {
        return switch (alertType) {
            case WARNING_50, WARNING_80 -> NotificationType.BUDGET_WARNING;
            case EXCEEDED_100 -> NotificationType.BUDGET_EXCEEDED;
        };
    }

    private String buildTitle(AlertType alertType) {
        return switch (alertType) {
            case WARNING_50 -> "예산 50% 사용 경고";
            case WARNING_80 -> "예산 80% 사용 경고";
            case EXCEEDED_100 -> "예산 초과 알림";
        };
    }

    private String buildMessage(AlertType alertType, Budget budget) {
        return switch (alertType) {
            case WARNING_50 -> String.format("[%s] 예산의 50%%를 사용했습니다.", budget.getYearMonth());
            case WARNING_80 -> String.format("[%s] 예산의 80%%를 사용했습니다.", budget.getYearMonth());
            case EXCEEDED_100 -> String.format("[%s] 설정한 예산을 초과했습니다.", budget.getYearMonth());
        };
    }
}
