# 세션 vs JWT 로그인, 주문-재고 연동, Locust 부하 실험 예제

이 저장소는 네 개의 Spring Boot 마이크로서비스와 Locust 스크립트로 구성된 실습 예제입니다. 목표는 다음을 비교/실험하는 것입니다.

1. **세션 기반 로그인**(`auth-session`)
2. **JWT 기반 로그인**(`auth-jwt`)
3. **주문-재고 연동**(`order` ↔ `product`)
4. **Locust 부하 실험**을 통한 p95 / 5xx / RPS 관찰

## 모듈 개요

| 모듈 | 포트 | 설명 |
| --- | --- | --- |
| `login_session` | `8081` | 세션 기반 로그인 서버 (HttpSession) |
| `login_jwt` | `8082` | JWT 기반 로그인 서버 (HS256) |
| `server_order` | `8083` | 주문 API. 세션 쿠키 또는 JWT 토큰으로 인증 후 상품 재고 예약 |
| `server_product` | `8084` | 상품/재고 API. 재고 감소 트랜잭션 처리 |

각 서비스는 `Spring Web`, `Spring Data JPA`, `Actuator`, `H2/MySQL` 드라이버를 사용합니다. 기본 데이터는 애플리케이션 구동 시 자동으로 시드됩니다.

## 실행 방법

### 1. MySQL 없이(H2 인메모리)

모든 서비스는 기본적으로 H2 인메모리 데이터베이스를 사용합니다. 개별 서비스 디렉터리에서 다음 명령으로 실행합니다.

```bash
./gradlew bootRun
```

각 서비스가 동시에 필요하므로 터미널을 4개 열거나 `tmux` 등을 사용하세요.

### 2. MySQL 사용

1. 로컬 MySQL 또는 Docker(MySQL 8)를 실행합니다.
2. 각 서비스의 `src/main/resources/application.properties` 파일에서 H2 설정을 주석 처리하고, MySQL 설정 주석을 해제 후 접속 정보를 맞춥니다.
3. 테이블은 JPA `ddl-auto=update` 옵션으로 자동 생성됩니다.

예시 Docker 실행:

```bash
docker run --name demo-mysql -e MYSQL_ROOT_PASSWORD=local-password -e MYSQL_DATABASE=demo -p 3306:3306 -d mysql:8
```

## API 사용 예시

### 세션 로그인

```bash
# 로그인 (쿠키 확보)
curl -i -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password"}' \
  http://localhost:8081/login

# 세션 인증 확인
curl -i -X GET \
  -H "Cookie: JSESSIONID=<위에서 받은 값>" \
  http://localhost:8081/whoami
```

### JWT 로그인

```bash
# 토큰 발급
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"password"}' \
  http://localhost:8082/login | jq -r '.token')

# 토큰 검증
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8082/whoami
```

### 주문 생성

```bash
# JWT로 주문 생성
curl -i -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":1}' \
  http://localhost:8083/order

# 세션으로 주문 조회 (세션 쿠키 재사용)
curl -i -H "Cookie: JSESSIONID=<세션 값>" http://localhost:8083/order/1
```

`order` 서비스는 우선 JWT 인증 헤더를 검증하고, 없을 경우 `Cookie` 헤더를 `auth-session`의 `/whoami`에 포워딩하여 인증합니다.

## Locust 부하 실험

### 사전 준비

1. Python 3.9+ 환경에서 Locust를 설치합니다.

```bash
pip install locust
```

2. 모든 Spring Boot 서비스가 기동 중이어야 합니다.

### 실행

```bash
locust -f locustfile.py
```

Locust UI(`http://localhost:8089`)에 접속하여 사용자 수/스폰 속도를 입력하고 다음 시나리오를 순서대로 수행합니다.

1. **Baseline (10 동시 사용자)**: `BaselineUser`만 선택. `/health`, `/product/{id}` 응답 p95, RPS 기준선 확인
2. **세션 로그인 공격**: `SessionAttackUser`만 선택. 대량 로그인 실패/성공 혼합으로 세션 서버 지연 및 5xx 여부 관찰
3. **세션 공격 + 주문 정상요청**: `SessionAttackUser`, `JwtAndOrderUser` 동시 선택. 세션 서버 장애 전파가 주문 API에 미치는 영향 확인
4. **JWT 로그인 집중 공격**: `JwtAndOrderUser`만 선택하고 주문 수량을 크게. 토큰 재사용 때문에 주문 API가 상대적으로 안정적인지 확인
5. **JWT 공격 + 주문 혼합**: JWT 사용자와 Baseline을 함께 돌려 재고/주문 처리의 지속성 확인

각 단계에서 Locust UI의 **p95 latency**, **#failures(5xx)**, **RPS** 그래프/표를 스크린샷으로 남겨 README나 문서에 기록하면 비교가 쉽습니다.

### 기대 관찰 포인트

- 세션 방식은 로그인 서버의 상태(세션 저장소, 동시 접속) 영향이 커서 공격 시 전체 응답 지연과 실패가 급증할 수 있습니다.
- JWT 방식은 토큰 재사용이 가능하므로 로그인 서버가 순간적으로 느려져도 기존 토큰을 활용해 주문 API가 비교적 안정적으로 동작합니다.

## 프로젝트 구조

```
DDD/
├── README.md
├── locustfile.py
├── login_session/
├── login_jwt/
├── server_order/
└── server_product/
```

각 모듈은 독립적인 Gradle 프로젝트이며 `./gradlew bootRun` 또는 `./gradlew build`로 개별 실행/빌드가 가능합니다.

## 추가 메모

- 실험을 자동화하려면 Docker Compose로 4개의 서비스를 한번에 띄우고 Locust를 사이드카로 붙이는 방법도 고려할 수 있습니다.
- 보안을 단순화하기 위해 비밀번호는 평문으로 저장했습니다. 실제 환경에서는 BCrypt, HTTPS, 중앙 세션 스토어/토큰 서명 키 관리 등이 필요합니다.
