<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Checkout"/>
<%@ include file="../fragments/header.jspf" %>

<h1>Checkout</h1>

<c:choose>
    <c:when test="${empty cart.items}">
        <div class="card">
            <p class="muted">Your cart is empty.</p>
            <a class="button" href="${ctx}/products">Browse products</a>
        </div>
    </c:when>
    <c:when test="${empty addresses}">
        <div class="card">
            <p class="muted">Add a delivery address before checking out.</p>
            <a class="button" href="${ctx}/profile/addresses">Add address</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="columns">
            <div>
                <h2 style="margin-top: 0;">Delivery address</h2>
                <form method="get" action="${ctx}/checkout">
                    <c:forEach var="address" items="${addresses}">
                        <label class="address-option ${address.id eq selectedAddress.id ? 'selected' : ''}">
                            <input type="radio" name="addressId" value="${address.id}"
                                   onchange="this.form.submit()"
                                    ${address.id eq selectedAddress.id ? 'checked' : ''}>
                            <strong>${empty address.label ? 'Address' : address.label}</strong>
                            <div class="muted">${address.summary}</div>
                        </label>
                    </c:forEach>
                </form>
                <p class="muted"><a href="${ctx}/profile/addresses">Manage addresses</a></p>

                <h2>Delivery eligibility</h2>
                <c:choose>
                    <c:when test="${quote.homeDeliveryAvailable}">
                        <div class="delivery-ok">
                            <strong>Home delivery available</strong>
                            <p>${quote.nearestStore.name} is ${quote.formattedDistance} km away, within the
                                ${quote.radiusKm} km radius.</p>
                            <p class="muted">Delivery charge is
                                <fmt:formatNumber value="${feePerSlab}" type="currency"
                                                  currencySymbol="&#8377;" maxFractionDigits="2"/>
                                per ${feeSlabKm} km slab, so this address costs
                                <fmt:formatNumber value="${quote.fee}" type="currency"
                                                  currencySymbol="&#8377;" maxFractionDigits="2"/>.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="delivery-blocked">
                            <strong>Outside the home-delivery radius</strong>
                            <p>${quote.nearestStore.name} is ${quote.formattedDistance} km away, beyond the
                                ${quote.radiusKm} km radius. Choose store pickup or a nearer address.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="card">
                <h2 style="margin-top: 0;">Order summary</h2>
                <table>
                    <tbody>
                    <c:forEach var="item" items="${cart.items}">
                        <tr>
                            <td>${item.product.name} &times; ${item.quantity}</td>
                            <td class="right"><fmt:formatNumber value="${item.lineTotal}" type="currency"
                                                                currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                        </tr>
                    </c:forEach>
                    <tr>
                        <td>Subtotal</td>
                        <td class="right"><fmt:formatNumber value="${cart.subtotal}" type="currency"
                                                            currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                    </tr>
                    <tr>
                        <td>${quote.homeDeliveryAvailable ? 'Delivery fee' : 'Pickup fee'}</td>
                        <td class="right"><fmt:formatNumber value="${quote.fee}" type="currency"
                                                            currencySymbol="&#8377;" maxFractionDigits="2"/></td>
                    </tr>
                    <tr>
                        <td><strong>Total</strong></td>
                        <td class="right"><strong><fmt:formatNumber value="${cart.subtotal + quote.fee}"
                                                                    type="currency" currencySymbol="&#8377;"
                                                                    maxFractionDigits="2"/></strong></td>
                    </tr>
                    </tbody>
                </table>

                <form method="post" action="${ctx}/checkout/place">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="addressId" value="${selectedAddress.id}"/>
                    <label for="fulfilmentType">Fulfilment</label>
                    <select id="fulfilmentType" name="fulfilmentType">
                        <c:if test="${quote.homeDeliveryAvailable}">
                            <option value="HOME_DELIVERY">Home delivery (within ${quote.radiusKm} km)</option>
                        </c:if>
                        <option value="STORE_PICKUP">Store pickup at ${quote.nearestStore.name}</option>
                    </select>
                    <p class="muted">Payment is captured by a mock gateway in this build.</p>
                    <button type="submit">Place order</button>
                </form>
            </div>

            <div class="card">
                <h2 style="margin-top: 0;">Pay to</h2>
                <table>
                    <tbody>
                    <tr><td>Beneficiary</td><td class="right">${payment.payeeName}</td></tr>
                    <tr><td>Bank</td><td class="right">${payment.bankName}</td></tr>
                    <tr><td>Account number</td><td class="right">${payment.accountNumber}</td></tr>
                    <tr><td>IFSC</td><td class="right">${payment.ifsc}</td></tr>
                    <tr>
                        <td>Amount payable</td>
                        <td class="right"><strong><fmt:formatNumber value="${payable}" type="currency"
                                                                    currencySymbol="&#8377;"
                                                                    maxFractionDigits="2"/></strong></td>
                    </tr>
                    </tbody>
                </table>
                <p class="muted">Scan to pay:</p>
                <img class="payment-qr" alt="Payment QR code"
                     src="${ctx}/checkout/payment-qr.png?addressId=${selectedAddress.id}"/>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../fragments/footer.jspf" %>
