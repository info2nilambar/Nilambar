package com.nilambar.erp.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "erp")
public class ErpProperties {

    private final Otp otp = new Otp();
    private final Delivery delivery = new Delivery();
    private final Catalog catalog = new Catalog();
    private final Kafka kafka = new Kafka();
    private final Payment payment = new Payment();
    private final Returns returns = new Returns();

    public Otp getOtp() {
        return otp;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public Payment getPayment() {
        return payment;
    }

    public Returns getReturns() {
        return returns;
    }

    public static class Otp {
        private int length = 6;
        private int ttlMinutes = 5;
        private int maxAttempts = 5;
        private int rateLimitWindowMinutes = 15;
        private int maxRequestsPerWindow = 3;
        private String sender = "logging";

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getRateLimitWindowMinutes() {
            return rateLimitWindowMinutes;
        }

        public void setRateLimitWindowMinutes(int rateLimitWindowMinutes) {
            this.rateLimitWindowMinutes = rateLimitWindowMinutes;
        }

        public int getMaxRequestsPerWindow() {
            return maxRequestsPerWindow;
        }

        public void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
            this.maxRequestsPerWindow = maxRequestsPerWindow;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }
    }

    public static class Delivery {
        private double radiusKm = 5;
        private double feeSlabKm = 1.5;
        private BigDecimal feePerSlab = new BigDecimal("30.00");
        private BigDecimal pickupFee = BigDecimal.ZERO;

        public double getFeeSlabKm() {
            return feeSlabKm;
        }

        public void setFeeSlabKm(double feeSlabKm) {
            this.feeSlabKm = feeSlabKm;
        }

        public BigDecimal getFeePerSlab() {
            return feePerSlab;
        }

        public void setFeePerSlab(BigDecimal feePerSlab) {
            this.feePerSlab = feePerSlab;
        }

        public double getRadiusKm() {
            return radiusKm;
        }

        public void setRadiusKm(double radiusKm) {
            this.radiusKm = radiusKm;
        }

        public BigDecimal getPickupFee() {
            return pickupFee;
        }

        public void setPickupFee(BigDecimal pickupFee) {
            this.pickupFee = pickupFee;
        }
    }

    public static class Returns {
        private int windowDays = 7;
        private boolean autoApprove = true;

        public int getWindowDays() {
            return windowDays;
        }

        public void setWindowDays(int windowDays) {
            this.windowDays = windowDays;
        }

        public boolean isAutoApprove() {
            return autoApprove;
        }

        public void setAutoApprove(boolean autoApprove) {
            this.autoApprove = autoApprove;
        }
    }

    public static class Payment {
        private String payeeName = "Nilambar ERP";
        private String accountNumber = "11540334561";
        private String ifsc = "SBIN0007021";
        private String bankName = "State Bank of India";
        private String upiId = "";

        public String getPayeeName() {
            return payeeName;
        }

        public void setPayeeName(String payeeName) {
            this.payeeName = payeeName;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getIfsc() {
            return ifsc;
        }

        public void setIfsc(String ifsc) {
            this.ifsc = ifsc;
        }

        public String getBankName() {
            return bankName;
        }

        public void setBankName(String bankName) {
            this.bankName = bankName;
        }

        public String getUpiId() {
            return upiId;
        }

        public void setUpiId(String upiId) {
            this.upiId = upiId;
        }
    }

    public static class Catalog {
        private int pageSize = 12;

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }

    public static class Kafka {
        private final Topics topics = new Topics();

        public Topics getTopics() {
            return topics;
        }

        public static class Topics {
            private String otpRequested = "erp.otp.requested";
            private String userRegistered = "erp.user.registered";
            private String orderPlaced = "erp.order.placed";
            private String returnRequested = "erp.order.return.requested";

            public String getReturnRequested() {
                return returnRequested;
            }

            public void setReturnRequested(String returnRequested) {
                this.returnRequested = returnRequested;
            }

            public String getOtpRequested() {
                return otpRequested;
            }

            public void setOtpRequested(String otpRequested) {
                this.otpRequested = otpRequested;
            }

            public String getUserRegistered() {
                return userRegistered;
            }

            public void setUserRegistered(String userRegistered) {
                this.userRegistered = userRegistered;
            }

            public String getOrderPlaced() {
                return orderPlaced;
            }

            public void setOrderPlaced(String orderPlaced) {
                this.orderPlaced = orderPlaced;
            }
        }
    }
}
