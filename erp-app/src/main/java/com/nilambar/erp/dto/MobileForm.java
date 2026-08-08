package com.nilambar.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MobileForm {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a valid 10-digit Indian mobile number")
    private String mobile;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
