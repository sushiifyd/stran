package com.example.stran.dto.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level wrapper for the rate-recomm-prd MSK topic event.
 *
 * Example key: "FNLCO::NG7BCD" (propCode::ratePlanCode)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryEvent {

    private String key;
    private InventoryEventValue value;
}
