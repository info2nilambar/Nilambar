<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Order ${order.orderNumber}"/>
<%@ include file="../fragments/header.jspf" %>

<p class="muted"><a href="${ctx}/orders">&larr; Back to orders</a></p>

<h1>Order ${order.orderNumber}</h1>
<p class="subtitle">
    <span class="status ${order.status}">${order.status}</span>
    &middot; ${order.fulfilmentType}
    &middot; <fmt:formatNumber value="${order.distanceKm}" maxFractionDigits="2"/> km from ${order.store.name}
</p>

<div class="columns">
    <div>
        <table>
            <thead>
            <tr>
                <th>Item</th>
                <th>Unit price</th>
                <th>Qty</th>
                <th class="right">Line total</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="item" items="${order.items}">
                <tr>
                    <td>${item.productName}</td>
                    <td><fmt:formatNumber value="${item.unitPrice}" type="currency" currencySymbol="&#8377;"
                                          maxFractionDigits="2"/></td>
                    <td>${item.quantity}</td>
                    <td class="right"><fmt:formatNumber value="${item.lineTotal}" type="currency"
                                                        currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <div class="card">
        <h2 style="margin-top: 0;">Summary</h2>
        <p>Subtotal: <fmt:formatNumber value="${order.subtotal}" type="currency" currencySymbol="&#8377;"
                                       maxFractionDigits="2"/></p>
        <p>Delivery fee: <fmt:formatNumber value="${order.deliveryFee}" type="currency" currencySymbol="&#8377;"
                                           maxFractionDigits="2"/></p>
        <p><strong>Total: <fmt:formatNumber value="${order.total}" type="currency" currencySymbol="&#8377;"
                                            maxFractionDigits="2"/></strong></p>
        <p class="muted">Payment reference: ${order.paymentReference}</p>
        <h2>Delivery to</h2>
        <p>${order.deliveryAddressSnapshot}</p>
    </div>
</div>

<%@ include file="../fragments/footer.jspf" %>
