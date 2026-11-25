<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<div class="page-heading">
    <h1>식단 상세</h1>
    <a class="btn btn-outline-secondary" href="<c:url value='/app/meals'/>">식단 목록</a>
</div>
<section class="panel">
    <h2><c:out value="${meal.date}"/> · <c:out value="${meal.type}"/></h2>
    <p class="muted count-line">식단 ID <c:out value="${meal.id}"/></p>
    <ul class="detail-foods mt-3">
        <c:forEach var="item" items="${meal.items}">
            <li><c:out value="${foodsByCode[item.foodId].name}"/> · <c:out value="${item.amount}"/>
                <c:out value="${item.unit}"/></li>
        </c:forEach>
    </ul>
    <p id="detail-total">
        <fmt:formatNumber value="${total.calories}" minFractionDigits="1" maxFractionDigits="1"/>
        kcal
        · 탄수화물 <fmt:formatNumber value="${total.carbs}" maxFractionDigits="1"/> g
        · 단백질 <fmt:formatNumber value="${total.protein}" maxFractionDigits="1"/> g
        · 지방 <fmt:formatNumber value="${total.fat}" maxFractionDigits="1"/> g
    </p>
    <c:if test="${not empty meal.note}"><p class="muted" id="detail-note"><c:out
            value="${meal.note}"/></p></c:if>
    <c:url var="editLink" value="/app/meal/edit"><c:param name="id" value="${meal.id}"/></c:url>
    <a class="btn btn-primary" href="<c:out value='${editLink}'/>">식단 수정</a>
</section>
<%@ include file="footer.jspf" %>
