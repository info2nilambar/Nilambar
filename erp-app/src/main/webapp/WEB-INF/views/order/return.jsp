<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Return items from ${order.orderNumber}"/>
<%@ include file="../fragments/header.jspf" %>

<p class="muted"><a href="${ctx}/orders/${order.id}">&larr; Back to order ${order.orderNumber}</a></p>

<h1>Return items</h1>
<p class="subtitle">Order ${order.orderNumber} &middot; returns accepted within ${returnWindowDays} days of
    delivery.</p>

<c:choose>
    <c:when test="${not returnAllowed}">
        <div class="card">
            <p>This order cannot be returned. Returns are open for ${returnWindowDays} days after delivery or
                pickup, and every item may only be returned once.</p>
        </div>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/orders/${order.id}/return">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <table>
                <thead>
                <tr>
                    <th>Item</th>
                    <th>Unit price</th>
                    <th>Returnable</th>
                    <th>Return qty</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="item" items="${order.items}">
                    <c:set var="max" value="${returnable[item.id]}"/>
                    <tr>
                        <td>${item.productName}</td>
                        <td><fmt:formatNumber value="${item.unitPrice}" type="currency" currencySymbol="&#8377;"
                                              maxFractionDigits="2"/></td>
                        <td>${max} of ${item.quantity}</td>
                        <td>
                            <c:choose>
                                <c:when test="${max gt 0}">
                                    <input type="number" name="quantity_${item.id}" value="0" min="0"
                                           max="${max}" style="width: 80px;"/>
                                </c:when>
                                <c:otherwise><span class="muted">Already returned</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <label for="reason">Reason</label>
            <select id="reason" name="reason">
                <c:forEach var="option" items="${reasons}">
                    <option value="${option}">${option.label}</option>
                </c:forEach>
            </select>

            <label for="comment">Comment (optional)</label>
            <textarea id="comment" name="comment" rows="4"></textarea>

            <button type="submit">Request return</button>
        </form>
    </c:otherwise>
</c:choose>

<%@ include file="../fragments/footer.jspf" %>
