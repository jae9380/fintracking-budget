package com.ft.budget.application;

import com.ft.budget.application.dto.BudgetResult;
import com.ft.budget.application.dto.CreateBudgetCommand;
import com.ft.budget.application.port.BudgetAlertRepository;
import com.ft.budget.application.port.BudgetRepository;
import com.ft.budget.application.port.MonthlyExpenseRepository;
import com.ft.budget.domain.Budget;
import com.ft.common.exception.CustomException;
import com.ft.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService 단위 테스트")
class BudgetServiceTest {
    @Mock BudgetRepository budgetRepository;
    @Mock BudgetAlertRepository budgetAlertRepository;
    @Mock MonthlyExpenseRepository monthlyExpenseRepository;

    @InjectMocks BudgetService budgetService;

    private static final YearMonth THIS_MONTH = YearMonth.of(2026, 4);

    private Budget budget(Long userId) {
        return Budget.create(userId, 5L, THIS_MONTH, new BigDecimal("100000"));
    }

    @Nested
    @DisplayName("예산 생성")
    class Create {
        @Test
        @DisplayName("성공 - 중복이 없으면 예산을 저장한다")
        void create_whenNoDuplicate_returnsBudgetResult() {
            // given
            CreateBudgetCommand command = new CreateBudgetCommand(5L, THIS_MONTH, new BigDecimal("100000"));
            Budget saved = budget(1L);
            given(budgetRepository.findByUserIdAndCategoryIdAndYearMonth(1L, 5L, "2026-04"))
                    .willReturn(Optional.empty());
            given(budgetRepository.save(any(Budget.class))).willReturn(saved);

            // when
            BudgetResult result = budgetService.create(1L, command);

            // then
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("실패 - 동일 월에 같은 카테고리 예산이 이미 존재하면 예외 발생")
        void create_whenDuplicateBudget_throwsCustomException() {
            // given
            CreateBudgetCommand command = new CreateBudgetCommand(5L, THIS_MONTH, new BigDecimal("100000"));
            given(budgetRepository.findByUserIdAndCategoryIdAndYearMonth(1L, 5L, "2026-04"))
                    .willReturn(Optional.of(budget(1L)));

            // when & then
            assertThatThrownBy(() -> budgetService.create(1L, command))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BUDGET_DUPLICATE));
        }
    }

    @Nested
    @DisplayName("예산 목록 조회")
    class FindAll {
        @Test
        @DisplayName("성공 - 해당 월의 예산 목록을 반환한다")
        void findAll_whenBudgetsExist_returnsBudgetsForMonth() {
            // given
            given(budgetRepository.findAllByUserIdAndYearMonth(1L, "2026-04"))
                    .willReturn(List.of(budget(1L), budget(1L)));
            given(monthlyExpenseRepository.findByUserIdAndCategoryIdAndYearMonth(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(budgetAlertRepository.findSentAlertTypesByBudgetId(any())).willReturn(List.of());

            // when
            List<BudgetResult> results = budgetService.findAll(1L, THIS_MONTH);

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("예산 단건 조회")
    class FindById {
        @Test
        @DisplayName("성공 - 본인의 예산이면 결과를 반환한다")
        void findById_whenValidOwner_returnsBudgetResult() {
            // given
            Budget b = budget(1L);
            given(budgetRepository.findById(10L)).willReturn(Optional.of(b));
            given(monthlyExpenseRepository.findByUserIdAndCategoryIdAndYearMonth(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(budgetAlertRepository.findSentAlertTypesByBudgetId(any())).willReturn(List.of());

            // when
            BudgetResult result = budgetService.findById(1L, 10L);

            // then
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 예산이면 예외 발생")
        void findById_whenWrongOwner_throwsCustomException() {
            // given
            given(budgetRepository.findById(10L)).willReturn(Optional.of(budget(1L)));

            // when & then
            assertThatThrownBy(() -> budgetService.findById(999L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BUDGET_NO_ACCESS));
        }

        @Test
        @DisplayName("실패 - 예산이 존재하지 않으면 예외 발생")
        void findById_whenNotFound_throwsCustomException() {
            // given
            given(budgetRepository.findById(10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> budgetService.findById(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BUDGET_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("예산 금액 수정")
    class UpdateAmount {
        @Test
        @DisplayName("성공 - 유효한 소유자이면 금액이 수정된다")
        void updateAmount_whenValidOwner_updatesAmount() {
            // given
            Budget b = budget(1L);
            given(budgetRepository.findById(10L)).willReturn(Optional.of(b));
            given(monthlyExpenseRepository.findByUserIdAndCategoryIdAndYearMonth(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(budgetAlertRepository.findSentAlertTypesByBudgetId(any())).willReturn(List.of());

            // when
            BudgetResult result = budgetService.updateAmount(1L, 10L, new BigDecimal("200000"));

            // then
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("200000"));
        }
    }

    @Nested
    @DisplayName("예산 삭제")
    class Delete {
        @Test
        @DisplayName("성공 - 유효한 소유자이면 삭제를 호출한다")
        void delete_whenValidOwner_deletesBudget() {
            // given
            Budget b = budget(1L);
            given(budgetRepository.findById(10L)).willReturn(Optional.of(b));

            // when
            budgetService.delete(1L, 10L);

            // then
            then(budgetRepository).should().delete(b);
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 예산이면 예외 발생")
        void delete_whenWrongOwner_throwsCustomException() {
            // given
            given(budgetRepository.findById(10L)).willReturn(Optional.of(budget(1L)));

            // when & then
            assertThatThrownBy(() -> budgetService.delete(999L, 10L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BUDGET_NO_ACCESS));
        }
    }
}
