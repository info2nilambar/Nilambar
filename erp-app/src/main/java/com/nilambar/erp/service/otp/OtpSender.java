package com.nilambar.erp.service.otp;

public interface OtpSender {

    void send(String mobile, String code);

    String channel();
}
