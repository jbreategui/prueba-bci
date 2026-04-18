package com.brr.customers.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SignUpResponse(
        UUID id,
        String name,
        String email,
        List<PhoneResponse> phones,
        LocalDateTime created,
        LocalDateTime modified,
        @JsonProperty("last_login") LocalDateTime lastLogin,
        String token,
        @JsonProperty("isactive") boolean isActive
) {
}
