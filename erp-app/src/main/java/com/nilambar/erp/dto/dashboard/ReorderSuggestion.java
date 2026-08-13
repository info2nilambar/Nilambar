package com.nilambar.erp.dto.dashboard;

public record ReorderSuggestion(String sku,
                                String productName,
                                int stockOnHand,
                                int reorderLevel,
                                double dailyDemand,
                                int recommendedQuantity,
                                int daysOfCoverLeft) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getSku() {
        return sku;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getProductName() {
        return productName;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public int getStockOnHand() {
        return stockOnHand;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public int getReorderLevel() {
        return reorderLevel;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public double getDailyDemand() {
        return dailyDemand;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public int getRecommendedQuantity() {
        return recommendedQuantity;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public int getDaysOfCoverLeft() {
        return daysOfCoverLeft;
    }
}
