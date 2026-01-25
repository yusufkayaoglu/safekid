package com.safekid.auth.repository;


import com.safekid.auth.entity.ParentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<ParentEntity, String> {

    // login için
    Optional<ParentEntity> findByEbeveynUserCode(String ebeveynUserCode);

    // register sırasında duplicate kontrolü
    boolean existsByEbeveynUserCode(String ebeveynUserCode);

    boolean existsByEbeveynMailAdres(String ebeveynMailAdres);

    // JWT logout / session kontrolü için (opsiyonel ama profesyonel)
    @Query("""
        select p
        from ParentEntity p
        where p.ebeveynUniqueId = :parentId
          and p.ebeveynToken = :sessionToken
    """)
    Optional<ParentEntity> findActiveSession(
            @Param("parentId") String parentId,
            @Param("sessionToken") String sessionToken
    );
}

