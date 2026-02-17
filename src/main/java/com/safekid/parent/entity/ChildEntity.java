package com.safekid.parent.entity;

import com.safekid.auth.entity.ParentEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cocuk")
public class ChildEntity {

    @Id
    @Column(name = "cocuk_unique_id", nullable = false, length = 32)
    private String cocukUniqueId; // ULID/string

    @Column(name = "cocuk_adi", nullable = false)
    private String cocukAdi;

    @Column(name = "cocuk_soyadi", nullable = false)
    private String cocukSoyadi;

    @Column(name = "cocuk_telefon_no")
    private String cocukTelefonNo;

    @Column(name = "cocuk_mail")
    private String cocukMail;

    // Parent ilişkisi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ebeveyn_unique_id", nullable = false)
    private ParentEntity parent;
}
