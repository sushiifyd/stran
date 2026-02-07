package com.example.stran.dto.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard MSK event context/envelope metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventContext {

    private String timestamp;
    private String publisher;
    private String method;
    private String resource;
    private String messageId;
    private String operationId;
    private String userRealm;
    private String userType;
    private String keyIdentifier;
    private String applicationId;
    private String domainUserName;
}
