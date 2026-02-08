package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.dto.inventory.RoomRecommendation;
import com.example.stran.entity.StaySubscription;
import com.example.stran.entity.SubscriptionSearchType;
import com.example.stran.entity.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventProcessingServiceTest {

    @Mock
    private SubscriptionMatchingStrategy strategy1;

    @Mock
    private SubscriptionMatchingStrategy strategy2;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    private InventoryEventProcessingService service;

    private InventoryEventBody testEventBody;
    private StaySubscription testSubscription;

    @BeforeEach
    void setUp() {
        service = new InventoryEventProcessingService(
                List.of(strategy1, strategy2), notificationDispatchService);

        testEventBody = InventoryEventBody.builder()
                .propCode("FNLCO")
                .startDate("2026-03-09")
                .endDate("2026-03-09")
                .roomRecommendations(List.of(
                        RoomRecommendation.builder()
                                .roomTypeCode("KING")
                                .lengthOfStayPattern(List.of("YYYNNNN"))
                                .build()))
                .build();

        testSubscription = new StaySubscription();
        testSubscription.setId(100L);
        testSubscription.setGuestId("guest-123");
        testSubscription.setSearchType(SubscriptionSearchType.PROPERTY);
        testSubscription.setPropertyId(1L);
        testSubscription.setCheckInDate(LocalDate.of(2026, 3, 9));
        testSubscription.setCheckOutDate(LocalDate.of(2026, 3, 12));
        testSubscription.setMaxPricePerNight(BigDecimal.valueOf(200));
        testSubscription.setCurrencyCode("USD");
        testSubscription.setNumAdults(2);
        testSubscription.setNumRooms(1);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setCreatedAt(Instant.now());
        testSubscription.setUpdatedAt(Instant.now());
        testSubscription.setCheckCount(0);
    }

    @Test
    @DisplayName("delegates to matching strategy and dispatches notifications")
    void process_matchesAndDispatches() {
        when(strategy1.supports(testEventBody)).thenReturn(true);
        when(strategy1.findMatchingSubscriptions(testEventBody)).thenReturn(List.of(testSubscription));
        when(strategy2.supports(testEventBody)).thenReturn(false);

        service.process(testEventBody);

        verify(strategy1).findMatchingSubscriptions(testEventBody);
        verify(strategy2, never()).findMatchingSubscriptions(any());
        verify(notificationDispatchService).dispatch(
                argThat(list -> list.size() == 1 && list.get(0).getId().equals(100L)),
                eq(testEventBody));
    }

    @Test
    @DisplayName("does not dispatch when no strategies match")
    void process_noDispatchWhenNoMatches() {
        when(strategy1.supports(testEventBody)).thenReturn(true);
        when(strategy1.findMatchingSubscriptions(testEventBody)).thenReturn(List.of());
        when(strategy2.supports(testEventBody)).thenReturn(true);
        when(strategy2.findMatchingSubscriptions(testEventBody)).thenReturn(List.of());

        service.process(testEventBody);

        verify(notificationDispatchService, never()).dispatch(anyList(), any());
    }

    @Test
    @DisplayName("deduplicates subscriptions matched by multiple strategies")
    void process_deduplicatesAcrossStrategies() {
        when(strategy1.supports(testEventBody)).thenReturn(true);
        when(strategy1.findMatchingSubscriptions(testEventBody)).thenReturn(List.of(testSubscription));
        when(strategy2.supports(testEventBody)).thenReturn(true);
        // Same subscription returned by both strategies
        when(strategy2.findMatchingSubscriptions(testEventBody)).thenReturn(List.of(testSubscription));

        service.process(testEventBody);

        verify(notificationDispatchService).dispatch(
                argThat(list -> list.size() == 1), // deduplicated
                eq(testEventBody));
    }

    @Test
    @DisplayName("collects matches from multiple strategies")
    void process_collectsFromMultipleStrategies() {
        StaySubscription sub2 = new StaySubscription();
        sub2.setId(101L);
        sub2.setGuestId("guest-456");
        sub2.setCheckInDate(LocalDate.of(2026, 3, 9));
        sub2.setCheckOutDate(LocalDate.of(2026, 3, 11));
        sub2.setStatus(SubscriptionStatus.ACTIVE);
        sub2.setCreatedAt(Instant.now());
        sub2.setUpdatedAt(Instant.now());
        sub2.setCheckCount(0);
        sub2.setMaxPricePerNight(BigDecimal.valueOf(150));
        sub2.setCurrencyCode("USD");
        sub2.setNumAdults(1);
        sub2.setNumRooms(1);

        when(strategy1.supports(testEventBody)).thenReturn(true);
        when(strategy1.findMatchingSubscriptions(testEventBody)).thenReturn(List.of(testSubscription));
        when(strategy2.supports(testEventBody)).thenReturn(true);
        when(strategy2.findMatchingSubscriptions(testEventBody)).thenReturn(List.of(sub2));

        service.process(testEventBody);

        verify(notificationDispatchService).dispatch(
                argThat(list -> list.size() == 2),
                eq(testEventBody));
    }

    @Test
    @DisplayName("skips unsupported strategies entirely")
    void process_skipsUnsupportedStrategies() {
        when(strategy1.supports(testEventBody)).thenReturn(false);
        when(strategy2.supports(testEventBody)).thenReturn(false);

        service.process(testEventBody);

        verify(strategy1, never()).findMatchingSubscriptions(any());
        verify(strategy2, never()).findMatchingSubscriptions(any());
        verify(notificationDispatchService, never()).dispatch(anyList(), any());
    }
}
