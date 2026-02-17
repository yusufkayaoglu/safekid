package com.safekid.parent.repository;

import com.safekid.parent.entity.ChildEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<ChildEntity, String> {

    List<ChildEntity> findAllByParent_EbeveynUniqueId(String parentId);

    Optional<ChildEntity> findByCocukUniqueIdAndParent_EbeveynUniqueId(String childId, String parentId);

    boolean existsByCocukTelefonNoAndParent_EbeveynUniqueId(String phone, String parentId);

    Optional<ChildEntity> findByCocukUniqueId(String cocukUniqueId);

}
