package com.ft.budget.kafka;

import com.ft.budget.application.BudgetAlertEventPublisher;
import com.ft.common.event.BudgetAlertEvent;
import com.ft.common.kafka.KafkaTopic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Kafka 토픽 발행 로그 테스트 — budget-service
 *
 * 실제 Kafka 브로커 없이 KafkaTemplate을 Mock 처리하고,
 * ArgumentCaptor로 실제 발행될 토픽명과 페이로드를 캡처하여 콘솔에 출력한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka 토픽 발행 로그 테스트 - budget-service")
class KafkaTopicLogTest {
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;
    BudgetAlertEventPublisher publisher;

    @BeforeEach
    void setUp() {
        given(kafkaTemplate.send(anyString(), any())).willReturn(null);
        publisher = new BudgetAlertEventPublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("성공 - WARNING_50 이벤트 발행 시 budget.alert 토픽으로 전송된다")
    void publish_whenWarning50_sendsToBudgetAlertTopic() {
        // given
        BudgetAlertEvent event = new BudgetAlertEvent(
                "evt-001", 1L, 10L, 5L, "식비",
                "WARNING_50",
                new BigDecimal("50000"), new BigDecimal("100000"),
                new BigDecimal("50.00"), "2026-04"
        );

        // when
        publisher.publish(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(kafkaTemplate).should().send(topicCaptor.capture(), payloadCaptor.capture());

        BudgetAlertEvent captured = (BudgetAlertEvent) payloadCaptor.getValue();

        log.info("┌─────────────────────────────────────────────────");
        log.info("│ [KAFKA-LOG] budget-service → PUBLISH");
        log.info("│ topic       : {}", topicCaptor.getValue());
        log.info("│ alertType   : {}", captured.alertType());
        log.info("│ spentAmount : {}", captured.spentAmount());
        log.info("│ limitAmount : {}", captured.limitAmount());
        log.info("│ usageRate   : {}%", captured.usageRate());
        log.info("└─────────────────────────────────────────────────");

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopic.BUDGET_ALERT);
        assertThat(captured.alertType()).isEqualTo("WARNING_50");
    }

    @Test
    @DisplayName("성공 - EXCEEDED_100 이벤트 발행 시 budget.alert 토픽으로 전송된다")
    void publish_whenExceeded100_sendsToBudgetAlertTopic() {
        // given
        BudgetAlertEvent event = new BudgetAlertEvent(
                "evt-002", 1L, 10L, 5L, "식비",
                "EXCEEDED_100",
                new BigDecimal("120000"), new BigDecimal("100000"),
                new BigDecimal("120.00"), "2026-04"
        );

        // when
        publisher.publish(event);

        // then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(kafkaTemplate).should().send(topicCaptor.capture(), payloadCaptor.capture());

        BudgetAlertEvent captured = (BudgetAlertEvent) payloadCaptor.getValue();

        log.info("┌─────────────────────────────────────────────────");
        log.info("│ [KAFKA-LOG] budget-service → PUBLISH");
        log.info("│ topic       : {}", topicCaptor.getValue());
        log.info("│ alertType   : {}", captured.alertType());
        log.info("│ usageRate   : {}%", captured.usageRate());
        log.info("└─────────────────────────────────────────────────");

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopic.BUDGET_ALERT);
        assertThat(captured.alertType()).isEqualTo("EXCEEDED_100");
    }
}
