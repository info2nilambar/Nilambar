<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Verify OTP"/>
<%@ include file="../fragments/header.jspf" %>

<div class="card narrow">
    <h1>Enter the OTP</h1>
    <p class="subtitle">Sent to <strong>${otpForm.mobile}</strong>. Valid for 5 minutes.</p>

    <form:form method="post" action="${ctx}/auth/otp/verify" modelAttribute="otpForm">
        <form:hidden path="mobile"/>
        <label for="code">6-digit OTP</label>
        <form:input path="code" id="code" type="text" maxlength="6" autocomplete="one-time-code"/>
        <form:errors path="code" cssClass="field-error" element="span"/>

        <p></p>
        <button type="submit">Verify &amp; continue</button>
    </form:form>

    <form method="post" action="${ctx}/auth/otp/request">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <input type="hidden" name="mobile" value="${otpForm.mobile}"/>
        <button type="submit" class="link-button" style="color: var(--brand); margin-top: 14px;">Resend OTP</button>
    </form>

    <p class="muted">Development build: the OTP is printed in the application log
        (look for <code>=== DEV OTP ===</code>).</p>
</div>

<%@ include file="../fragments/footer.jspf" %>
