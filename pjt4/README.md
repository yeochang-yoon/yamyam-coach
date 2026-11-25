# 냠냠코치 · Servlet/JSP 식단 관리

Java와 브라우저 화면을 만든 경험을 바탕으로 HTTP 요청, Servlet, JSP, 세션을 적용했습니다. PJT3의 HTML 폼과 CSS를 JSP로 옮기고 PJT2의 Java 서비스와 JSON 저장소를 연결했습니다. 외부 Tomcat에서 폼 입력을 검증하고, 회원별 식단 기록과 로그인 상태를 서버에서 관리합니다.

## 주요 기능

- 회원가입, 세션 기반 로그인·로그아웃, 내 프로필 조회·수정·탈퇴
- 음식 이름·식품코드 검색과 이름 시작 부분 검색
- 식단 작성, 전체·날짜별 목록과 상세 조회, 수정·삭제
- g·mL 섭취량에 따른 열량·영양소 합계와 음식별 섭취 열량 순위

## 외부 Tomcat에서 실행

JDK 17 이상과 Apache Tomcat 10.1이 필요합니다. `JAVA_HOME`을 JDK 설치 경로로 설정한 뒤 저장소 루트에서 WAR를 빌드합니다.

```powershell
cd pjt4
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd clean package
```

생성된 `target/pjt4.war`를 Tomcat의 `webapps`에 복사한 뒤 Tomcat을 시작합니다. 아래 `$tomcat`은 자신의 설치 폴더로 바꿉니다.

```powershell
$tomcat = 'C:\apache-tomcat-10.1.62'
Copy-Item -LiteralPath 'target/pjt4.war' -Destination (Join-Path $tomcat 'webapps/pjt4.war')
& (Join-Path $tomcat 'bin/catalina.bat') run
```

기본 포트를 사용하면 브라우저에서 `http://localhost:8080/pjt4/`로 접속합니다. Linux와 macOS에서는 `sh ./mvnw clean package`와 Tomcat의 `bin/catalina.sh run`을 사용합니다.

첫 화면에서 **로그인 정보 채우기**를 누르거나 `demo01` / `DemoMeal01!`로 로그인합니다. `demo02` / `DemoMeal02!`에도 별도 식단이 있습니다. 날짜를 `2025-04-21`로 바꾸고 **조회**하면 데모 계정의 하루 분석을 확인할 수 있습니다.

## 요청 처리와 저장

| 구성 | 역할 |
| --- | --- |
| `FrontControllerServlet` | GET 요청에 맞는 JSP를 선택하고 POST 폼 입력을 서비스에 전달합니다. 식단을 저장하면 해당 날짜가 선택된 목록으로 돌아갑니다. |
| `CoachService`, `FoodCatalog` | 회원 인증, 식단 검증, 음식 검색 및 영양 합계를 서버에서 처리합니다. |
| `FileRepository` | 회원과 식단 변경을 Gson으로 JSON에 저장합니다. |
| `WEB-INF/views/` | JSP가 EL과 JSTL로 폼, 식단 목록, 상세 정보, 분석 결과를 출력합니다. |
| `js/page.js` | 식단 폼의 음식 검색, 음식 입력 칸 추가·제거, 단위 표시를 처리합니다. |

`index.jsp`는 Tomcat이 프로젝트 주소를 열 때 실행하는 시작 파일입니다. 요청을 `/app/meals`에 넘기면 서블릿이 로그인 페이지 또는 식단 목록을 보여줍니다. 화면 JSP는 `header.jspf`에서 공통 메뉴와 스타일·스크립트 연결을 가져오고, `footer.jspf`에서 HTML 닫는 태그를 가져옵니다.

첫 실행은 음식 5,789건, 가상 회원 2명, 식단 42건으로 시작합니다. 회원과 식단을 변경하면 Tomcat의 `CATALINA_BASE/yamyam-pjt4/state.json`에 저장합니다. 이 파일은 WAR 바깥에 있어 Tomcat을 재시작해도 기록을 다시 읽습니다. 로그인한 사용자 ID는 `HttpSession`에서 관리하고, 식단 조회와 변경에는 해당 회원의 ID를 사용합니다.

## 검증

`.\mvnw.cmd test`로 도메인·저장·검색·분석 테스트 8개를 실행합니다. 외부 Tomcat 10.1.62에 WAR를 배포하고 Chrome에서 세션 로그인, 식단 변경과 서버 재시작 후 복구, 회원별 접근, 모바일 화면을 확인했습니다.
