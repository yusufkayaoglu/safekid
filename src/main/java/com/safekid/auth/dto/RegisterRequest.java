package com.safekid.auth.dto;

public record RegisterRequest(
        String ebeveynAdi,
        String ebeveynSoyadi,
        String ebeveynUserCode,
        String ebeveynPassword,
        String ebeveynMailAdres,
        String ebeveynAdres,
        String ebeveynIsAdresi
) {}
