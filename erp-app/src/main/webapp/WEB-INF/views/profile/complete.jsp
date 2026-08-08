<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Complete your profile"/>
<%@ include file="../fragments/header.jspf" %>

<h1>Complete your profile</h1>
<p class="subtitle">We need a delivery address with coordinates to check whether you are inside the
    home-delivery radius.</p>

<div class="card" style="max-width: 720px;">
    <form:form method="post" action="${ctx}/profile/complete" modelAttribute="profileForm">
        <label for="name">Full name</label>
        <form:input path="name" id="name" type="text"/>
        <form:errors path="name" cssClass="field-error" element="span"/>

        <label for="email">Email (optional)</label>
        <form:input path="email" id="email" type="text"/>
        <form:errors path="email" cssClass="field-error" element="span"/>

        <h2>Delivery address</h2>

        <label for="label">Label (Home, Office...)</label>
        <form:input path="address.label" id="label" type="text"/>

        <label for="line1">Address line 1</label>
        <form:input path="address.line1" id="line1" type="text"/>
        <form:errors path="address.line1" cssClass="field-error" element="span"/>

        <label for="line2">Address line 2</label>
        <form:input path="address.line2" id="line2" type="text"/>

        <label for="city">City</label>
        <form:input path="address.city" id="city" type="text"/>
        <form:errors path="address.city" cssClass="field-error" element="span"/>

        <label for="state">State</label>
        <form:input path="address.state" id="state" type="text"/>
        <form:errors path="address.state" cssClass="field-error" element="span"/>

        <label for="pincode">Pincode</label>
        <form:input path="address.pincode" id="pincode" type="text" maxlength="6"/>
        <form:errors path="address.pincode" cssClass="field-error" element="span"/>

        <label for="latitude">Latitude</label>
        <form:input path="address.latitude" id="latitude" type="text" placeholder="e.g. 12.9756"/>
        <form:errors path="address.latitude" cssClass="field-error" element="span"/>

        <label for="longitude">Longitude</label>
        <form:input path="address.longitude" id="longitude" type="text" placeholder="e.g. 77.6050"/>
        <form:errors path="address.longitude" cssClass="field-error" element="span"/>

        <p>
            <button type="button" class="secondary" id="use-location">Use my current location</button>
            <span class="muted" id="location-status"></span>
        </p>

        <button type="submit">Save and start shopping</button>
    </form:form>
</div>

<script src="${ctx}/js/geolocate.js"></script>

<%@ include file="../fragments/footer.jspf" %>
