package com.nilambar.erp.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "erp")
public class ErpProperties {

    private final Otp otp = new Otp();
    private final Delivery delivery = new Delivery();
    private final Catalog catalog = new Catalog();
    private final Kafka kafka = new Kafka();

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
        private BigDecimal fee = BigDecimal.ZERO;
        private BigDecimal pickupFee = BigDecimal.ZERO;

        public double getRadiusKm() {
            return radiusKm;
        }

        public void setRadiusKm(double radiusKm) {
            this.radiusKm = radiusKm;
        }

        public BigDecimal getFee() {
            return fee;
        }

        public void setFee(BigDecimal fee) {
            this.fee = fee;
        }

        public BigDecimal getPickupFee() {
            return pickupFee;
        }

        public void setPickupFee(BigDecimal pickupFee) {
            this.pickupFee = pickupFee;
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
