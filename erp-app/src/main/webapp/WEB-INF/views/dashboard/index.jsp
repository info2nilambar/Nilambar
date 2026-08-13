<%@ include file="../fragments/head.jspf" %>
<c:set var="pageTitle" value="Dashboard"/>
<%@ include file="../fragments/header.jspf" %>

<div class="dash-head">
    <div>
        <h1>AI-powered business dashboard</h1>
        <p class="subtitle">
            Last <c:out value="${snapshot.windowDays}"/> days &middot; generated
            <fmt:formatDate value="${dashGeneratedAt}" pattern="dd MMM yyyy HH:mm"/>
        </p>
    </div>
    <form method="get" action="${ctx}/dashboard" class="dash-window">
        <label for="days">Window</label>
        <select id="days" name="days" onchange="this.form.submit()">
            <c:forEach var="option" items="${windowOptions}">
                <option value="${option}" ${option eq snapshot.windowDays ? 'selected' : ''}>${option} days</option>
            </c:forEach>
        </select>
        <noscript><button type="submit">Apply</button></noscript>
        <a class="button secondary" href="${ctx}/dashboard/api?days=${snapshot.windowDays}">JSON</a>
    </form>
</div>

<div class="card ai-narrative">
    <h2>&#129302; AI summary</h2>
    <p>${fn:escapeXml(snapshot.aiNarrative)}</p>
</div>

<c:forEach var="group" items="${snapshot.groups}">
    <section class="kpi-section">
        <h2>${group.icon} ${fn:escapeXml(group.title)}</h2>
        <div class="kpi-grid">
            <c:forEach var="card" items="${group.cards}">
                <div class="kpi-card status-${fn:toLowerCase(card.status)}">
                    <span class="kpi-label">${fn:escapeXml(card.label)}</span>
                    <span class="kpi-value">${fn:escapeXml(card.value)}</span>
                    <span class="kpi-detail">${fn:escapeXml(card.detail)}</span>
                </div>
            </c:forEach>
        </div>
    </section>
</c:forEach>

<section class="kpi-section">
    <h2>&#128200; Revenue trend and forecast</h2>
    <div class="card chart-card">
        <div class="bar-chart">
            <c:forEach var="point" items="${snapshot.revenueTrend}">
                <div class="bar-slot" title="${fn:escapeXml(point.label)}: ${point.value}">
                    <div class="bar actual" style="height:${maxRevenue > 0 ? (point.value / maxRevenue) * 100 : 0}%"></div>
                    <span class="bar-label">${fn:escapeXml(point.label)}</span>
                </div>
            </c:forEach>
            <c:forEach var="point" items="${snapshot.revenueForecast}">
                <div class="bar-slot" title="Forecast ${fn:escapeXml(point.label)}: ${point.value}">
                    <div class="bar forecast" style="height:${maxRevenue > 0 ? (point.value / maxRevenue) * 100 : 0}%"></div>
                    <span class="bar-label">${fn:escapeXml(point.label)}</span>
                </div>
            </c:forEach>
        </div>
        <p class="legend">
            <span class="swatch actual"></span> actual revenue
            <span class="swatch forecast"></span> forecast
        </p>
    </div>
</section>

<div class="columns dash-columns">
    <section>
        <h2>&#127942; Top-selling products</h2>
        <div class="card">
            <c:choose>
                <c:when test="${empty snapshot.topProducts}">
                    <p class="muted">No sales in this window.</p>
                </c:when>
                <c:otherwise>
                    <ul class="rank-list">
                        <c:forEach var="item" items="${snapshot.topProducts}">
                            <li><span>${fn:escapeXml(item.name)}</span><strong>${fn:escapeXml(item.display)}</strong></li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
    <section>
        <h2>&#128101; Productivity per team</h2>
        <div class="card">
            <c:choose>
                <c:when test="${empty snapshot.teamProductivity}">
                    <p class="muted">No workforce data yet.</p>
                </c:when>
                <c:otherwise>
                    <ul class="rank-list">
                        <c:forEach var="item" items="${snapshot.teamProductivity}">
                            <li><span>${fn:escapeXml(item.name)}</span><strong>${fn:escapeXml(item.display)}</strong></li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</div>

<section class="kpi-section">
    <h2>&#128230; Recommended reorders</h2>
    <div class="card">
        <c:choose>
            <c:when test="${empty snapshot.reorderSuggestions}">
                <p class="muted">Stock levels are healthy — nothing to reorder.</p>
            </c:when>
            <c:otherwise>
                <table class="table">
                    <thead>
                    <tr>
                        <th>SKU</th>
                        <th>Product</th>
                        <th>On hand</th>
                        <th>Reorder level</th>
                        <th>Daily demand</th>
                        <th>Days of cover</th>
                        <th>Recommended qty</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="row" items="${snapshot.reorderSuggestions}">
                        <tr>
                            <td>${fn:escapeXml(row.sku)}</td>
                            <td>${fn:escapeXml(row.productName)}</td>
                            <td>${row.stockOnHand}</td>
                            <td>${row.reorderLevel}</td>
                            <td><fmt:formatNumber value="${row.dailyDemand}" maxFractionDigits="2"/></td>
                            <td>${row.daysOfCoverLeft}</td>
                            <td><strong>${row.recommendedQuantity}</strong></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<section class="kpi-section">
    <h2>&#128680; Risk alerts</h2>
    <div class="card">
        <c:choose>
            <c:when test="${empty snapshot.alerts}">
                <p class="muted">No active operational alerts.</p>
            </c:when>
            <c:otherwise>
                <ul class="alert-list">
                    <c:forEach var="alert" items="${snapshot.alerts}">
                        <li class="status-${fn:toLowerCase(alert.status)}">
                            <span class="alert-cat">${fn:escapeXml(alert.category)}</span>
                            ${fn:escapeXml(alert.message)}
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
