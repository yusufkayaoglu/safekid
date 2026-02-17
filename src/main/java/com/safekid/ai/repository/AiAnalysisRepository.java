package com.safekid.ai.repository;

import com.safekid.ai.entity.AiAnalysisEntity;
import com.safekid.ai.entity.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysisEntity, Long> {

    List<AiAnalysisEntity> findByChild_CocukUniqueIdAndAnalysisTypeAndAcknowledgedFalseOrderByCreatedAtDesc(
            String childId, AnalysisType type);

    List<AiAnalysisEntity> findByChild_Parent_EbeveynUniqueIdAndAcknowledgedFalseOrderByCreatedAtDesc(
            String parentId);
}
