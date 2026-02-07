package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.entity.Property;
import com.example.stran.entity.StaySubscription;
import com.example.stran.repository.PropertyRepository;
import com.example.stran.repository.StaySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Matching strategy for PROPERTY-type subscriptions.
 *
 * <p>Resolves the event's propCode to a propertyId, queries for active
 * PROPERTY subscriptions matching that property and check-in date,
 * then filters by length-of-stay pattern availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyAvailabilityMatchingService implements SubscriptionMatchingStrategy {

    private final PropertyRepository propertyRepository;
    private final StaySubscriptionRepository subscriptionRepository;

    @Override
    public boolean supports(InventoryEventBody eventBody) {
        // This strategy handles all events — it looks up PROPERTY-type subscriptions.
        // Phase 2 will add an AreaAvailabilityMatchingService alongside this.
        return eventBody != null && eventBody.getPropCode() != null;
    }

    @Override
    public List<StaySubscription> findMatchingSubscriptions(InventoryEventBody eventBody) {
        String propCode = eventBody.getPropCode();
        LocalDate eventDate = LocalDate.parse(eventBody.getStartDate());

        // Step 1: Resolve propCode → propertyId
        Optional<Property> propertyOpt = propertyRepository.findByPropCode(propCode);
        if (propertyOpt.isEmpty()) {
            log.debug("No property found for propCode={}, skipping", propCode);
            return Collections.emptyList();
        }

        Long propertyId = propertyOpt.get().getPropertyId();

        // Step 2: Query active PROPERTY subscriptions for this property + date
        List<StaySubscription> candidates = subscriptionRepository
                .findActivePropertySubscriptions(propertyId, eventDate);

        if (candidates.isEmpty()) {
            log.debug("No active subscriptions for propertyId={} on date={}", propertyId, eventDate);
            return Collections.emptyList();
        }

        // Step 3: Filter by length-of-stay pattern
        List<StaySubscription> matched = candidates.stream()
                .filter(sub -> {
                    int nights = sub.getNights();
                    boolean available = LengthOfStayPatternUtil.isAvailableForNights(
                            eventBody.getRoomRecommendations(), nights);
                    if (!available) {
                        log.debug("Subscription id={} wants {} nights but not available at propCode={}",
                                sub.getId(), nights, propCode);
                    }
                    return available;
                })
                .collect(Collectors.toList());

        log.info("Matched {} subscriptions for propCode={} on date={} (out of {} candidates)",
                matched.size(), propCode, eventDate, candidates.size());

        return matched;
    }
}
