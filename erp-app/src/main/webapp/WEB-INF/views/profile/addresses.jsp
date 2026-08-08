<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Addresses"/>
<%@ include file="../fragments/header.jspf" %>

<h1>Saved addresses</h1>
<p class="subtitle">The default address is pre-selected at checkout.</p>

<div class="columns">
    <div>
        <c:choose>
            <c:when test="${empty addresses}">
                <div class="card"><p class="muted">No addresses saved yet.</p></div>
            </c:when>
            <c:otherwise>
                <c:forEach var="address" items="${addresses}">
                    <div class="card" style="margin-bottom: 12px;">
                        <strong>${empty address.label ? 'Address' : address.label}</strong>
                        <c:if test="${address.defaultAddress}"><span class="status">Default</span></c:if>
                        <p>${address.summary}</p>
                        <p class="muted">Lat ${address.latitude}, Lng ${address.longitude}</p>
                        <c:if test="${not address.defaultAddress}">
                            <form method="post" action="${ctx}/profile/addresses/${address.id}/default" class="inline">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="secondary">Make default</button>
                            </form>
                            <form method="post" action="${ctx}/profile/addresses/${address.id}/delete" class="inline">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="danger">Delete</button>
                            </form>
                        </c:if>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h2 style="margin-top: 0;">Add an address</h2>
        <form:form method="post" action="${ctx}/profile/addresses" modelAttribute="addressForm">
            <label for="label">Label</label>
            <form:input path="label" id="label" type="text"/>

            <label for="line1">Address line 1</label>
            <form:input path="line1" id="line1" type="text"/>
            <form:errors path="line1" cssClass="field-error" element="span"/>

            <label for="line2">Address line 2</label>
            <form:input path="line2" id="line2" type="text"/>

            <label for="city">City</label>
            <form:input path="city" id="city" type="text"/>
            <form:errors path="city" cssClass="field-error" element="span"/>

            <label for="state">State</label>
            <form:input path="state" id="state" type="text"/>
            <form:errors path="state" cssClass="field-error" element="span"/>

            <label for="pincode">Pincode</label>
            <form:input path="pincode" id="pincode" type="text" maxlength="6"/>
            <form:errors path="pincode" cssClass="field-error" element="span"/>

            <label for="latitude">Latitude</label>
            <form:input path="latitude" id="latitude" type="text"/>
            <form:errors path="latitude" cssClass="field-error" element="span"/>

            <label for="longitude">Longitude</label>
            <form:input path="longitude" id="longitude" type="text"/>
            <form:errors path="longitude" cssClass="field-error" element="span"/>

            <label><form:checkbox path="makeDefault"/> Make this my default address</label>

            <p>
                <button type="button" class="secondary" id="use-location">Use my current location</button>
                <span class="muted" id="location-status"></span>
            </p>

            <button type="submit">Save address</button>
        </form:form>
    </div>
</div>

<script src="${ctx}/js/geolocate.js"></script>

<%@ include file="../fragments/footer.jspf" %>
