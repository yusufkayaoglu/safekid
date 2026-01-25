package com.safekid.parent.dto;

public record ChildCreateRequest(
        String cocukAdi,
        String cocukSoyadi,
        String cocukTelefonNo,
        String cocukMail
) {}
