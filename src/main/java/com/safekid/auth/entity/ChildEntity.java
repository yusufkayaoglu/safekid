package com.safekid.auth.entity;

import com.safekid.auth.util.IdGenerator;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cocuk")
@Data
public class ChildEntity {

    @Id
    @Column(name = "cocuk_unique_id", length = 26, nullable = false)
    private String cocukUniqueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebeveyn_unique_id", nullable = false)
    private ParentEntity parent;

    @Column(nullable = false) private String cocukAdi;
    @Column(nullable = false) private String cocukSoyadi;

    private String cocukTelefonNo;
    private String cocukMail;

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optime DESC")
    private List<ChildLocationEntity> cocukKonumlar = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (cocukUniqueId == null) cocukUniqueId = IdGenerator.newId();
    }
}
