package com.safekid.ai.scheduler;

import com.safekid.ai.service.AnomalyDetectionService;
import com.safekid.auth.entity.ParentEntity;
import com.safekid.billing.entity.SubscriptionStatus;
import com.safekid.billing.repository.SubscriptionRepository;
import com.safekid.parent.entity.ChildEntity;
import com.safekid.parent.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetectionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final ChildRepository childRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void checkPremiumSubscribersAnomalies() {
        List<ParentEntity> premiumParents =
                subscriptionRepository.findParentsWithStatus(SubscriptionStatus.ACTIVE);

        log.info("Anomali tarama başladı — aktif aboneli ebeveyn sayısı: {}", premiumParents.size());

        for (ParentEntity parent : premiumParents) {
            List<ChildEntity> children =
                    childRepository.findAllByParent_EbeveynUniqueId(parent.getEbeveynUniqueId());

            for (ChildEntity child : children) {
                try {
                    anomalyDetectionService.checkAnomaly(
                            parent.getEbeveynUniqueId(),
                            child.getCocukUniqueId()
                    );
                } catch (Exception e) {
                    log.error("Anomali kontrolü başarısız — çocuk: {}, hata: {}",
                            child.getCocukUniqueId(), e.getMessage());
                }
            }
        }

        log.info("Anomali tarama tamamlandı.");
    }
}
