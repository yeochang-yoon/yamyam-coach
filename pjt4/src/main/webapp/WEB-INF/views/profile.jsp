<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<%@ include file="header.jspf" %>
<div class="page-heading"><h1>내 정보</h1></div>
<div class="profile-layout">
    <section class="panel" aria-labelledby="profile-edit-title">
        <h2 id="profile-edit-title">프로필</h2>
        <p class="muted profile-id">아이디 <c:out value="${currentUser.username}"/></p>
        <form method="post" action="<c:url value='/app/profile'/>" class="stack-form">
            <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>">
            <label for="profile-name">이름</label>
            <input class="form-control" id="profile-name" name="name" required maxlength="40"
                   autocomplete="name"
                   value="<c:out value='${submitted ? param.name : currentUser.name}'/>">
            <div class="field-pair">
                <div><label for="profile-height">키 (cm)</label><input class="form-control"
                                                                      id="profile-height"
                                                                      name="height"
                                                                      type="number" min="50"
                                                                      max="250" step="any" required
                                                                      value="<c:out value='${submitted ? param.height : currentUser.height}'/>">
                </div>
                <div><label for="profile-weight">몸무게 (kg)</label><input class="form-control"
                                                                        id="profile-weight"
                                                                        name="weight"
                                                                        type="number" min="10"
                                                                        max="500" step="any"
                                                                        required
                                                                        value="<c:out value='${submitted ? param.weight : currentUser.weight}'/>">
                </div>
            </div>
            <label for="profile-conditions">질환 (선택)</label>
            <input class="form-control" id="profile-conditions" name="conditions" maxlength="300"
                   value="<c:out value='${submitted ? param.conditions : currentUser.conditions}'/>">
            <button type="submit" class="btn btn-primary align-self-start mt-2">프로필 저장</button>
        </form>
    </section>
    <section class="panel" aria-labelledby="delete-title">
        <h2 id="delete-title">회원 탈퇴</h2>
        <p class="muted count-line">탈퇴하면 이 계정의 식단 기록도 삭제됩니다.</p>
        <form method="post" action="<c:url value='/app/profile/delete'/>" class="stack-form"
              data-confirm="이 계정과 식단 기록을 삭제하시겠습니까?">
            <input type="hidden" name="csrf" value="<c:out value='${csrf}'/>">
            <label for="delete-password">비밀번호 확인</label>
            <input class="form-control" id="delete-password" name="password" type="password"
                   required maxlength="128" autocomplete="current-password">
            <button type="submit" class="btn btn-outline-danger align-self-start mt-2">회원 탈퇴
            </button>
        </form>
    </section>
</div>
<%@ include file="footer.jspf" %>
