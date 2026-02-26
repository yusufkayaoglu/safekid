package com.safekid.geofence.entity;

import com.safekid.parent.entity.ChildEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "alan_ihlal_bildirimi")
public class GeofenceAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cocuk_unique_id", nullable = false)
    private ChildEntity child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "geofence_id", nullable = false)
    private GeofenceEntity geofence;

    /** Snapshot: alan silinse bile uyarı okunabilsin. */
    @Column(name = "alan_adi", nullable = false, length = 100)
    private String alanAdi;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false)
    private Instant zaman;

    @Column(nullable = false)
    private boolean okundu = false;

    @PrePersist
    void prePersist() {
        if (zaman == null) zaman = Instant.now();
    }
}
