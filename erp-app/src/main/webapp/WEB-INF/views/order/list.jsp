<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="My orders"/>
<%@ include file="../fragments/header.jspf" %>

<h1>My orders</h1>

<c:choose>
    <c:when test="${empty orders}">
        <div class="card">
            <p class="muted">You have not placed any orders yet.</p>
            <a class="button" href="${ctx}/products">Browse products</a>
        </div>
    </c:when>
    <c:otherwise>
        <table>
            <thead>
            <tr>
                <th>Order</th>
                <th>Placed</th>
                <th>Fulfilment</th>
                <th>Status</th>
                <th class="right">Total</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="order" items="${orders}">
                <tr>
                    <td><a href="${ctx}/orders/${order.id}">${order.orderNumber}</a></td>
                    <td>${order.createdAt}</td>
                    <td>${order.fulfilmentType} (<fmt:formatNumber value="${order.distanceKm}"
                                                                   maxFractionDigits="2"/> km)</td>
                    <td><span class="status ${order.status}">${order.status}</span></td>
                    <td class="right"><fmt:formatNumber value="${order.total}" type="currency"
                                                        currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<%@ include file="../fragments/footer.jspf" %>
