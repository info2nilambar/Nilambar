package com.nilambar.erp.dto.dashboard;

public record AlertItem(String category, String message, KpiStatus status) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getCategory() {
        return category;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getMessage() {
        return message;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public KpiStatus getStatus() {
        return status;
    }
}
