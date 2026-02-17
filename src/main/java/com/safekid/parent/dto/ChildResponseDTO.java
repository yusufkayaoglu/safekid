package com.safekid.parent.dto;

import com.safekid.parent.entity.ChildEntity;

public record ChildResponseDTO(
        String cocukUniqueId,
        String cocukAdi,
        String cocukSoyadi,
        String cocukTelefonNo,
        String cocukMail
) {
    public static ChildResponseDTO fromEntity(ChildEntity entity) {
        return new ChildResponseDTO(
                entity.getCocukUniqueId(),
                entity.getCocukAdi(),
                entity.getCocukSoyadi(),
                entity.getCocukTelefonNo(),
                entity.getCocukMail()
        );
    }
}