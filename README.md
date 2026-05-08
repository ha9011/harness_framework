# 영어 패턴 학습기

## 사전 조건
- Java 21
- Node.js 20+
- Docker Desktop

## 실행
```bash
docker compose up -d
npm run dev
```

- 백엔드: http://localhost:8080
- 프론트엔드: http://localhost:3000
- 백엔드만 실행: `npm run dev:backend`
- 프론트엔드만 실행: `npm run dev:frontend`

## 환경 변수
```bash
export GEMINI_API_KEY=your_api_key
export APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
export NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

- `GEMINI_API_KEY`가 없으면 이미지 추출/예문 생성은 `AI_SERVICE_ERROR`로 표시됩니다.
- `APP_CORS_ALLOWED_ORIGINS`는 Cookie 인증을 위해 와일드카드 대신 정확한 origin을 사용합니다.

## 검증
```bash
npm run test
npm run lint
npm run build
```
