<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Catalog"/>
<%@ include file="../fragments/header.jspf" %>

<h1>Product catalog</h1>
<p class="subtitle">${products.totalElements} product(s) available.</p>

<form method="get" action="${ctx}/products" class="toolbar">
    <div class="field">
        <label for="q">Search</label>
        <input type="text" id="q" name="q" value="${keyword}" placeholder="Name or description">
    </div>
    <div class="field">
        <label for="category">Category</label>
        <select id="category" name="category">
            <option value="">All categories</option>
            <c:forEach var="cat" items="${categories}">
                <option value="${cat}" ${cat eq selectedCategory ? 'selected' : ''}>${cat}</option>
            </c:forEach>
        </select>
    </div>
    <button type="submit">Apply</button>
    <a class="button secondary" href="${ctx}/products">Reset</a>
</form>

<c:choose>
    <c:when test="${products.totalElements eq 0}">
        <div class="card"><p class="muted">No products match your filters.</p></div>
    </c:when>
    <c:otherwise>
        <div class="grid">
            <c:forEach var="product" items="${products.content}">
                <div class="product-card">
                    <a href="${ctx}/products/${product.id}">
                        <img src="${ctx}${product.imagePath}" alt="${product.name}">
                    </a>
                    <div class="body">
                        <span class="category-tag">${product.category}</span>
                        <a class="name" href="${ctx}/products/${product.id}">${product.name}</a>
                        <span class="price"><fmt:formatNumber value="${product.price}" type="currency"
                                                              currencySymbol="&#8377;" maxFractionDigits="2"/></span>
                        <c:choose>
                            <c:when test="${product.inStock}">
                                <span class="stock-in">In stock (${product.stockQuantity})</span>
                            </c:when>
                            <c:otherwise><span class="stock-out">Out of stock</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="pagination">
            <c:if test="${products.number > 0}">
                <a class="button secondary"
                   href="${ctx}/products?page=${products.number - 1}&category=${selectedCategory}&q=${keyword}">Previous</a>
            </c:if>
            <span class="muted">Page ${products.number + 1} of ${products.totalPages}</span>
            <c:if test="${products.number + 1 < products.totalPages}">
                <a class="button secondary"
                   href="${ctx}/products?page=${products.number + 1}&category=${selectedCategory}&q=${keyword}">Next</a>
            </c:if>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../fragments/footer.jspf" %>
