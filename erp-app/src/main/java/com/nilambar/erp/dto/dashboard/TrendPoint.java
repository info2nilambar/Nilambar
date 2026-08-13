package com.nilambar.erp.dto.dashboard;

public record TrendPoint(String label, double value) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getLabel() {
        return label;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public double getValue() {
        return value;
    }
}
