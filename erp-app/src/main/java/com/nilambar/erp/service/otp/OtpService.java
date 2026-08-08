package com.nilambar.erp.service.otp;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.OtpCode;
import com.nilambar.erp.domain.OtpRequestLog;
import com.nilambar.erp.event.OtpRequestedEvent;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OtpCodeRepository;
import com.nilambar.erp.repository.OtpRequestLogRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final OtpRequestLogRepository otpRequestLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final EventPublisher eventPublisher;
    private final ErpProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpCodeRepository,
                      OtpRequestLogRepository otpRequestLogRepository,
                      PasswordEncoder passwordEncoder,
                      OtpSender otpSender,
                      EventPublisher eventPublisher,
                      ErpProperties properties,
                      Clock clock) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpRequestLogRepository = otpRequestLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestOtp(String mobile) {
        ErpProperties.Otp config = properties.getOtp();
        Instant now = clock.instant();
        Instant windowStart = now.minus(Duration.ofMinutes(config.getRateLimitWindowMinutes()));

        long recentRequests = otpRequestLogRepository.countByMobileAndRequestedAtAfter(mobile, windowStart);
        if (recentRequests >= config.getMaxRequestsPerWindow()) {
            throw new OtpRateLimitException("Too many OTP requests. Please try again in %d minutes."
                    .formatted(config.getRateLimitWindowMinutes()));
        }

        String code = generateCode(config.getLength());

        otpCodeRepository.consumeAllFor(mobile);

        OtpCode otpCode = new OtpCode();
        otpCode.setMobile(mobile);
        otpCode.setCodeHash(passwordEncoder.encode(code));
        otpCode.setExpiresAt(now.plus(Duration.ofMinutes(config.getTtlMinutes())));
        otpCode.setCreatedAt(now);
        otpCodeRepository.save(otpCode);

        otpRequestLogRepository.save(new OtpRequestLog(mobile, now));

        otpSender.send(mobile, code);

        eventPublisher.publish(
                properties.getKafka().getTopics().getOtpRequested(),
                mobile,
                new OtpRequestedEvent(UUID.randomUUID().toString(), mobile, otpSender.channel(), now.toString()));
    }

    @Transactional
    public void verify(String mobile, String code) {
        ErpProperties.Otp config = properties.getOtp();
        Instant now = clock.instant();

        OtpCode otpCode = otpCodeRepository.findFirstByMobileAndConsumedFalseOrderByIdDesc(mobile)
                .orElseThrow(() -> new OtpVerificationException("No active OTP for this number. Please request a new one."));

        if (otpCode.isExpired(now)) {
            otpCode.setConsumed(true);
            throw new OtpVerificationException("This OTP has expired. Please request a new one.");
        }

        if (otpCode.getAttempts() >= config.getMaxAttempts()) {
            otpCode.setConsumed(true);
            throw new OtpVerificationException("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(code, otpCode.getCodeHash())) {
            otpCode.setAttempts(otpCode.getAttempts() + 1);
            int remaining = config.getMaxAttempts() - otpCode.getAttempts();
            if (remaining <= 0) {
                otpCode.setConsumed(true);
                throw new OtpVerificationException("Too many incorrect attempts. Please request a new OTP.");
            }
            throw new OtpVerificationException("Incorrect OTP. %d attempt(s) remaining.".formatted(remaining));
        }

        otpCode.setConsumed(true);
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
