# 의존성

**Gradle에 선언된 라이브러리·빌드 도구**만 정리한다. 배포 환경 변수·비밀값 목록은 여기 없다.

## 툴체인·플러그인

| 항목 | 버전·설명 |
|------|-----------|
| JDK | **21** (`build.gradle` 의 `java.toolchain`) |
| Gradle (Wrapper) | **9.4.1** (`gradle/wrapper/gradle-wrapper.properties`) — `./gradlew` 사용 권장 |
| `java` 플러그인 | 표준 Gradle Java 빌드 |
| `org.springframework.boot` | **4.0.5** |
| `io.spring.dependency-management` | **1.1.7** (Spring Boot BOM으로 starter 버전 정렬) |

## Gradle 의존성 선언

아래 좌표는 `build.gradle` 과 동일하다. `org.springframework.boot:*` 버전은 BOM이 관리한다(별도 오버라이드 없음).

| 구분 | Gradle 좌표 |
|------|-------------|
| `implementation` | `org.springframework.boot:spring-boot-starter` |
| `implementation` | `org.springframework.boot:spring-boot-starter-web` |
| `implementation` | `org.springframework.boot:spring-boot-starter-data-jpa` |
| `implementation` | `org.springframework.boot:spring-boot-starter-validation` |
| `implementation` | `org.springframework.boot:spring-boot-starter-security` |
| `implementation` | `com.fasterxml.jackson.core:jackson-databind` |
| `runtimeOnly` | `org.postgresql:postgresql` |
| `compileOnly` | `org.projectlombok:lombok` |
| `annotationProcessor` | `org.projectlombok:lombok` |

### 참고

- **PostgreSQL**: JDBC 드라이버만 포함한다. DB 서버는 별도로 띄운다.
- **`jackson-databind`**: `Post`·`Comment` 의 JSON 컬럼(`content`) 매핑에 사용한다(Boot 의 Jackson 설정과 함께).
