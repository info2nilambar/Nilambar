<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="${product.name}"/>
<%@ include file="../fragments/header.jspf" %>

<p class="muted"><a href="${ctx}/products">&larr; Back to catalog</a></p>

<div class="detail">
    <div>
        <img src="${ctx}${product.imagePath}" alt="${product.name}">
    </div>
    <div>
        <span class="category-tag">${product.category} &middot; ${product.sku}</span>
        <h1>${product.name}</h1>
        <p class="price"><fmt:formatNumber value="${product.price}" type="currency" currencySymbol="&#8377;"
                                           maxFractionDigits="2"/></p>
        <c:choose>
            <c:when test="${product.inStock}">
                <p class="stock-in">In stock &middot; ${product.stockQuantity} unit(s) available</p>
            </c:when>
            <c:otherwise><p class="stock-out">Currently out of stock</p></c:otherwise>
        </c:choose>

        <p>${product.description}</p>

        <c:choose>
            <c:when test="${empty currentUser}">
                <a class="button" href="${ctx}/auth/login">Sign in to buy</a>
            </c:when>
            <c:when test="${product.inStock}">
                <form method="post" action="${ctx}/cart/add">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input type="hidden" name="productId" value="${product.id}"/>
                    <label for="quantity">Quantity</label>
                    <input type="number" id="quantity" name="quantity" value="1" min="1"
                           max="${product.stockQuantity}" style="max-width: 120px;">
                    <p></p>
                    <button type="submit">Add to cart</button>
                </form>
            </c:when>
            <c:otherwise>
                <button type="button" disabled>Add to cart</button>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../fragments/footer.jspf" %>
