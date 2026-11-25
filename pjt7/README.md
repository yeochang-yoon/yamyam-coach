# 냠냠코치 · 식단 관리 API

PJT5의 MySQL 설계와 식단 관리 기능에 Spring Boot, Spring MVC, 의존성 주입, MyBatis를 적용했습니다. 세션 로그인, 회원·식단 관리, 음식 검색, 하루 영양 분석을 JSON API로 제공합니다. JDBC에서 직접 조립하던 조회 조건은 MyBatis XML의 동적 쿼리로 옮겼습니다.

## 실행

JDK 17 이상과 MySQL 8.4가 필요합니다. 처음 실행할 때 MySQL Workbench에서 아래 파일을 순서대로 실행합니다.

| 순서 | 파일 | 내용 |
| --- | --- | --- |
| 1 | [create-database.sql](db/create-database.sql) | `yamyam_pjt7` 스키마 생성 |
| 2 | [schema.sql](db/schema.sql) | 회원·음식·식단·식단 항목 테이블, 외래 키, 인덱스 생성 |
| 3 | [foods.sql](db/foods.sql) | 기준량·단위·영양값이 있는 음식 5,789건 등록 |
| 4 | [demo.sql](db/demo.sql) | 데모 회원 2명과 2025년 4월 21~27일 식단 42건 등록 |

1번 실행 후 `yamyam_pjt7`을 기본 스키마로 선택하고, 3·4번은 빈 테이블에 한 번 실행합니다. [inspect.sql](db/inspect.sql)로 건수와 날짜별 조회 계획을 확인할 수 있습니다.

저장소 루트에서 다음 명령을 실행합니다.

```powershell
cd pjt7
$env:YAMYAM_DB_USER = Read-Host 'MySQL 사용자명'
$dbSecret = Read-Host 'MySQL 비밀번호' -AsSecureString
$env:YAMYAM_DB_PASSWORD = [pscredential]::new($env:YAMYAM_DB_USER, $dbSecret).GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run
```

기본 연결 주소는 `jdbc:mysql://127.0.0.1:3306/yamyam_pjt7?sslMode=REQUIRED`입니다. MySQL 주소를 바꿀 때는 `YAMYAM_DB_URL`을 설정합니다. macOS/Linux에서는 `sh ./mvnw spring-boot:run`을 사용합니다. `.\mvnw.cmd package`를 실행하면 `java -jar target/pjt7.jar`로 열 수 있는 JAR가 만들어집니다.

