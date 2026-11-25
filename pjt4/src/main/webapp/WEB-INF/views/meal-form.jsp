<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<div class="page-heading">
    <h1><c:out value="${title}"/></h1>
    <a class="btn btn-outline-secondary" href="<c:url value='/app/meals'/>">식단 목록</a>
</div>
<section class="panel">
    <form id="meal-form" class="stack-form" method="post" action="<c:url value='/app/meal/save'/>"
          data-food-url="<c:url value='/app/food-options'/>">
        <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>">
        <input type="hidden" name="id" value="<c:out value='${mealId}'/>">
        <div class="field-pair">
            <div><label for="meal-date">날짜</label><input class="form-control" id="meal-date"
                                                         name="date"
                                                         type="date"
                                                         value="<c:out value='${mealDate}'/>"
                                                         required></div>
            <div><label for="meal-type">끼니</label><select class="form-select" id="meal-type"
                                                          name="type">
                <c:forEach var="type" items="${mealTypes}">
                    <option value="<c:out value='${type}'/>" ${mealType eq type ? 'selected' : ''}>
                        <c:out value="${type}"/></option>
                </c:forEach>
            </select></div>
        </div>
        <div id="food-items">
            <c:forEach var="row" items="${rows}">
                <div class="food-entry">
                    <div class="item-search-box">
                        <label>음식명 또는 코드<input class="form-control item-query" type="search"
                                               value="<c:out value='${row.food.foodCode}'/>"></label>
                        <button type="button" class="btn btn-outline-secondary btn-sm item-search">
                            찾기
                        </button>
                    </div>
                    <label>음식 선택
                        <select class="form-select item-food" name="foodCode" required>
                            <option value="">음식을 선택하세요</option>
                            <c:if test="${not empty row.food}">
                                <option value="<c:out value='${row.food.foodCode}'/>"
                                        data-unit="<c:out value='${row.food.basisUnit}'/>" selected>
                                    <c:out value="${row.food.name}"/></option>
                            </c:if>
                        </select>
                    </label>
                    <label>섭취량 <span class="item-unit"><c:out value="${row.food.basisUnit}"/></span>
                        <input class="form-control item-amount" name="amount" type="number"
                               step="any" max="5000"
                               value="<c:out value='${row.amount}'/>" required>
                    </label>
                    <button type="button" class="btn btn-outline-danger btn-sm item-remove">제거
                    </button>
                    <p class="item-message muted" role="status"></p>
                </div>
            </c:forEach>
        </div>
        <button type="button" class="btn btn-outline-secondary btn-sm align-self-start"
                id="add-food-row">+ 음식 추가
        </button>
        <p class="muted" id="editor-message" role="status" hidden></p>
        <label for="meal-note">메모 (선택)</label>
        <textarea class="form-control" id="meal-note" name="note" maxlength="500" rows="2"><c:out
                value="${mealNote}"/></textarea>
        <button type="submit" class="btn btn-primary align-self-start">식단 저장</button>
    </form>
</section>
<template id="food-row-template">
    <div class="food-entry">
        <div class="item-search-box">
            <label>음식명 또는 코드<input class="form-control item-query" type="search"></label>
            <button type="button" class="btn btn-outline-secondary btn-sm item-search">찾기</button>
        </div>
        <label>음식 선택<select class="form-select item-food" name="foodCode" required>
            <option value="">음식을 선택하세요</option>
        </select></label>
        <label>섭취량 <span class="item-unit"></span><input class="form-control item-amount"
                                                         name="amount" type="number" step="any"
                                                         max="5000" required></label>
        <button type="button" class="btn btn-outline-danger btn-sm item-remove">제거</button>
        <p class="item-message muted" role="status"></p>
    </div>
</template>
<%@ include file="footer.jspf" %>
