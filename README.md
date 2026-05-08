# 영어 패턴 학습기 실행 안내

## 1) DB 실행 (PostgreSQL 16)
```bash
docker compose up -d
```

## 2) Gemini API 키 설정
```bash
export GEMINI_API_KEY=your_api_key
```

`GEMINI_API_KEY`가 없으면 Gemini 실제 호출은 수행되지 않습니다.

## 3) 백엔드 실행
```bash
cd backend
./mvnw spring-boot:run
```

## 4) 프론트엔드 실행
```bash
cd frontend
npm run dev
```
