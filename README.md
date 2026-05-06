# fintracking-budget

> 예산 설정 · 월간 지출 집계 · Chain of Responsibility 알림

---

## 사용 기술

| 분류        | 기술                               |
| ----------- | ---------------------------------- |
| 메시지      | Apache Kafka (Consumer + Producer) |
| 설정 관리   | Spring Cloud Config                |
| 서비스 등록 | Spring Cloud Eureka Client         |

---

## 핵심 설계 패턴 — Chain of Responsibility

### 문제 상황

예산 임계값이 여러 개 있고(50%, 80%, 100%), 각 임계값마다 알림을 보내야 한다.
단순 `if-else`로 작성하면 임계값이 추가될 때마다 기존 코드를 수정해야 한다.

```java
// 나쁜 예 — 새 임계값마다 코드 수정 필요
if (pct >= 100) { 알림(); }
else if (pct >= 80) { 알림(); }
else if (pct >= 50) { 알림(); }
// 70% 추가하려면? → else if 하나 더 추가, 기존 코드 건드려야 함
```

### 해결 — Chain of Responsibility 패턴

각 임계값 처리를 독립적인 핸들러로 분리하고, 체인으로 연결한다.

```
새로운 거래 발생 → BudgetAlertChain 실행
                            │
                            ▼
                    Warning50Handler      ← 50% 이상이고 아직 50% 알림 안 보냈으면 → 알림 추가
                            │
                            ▼
                    Warning80Handler      ← 80% 이상이고 아직 80% 알림 안 보냈으면 → 알림 추가
                            │
                            ▼
                    Exceeded100Handler    ← 100% 이상이고 아직 100% 알림 안 보냈으면 → 알림 추가
                            │
                            ▼
                    (다음 핸들러 없음 → 체인 종료)
```

**효과:** 70% 임계값을 추가하고 싶으면 `Warning70Handler` 하나만 만들어서 체인에 끼우면 된다.  
기존 핸들러 코드는 전혀 수정하지 않아도 된다.

```java
// BudgetAlertHandler — 추상 부모 클래스
public abstract class BudgetAlertHandler {
    private BudgetAlertHandler next; // 다음 핸들러

    // 체인 실행: 내 조건 확인 → 다음 핸들러로 전달
    public void handle(Budget budget, BigDecimal spent,
                       List<AlertType> sentAlerts, List<AlertType> result) {
        if (shouldAlert(budget, spent, sentAlerts)) {
            result.add(getAlertType()); // 이번 핸들러가 알림 추가
        }
        if (next != null) {
            next.handle(budget, spent, sentAlerts, result); // 다음으로 전달
        }
    }

    protected abstract boolean shouldAlert(...); // 자식이 조건 구현
    protected abstract AlertType getAlertType(); // 자식이 타입 반환
}
```

---

## Kafka 이벤트 처리

### Consumer — TransactionCreatedEvent 수신

거래가 생성되면 이 서비스가 이벤트를 받아 지출을 누적한다.

```
[transaction-service]
    거래 생성 후 이벤트 발행
        │
        │ topic: transaction.created
        ▼
[BudgetAlertEventListener]
        │
        │  1. type == EXPENSE 인지 확인 (수입/이체는 무시)
        │  2. MonthlyExpense 테이블에 지출 누적
        │  3. 해당 카테고리의 Budget 조회
        │  4. 소비율 계산 → Chain of Responsibility 실행
        │  5. 새로운 임계값 초과 시 BudgetAlertEvent 발행
        ▼
[budget.alert 토픽]
        │
        ▼
[notification-service]
    알림 발송
```

### Producer — BudgetAlertEvent 발행

```java
record BudgetAlertEvent(
    String eventId,      // UUID
    Long userId,
    Long categoryId,
    String alertType,    // "WARNING_50" / "WARNING_80" / "EXCEEDED_100"
    BigDecimal budgetAmount,
    BigDecimal spentAmount,
    String yearMonth     // "2026-05"
)
```

---

## MonthlyExpense — 자체 집계 테이블

**MSA 핵심 원칙**: 거래 데이터는 `transaction-service`의 소유입니다.
`budget-service`가 `transaction-service`의 DB에 직접 접근하는 것은 금지다.

