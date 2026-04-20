package com.brr.customers.api.dto;

public record PhoneResponse(
        String number,
        String citycode,
        String contrycode
) {}
