package com.safekid.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "belirlenen_alan")
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebeveyn_unique_id", nullable = false)
    private ParentEntity parent;

    @Column(nullable = false) private Double latitude;
    @Column(nullable = false) private Double longitude;

    @Column(nullable = false)
    private Double determinedDistanceForArea; // metre

    @Column(nullable = false) private Instant optime;

    @PrePersist
    void prePersist() { if (optime == null) optime = Instant.now(); }
}
