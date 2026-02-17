package com.safekid.child.entity;

import com.safekid.parent.entity.ChildEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cocuk_konum")
public class CocukKonumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocuk_unique_id", referencedColumnName = "cocuk_unique_id", nullable = false)
    private ChildEntity child;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lng", nullable = false)
    private Double lng;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
