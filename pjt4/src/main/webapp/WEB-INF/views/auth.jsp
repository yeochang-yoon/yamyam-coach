<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<section class="auth-layout" aria-labelledby="auth-title">
    <div class="panel auth-form-panel">
        <h1 id="auth-title"><c:out value="${title}"/></h1>
        <div class="auth-tabs">
            <a class="auth-tab ${not register ? 'active' : ''}" href="<c:url value='/app/login'/>">로그인</a>
            <a class="auth-tab ${register ? 'active' : ''}" href="<c:url value='/app/register'/>">회원가입</a>
        </div>
        <c:url var="authAction" value="${register ? '/app/register' : '/app/login'}"/>
        <form id="auth-form" method="post" action="<c:out value='${authAction}'/>"
              class="stack-form">
            <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>">
            <label for="login-id">아이디</label>
            <input class="form-control" id="login-id" name="loginId"
                   value="<c:out value='${param.loginId}'/>"
                   required maxlength="24"
                   autocomplete="username" ${register ? 'pattern="[A-Za-z0-9_]{3,24}"' : ''}>
            <label for="login-password">비밀번호 ${register ? '(8자 이상)' : ''}</label>
            <input class="form-control" type="password" id="login-password" name="password" required
                   maxlength="128" ${register ? 'minlength="8"' : ''}
                   autocomplete="${register ? 'new-password' : 'current-password'}">
            <c:if test="${register}">
                <label for="register-name">이름</label>
                <input class="form-control" id="register-name" name="name"
                       value="<c:out value='${param.name}'/>"
                       required maxlength="40" autocomplete="name">
                <div class="field-pair">
                    <div><label for="register-height">키 (cm)</label><input class="form-control"
                                                                           id="register-height"
                                                                           name="height"
                                                                           type="number" min="50"
                                                                           max="250" step="any"
                                                                           required
                                                                           value="<c:out value='${param.height}'/>">
                    </div>
                    <div><label for="register-weight">몸무게 (kg)</label><input class="form-control"
                                                                             id="register-weight"
                                                                             name="weight"
                                                                             type="number" min="10"
                                                                             max="500" step="any"
                                                                             required
                                                                             value="<c:out value='${param.weight}'/>">
                    </div>
                </div>
                <label for="register-conditions">질환 (선택)</label>
                <input class="form-control" id="register-conditions" name="conditions"
                       maxlength="300"
                       value="<c:out value='${param.conditions}'/>">
            </c:if>
            <button type="submit" class="btn btn-primary mt-2">${register ? '가입하기' : '로그인'}</button>
        </form>
    </div>
    <div class="demo-panel">
        <div>
            <h2>데모 식단으로<br>둘러보기</h2>
            <p>2025년 4월 21~27일 데모 식단 21건</p>
        </div>
        <div class="demo-account">
            <div><span>아이디</span><strong>demo01</strong></div>
            <div><span>비밀번호</span><strong>DemoMeal01!</strong></div>
            <c:if test="${not register}">
                <button type="button" class="btn btn-light" id="fill-demo">로그인 정보 채우기</button>
            </c:if>
        </div>
    </div>
</section>
<%@ include file="footer.jspf" %>
