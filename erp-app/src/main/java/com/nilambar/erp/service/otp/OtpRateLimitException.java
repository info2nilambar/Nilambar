package com.nilambar.erp.service.otp;

import com.nilambar.erp.service.BusinessException;

public class OtpRateLimitException extends BusinessException {

    public OtpRateLimitException(String message) {
        super(message);
    }
}
