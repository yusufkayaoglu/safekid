package com.safekid.auth.dto;

public record VerifyRequest(
        String ebeveynMailAdres,
        String dogrulamaKodu
) {}
