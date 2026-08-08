package com.nilambar.erp.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder for a real SMS gateway integration. Activated with {@code erp.otp.sender=sms}.
 * The HTTP call is intentionally not implemented: no provider account is configured.
 */
@Component
@ConditionalOnProperty(name = "erp.otp.sender", havingValue = "sms")
public class SmsGatewayOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayOtpSender.class);

    @Override
    public void send(String mobile, String code) {
        log.info("Would dispatch OTP to {} via SMS gateway (stub, code not logged)", mobile);
        throw new UnsupportedOperationException(
                "SMS gateway integration is not configured. Set erp.otp.sender=logging for development.");
    }

    @Override
    public String channel() {
        return "SMS";
    }
}