대신, Kafka 이벤트로 받은 거래를 자체 집계 테이블(`monthly_expense`)에 누적한다.

```
monthly_expense 테이블
┌─────────────────────────────────────────────────────────┐
│ id  │ user_id │ category_id │ year_month │ total_amount │
│ 1   │ 42      │ 3           │ 2026-05    │ 85,000       │
│ 2   │ 42      │ 5           │ 2026-05    │ 230,000      │
└─────────────────────────────────────────────────────────┘

UNIQUE 제약: (user_id, category_id, year_month)
→ 같은 사용자·카테고리·월은 항상 하나의 행에 누적됨
```

---

## 패키지 구조

```
com.ft.budget
├── domain/
│   ├── Budget.java                    ← 예산 엔티티 (amount, yearMonth)
│   ├── MonthlyExpense.java            ← 월간 지출 집계 엔티티
│   ├── BudgetAlert.java               ← 알림 발송 이력 (중복 알림 방지)
│   ├── AlertType.java                 ← WARNING_50 / WARNING_80 / EXCEEDED_100
│   └── handler/
│       ├── BudgetAlertHandler.java    ← Chain of Responsibility 추상 클래스
│       ├── Warning50Handler.java      ← 50% 초과 핸들러
│       ├── Warning80Handler.java      ← 80% 초과 핸들러
│       └── Exceeded100Handler.java   ← 100% 초과 핸들러
│
├── application/
│   ├── BudgetService.java               ← 예산 CRUD 유스케이스
│   ├── BudgetAlertEventListener.java    ← Kafka Consumer (transaction.created)
│   ├── BudgetAlertEventPublisher.java   ← Kafka Producer (budget.alert)
│   └── port/
│       ├── BudgetRepository.java
│       ├── MonthlyExpenseRepository.java
│       └── BudgetAlertRepository.java   ← 알림 이력 저장소
│
├── infrastructure/
│   └── persistence/                    ← JPA 구현체들
│
└── presentation/
    ├── BudgetController.java
    └── dto/
        ├── CreateBudgetRequest.java
        ├── UpdateBudgetRequest.java
        └── BudgetResponse.java          ← spentAmount, 소비율(%) 포함
```

---

## API 엔드포인트

| 메서드 | 경로                                  | 설명                                  |
| ------ | ------------------------------------- | ------------------------------------- |
| GET    | `/budget-service/api/v1/budgets`      | 예산 목록 조회 (`?yearMonth=2026-05`) |
| POST   | `/budget-service/api/v1/budgets`      | 예산 등록                             |
| PUT    | `/budget-service/api/v1/budgets/{id}` | 예산 금액 수정                        |
| DELETE | `/budget-service/api/v1/budgets/{id}` | 예산 삭제                             |

---

## 알림 중복 방지

같은 임계값 알림을 한 달에 두 번 보내지 않기 위해 `BudgetAlert` 테이블에 발송 이력을 저장한다.

```
Chain 실행 시 shouldAlert() 체크:
  1. 소비율 >= 임계값?  (예: 50%)
  2. 이번 달에 이미 이 알림을 보냈는가? (BudgetAlert 테이블 확인)
  → 둘 다 조건 충족 시에만 알림 발행
```

---

## 에러 코드

| 코드                    | HTTP | 설명                              |
| ----------------------- | ---- | --------------------------------- |
| `BUDGET_NOT_FOUND`      | 404  | 예산 정보를 찾을 수 없음          |
| `BUDGET_DUPLICATE`      | 409  | 해당 월·카테고리 예산이 이미 존재 |
| `BUDGET_NO_ACCESS`      | 403  | 접근 권한 없음                    |
| `BUDGET_INVALID_AMOUNT` | 400  | 예산 금액이 0 이하                |

---

## 테스트

```
test/
├── domain/
│   ├── BudgetTest.java              ← 예산 검증 단위 테스트
│   └── BudgetAlertChainTest.java    ← Chain 동작 (50%/80%/100%) 단위 테스트
├── application/
│   └── BudgetServiceTest.java       ← 유스케이스 (Mockito)
├── infrastructure/
│   └── JpaBudgetRepositoryTest.java ← @DataJpaTest + H2
└── kafka/
    └── KafkaTopicLogTest.java
```
