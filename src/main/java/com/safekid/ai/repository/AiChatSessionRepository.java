package com.safekid.ai.repository;

import com.safekid.ai.entity.AiChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long> {

    List<AiChatSessionEntity> findTop20ByParentIdAndChild_CocukUniqueIdOrderByCreatedAtDesc(
            String parentId, String childId);
}
