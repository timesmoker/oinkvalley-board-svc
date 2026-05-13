# board-svc

Spring Boot **게시판 REST API**다. 게시판(Board)·게시글(Post)·댓글(Comment)을 PostgreSQL에 두고, **JWT(HS256)** 로 상태 없이 인증한다. 클라이언트는 `**Authorization: Bearer <token>`** 만 보내며, 쿠키에 실린 토큰은 이 서비스에서 직접 읽지 않는다.

의존성·JDK·Gradle 버전 요약은 `**[DEPENDENCIES.md](DEPENDENCIES.md)**` 를 본다.

**단일 진실 소스(SOT):** 배포·운영에서 쓰는 값의 기준은 **무조건 `infra` 폴더**(Helm values, 매니페스트, 환경 변수 정의 등)에 있다. 이 저장소의 `application.properties` 와 여기 문서는 편의·개발용 설명이며, 충돌하면 `**infra` 쪽이 정답이다.**

## 목차

- [빠른 시작](#빠른-시작)
- [설정](#설정)
- [프로젝트 구조](#프로젝트-구조)
- [HTTP API](#http-api)

## 빠른 시작

- **JDK 21**, 저장소에 포함된 **Gradle Wrapper** (`./gradlew`) 를 쓴다.
- **빌드:** `./gradlew bootJar`
- **로컬 실행:** `./gradlew bootRun` — DB·JWT 등은 아래 [설정](#설정)을 맞춘다.

## 설정

런타임은 `**src/main/resources/application.properties`** 를 따른다. **DB 연결은 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 를 반드시 준다(기본값 없음).** 로컬 `bootRun` 도 동일하게 환경 변수를 맞춘다. `JWT_SECRET`·`SPRING_JPA_HIBERNATE_DDL_AUTO` 만 프로퍼티에 `${변수:기본값}` 가 있다.

**SOT 재확인:** 클러스터·배포에 실제로 쓰는 키·값은 `**infra` 폴더**를 따른다(이 절의 표는 이름·역할 참고용).


| 환경 변수                           | 바인딩(요지)                         | 설명                                                                               |
| ------------------------------- | ------------------------------- | -------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`         | `spring.datasource.url`         | JDBC URL (**필수**)                                                                |
| `SPRING_DATASOURCE_USERNAME`    | `spring.datasource.username`    | DB 사용자 (**필수**)                                                                  |
| `SPRING_DATASOURCE_PASSWORD`    | `spring.datasource.password`    | DB 비밀번호 (**필수**)                                                                 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` | 예: `validate`, `update` (기본값 `validate`)                                         |
| `JWT_SECRET`                    | `jwt.secret`                    | HS256 검증용 비밀키(UTF-8 **32바이트 이상** 권장). 토큰을 **발급하는 서비스(예: auth-svc)와 같은 값**이어야 한다. |


**JWT:** 이 서비스는 토큰을 **검증만** 한다. 클레임·발급 정책은 발급 쪽과 맞춘다. 자세한 헤더·규칙은 아래 [보안](#보안-구현-요약)을 본다.

## 프로젝트 구조


| 경로               | 역할                                                                           |
| ---------------- | ---------------------------------------------------------------------------- |
| `controller/`    | `BoardController`, `PostController`, `CommentController`, `HealthController` |
| `service/`       | 도메인 로직                                                                       |
| `dto/`           | 요청·응답 레코드                                                                    |
| `db/domain/`     | JPA 엔티티 `Board`, `Post`, `Comment`                                           |
| `db/repository/` | `BoardRepository`, `PostRepository`, `CommentRepository`                     |
| `security/`      | JWT 검증, `RestrictedBoardReadRequestMatcher`                                  |
| `config/`        | `SecurityConfig` 등                                                           |


## HTTP API

베이스 URL·리버스 프록시 접두 경로는 이 저장소에서 고정하지 않는다.

### 보안 (구현 요약)

- Spring Security, **무상태**(세션 미사용), **CSRF 비활성화**. CORS는 이 애플리케이션에 두지 않았고, 필요하면 게이트웨이·프록시에서 맞춘다.
- JWT는 `**Authorization: Bearer <token>`** 만 처리한다.
- `JwtAuthenticationFilter` 가 토큰을 검증하고 `SecurityContext` 를 채운다.
- **규칙 순서 (`SecurityConfig`):** 위에서 아래로 먼저 매칭된다.
  1. `GET /health` → 허용.
  2. `RestrictedBoardReadRequestMatcher` 에 걸리는 **GET** → 인증 필요(유효 JWT). 대상 게시판이 `**isPrivate`** 이거나 `**isActive == false**` 이면 익명 요청은 거부된다.
  3. `GET /boards/**`, `GET /posts/**`, `GET /comments/**` → 매처 다음 규칙상 허용이지만, **2번이 먼저** 적용된다.
  4. 나머지 → 인증 필요(`POST` / `PUT` / `DELETE` 등).

#### 비공개 보드와 조회


| 게시판 상태                                     | 해당 게시판으로 이어지는 **조회용 GET** (예: `/boards/{segment}`, 글·댓글 조회) |
| ------------------------------------------ | ----------------------------------------------------------- |
| `isPrivate == false` 이고 `isActive == true` | JWT 없이(익명) 허용                                               |
| `isPrivate == true` 또는 `isActive == false` | 유효한 JWT 가 있는 요청만 허용                                         |


보드별 멤버십은 검사하지 않는다. `GET /boards` 목록은 매처 밖이라, 구현상 비공개 게시판 메타가 목록에 나올 수 있다.

**토큰 검증 시점에 쓰는 클레임(다른 서비스에서 맞출 때):** HS256, `sub` 는 **숫자 문자열**(사용자 ID), `roles` 문자열 배열(클레임에는 `ROLE_` 접두사 없이, 필터에서 스프링 규약에 맞게 붙임), 선택 `exp` 등.

게시글 단건은 `GET /posts/{id}` 가 없고, `**GET /boards/{segment}/{postId}`** 만 제공한다.

### 경로 `{segment}` (`/boards/{segment}/…`)

- 세그먼트가 **숫자만**이면 게시판 **PK** (`Long`).
- 아니면 **slug** 문자열.

컨트롤러 매핑 순서: `/{segment}/write` → `/{segment}/{postId}` → `/{segment}`.

### 엔드포인트

표의 **매처** 는 `RestrictedBoardReadRequestMatcher` 를 뜻한다(비공개·비활성 보드면 JWT 필요).


| 메서드    | 경로                           | 인증  | 비고                               |
| ------ | ---------------------------- | --- | -------------------------------- |
| GET    | `/health`                    | 불필요 | 문자열 본문                           |
| GET    | `/boards`                    | 불필요 | 활성 게시판만. `BoardResponse[]`       |
| GET    | `/boards/{segment}`          | 매처  | `BoardPostsBundleResponse`       |
| GET    | `/boards/{segment}/write`    | 매처  | 글쓰기 화면용 게시판 메타 `BoardResponse`   |
| GET    | `/boards/{segment}/{postId}` | 매처  | 글이 해당 게시판에 없으면 **404**           |
| POST   | `/boards`                    | 필요  | `BoardResponse`                  |
| PUT    | `/boards/{boardId}`          | 필요  | `{boardId}` 는 숫자 PK 만            |
| POST   | `/posts`                     | 필요  | `PostResponse`                   |
| PUT    | `/posts/{postId}`            | 필요  | 작성자만 **200** / **403** / **404** |
| DELETE | `/posts/{postId}`            | 필요  | 작성자만 **204** / **403** / **404** |
| GET    | `/comments?postId=`          | 매처  | `Page<CommentResponse>`          |
| GET    | `/comments/{commentId}`      | 매처  | `CommentResponse`                |
| POST   | `/comments?postId=`          | 필요  | `CommentResponse`                |
| PUT    | `/comments/{commentId}`      | 필요  | 작성자만 **200** / **403** / **404** |
| DELETE | `/comments/{commentId}`      | 필요  | 작성자만 **204** / **403** / **404** |


페이지네이션: Spring `Pageable` — 쿼리 `page`, `size`, 선택 `sort`.

JSON 은 **camelCase** 다.

### 응답·요청 필드 요약

- **BoardResponse:** `id`, `name`, `slug`, `summary`, `isPrivate`, `isActive`, `createdAt`, `updatedAt`
- **BoardPostsBundleResponse:** `board`, `posts`
- **PostSummaryResponse:** `id`, `title`, `userId`, `createdAt`, `commentCount`
- **PostResponse:** `id`, `userId`, `boardId`, `title`, `content`(JSON 객체), `createdAt`, `updatedAt`
- **CommentResponse:** `id`, `userId`, `postId`, `content`(JSON 객체), `createdAt`, `updatedAt`

**요청 본문**

- Board 생성·수정: `name`, `slug`, `summary`, `isPrivate`, `isActive`
- Post 생성: `boardId`, `title`, `content` — 작성자 ID 는 인증에서 결정
- Post 수정: `title`, `content`
- Comment 생성·수정: `content`

### 삭제 동작

Post 삭제 시 연관 Comment 는 JPA cascade · orphanRemoval 으로 함께 제거된다.
