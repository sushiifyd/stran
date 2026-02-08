package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEvent;
import com.example.stran.dto.inventory.InventoryEventBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens on the {@code rate-recomm-prd} MSK topic
 * for hotel inventory availability events.
 *
 * <p>Each event is validated and forwarded to the
 * {@link InventoryEventProcessingService} orchestrator for matching
 * and notification dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryEventProcessingService processingService;

    @KafkaListener(
            topics = "${kafka.topic.inventory}",
            containerFactory = "inventoryEventListenerContainerFactory"
    )
    public void onInventoryEvent(ConsumerRecord<String, InventoryEvent> record) {
        InventoryEvent event = record.value();

        if (event == null || event.getValue() == null || event.getValue().getBody() == null) {
            log.warn("Received null or malformed inventory event at offset={}, partition={}",
                    record.offset(), record.partition());
            return;
        }

        InventoryEventBody body = event.getValue().getBody();

        log.info("Received inventory event: propCode={}, date={}, key={}, partition={}, offset={}",
                body.getPropCode(), body.getStartDate(), record.key(),
                record.partition(), record.offset());

        processingService.process(body);
    }
}
