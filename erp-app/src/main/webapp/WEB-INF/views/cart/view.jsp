<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Cart"/>
<%@ include file="../fragments/header.jspf" %>

<h1>Your cart</h1>

<c:choose>
    <c:when test="${cart.empty}">
        <div class="card">
            <p class="muted">Your cart is empty.</p>
            <a class="button" href="${ctx}/products">Browse products</a>
        </div>
    </c:when>
    <c:otherwise>
        <table>
            <thead>
            <tr>
                <th>Product</th>
                <th>Unit price</th>
                <th>Quantity</th>
                <th class="right">Line total</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="item" items="${cart.items}">
                <tr>
                    <td><a href="${ctx}/products/${item.product.id}">${item.product.name}</a></td>
                    <td><fmt:formatNumber value="${item.product.price}" type="currency" currencySymbol="&#8377;"
                                          maxFractionDigits="2"/></td>
                    <td>
                        <form method="post" action="${ctx}/cart/items/${item.id}" class="inline">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            <input type="number" name="quantity" value="${item.quantity}" min="0"
                                   max="${item.product.stockQuantity}" style="width: 80px; display: inline-block;">
                            <button type="submit" class="secondary">Update</button>
                        </form>
                    </td>
                    <td class="right"><fmt:formatNumber value="${item.lineTotal}" type="currency"
                                                        currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                    <td class="right">
                        <form method="post" action="${ctx}/cart/items/${item.id}/remove" class="inline">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            <button type="submit" class="danger">Remove</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <p class="right" style="font-size: 18px; margin-top: 16px;">
            Subtotal: <strong><fmt:formatNumber value="${cart.subtotal}" type="currency" currencySymbol="&#8377;"
                                                maxFractionDigits="2"/></strong>
        </p>
        <p class="right"><a class="button" href="${ctx}/checkout">Proceed to checkout</a></p>
    </c:otherwise>
</c:choose>

<%@ include file="../fragments/footer.jspf" %>
