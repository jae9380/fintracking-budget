# fintracking-budget

예산 설정, 한도 초과 알림, Chain of Responsibility 패턴

---

## 패턴: Chain of Responsibility

```
BudgetAlertChain
  ├── Handler50  — 50% 초과 시 처리
  ├── Handler80  — 80% 초과 시 처리
  └── Handler100 — 100% 초과 시 처리
```

새 임계값 추가 시 핸들러만 추가, 기존 코드 수정 없음.

---

## Kafka 흐름

### Consumer: TransactionCreatedEvent

`BudgetAlertEventListener` — `transaction.created` 토픽 구독

```
TransactionCreatedEvent 수신
  → MonthlyExpense 누적 업데이트
  → Chain 실행 (소비율 계산)
  → 임계값 초과 시 BudgetAlertEvent 발행
```

### Producer: BudgetAlertEvent

`BudgetAlertEventPublisher` — `AbstractEventPublisher<BudgetAlertEvent>` 상속

```java
// BudgetAlertEvent (fintracking-common)
record BudgetAlertEvent(
    String eventId,
    Long userId,
    Long categoryId,
    String alertType,    // EXCEEDED_50 / EXCEEDED_80 / EXCEEDED_100
    BigDecimal budgetAmount,
    BigDecimal spentAmount,
    String yearMonth     // "2026-04"
)
```

- 토픽: `KafkaTopic.BUDGET_ALERT` (`budget.alert`)

---

## MonthlyExpense (이 서비스 전용 엔티티)

MSA 원칙: 거래 데이터는 transaction-service 소유 → budget-service는 자체 집계 테이블 유지

```java
@Entity
// UNIQUE(userId, categoryId, yearMonth)
class MonthlyExpense {
    Long userId
    Long categoryId
    String yearMonth  // "2026-04"
    BigDecimal totalAmount
}
```

- TransactionCreatedEvent 수신 시 `totalAmount += amount` 업데이트
- `EXPENSE` 타입만 누적 (INCOME/TRANSFER 무시)

---

## BudgetService 책임

- 예산 CRUD
- 월간 지출 조회 (MonthlyExpense 기반)
- 소비율 계산 및 응답

**금지**:
- TransactionRepository 직접 주입 금지
- NotificationService 직접 호출 금지 (Kafka로만 통신)

---

## 패키지 구조

```
com.ft.budget
  ├── domain/          — Budget, MonthlyExpense 엔티티, Chain 핸들러
  ├── application/     — BudgetService, BudgetAlertEventListener, BudgetAlertEventPublisher
  ├── infrastructure/  — JPA
  └── presentation/    — BudgetController, DTO
```

---

## 주요 ErrorCode

```java
BUDGET_NOT_FOUND(404, "BUDGET_001", "Budget not found")
BUDGET_ALREADY_EXISTS(409, "BUDGET_002", "Budget already exists for this period")
```
