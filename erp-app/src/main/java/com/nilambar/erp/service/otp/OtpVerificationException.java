package com.nilambar.erp.service.otp;

import com.nilambar.erp.service.BusinessException;

public class OtpVerificationException extends BusinessException {

    public OtpVerificationException(String message) {
        super(message);
    }
}
