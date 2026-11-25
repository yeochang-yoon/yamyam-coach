# 외부 구성요소 안내

## 저장소에 포함한 배포 파일

Bootstrap 5.3.8 CSS를 PJT3의 `web/vendor`, PJT4·5의 `src/main/webapp/vendor`에 포함한다. Copyright (c) 2011-2025 The Bootstrap Authors. MIT License. 각 폴더의 `bootstrap.LICENSE`를 참조한다.

Maven Wrapper 3.3.4 실행 스크립트와 배포 설정을 PJT1·2·4·5·7에 포함한다. Apache License 2.0. 각 실행 스크립트의 저작권·라이선스 주석을 보존했다. Maven 자체 배포본과 라이브러리 JAR는 저장소에 포함하지 않는다.

## 빌드·실행 의존성

Java 프로젝트는 단계별로 Gson, JUnit, Tomcat, JSTL과 Maven 플러그인을 사용한다. PJT5와 PJT7은 MySQL Connector/J를 사용하며, PJT7은 Spring Boot, MyBatis와 springdoc-openapi를 사용한다. 각 패키지의 라이선스는 해당 프로젝트와 배포 메타데이터를 따른다.

## 데이터

기본 음식 목록은 식품의약품안전처와 농촌진흥청의 음식 영양성분 자료에서 추출했다. PJT1~4는 JSON, PJT5·7은 SQL 형식으로 제공한다. 회원과 식단 샘플은 가상 데이터다. 외부 음식 DB를 변환해 배포할 때는 해당 데이터의 출처와 이용 조건을 유지해야 한다.
