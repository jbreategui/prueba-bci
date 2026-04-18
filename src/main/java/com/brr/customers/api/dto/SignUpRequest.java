package com.brr.customers.api.dto;

import com.brr.customers.api.domain.Phone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SignUpRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank  @Pattern(regexp = "${app.password.regex}") String password,
        @NotEmpty @Valid List <Phone> phones
) {
}
