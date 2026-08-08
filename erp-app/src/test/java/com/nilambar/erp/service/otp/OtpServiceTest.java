package com.nilambar.erp.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.OtpCode;
import com.nilambar.erp.domain.OtpRequestLog;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OtpCodeRepository;
import com.nilambar.erp.repository.OtpRequestLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class OtpServiceTest {

    private static final String MOBILE = "9876543210";

    private OtpCodeRepository otpCodeRepository;
    private OtpRequestLogRepository otpRequestLogRepository;
    private RecordingOtpSender otpSender;
    private ErpProperties properties;
    private Instant now;
    private OtpService otpService;
    private OtpCode saved;

    @BeforeEach
    void setUp() {
        otpCodeRepository = mock(OtpCodeRepository.class);
        otpRequestLogRepository = mock(OtpRequestLogRepository.class);
        otpSender = new RecordingOtpSender();
        properties = new ErpProperties();
        now = Instant.parse("2026-01-01T10:00:00Z");

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(invocation -> {
            saved = invocation.getArgument(0);
            return saved;
        });
        when(otpRequestLogRepository.save(any(OtpRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpCodeRepository.findFirstByMobileAndConsumedFalseOrderByIdDesc(MOBILE))
                .thenAnswer(invocation -> Optional.ofNullable(saved).filter(code -> !code.isConsumed()));

        otpService = new OtpService(otpCodeRepository, otpRequestLogRepository, passwordEncoder, otpSender,
                mock(EventPublisher.class), properties, clock);
    }

    @Test
    void requestStoresHashedCodeAndDispatchesIt() {
        otpService.requestOtp(MOBILE);

        assertThat(otpSender.lastCode).hasSize(6).containsOnlyDigits();
        assertThat(saved.getCodeHash()).isNotEqualTo(otpSender.lastCode);
        assertThat(saved.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
        verify(otpCodeRepository).consumeAllFor(MOBILE);
    }

    @Test
    void correctCodeVerifiesAndIsConsumed() {
        otpService.requestOtp(MOBILE);

        assertThatCode(() -> otpService.verify(MOBILE, otpSender.lastCode)).doesNotThrowAnyException();
        assertThat(saved.isConsumed()).isTrue();
    }

    @Test
    void codeCannotBeReused() {
        otpService.requestOtp(MOBILE);
        otpService.verify(MOBILE, otpSender.lastCode);

        assertThatThrownBy(() -> otpService.verify(MOBILE, otpSender.lastCode))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("No active OTP");
    }

    @Test
    void wrongCodeCountsAttemptsAndLocksAfterTheLimit() {
        otpService.requestOtp(MOBILE);

        for (int attempt = 1; attempt < 5; attempt++) {
            int remaining = 5 - attempt;
            assertThatThrownBy(() -> otpService.verify(MOBILE, "000000"))
                    .isInstanceOf(OtpVerificationException.class)
                    .hasMessageContaining(remaining + " attempt(s) remaining");
        }

        assertThatThrownBy(() -> otpService.verify(MOBILE, "000000"))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("Too many incorrect attempts");
        assertThat(saved.isConsumed()).isTrue();
    }

    @Test
    void expiredCodeIsRejected() {
        otpService.requestOtp(MOBILE);
        saved.setExpiresAt(now.minusSeconds(1));

        assertThatThrownBy(() -> otpService.verify(MOBILE, otpSender.lastCode))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rateLimitBlocksTheFourthRequestInTheWindow() {
        when(otpRequestLogRepository.countByMobileAndRequestedAtAfter(eq(MOBILE), any(Instant.class))).thenReturn(3L);

        assertThatThrownBy(() -> otpService.requestOtp(MOBILE))
                .isInstanceOf(OtpRateLimitException.class)
                .hasMessageContaining("Too many OTP requests");
    }

    @Test
    void otpLengthFollowsConfiguration() {
        properties.getOtp().setLength(4);

        otpService.requestOtp(MOBILE);

        assertThat(otpSender.lastCode).hasSize(4);
    }

    private static class RecordingOtpSender implements OtpSender {
        private String lastCode;

        @Override
        public void send(String mobile, String code) {
            this.lastCode = code;
        }

        @Override
        public String channel() {
            return "TEST";
        }
    }
}
