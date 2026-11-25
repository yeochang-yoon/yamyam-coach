<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<div class="page-heading"><h1>음식 검색</h1></div>
<section class="panel">
    <form method="get" action="<c:url value='/app/foods'/>" class="search-form">
        <div class="search-input">
            <label for="food-query">음식명 또는 식품코드</label>
            <input class="form-control" type="search" id="food-query" name="q"
                   value="<c:out value='${query}'/>" placeholder="예시 현미밥">
        </div>
        <div>
            <label for="food-search-mode">검색 방식</label>
            <select class="form-select" id="food-search-mode" name="mode">
                <option value="contains" ${mode eq 'contains' ? 'selected' : ''}>이름 포함 · 식품코드
                </option>
                <option value="prefix" ${mode eq 'prefix' ? 'selected' : ''}>이름 시작</option>
            </select>
        </div>
        <button type="submit" class="btn btn-primary">검색</button>
    </form>
    <p class="muted count-line" id="food-count">음식 <fmt:formatNumber value="${foodCount}"/>종</p>
    <div class="table-responsive">
        <table class="table align-middle catalog-table">
            <thead>
            <tr>
                <th scope="col">음식</th>
                <th scope="col">기준량</th>
                <th scope="col">열량</th>
                <th scope="col">탄수화물</th>
                <th scope="col">단백질</th>
                <th scope="col">지방</th>
            </tr>
            </thead>
            <tbody id="catalog-rows">
            <c:if test="${empty foods}">
                <tr>
                    <td colspan="6" class="muted">검색 결과가 없습니다.</td>
                </tr>
            </c:if>
            <c:forEach var="food" items="${foods}">
                <tr>
                    <td><c:out value="${food.name}"/><span class="food-code"><c:out
                            value="${food.foodCode}"/></span></td>
                    <td><c:out value="${food.basisAmount}"/> <c:out value="${food.basisUnit}"/></td>
                    <td><fmt:formatNumber value="${food.calories}" maxFractionDigits="1"/> kcal</td>
                    <td><fmt:formatNumber value="${food.carbs}" maxFractionDigits="1"/> g</td>
                    <td><fmt:formatNumber value="${food.protein}" maxFractionDigits="1"/> g</td>
                    <td><fmt:formatNumber value="${food.fat}" maxFractionDigits="1"/> g</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="pagination-controls">
        <c:if test="${pageNumber gt 1}">
            <c:url var="previous" value="/app/foods"><c:param name="q" value="${query}"/><c:param
                    name="mode" value="${mode}"/>
                <c:param name="page" value="${pageNumber - 1}"/></c:url>
            <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${previous}'/>">이전</a>
        </c:if>
        <span><c:out value="${pageNumber}"/> / <c:out value="${pageCount}"/></span>
        <c:if test="${pageNumber lt pageCount}">
            <c:url var="next" value="/app/foods"><c:param name="q" value="${query}"/><c:param
                    name="mode" value="${mode}"/>
                <c:param name="page" value="${pageNumber + 1}"/></c:url>
            <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${next}'/>">다음</a>
        </c:if>
    </div>
</section>
<%@ include file="footer.jspf" %>
