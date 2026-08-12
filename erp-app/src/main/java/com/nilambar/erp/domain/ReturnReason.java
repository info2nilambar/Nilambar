package com.nilambar.erp.domain;

public enum ReturnReason {
    DAMAGED("Arrived damaged"),
    WRONG_ITEM("Wrong item delivered"),
    NOT_AS_DESCRIBED("Not as described"),
    NO_LONGER_NEEDED("No longer needed"),
    OTHER("Other");

    private final String label;

    ReturnReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
