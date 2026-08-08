package com.nilambar.erp.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "erp.otp.sender", havingValue = "logging", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String mobile, String code) {
        log.info("=== DEV OTP === mobile={} code={} ===", mobile, code);
    }

    @Override
    public String channel() {
        return "LOG";
    }
}
