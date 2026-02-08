package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.dto.inventory.RoomRecommendation;
import com.example.stran.entity.Property;
import com.example.stran.entity.StaySubscription;
import com.example.stran.entity.SubscriptionSearchType;
import com.example.stran.entity.SubscriptionStatus;
import com.example.stran.repository.PropertyRepository;
import com.example.stran.repository.StaySubscriptionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyAvailabilityMatchingServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private StaySubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PropertyAvailabilityMatchingService service;

    private Property testProperty;
    private StaySubscription testSubscription;
    private InventoryEventBody testEventBody;

    @BeforeEach
    void setUp() {
        testProperty = new Property(1L, "Test Hotel", "FNLCOHF", 40.0, -74.0, "HF", "FNLCO",
                Instant.now(), Instant.now(), 0);

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

        RoomRecommendation room = RoomRecommendation.builder()
                .roomTypeCode("KING")
                .lengthOfStayPattern(List.of("YYYNNNN"))
                .build();

        testEventBody = InventoryEventBody.builder()
                .recommendationId(1L)
                .propCode("FNLCO")
                .startDate("2026-03-09")
                .endDate("2026-03-09")
                .ratePlanCode("NG7BCD")
                .roomRecommendations(List.of(room))
                .build();
    }

    @Test
    @DisplayName("supports returns true when propCode is present")
    void supports_returnsTrueWhenPropCodePresent() {
        assertThat(service.supports(testEventBody)).isTrue();
    }

    @Test
    @DisplayName("supports returns false when eventBody is null")
    void supports_returnsFalseWhenNull() {
        assertThat(service.supports(null)).isFalse();
    }

    @Test
    @DisplayName("supports returns false when propCode is null")
    void supports_returnsFalseWhenPropCodeNull() {
        testEventBody.setPropCode(null);
        assertThat(service.supports(testEventBody)).isFalse();
    }

    @Test
    @DisplayName("returns matching subscriptions when property exists and LOS pattern matches")
    void findMatchingSubscriptions_returnsMatches() {
        when(propertyRepository.findByPropCode("FNLCO")).thenReturn(Optional.of(testProperty));
        when(subscriptionRepository.findActivePropertySubscriptions(1L, LocalDate.of(2026, 3, 9)))
                .thenReturn(List.of(testSubscription));

        List<StaySubscription> result = service.findMatchingSubscriptions(testEventBody);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("returns empty when property not found for propCode")
    void findMatchingSubscriptions_returnsEmptyWhenPropertyNotFound() {
        when(propertyRepository.findByPropCode("FNLCO")).thenReturn(Optional.empty());

        List<StaySubscription> result = service.findMatchingSubscriptions(testEventBody);

        assertThat(result).isEmpty();
        verify(subscriptionRepository, never()).findActivePropertySubscriptions(any(), any());
    }

    @Test
    @DisplayName("returns empty when no active subscriptions exist")
    void findMatchingSubscriptions_returnsEmptyWhenNoSubscriptions() {
        when(propertyRepository.findByPropCode("FNLCO")).thenReturn(Optional.of(testProperty));
        when(subscriptionRepository.findActivePropertySubscriptions(1L, LocalDate.of(2026, 3, 9)))
                .thenReturn(List.of());

        List<StaySubscription> result = service.findMatchingSubscriptions(testEventBody);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("filters out subscriptions where LOS pattern does not match")
    void findMatchingSubscriptions_filtersOutNonMatchingLOS() {
        // Subscription wants 5 nights but pattern only supports 1-3
        testSubscription.setCheckOutDate(LocalDate.of(2026, 3, 14)); // 5 nights

        when(propertyRepository.findByPropCode("FNLCO")).thenReturn(Optional.of(testProperty));
        when(subscriptionRepository.findActivePropertySubscriptions(1L, LocalDate.of(2026, 3, 9)))
                .thenReturn(List.of(testSubscription));

        List<StaySubscription> result = service.findMatchingSubscriptions(testEventBody);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("matches multiple subscriptions with different night counts")
    void findMatchingSubscriptions_matchesMultipleSubscriptions() {
        StaySubscription sub2 = new StaySubscription();
        sub2.setId(101L);
        sub2.setGuestId("guest-456");
        sub2.setSearchType(SubscriptionSearchType.PROPERTY);
        sub2.setPropertyId(1L);
        sub2.setCheckInDate(LocalDate.of(2026, 3, 9));
        sub2.setCheckOutDate(LocalDate.of(2026, 3, 10)); // 1 night
        sub2.setMaxPricePerNight(BigDecimal.valueOf(150));
        sub2.setCurrencyCode("USD");
        sub2.setNumAdults(1);
        sub2.setNumRooms(1);
        sub2.setStatus(SubscriptionStatus.ACTIVE);
        sub2.setCreatedAt(Instant.now());
        sub2.setUpdatedAt(Instant.now());
        sub2.setCheckCount(0);

        when(propertyRepository.findByPropCode("FNLCO")).thenReturn(Optional.of(testProperty));
        when(subscriptionRepository.findActivePropertySubscriptions(1L, LocalDate.of(2026, 3, 9)))
                .thenReturn(List.of(testSubscription, sub2));

        List<StaySubscription> result = service.findMatchingSubscriptions(testEventBody);

        assertThat(result).hasSize(2);
    }
}