[Swagger UI](http://127.0.0.1:8077/swagger-ui/index.html)에서 `POST /api/session`의 **Try it out**을 열고 아래 JSON을 보냅니다. 같은 탭에서 요청하면 브라우저가 세션 쿠키를 함께 보냅니다. [OpenAPI JSON](http://127.0.0.1:8077/v3/api-docs)도 제공됩니다.

```json
{
  "username": "demo01",
  "password": "DemoMeal01!"
}
```

`GET /api/meals/analysis?date=2025-04-21`로 첫날 데모 식단을 분석할 수 있습니다. `demo02` / `DemoMeal02!`에는 별도의 식단이 있습니다.

## API

| 메서드와 경로 | 기능 | 로그인 | 성공 |
| --- | --- | --- | --- |
| `POST /api/users` | 회원가입 | 선택 | 201 |
| `POST /api/session` | 아이디와 비밀번호로 로그인 | 선택 | 201 |
| `GET /api/session` | 현재 로그인 확인 | 필요 | 200 |
| `DELETE /api/session` | 로그아웃 | 선택 | 204 |
| `GET /api/users/me` | 내 프로필 조회 | 필요 | 200 |
| `PUT /api/users/me` | 내 프로필 변경 | 필요 | 200 |
| `DELETE /api/users/me` | 비밀번호 확인 후 회원과 내 식단 삭제 | 필요 | 204 |
| `GET /api/foods` | 음식 이름·코드 검색 | 선택 | 200 |
| `GET /api/meals` | 내 식단 전체·날짜별 목록 | 필요 | 200 |
| `GET /api/meals/{id}` | 내 식단 상세와 한 끼 영양 합계 | 필요 | 200 |
| `POST /api/meals` | 식단 작성 | 필요 | 201 |
| `PUT /api/meals/{id}` | 식단 수정 | 필요 | 200 |
| `DELETE /api/meals/{id}` | 식단 삭제 | 필요 | 204 |
| `GET /api/meals/analysis?date=YYYY-MM-DD` | 하루 영양 합계와 음식별 열량 순위 | 필요 | 200 |

회원가입은 `username`, `password`, `name`, `height`, `weight`를 받습니다. 프로필을 수정할 때는 `name`, `height`, `weight`를 보냅니다. 두 요청에서 `conditions`는 선택이며, 탈퇴 요청은 JSON 본문에 `password`를 넣습니다.

음식 검색은 `q`, `mode=contains|prefix`, `page`, `size`를 받습니다. 기본 방식은 이름에 포함된 검색어 또는 대소문자를 구분하지 않는 식품코드 검색입니다. `page`는 1부터 시작하고 `size`는 최대 100입니다. 식단 목록은 선택적으로 `date=YYYY-MM-DD`를 받습니다.

식단 생성과 수정의 요청 예시입니다. `note`와 `unit`은 생략할 수 있으며 `unit`을 보내면 음식의 기준 단위와 일치해야 합니다. 생성 응답의 `Location` 헤더에서 상세 주소를 확인할 수 있습니다.

```json
{
  "date": "2025-04-21",
  "type": "점심",
  "note": "현미밥과 수박주스",
  "items": [
    { "foodCode": "D101-050000000-0001", "amount": 150, "unit": "g" },
    { "foodCode": "D220-716000000-0068", "amount": 200, "unit": "mL" }
  ]
}
```

API 오류 응답에는 `status`와 `message`가 들어갑니다. 잘못된 입력은 400, 로그인 필요 또는 로그인 실패는 401, 찾을 수 없거나 다른 회원의 식단인 경우 404, 중복 아이디는 409를 반환합니다.

## 구현 구조

| 구성 | 역할 |
| --- | --- |
| `controller` | HTTP 요청·응답, 세션, 공통 오류 응답 |
| `service` | 입력 검증, 영양 분석, 식단 저장 트랜잭션 |
| `repository` | MyBatis Mapper 인터페이스 |
| `domain` | 음식·회원·식단·영양값 모델 |
| `dto` | API 요청과 응답 형식. 회원 응답에는 비밀번호 해시를 담지 않습니다. |
| `src/main/resources/mappers` | SQL과 결과 매핑을 정의한 XML |
| `db` | 스키마, 음식·데모 데이터, 확인용 SQL |

Spring은 컨트롤러에 서비스를, 서비스에 Mapper를 생성자로 주입합니다. MyBatis는 XML의 `namespace`와 SQL의 `id`로 인터페이스의 메서드를 찾습니다. 음식 검색과 날짜별 목록에는 `<if>`, `<choose>`, `<where>`를 사용하고 여러 식재료를 저장할 때는 `<foreach>`를 사용합니다. 식단과 음식 항목의 저장은 Spring AOP가 적용되는 `@Transactional` 서비스 메서드에서 함께 처리합니다.

회원 한 명은 여러 식단을 갖고, 식단 하나는 여러 음식 항목을 갖습니다. 항목에는 음식의 식품코드와 섭취량을 저장합니다. 회원을 삭제하면 해당 회원의 식단과 음식 항목도 삭제됩니다. 영양 조회는 음식의 기준량과 실제 섭취량으로 계산합니다.

로컬 프론트엔드 개발 시 기본 CORS 허용 주소는 `http://localhost:5500`과 `http://127.0.0.1:5500`입니다. 변경하려면 `YAMYAM_CORS_ORIGINS`에 주소를 쉼표로 구분해 설정합니다. 프론트엔드와 API가 같은 호스트 이름을 쓰도록 맞추고, 별도 포트에서 호출할 때 브라우저 요청에 `credentials: "include"`를 지정합니다.

## 검증

초기 SQL을 적용한 MySQL의 접속 정보를 `MYSQL_TEST_URL`, `MYSQL_TEST_USER`, `MYSQL_TEST_PASSWORD`에 설정합니다. Windows의 `.\mvnw.cmd test` 또는 Linux/macOS의 `sh ./mvnw test`로 HTTP 통합 테스트 3개를 실행합니다. 실제 MySQL에서 회원·식단 변경, 세션과 회원별 접근, 음식 검색과 날짜 필터, Swagger·CORS를 확인합니다.
