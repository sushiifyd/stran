package com.example.stran.service;

import com.example.stran.dto.inventory.InventoryEventBody;
import com.example.stran.entity.StaySubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrator that coordinates the full event processing pipeline:
 * matching inventory events against subscriptions and dispatching notifications.
 *
 * <ol>
 *   <li>Iterates over registered {@link SubscriptionMatchingStrategy} implementations</li>
 *   <li>Collects all matched subscriptions across strategies</li>
 *   <li>Delegates to {@link NotificationDispatchService} for notification publishing</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventProcessingService {

    private final List<SubscriptionMatchingStrategy> matchingStrategies;
    private final NotificationDispatchService notificationDispatchService;

    /**
     * Process an incoming inventory event through the full pipeline.
     *
     * @param eventBody the inventory event body to process
     */
    public void process(InventoryEventBody eventBody) {
        log.info("Processing inventory event: propCode={}, date={}",
                eventBody.getPropCode(), eventBody.getStartDate());

        List<StaySubscription> allMatches = matchingStrategies.stream()
                .filter(strategy -> strategy.supports(eventBody))
                .flatMap(strategy -> {
                    List<StaySubscription> matches = strategy.findMatchingSubscriptions(eventBody);
                    log.debug("Strategy {} found {} match(es) for propCode={}",
                            strategy.getClass().getSimpleName(), matches.size(), eventBody.getPropCode());
                    return matches.stream();
                })
                .toList();

        if (allMatches.isEmpty()) {
            log.info("No matching subscriptions for propCode={} on date={}",
                    eventBody.getPropCode(), eventBody.getStartDate());
            return;
        }

        notificationDispatchService.dispatch(allMatches, eventBody);
    }
}
