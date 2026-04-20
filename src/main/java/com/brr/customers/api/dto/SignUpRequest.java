package com.brr.customers.api.dto;

import com.brr.customers.api.validation.ValidEmail;
import com.brr.customers.api.validation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SignUpRequest(
        @NotBlank String name,
        @NotBlank @ValidEmail String email,
        @NotBlank @ValidPassword String password,
        @NotEmpty @Valid List<PhoneRequest> phones
) {
}
