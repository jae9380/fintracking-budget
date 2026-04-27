package com.ft.budget.domain;

import com.ft.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static com.ft.common.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Budget 도메인 테스트")
class BudgetTest {
    private static final YearMonth THIS_MONTH = YearMonth.of(2026, 4);

    @Nested
    @DisplayName("예산 생성")
    class Create {
        @Test
        @DisplayName("성공 - 예산이 정상 생성된다")
        void success_validParams_createsBudget() {
            // given
            Long userId = 1L;
            Long categoryId = 5L;
            BigDecimal amount = new BigDecimal("100000");

            // when
            Budget budget = Budget.create(userId, categoryId, THIS_MONTH, amount);

            // then
            assertThat(budget.getUserId()).isEqualTo(1L);
            assertThat(budget.getCategoryId()).isEqualTo(5L);
            assertThat(budget.getYearMonth()).isEqualTo("2026-04");
            assertThat(budget.getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("실패 - 금액이 0이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // when & then
            assertThatThrownBy(() -> Budget.create(1L, 5L, THIS_MONTH, BigDecimal.ZERO))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_INVALID_AMOUNT));
        }

        @Test
        @DisplayName("실패 - 금액이 음수이면 예외 발생")
        void fail_negativeAmount_throwsException() {
            // when & then
            assertThatThrownBy(() -> Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("-1000")))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_INVALID_AMOUNT));
        }

        @Test
        @DisplayName("실패 - 금액이 null이면 예외 발생")
        void fail_nullAmount_throwsException() {
            // when & then
            assertThatThrownBy(() -> Budget.create(1L, 5L, THIS_MONTH, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("소유자 검증")
    class ValidateOwner {
        @Test
        @DisplayName("성공 - 본인이면 예외 없음")
        void success_sameUser_noException() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThatNoException().isThrownBy(() -> budget.validateOwner(1L));
        }

        @Test
        @DisplayName("실패 - 다른 사용자이면 예외 발생")
        void fail_differentUser_throwsException() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> budget.validateOwner(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_NO_ACCESS));
        }
    }

    @Nested
    @DisplayName("예산 금액 수정")
    class UpdateAmount {
        @Test
        @DisplayName("성공 - 금액이 변경된다")
        void success_validAmount_amountUpdated() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when
            budget.updateAmount(new BigDecimal("200000"));

            // then
            assertThat(budget.getAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("실패 - 0원이면 예외 발생")
        void fail_zeroAmount_throwsException() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> budget.updateAmount(BigDecimal.ZERO))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("소비율 계산")
    class CalculateUsageRate {
        @Test
        @DisplayName("성공 - 절반을 사용했으면 50%를 반환한다")
        void calculateUsageRate_whenHalfSpent_returns50Percent() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when
            BigDecimal rate = budget.calculateUsageRate(new BigDecimal("50000"));

            // then
            assertThat(rate).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("실패 - 지출이 음수이면 예외 발생")
        void calculateUsageRate_whenNegativeSpent_throwsException() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThatThrownBy(() -> budget.calculateUsageRate(new BigDecimal("-1")))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(BUDGET_EXPENSE_INVALID_AMOUNT));
        }
    }

    @Nested
    @DisplayName("초과 여부 확인")
    class IsExceeded {
        @Test
        @DisplayName("성공 - 예산 미만이면 false를 반환한다")
        void isExceeded_whenSpentLessThanBudget_returnsFalse() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThat(budget.isExceeded(new BigDecimal("99999"))).isFalse();
        }

        @Test
        @DisplayName("성공 - 예산과 동일하면 true를 반환한다")
        void isExceeded_whenSpentEqualToBudget_returnsTrue() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThat(budget.isExceeded(new BigDecimal("100000"))).isTrue();
        }

        @Test
        @DisplayName("성공 - 예산 초과이면 true를 반환한다")
        void isExceeded_whenSpentMoreThanBudget_returnsTrue() {
            // given
            Budget budget = Budget.create(1L, 5L, THIS_MONTH, new BigDecimal("100000"));

            // when & then
            assertThat(budget.isExceeded(new BigDecimal("120000"))).isTrue();
        }
    }
}
