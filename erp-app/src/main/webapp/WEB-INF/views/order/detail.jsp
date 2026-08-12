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

    <div class="card">
        <h2 style="margin-top: 0;">Pay to</h2>
        <p>${payment.payeeName} &middot; ${payment.bankName}</p>
        <p>A/c ${payment.accountNumber} &middot; IFSC ${payment.ifsc}</p>
        <p class="muted">Scan to pay
            <fmt:formatNumber value="${order.total}" type="currency" currencySymbol="&#8377;"
                              maxFractionDigits="2"/> (reference ${order.orderNumber}):</p>
        <img class="payment-qr" alt="Payment QR code for order ${order.orderNumber}"
             src="${ctx}/orders/${order.id}/payment-qr.png"/>
    </div>

    <div class="card">
        <h2 style="margin-top: 0;">Returns</h2>
        <c:forEach var="orderReturn" items="${returns}">
            <p>
                <span class="status ${orderReturn.status}">${orderReturn.status}</span>
                ${orderReturn.returnNumber} &middot; ${orderReturn.reason.label} &middot;
                <fmt:formatNumber value="${orderReturn.refundAmount}" type="currency" currencySymbol="&#8377;"
                                  maxFractionDigits="2"/>
            </p>
            <ul class="muted">
                <c:forEach var="line" items="${orderReturn.items}">
                    <li>${line.quantity} &times; ${line.orderItem.productName}</li>
                </c:forEach>
            </ul>
            <c:if test="${not empty orderReturn.resolutionNote}">
                <p class="muted">${orderReturn.resolutionNote}
                    <c:if test="${not empty orderReturn.refundReference}">
                        (ref ${orderReturn.refundReference})</c:if></p>
            </c:if>
        </c:forEach>
        <c:choose>
            <c:when test="${returnAllowed}">
                <p><a href="${ctx}/orders/${order.id}/return">Return items from this order</a></p>
            </c:when>
            <c:when test="${empty returns}">
                <p class="muted">Items can be returned within ${returnWindowDays} days of delivery or pickup.</p>
            </c:when>
        </c:choose>
    </div>

    <div class="card">
        <h2 style="margin-top: 0;">Your feedback</h2>
        <c:choose>
            <c:when test="${not empty feedback}">
                <p class="rating">
                    <c:forEach var="star" begin="1" end="5">
                        <span class="${star le feedback.rating ? 'star on' : 'star'}">&#9733;</span>
                    </c:forEach>
                    ${feedback.rating}/5
                </p>
                <c:if test="${not empty feedback.comment}">
                    <p>${feedback.comment}</p>
                </c:if>
            </c:when>
            <c:when test="${order.status eq 'DELIVERED' or order.status eq 'PICKED_UP'}">
                <form:form method="post" action="${ctx}/orders/${order.id}/feedback"
                           modelAttribute="feedbackForm">
                    <label for="rating">Rating</label>
                    <form:select path="rating" id="rating">
                        <form:option value="5" label="5 - Excellent"/>
                        <form:option value="4" label="4 - Good"/>
                        <form:option value="3" label="3 - Okay"/>
                        <form:option value="2" label="2 - Poor"/>
                        <form:option value="1" label="1 - Bad"/>
                    </form:select>
                    <form:errors path="rating" cssClass="field-error" element="span"/>

                    <label for="comment">Comment (optional)</label>
                    <form:textarea path="comment" id="comment" rows="4"/>
                    <form:errors path="comment" cssClass="field-error" element="span"/>

                    <button type="submit">Submit feedback</button>
                </form:form>
            </c:when>
            <c:otherwise>
                <p class="muted">You can rate this order once it is delivered or picked up.</p>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../fragments/footer.jspf" %>
