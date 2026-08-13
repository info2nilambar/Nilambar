package com.nilambar.erp.dto.dashboard;

import java.util.List;

public record KpiGroup(String key, String title, String icon, List<KpiCard> cards) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getKey() {
        return key;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getTitle() {
        return title;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getIcon() {
        return icon;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<KpiCard> getCards() {
        return cards;
    }
}
