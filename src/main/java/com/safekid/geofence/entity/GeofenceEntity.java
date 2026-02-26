package com.safekid.geofence.entity;

import com.safekid.parent.entity.ChildEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "guvenli_alan")
public class GeofenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cocuk_unique_id", nullable = false)
    private ChildEntity child;

    /** Ebeveynin verdiği isim: "Ev", "Okul", "Park" vb. */
    @Column(name = "alan_adi", nullable = false, length = 100)
    private String alanAdi;

    /**
     * GeoJSON Polygon — örnek:
     * {"type":"Polygon","coordinates":[[[28.97,41.01],[28.98,41.01],[28.98,41.02],[28.97,41.01]]]}
     * Koordinatlar [longitude, latitude] sırasıyla saklanır (GeoJSON standardı).
     */
    @Column(name = "geo_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String geoJson;

    /** Soft-delete: false ise ebeveyn tarafından silinmiş sayılır. */
    @Column(name = "aktif", nullable = false)
    private boolean aktif = true;

    @Column(name = "olusturulma_tarihi", nullable = false, updatable = false)
    private Instant olusturmaTarihi;

    /** Son ihlal bildiriminin gönderildiği zaman. Cooldown kontrolü için DB'de saklanır. */
    @Column(name = "son_bildirim_zamani")
    private Instant sonBildirimZamani;

    @PrePersist
    void prePersist() {
        if (olusturmaTarihi == null) olusturmaTarihi = Instant.now();
    }
}
