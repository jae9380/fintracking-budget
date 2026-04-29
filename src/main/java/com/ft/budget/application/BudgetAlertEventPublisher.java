package com.ft.budget.application;

import com.ft.common.event.BudgetAlertEvent;
import com.ft.common.kafka.AbstractEventPublisher;
import com.ft.common.kafka.KafkaTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BudgetAlertEventPublisher extends AbstractEventPublisher<BudgetAlertEvent> {

    public BudgetAlertEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    @Override
    public String topic() {
        return KafkaTopic.BUDGET_ALERT;
    }
}
