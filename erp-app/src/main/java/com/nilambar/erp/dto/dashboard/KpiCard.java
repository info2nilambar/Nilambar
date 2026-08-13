package com.nilambar.erp.dto.dashboard;

/**
 * A single KPI tile: headline {@code value} with a supporting {@code detail} line.
 */
public record KpiCard(String key, String label, String value, String detail, KpiStatus status) {

    public static KpiCard of(String key, String label, String value, String detail) {
        return new KpiCard(key, label, value, detail, KpiStatus.NEUTRAL);
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getKey() {
        return key;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getLabel() {
        return label;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getValue() {
        return value;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getDetail() {
        return detail;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public KpiStatus getStatus() {
        return status;
    }
}
