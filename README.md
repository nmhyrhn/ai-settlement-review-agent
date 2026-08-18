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
- 검수 규칙 단위 테스트

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Web
- Bean Validation
- JUnit 5 / AssertJ
- Gradle

## 실행

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

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
- [AI 협업 기록](docs/ai-collaboration.md)

## 다음 구현

- CSV 업로드
- Spring AI 연동 및 구조화된 응답
- AI API 실패 fallback
- 사용자 승인·보류와 처리 이력
- 검수 결과 화면


