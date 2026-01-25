package com.safekid.parent.dto;

public record ChildResponse(
        String cocukUniqueId,
        String cocukAdi,
        String cocukSoyadi,
        String cocukTelefonNo,
        String cocukMail
) {}
