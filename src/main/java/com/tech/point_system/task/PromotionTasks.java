package com.tech.point_system.task;

import com.tech.point_system.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionTasks {
    private final PromotionRepository promotionRepository;

    @Scheduled(cron = "0 0 * * * ?")
    public void deleteExpiredPromotions(){

    }
}
