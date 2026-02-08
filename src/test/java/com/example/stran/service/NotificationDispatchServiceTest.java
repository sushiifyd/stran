package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.dto.inventory.RoomRecommendation;
import com.example.stran.dto.notification.NotificationMessage;
import com.example.stran.entity.StaySubscription;
import com.example.stran.entity.SubscriptionSearchType;
import com.example.stran.entity.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private NotificationDispatchService service;

    private StaySubscription testSubscription;
    private InventoryEventBody testEventBody;

    @BeforeEach
    void setUp() {
        testSubscription = new StaySubscription();
        testSubscription.setId(100L);
        testSubscription.setGuestId("guest-123");
        testSubscription.setSearchType(SubscriptionSearchType.PROPERTY);
        testSubscription.setPropertyId(1L);
        testSubscription.setCheckInDate(LocalDate.of(2026, 3, 9));
        testSubscription.setCheckOutDate(LocalDate.of(2026, 3, 12)); // 3 nights
        testSubscription.setMaxPricePerNight(BigDecimal.valueOf(200));
        testSubscription.setCurrencyCode("USD");
        testSubscription.setNumAdults(2);
        testSubscription.setNumRooms(1);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setCreatedAt(Instant.now());
        testSubscription.setUpdatedAt(Instant.now());
        testSubscription.setCheckCount(0);

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
    }

    @Test
    @DisplayName("builds and sends notifications for matched subscriptions")
    void dispatch_buildsAndSendsNotifications() {
        List<NotificationMessage> result = service.dispatch(List.of(testSubscription), testEventBody);

        assertThat(result).hasSize(1);
        NotificationMessage msg = result.get(0);
        assertThat(msg.getSubscriptionId()).isEqualTo(100L);
        assertThat(msg.getGuestId()).isEqualTo("guest-123");
        assertThat(msg.getPropCode()).isEqualTo("FNLCO");
        assertThat(msg.getCheckInDate()).isEqualTo("2026-03-09");
        assertThat(msg.getNights()).isEqualTo(3);
        assertThat(msg.getNotificationId()).isNotBlank();
        assertThat(msg.getTimestamp()).isNotNull();
        assertThat(msg.getMessage()).contains("FNLCO", "3 night(s)", "2026-03-09");

        verify(notificationProducer).send(anyList());
    }

    @Test
    @DisplayName("returns empty list and skips producer when no matches")
    void dispatch_returnsEmptyWhenNoMatches() {
        List<NotificationMessage> result = service.dispatch(Collections.emptyList(), testEventBody);

        assertThat(result).isEmpty();
        verify(notificationProducer, never()).send(anyList());
    }

    @Test
    @DisplayName("returns empty list when null subscriptions passed")
    void dispatch_returnsEmptyWhenNull() {
        List<NotificationMessage> result = service.dispatch(null, testEventBody);

        assertThat(result).isEmpty();
        verify(notificationProducer, never()).send(anyList());
    }

    @Test
    @DisplayName("dispatches multiple notifications for multiple subscriptions")
    void dispatch_handlesMultipleSubscriptions() {
        StaySubscription sub2 = new StaySubscription();
        sub2.setId(101L);
        sub2.setGuestId("guest-456");
        sub2.setSearchType(SubscriptionSearchType.PROPERTY);
        sub2.setPropertyId(1L);
        sub2.setCheckInDate(LocalDate.of(2026, 3, 9));
        sub2.setCheckOutDate(LocalDate.of(2026, 3, 11)); // 2 nights
        sub2.setMaxPricePerNight(BigDecimal.valueOf(150));
        sub2.setCurrencyCode("USD");
        sub2.setNumAdults(1);
        sub2.setNumRooms(1);
        sub2.setStatus(SubscriptionStatus.ACTIVE);
        sub2.setCreatedAt(Instant.now());
        sub2.setUpdatedAt(Instant.now());
        sub2.setCheckCount(0);

        List<NotificationMessage> result = service.dispatch(
                List.of(testSubscription, sub2), testEventBody);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(NotificationMessage::getSubscriptionId)
                .containsExactlyInAnyOrder(100L, 101L);

        verify(notificationProducer).send(argThat(list -> list.size() == 2));
    }

    @Test
    @DisplayName("each notification has a unique ID")
    void dispatch_generatesUniqueIds() {
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

        List<NotificationMessage> result = service.dispatch(
                List.of(testSubscription, sub2), testEventBody);

        assertThat(result.get(0).getNotificationId())
                .isNotEqualTo(result.get(1).getNotificationId());
    }
}
