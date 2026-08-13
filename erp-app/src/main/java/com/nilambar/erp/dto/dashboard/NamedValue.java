package com.nilambar.erp.dto.dashboard;

public record NamedValue(String name, double value, String display) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getName() {
        return name;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public double getValue() {
        return value;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getDisplay() {
        return display;
    }
}
