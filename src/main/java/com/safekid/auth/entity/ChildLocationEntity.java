package com.safekid.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "cocuk_konum")
public class ChildLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocuk_unique_id", nullable = false)
    private ChildEntity child;

    @Column(nullable = false) private Double latitude;
    @Column(nullable = false) private Double longitude;

    @Column(nullable = false) private Instant optime;

    @PrePersist
    void prePersist() {
        if (optime == null) optime = Instant.now();
    }
}
