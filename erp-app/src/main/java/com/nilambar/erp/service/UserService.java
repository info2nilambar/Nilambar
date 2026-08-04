package com.nilambar.erp.service;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.event.UserRegisteredEvent;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ErpProperties properties;
    private final Clock clock;

    public UserService(UserRepository userRepository,
                       EventPublisher eventPublisher,
                       ErpProperties properties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public User findOrCreate(String mobile) {
        return userRepository.findByMobile(mobile).orElseGet(() -> {
            User user = new User();
            user.setMobile(mobile);
            user.setCreatedAt(clock.instant());
            User saved = userRepository.save(user);
            eventPublisher.publish(
                    properties.getKafka().getTopics().getUserRegistered(),
                    mobile,
                    new UserRegisteredEvent(UUID.randomUUID().toString(), saved.getId(), mobile,
                            saved.getCreatedAt().toString()));
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public User requireByMobile(String mobile) {
        return userRepository.findByMobile(mobile)
                .orElseThrow(() -> new BusinessException("User not found for mobile " + mobile));
    }

    @Transactional
    public User updateProfile(Long userId, String name, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.setName(name);
        user.setEmail(email);
        user.setProfileCompleted(true);
        return user;
    }
}
