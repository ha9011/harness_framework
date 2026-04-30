# 아키텍처

## 디렉토리 구조
```
design/                # HTML 디자인 목업 시안
backend/               # 백엔드 코드 (API, 서버)
frontend/              # 프론트엔드 코드 (UI, 클라이언트)
docs/                  # 프로젝트 문서
phases/                # harness 실행 메타데이터
harness/               # harness 프레임워크
```

## 패턴
{사용하는 디자인 패턴 (예: Server Components 기본, 인터랙션이 필요한 곳만 Client Component)}

## 데이터 흐름
```
{데이터가 어떻게 흐르는지 (예:
사용자 입력 → Client Component → API Route → 외부 API → 응답 → UI 업데이트
)}
```

## 상태 관리
{상태 관리 방식 (예: 서버 상태는 Server Components, 클라이언트 상태는 useState/useReducer)}
