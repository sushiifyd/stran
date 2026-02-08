package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEvent;
import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.dto.inventory.InventoryEventValue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private InventoryEventProcessingService processingService;

    @InjectMocks
    private InventoryEventListener listener;

    private InventoryEventBody testBody;
    private InventoryEvent testEvent;

    @BeforeEach
    void setUp() {
        testBody = InventoryEventBody.builder()
                .propCode("FNLCO")
                .startDate("2026-03-09")
                .endDate("2026-03-09")
                .build();

        InventoryEventValue value = InventoryEventValue.builder()
                .body(testBody)
                .build();

        testEvent = InventoryEvent.builder()
                .key("FNLCO::NG7BCD")
                .value(value)
                .build();
    }

    @Test
    @DisplayName("delegates valid event to processing service")
    void onInventoryEvent_delegatesToProcessingService() {
        ConsumerRecord<String, InventoryEvent> record =
                new ConsumerRecord<>("rate-recomm-prd", 0, 42L, "FNLCO::NG7BCD", testEvent);

        listener.onInventoryEvent(record);

        verify(processingService).process(testBody);
    }

    @Test
    @DisplayName("skips processing when event is null")
    void onInventoryEvent_skipsNullEvent() {
        ConsumerRecord<String, InventoryEvent> record =
                new ConsumerRecord<>("rate-recomm-prd", 0, 42L, "key", null);

        listener.onInventoryEvent(record);

        verify(processingService, never()).process(any());
    }

    @Test
    @DisplayName("skips processing when event value is null")
    void onInventoryEvent_skipsNullValue() {
        InventoryEvent event = InventoryEvent.builder().key("key").value(null).build();
        ConsumerRecord<String, InventoryEvent> record =
                new ConsumerRecord<>("rate-recomm-prd", 0, 42L, "key", event);

        listener.onInventoryEvent(record);

        verify(processingService, never()).process(any());
    }

    @Test
    @DisplayName("skips processing when event body is null")
    void onInventoryEvent_skipsNullBody() {
        InventoryEventValue value = InventoryEventValue.builder().body(null).build();
        InventoryEvent event = InventoryEvent.builder().key("key").value(value).build();
        ConsumerRecord<String, InventoryEvent> record =
                new ConsumerRecord<>("rate-recomm-prd", 0, 42L, "key", event);

        listener.onInventoryEvent(record);

        verify(processingService, never()).process(any());
    }
}
