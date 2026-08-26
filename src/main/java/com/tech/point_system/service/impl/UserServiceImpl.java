package com.tech.point_system.service.impl;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.dto.user.UserDetailDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.UserMapper;
import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    @Value("${app.freetrial.duration-days:30}")
    private int daysOfFreeTrial;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDetailDTO getUserById(String supabaseUserId) {
        return userMapper.toDetailDTO(userById(supabaseUserId));
    }

    @Override
    @Transactional
    public UserDetailDTO enableFreeTrial(String userId){
        User user = userById(userId);
        LocalDate now = LocalDate.now();
        LocalDate endTime = now.plusDays(daysOfFreeTrial);
        user.setFreeTrialStartTime(now);
        user.setFreeTrialEndTime(endTime);
        user.setIsFreeTrialOver(false);
        user.setCurrentPlan(SubscriptionPlan.FREE_TRIAL);
        user.setPlanExpirationDate(endTime.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC));
        user = userRepository.save(user);
        return userMapper.toDetailDTO(user);
    }

    private User userById(String userId){
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
    }
}
