package com.ft.budget.infrastructure.persistence;

import com.ft.budget.domain.Budget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaBudgetRepository 테스트")
class JpaBudgetRepositoryTest {
    @Autowired
    JpaBudgetRepository jpaBudgetRepository;

    @Nested
    @DisplayName("유저/카테고리/월로 예산 조회")
    class FindByUserIdAndCategoryIdAndYearMonth {
        @Test
        @DisplayName("성공 - 존재하는 예산을 반환한다")
        void findByUserIdAndCategoryIdAndYearMonth_whenExists_returnsBudget() {
            // given
            Budget budget = Budget.create(1L, 5L, YearMonth.of(2026, 4), new BigDecimal("100000"));
            jpaBudgetRepository.save(budget);

            // when
            Optional<Budget> result = jpaBudgetRepository.findByUserIdAndCategoryIdAndYearMonth(1L, 5L, "2026-04");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("성공 - 존재하지 않으면 empty를 반환한다")
        void findByUserIdAndCategoryIdAndYearMonth_whenNotExists_returnsEmpty() {
            // when
            Optional<Budget> result = jpaBudgetRepository.findByUserIdAndCategoryIdAndYearMonth(1L, 999L, "2026-04");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("유저/월로 예산 목록 조회")
    class FindAllByUserIdAndYearMonth {
        @Test
        @DisplayName("성공 - 해당 유저와 월에 맞는 예산만 반환한다")
        void findAllByUserIdAndYearMonth_whenBudgetsExist_returnsMatchingBudgets() {
            // given
            jpaBudgetRepository.saveAll(List.of(
                    Budget.create(1L, 5L, YearMonth.of(2026, 4), new BigDecimal("100000")),
                    Budget.create(1L, 6L, YearMonth.of(2026, 4), new BigDecimal("50000")),
                    Budget.create(1L, 7L, YearMonth.of(2026, 3), new BigDecimal("80000")),  // 다른 월
                    Budget.create(2L, 5L, YearMonth.of(2026, 4), new BigDecimal("60000"))   // 다른 유저
            ));

            // when
            List<Budget> results = jpaBudgetRepository.findAllByUserIdAndYearMonth(1L, "2026-04");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(b -> b.getUserId().equals(1L) && b.getYearMonth().equals("2026-04"));
        }

        @Test
        @DisplayName("성공 - 해당하는 예산이 없으면 빈 목록을 반환한다")
        void findAllByUserIdAndYearMonth_whenNoMatch_returnsEmptyList() {
            // when
            List<Budget> results = jpaBudgetRepository.findAllByUserIdAndYearMonth(999L, "2026-04");

            // then
            assertThat(results).isEmpty();
        }
    }
}
