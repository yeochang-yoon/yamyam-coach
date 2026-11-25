<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<div class="page-heading">
    <h1>식단 기록</h1>
    <c:url var="newMeal" value="/app/meal/new"><c:param name="date" value="${date}"/></c:url>
    <a id="new-meal" class="btn btn-primary" href="<c:out value='${newMeal}'/>">+ 식단 작성</a>
</div>
<section class="panel analysis-panel" aria-labelledby="analysis-title">
    <div class="section-heading">
        <h2 id="analysis-title">하루 영양 분석</h2>
        <form method="get" action="<c:url value='/app/meals'/>" class="date-filter">
            <label for="analysis-date">분석 날짜</label>
            <input class="form-control" id="analysis-date" type="date" name="date"
                   value="<c:out value='${date}'/>" required>
            <input type="hidden" name="scope" value="${dailyOnly ? 'day' : 'all'}">
            <button class="btn btn-outline-secondary" type="submit">조회</button>
        </form>
    </div>
    <div class="nutrition-grid">
        <div class="nutrition-box"><span>열량</span><strong id="total-calories"><fmt:formatNumber
                value="${total.calories}" minFractionDigits="1"
                maxFractionDigits="1"/></strong><small>kcal</small></div>
        <div class="nutrition-box"><span>탄수화물</span><strong><fmt:formatNumber value="${total.carbs}"
                                                                              minFractionDigits="1"
                                                                              maxFractionDigits="1"/></strong><small>g</small>
        </div>
        <div class="nutrition-box"><span>단백질</span><strong><fmt:formatNumber
                value="${total.protein}" minFractionDigits="1"
                maxFractionDigits="1"/></strong><small>g</small></div>
        <div class="nutrition-box"><span>지방</span><strong><fmt:formatNumber value="${total.fat}"
                                                                            minFractionDigits="1"
                                                                            maxFractionDigits="1"/></strong><small>g</small>
        </div>
    </div>
    <p class="muted count-line" id="analysis-count">해당 날짜 식단 <c:out
            value="${summary.mealCount}"/>건</p>
</section>
<div class="meal-columns">
    <section class="panel" aria-labelledby="meal-list-title">
        <div class="section-heading">
            <h2 id="meal-list-title">내 식단</h2>
            <form method="get" action="<c:url value='/app/meals'/>" class="list-options">
                <input type="hidden" name="date" value="<c:out value='${date}'/>">
                <select class="form-select list-filter" name="scope" aria-label="식단 목록 범위">
                    <option value="all" ${not dailyOnly ? 'selected' : ''}>전체 식단</option>
                    <option value="day" ${dailyOnly ? 'selected' : ''}>분석 날짜만</option>
                </select>
                <button class="btn btn-outline-secondary btn-sm" type="submit">적용</button>
            </form>
        </div>
        <p class="muted count-line" id="meal-count">식단 <c:out value="${meals.size()}"/>건</p>
        <div class="table-responsive">
            <table class="table align-middle meal-table">
                <thead>
                <tr>
                    <th scope="col">날짜</th>
                    <th scope="col">끼니</th>
                    <th scope="col">음식</th>
                    <th scope="col">관리</th>
                </tr>
                </thead>
                <tbody id="meal-rows">
                <c:if test="${empty meals}">
                    <tr>
                        <td colspan="4" class="muted">등록한 식단이 없습니다.</td>
                    </tr>
                </c:if>
                <c:forEach var="meal" items="${meals}">
                    <c:url var="detailLink" value="/app/meal"><c:param name="id"
                                                                       value="${meal.id}"/></c:url>
                    <c:url var="editLink" value="/app/meal/edit"><c:param name="id"
                                                                          value="${meal.id}"/></c:url>
                    <tr data-id="<c:out value='${meal.id}'/>">
                        <td><c:out value="${meal.date}"/></td>
                        <td><c:out value="${meal.type}"/></td>
                        <td><c:out value="${foodsByCode[meal.items[0].foodId].name}"/>
                            <c:if test="${meal.items.size() gt 1}">외 <c:out
                                    value="${meal.items.size() - 1}"/>개</c:if>
                        </td>
                        <td>
                            <div class="row-actions">
                                <a class="btn btn-sm btn-outline-secondary"
                                   href="<c:out value='${detailLink}'/>">상세</a>
                                <a class="btn btn-sm btn-outline-secondary"
                                   href="<c:out value='${editLink}'/>">수정</a>
                                <form method="post" action="<c:url value='/app/meal/delete'/>"
                                      data-confirm="이 식단을 삭제하시겠습니까?">
                                    <input type="hidden" name="csrf"
                                           value="<c:out value='${csrf}'/>">
                                    <input type="hidden" name="id"
                                           value="<c:out value='${meal.id}'/>">
                                    <button class="btn btn-sm btn-outline-danger" type="submit">삭제
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </section>
    <section class="panel ranking-panel" aria-labelledby="ranking-title">
        <h2 id="ranking-title">음식별 섭취 열량</h2>
        <c:choose>
            <c:when test="${empty summary.contributions}"><p class="muted count-line">이 날짜의 음식 기록이
                없습니다.</p></c:when>
            <c:otherwise>
                <ol class="ranking-list">
                    <c:forEach var="item" items="${summary.contributions}">
                        <li>
                            <div class="rank-entry"><span><c:out value="${item.food.name}"/></span>
                                <strong><fmt:formatNumber value="${item.calories}"
                                                          minFractionDigits="1"
                                                          maxFractionDigits="1"/> kcal</strong>
                            </div>
                        </li>
                    </c:forEach>
                </ol>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<%@ include file="footer.jspf" %>
