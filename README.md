# AI Settlement Review Agent

정산 데이터의 이상 항목을 규칙 기반으로 검수하고, AI가 확인 이유와 후속 작업을 설명하는 Spring Boot 기반 업무 보조 Agent입니다.

## 프로젝트 목표

정확한 판별이 필요한 금액·중복·증빙 검사는 Java 코드로 처리합니다. AI는 검수 결과를 사용자가 이해하기 쉽게 설명하고 확인 사항을 제안하는 역할을 담당합니다. AI의 제안은 사용자가 승인하기 전까지 실제 처리 상태를 변경하지 않습니다.

## 현재 구현

- 1,000,000원 초과 거래 검수
- 증빙 번호 누락 검수
- 날짜·거래처·금액 기준 중복 결제 검수
- 여러 검수 규칙을 조합하는 서비스
- AI 장애 시에도 사용할 수 있는 기본 설명 구현
- JSON 기반 검수 API
- CSV 업로드·입력 검증 API
- 구조화된 AI 설명과 API 장애 fallback
- CSV 검수 결과 화면
- 사용자 정상 처리·재확인·보류와 MySQL 영구 이력
- 검수 규칙 단위 테스트

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Web
- Bean Validation
- JUnit 5 / AssertJ
- Gradle
- MySQL 8 이상

## 실행

MySQL 서버에서 최초 한 번 프로젝트 DB와 로컬 개발 계정을 생성합니다.

```bash
mysql -u root -p < docs/mysql-setup.sql
```

환경별 접속 정보는 `.env`에 둡니다. 기본 로컬 개발값은 `.env.example`과 같습니다.

```bash
cp .env.example .env
# .env에서 OPENAI_API_KEY와 DB 접속 정보를 환경에 맞게 수정
./gradlew clean test
./gradlew bootRun
```

브라우저에서 `http://localhost:8080`을 엽니다. API 키가 없으면 기본 설명으로 정상 동작합니다.

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

CSV 업로드 API는 `POST /api/reviews/upload`입니다.

```bash
curl -F 'file=@samples/settlements.csv' http://localhost:8080/api/reviews/upload
```

사용자 결정은 `POST /api/reviews/{transactionId}/decisions`, 이력 조회는 `GET`으로 제공합니다.

## 실행 화면

![AI 정산 검수 실행 화면](docs/images/ai-settlement-review-agent.jpg)

## API 예시

`POST /api/reviews/analyze`

```json
{
  "transactions": [
    {
      "transactionId": "T-001",
      "transactionDate": "2026-08-18",
      "merchant": "ABC상사",
      "amount": 1250000,
      "receiptNumber": null
    },
    {
      "transactionId": "T-002",
      "transactionDate": "2026-08-18",
      "merchant": "ABC상사",
      "amount": 1250000,
      "receiptNumber": "R-002"
    }
  ]
}
```

## 설계 문서

- [기획 및 구현 범위](docs/planning.md)
- [아키텍처 및 설계 의사결정](docs/architecture.md)
- [ERD 및 데이터 정의](docs/erd.md)
- [AI 협업 기록](docs/ai-collaboration.md)

## 검증 범위

- 검수 규칙·CSV 파서·처리 이력 단위 테스트
- CSV 업로드와 처리 결정 API 통합 테스트
- 샘플 CSV 브라우저 업로드와 AI/fallback 화면 확인
- H2 MySQL 호환 모드 통합 테스트

## 후속 구현

- 로그인과 사용자별 권한


