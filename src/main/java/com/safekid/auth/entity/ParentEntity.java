package com.safekid.auth.entity;

import com.safekid.auth.util.IdGenerator;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ebeveyn")
@Data
public class ParentEntity {

    @Id
    @Column(name = "ebeveyn_unique_id", length = 26, nullable = false)
    private String ebeveynUniqueId;

    @Column(nullable = false) private String ebeveynAdi;
    @Column(nullable = false) private String ebeveynSoyadi;

    @Column(nullable = false) private String ebeveynPassword;

    @Column(nullable = false, unique = true)
    private String ebeveynUserCode;

    @Column(unique = true) private String ebeveynMailAdres;

    private String ebeveynTelefonNumarasi;
    private String ebeveynEvTelefonNumarasi;

    @Column(length = 2048) private String ebeveynToken; // opsiyonel
    private String ebeveynAdres;
    private String ebeveynIsAdresi;

    @Column(unique = true) private String ebeveynUniqueQRCode;

    private Instant userOptimeForRegistration;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AreaEntity> belirlenenAlanlar = new ArrayList<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildEntity> cocukBilgileri = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (ebeveynUniqueId == null) ebeveynUniqueId = IdGenerator.newId();
        if (userOptimeForRegistration == null) userOptimeForRegistration = Instant.now();
        if (ebeveynUniqueQRCode == null) ebeveynUniqueQRCode = IdGenerator.newId(); // veya ayrı QR formatı
    }
}
