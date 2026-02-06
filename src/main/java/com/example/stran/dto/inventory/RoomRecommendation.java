package com.example.stran.dto.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single room type recommendation within an inventory event.
 *
 * <p>{@code lengthOfStayPattern} is a list of 7-character strings where each
 * position (1-7) represents availability for that number of nights:
 * <ul>
 *   <li>Position 1 = 1-night stay</li>
 *   <li>Position 2 = 2-night stay</li>
 *   <li>...</li>
 *   <li>Position 7 = 7-night stay</li>
 * </ul>
 * {@code 'Y'} = available, {@code 'N'} = not available.
 *
 * <p>Example: {@code "YYYNNNN"} → available for 1, 2, or 3 nights.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomRecommendation {

    private String roomTypeCode;
    private List<String> lengthOfStayPattern;
}
