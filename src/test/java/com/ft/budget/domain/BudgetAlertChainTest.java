package com.ft.budget.domain;

import com.ft.budget.domain.handler.BudgetAlertHandler;
import com.ft.budget.domain.handler.Exceeded100Handler;
import com.ft.budget.domain.handler.Warning50Handler;
import com.ft.budget.domain.handler.Warning80Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BudgetAlertChain 도메인 테스트")
class BudgetAlertChainTest {
    private Budget budget;
    private BudgetAlertHandler chain;

    @BeforeEach
    void setUp() {
        // 체인 구성: 50% → 80% → 100%
        budget = Budget.create(1L, 5L, YearMonth.of(2026, 4), new BigDecimal("100000"));
        chain = new Warning50Handler();
        chain.setNext(new Warning80Handler()).setNext(new Exceeded100Handler());
    }

    @Nested
    @DisplayName("알림 발생 조건")
    class AlertConditions {
        @Test
        @DisplayName("50% 미만 - 알림 없음")
        void handle_whenBelow50Percent_noAlerts() {
            // given
            BigDecimal spent = new BigDecimal("49000");
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, List.of(), result);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("50% 이상 80% 미만 - WARNING_50 발생")
        void handle_whenBetween50And79Percent_warning50Only() {
            // given
            BigDecimal spent = new BigDecimal("50000");
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, List.of(), result);

            // then
            assertThat(result).containsExactly(AlertType.WARNING_50);
        }

        @Test
        @DisplayName("80% 이상 100% 미만 - WARNING_50, WARNING_80 발생")
        void handle_whenBetween80And99Percent_warning50And80() {
            // given
            BigDecimal spent = new BigDecimal("85000");
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, List.of(), result);

            // then
            assertThat(result).containsExactlyInAnyOrder(AlertType.WARNING_50, AlertType.WARNING_80);
        }

        @Test
        @DisplayName("100% 이상 - WARNING_50, WARNING_80, EXCEEDED_100 모두 발생")
        void handle_whenExceeds100Percent_allThreeAlerts() {
            // given
            BigDecimal spent = new BigDecimal("100000");
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, List.of(), result);

            // then
            assertThat(result).containsExactlyInAnyOrder(
                    AlertType.WARNING_50, AlertType.WARNING_80, AlertType.EXCEEDED_100);
        }
    }

    @Nested
    @DisplayName("중복 알림 방지")
    class DuplicatePrevention {
        @Test
        @DisplayName("이미 전송된 WARNING_50은 다시 발생하지 않는다")
        void handle_whenWarning50AlreadySent_skipsWarning50() {
            // given
            BigDecimal spent = new BigDecimal("60000");
            List<AlertType> sentAlerts = List.of(AlertType.WARNING_50);
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, sentAlerts, result);

            // then
            assertThat(result).doesNotContain(AlertType.WARNING_50);
        }

        @Test
        @DisplayName("모든 알림이 이미 전송됐으면 새 알림 없음")
        void handle_whenAllAlreadySent_noNewAlerts() {
            // given
            List<AlertType> sentAlerts = List.of(AlertType.WARNING_50, AlertType.WARNING_80, AlertType.EXCEEDED_100);
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, new BigDecimal("120000"), sentAlerts, result);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("80%에서 WARNING_50 전송 완료 상태 - WARNING_80만 새로 발생")
        void handle_whenAt80PercentAndWarning50AlreadySent_onlyWarning80() {
            // given
            BigDecimal spent = new BigDecimal("85000");
            List<AlertType> sentAlerts = new ArrayList<>(List.of(AlertType.WARNING_50));
            List<AlertType> result = new ArrayList<>();

            // when
            chain.handle(budget, spent, sentAlerts, result);

            // then
            assertThat(result).containsExactly(AlertType.WARNING_80);
        }
    }
}
