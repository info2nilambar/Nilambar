<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Error"/>
<%@ include file="../fragments/header.jspf" %>

<div class="card narrow">
    <h1>Something went wrong</h1>
    <p class="subtitle">${errorMessage}</p>
    <a class="button" href="${ctx}/products">Back to catalog</a>
</div>

<%@ include file="../fragments/footer.jspf" %>
