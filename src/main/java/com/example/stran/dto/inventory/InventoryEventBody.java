package com.example.stran.dto.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body of the rate-recomm-prd event containing availability recommendations.
 *
 * <p>Each event represents availability for a single property + rate plan
 * on a specific date (startDate = endDate typically).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryEventBody {

    private Long recommendationId;
    private String propCode;
    private String startDate;
    private String endDate;
    private String ratePlanCode;
    private List<RoomRecommendation> roomRecommendations;
}
