# PLAN: 단어 벌크 보강 배치 최적화

## 작업 목표
단어 벌크 등록 시 Gemini API 호출을 건바이건(N회) → 25개 단위 배치(⌈N/25⌉회)로 변경하여 API 한도 절약

## 구현할 기능
1. 배열 기반 벌크 보강 프롬프트 + 응답 DTO
2. WordService.bulkCreate 배치 처리 로직
3. 기존 단건 등록(create)은 변경 없음

## 기술적 제약사항
- 배치 크기: 25개 (상수로 관리)
- 에러 처리: 배치 단위 실패 시 해당 배치 전체 미보강 저장, 나머지 배치 계속 진행
- 기존 3회 재시도 정책(GeminiClient.callWithRetry) 그대로 활용
- GeminiClient 자체는 변경 불필요 (프롬프트만 변경)

## 테스트 전략
- 기존 테스트 영향: WordServiceTest.BulkCreate 테스트 기대값 변경 (건바이건 → 배치 호출 검증)
- 신규 테스트:
  - 배치 분할 검증 (예: 55개 → 3배치: 25+25+5)
  - 배치 실패 시 해당 배치만 미보강 저장 확인
  - 배치 응답에서 개별 단어 매핑 정확성

## Phase/Step 초안

### Step 0: 벌크 보강 DTO + 프롬프트
- 작업:
  - `BulkWordEnrichment` DTO 생성 (배열 응답: `List<WordEnrichmentItem>`)
  - `WordService.buildBulkEnrichmentPrompt(List<WordCreateRequest>)` 메서드 추가
  - 프롬프트 구조: 단어 배열 입력 → 보강 배열 응답 (GenerateService 패턴 참고)
- 산출물: DTO + 프롬프트 빌더 (테스트 포함)
- 주요 파일:
  - `backend/src/main/java/com/english/word/BulkWordEnrichment.java` (신규)
  - `backend/src/main/java/com/english/word/WordService.java` (메서드 추가)
  - `backend/src/test/java/com/english/word/WordServiceTest.java` (테스트 추가)

### Step 1: bulkCreate 배치 처리 적용
- 작업:
  - `WordService.bulkCreate`를 배치 로직으로 리팩터링
  - 중복 검사(건바이건 유지) → 유효한 단어만 모아서 25개씩 분할 → 배치 보강 호출 → 결과 매핑
  - 배치 실패 시 해당 단어들 미보강 저장
- 산출물: 리팩터링된 bulkCreate (기존 테스트 수정 + 배치 테스트 추가)
- 주요 파일:
  - `backend/src/main/java/com/english/word/WordService.java` (bulkCreate 수정)
  - `backend/src/test/java/com/english/word/WordServiceTest.java` (테스트 수정/추가)

## 미결 사항
- 없음
