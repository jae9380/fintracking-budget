package com.ft.budget.presentation;

import com.ft.budget.application.BudgetService;
import com.ft.budget.presentation.dto.BudgetResponse;
import com.ft.budget.presentation.dto.CreateBudgetRequest;
import com.ft.budget.presentation.dto.UpdateBudgetRequest;
import com.ft.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;


@Tag(name = "Budget", description = "예산 API")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "예산 등록")
    @PostMapping
    public ApiResponse<BudgetResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateBudgetRequest request) {
        return ApiResponse.created(BudgetResponse.from(budgetService.create(userId, request.toCommand())));
    }

    @Operation(summary = "월별 예산 목록 조회")
    @GetMapping
    public ApiResponse<List<BudgetResponse>> findAll(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String yearMonth) {
        YearMonth ym = (yearMonth != null) ? YearMonth.parse(yearMonth) : YearMonth.now();
        List<BudgetResponse> responses = budgetService.findAll(userId, ym)
                .stream()
                .map(BudgetResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "예산 상세 조회")
    @GetMapping("/{budgetId}")
    public ApiResponse<BudgetResponse> findById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long budgetId) {
        return ApiResponse.success(BudgetResponse.from(budgetService.findById(userId, budgetId)));
    }

    @Operation(summary = "예산 금액 수정")
    @PutMapping("/{budgetId}")
    public ApiResponse<BudgetResponse> updateAmount(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) {
        return ApiResponse.success(BudgetResponse.from(budgetService.updateAmount(userId, budgetId, request.amount())));
    }

    @Operation(summary = "예산 삭제")
    @DeleteMapping("/{budgetId}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long budgetId) {
        budgetService.delete(userId, budgetId);
        return ApiResponse.noContent();
    }

}
