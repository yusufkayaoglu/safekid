package com.safekid.auth.repository;

import com.safekid.auth.entity.ChildEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<ChildEntity, String> {

    List<ChildEntity> findByParent_EbeveynUniqueId(String parentId);

    Optional<ChildEntity> findByCocukUniqueIdAndParent_EbeveynUniqueId(
            String cocukUniqueId,
            String parentId
    );
}
