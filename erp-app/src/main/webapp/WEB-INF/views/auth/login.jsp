<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Sign in"/>
<%@ include file="../fragments/header.jspf" %>

<div class="card narrow">
    <h1>Sign in with your mobile</h1>
    <p class="subtitle">We will send a 6-digit one-time password to verify your number.</p>

    <form:form method="post" action="${ctx}/auth/otp/request" modelAttribute="mobileForm">
        <label for="mobile">Mobile number</label>
        <form:input path="mobile" id="mobile" type="text" maxlength="10" placeholder="10-digit mobile number"/>
        <form:errors path="mobile" cssClass="field-error" element="span"/>

        <p></p>
        <button type="submit">Send OTP</button>
    </form:form>

    <p class="muted">New here? Verifying your number creates your account automatically.</p>
</div>

<%@ include file="../fragments/footer.jspf" %>
