# UI 디자인 가이드

> design/tokens.jsx와 design/shared.jsx에서 추출. Cozy Cafe & Coffee Tree 테마.

## 디자인 원칙
1. **매일 쓰는 학습 도구** — 마케팅 페이지가 아니라 매일 복습하는 대시보드. 심플하고 기능 중심
2. **따뜻한 카페 분위기** — 크림, 오트밀, 라떼 브라운 톤. 눈이 편안하고 아늑한 느낌
3. **한 손 복습** — 모바일 우선. 엄지 하나로 카드 플립 + 응답 가능한 레이아웃

## AI 슬롭 안티패턴 — 하지 마라
| 금지 사항 | 이유 |
|-----------|------|
| backdrop-filter: blur() | glass morphism은 AI 템플릿의 가장 흔한 징후 |
| gradient-text (배경 그라데이션 텍스트) | AI가 만든 SaaS 랜딩의 1번 특징 |
| box-shadow 글로우 애니메이션 | 네온 글로우 = AI 슬롭 |
| 보라/인디고 브랜드 색상 | "AI = 보라색" 클리셰. 우리는 브라운/세이지 |
| 배경 gradient orb (blur-3xl 원형) | 모든 AI 랜딩 페이지에 있는 장식 |

## 색상
### 배경
| 용도 | Light | Dark | Tailwind 참고 |
|------|-------|------|--------------|
| 페이지 배경 | #FAF6F0 | #1F1812 | bg-cream |
| 카드/올림면 | #FFFCF7 | #2A2018 | bg-raised |
| 크라프트지 | #E8DCC8 | #3A2D22 | bg-kraft |
| 부드러운 배경 | #F2EADD | #332821 | bg-soft |

### 텍스트
| 용도 | Light | Dark |
|------|-------|------|
| 주 텍스트 (ink) | #3D2E22 | #F5EBD9 |
| 본문 (inkSoft) | #6B5644 | #C8B59E |
| 보조 (inkMuted) | #9A8676 | #8C7A66 |

### 포인트 색상 (팔레트)
| 팔레트 | Primary | Deep | Soft |
|--------|---------|------|------|
| 라떼 브라운 | #A67C52 | #8C6440 | #E8D5BE |
| 모카 | #6F4E37 | #523926 | #D9C5B0 |
| 세이지 그린 | #7A8F6B | #5A6E4F | #D4DCC4 |

### 시맨틱 색상
| 용도 | 값 |
|------|------|
| 세이지 (식물/성장) | #7A8F6B |
| 세이지 배경 | #EBF0E2 |
| 경고/시들음 | #C77E47 |

## 컴포넌트
### 카드 (Surface)
```
bg-raised rounded-[20px] border border-hairline
shadow: 0 2px 6px rgba(120,90,60,0.08), 0 12px 28px -8px rgba(120,90,60,0.12)
```

### 버튼 (CafeButton)
```
Primary: bg-primary text-white rounded-[14px] h-[42px] shadow-primary
Secondary: bg-raised text-ink rounded-[14px] border-hairline shadow-card
Ghost: bg-transparent text-ink border-hairline
Sage: bg-sage text-white rounded-[14px] shadow-sage
```

### 탭 (TabPills)
```
컨테이너: bg-soft rounded-[14px] border-hairline p-1 flex gap-1.5
활성 탭: bg-raised text-ink font-semibold shadow-sm rounded-[10px]
비활성 탭: bg-transparent text-inkMuted
뱃지: bg-primary text-white text-[10px] px-1.5 rounded-full (활성 시)
```

### 입력 필드
```
bg-soft rounded-[12px] border border-hairline px-4 py-3 text-ink
```

### 상황 구름 말풍선 (SituationCloud)
```
bg-sageBg text-sage rounded-[18px] px-3.5 py-2 border border-sage/10
shadow: 0 2px 6px sage/10
꼬리: SVG 삼각형 (bottom-left)
```

### 칩 (Chip)
```
inline-flex px-2.5 py-1 rounded-full text-[11.5px] font-medium
default: bg-soft text-inkSoft
sage: bg-sageBg text-sage
warm: bg-primarySoft text-primaryDeep
```

## 레이아웃
- 전체 너비: 모바일 100%, 데스크톱 max-w-md (420px) 중앙 정렬
- 하단 네비게이션: 고정, paddingBottom 28px (safe area)
- 상단 헤더: paddingTop 40px (노치/다이나믹 아일랜드)
- 간격: gap-3~4, 섹션 간 mt-5~6
- 카드 패딩: p-4~5

## 타이포그래피
| 프리셋 | Heading | Body | Serif |
|--------|---------|------|-------|
| warm (기본) | Gowun Dodum + Fraunces | Gowun Dodum | Fraunces |
| modern | Pretendard + Inter | Pretendard | Fraunces |

| 용도 | 스타일 |
|------|--------|
| 페이지 제목 | text-lg font-semibold text-ink tracking-tight |
| 카드 제목 | text-sm font-semibold text-ink |
| 본문 | text-sm text-inkSoft leading-relaxed |
| 보조/라벨 | text-xs text-inkMuted |
| 섹션 헤더 | text-xs font-semibold text-inkSoft uppercase |

## 애니메이션
- **카드 플립**: CSS transform rotateY(180deg), transition 0.4s ease
- **버튼 누름**: translateY(1px) on mouseDown, 0.12s ease
- **탭 전환**: all 0.15s ease
- **커피나무 시들음/회복**: opacity + 색상 변화 transition
- **로딩 (CremaLoader)**: 크레마 나선 SVG — opacity 0→1→0 + rotate 360deg + scale 0.3→1→0.3, 2.5s ease-in-out infinite
- **로딩 (CoffeeSpinner)**: 커피잔 SVG 전체 회전, 1s linear infinite
- 그 외 불필요한 애니메이션 금지

## 로딩 컴포넌트

### CremaLoader (AI 호출용)
```
중앙 정렬, 머그잔 탑뷰 SVG (120x120 viewBox)
소서: kraft (#E8DCC8) — 외곽 원
머그잔: primary (#A67C52) — 중간 원
커피: mocha-deep (#523926) — 내부 원
크레마: primary-soft (#E8D5BE) — 나선 path, crema-swirl 애니메이션
손잡이: 우측 반원 path
아래 텍스트: text-sm text-ink-muted (message prop)
접근성: role="status" aria-live="polite"
```

### CoffeeSpinner (일반 로딩용)
```
인라인 사이드뷰 커피잔 SVG (20x20 기본)
coffee-spin 애니메이션으로 회전
텍스트와 나란히 배치:
  flex items-center justify-center gap-2 py-8
  <CoffeeSpinner /> + <span text-sm text-ink-muted>불러오는 중...</span>
```

## 아이콘
- SVG 인라인, strokeWidth 1.5~2
- 네비게이션: 활성 시 fill 12% opacity + strokeWidth 2
- 아이콘 컨테이너(둥근 배경 박스)로 감싸지 않는다

## 하단 네비게이션
```
[🏠 홈] [📖 단어] [🔤 패턴] [✨ 생성] [🃏 복습]
학습 기록, 설정은 홈 화면 내 링크로 접근
```

## 로그인/회원가입 페이지

> 목업: `design/screens-auth.jsx` 참조

### 공통
- 하단 네비게이션 숨김 (BottomNav 미표시)
- 중앙 정렬, max-w-md 유지
- 입력 필드: `bg-raised rounded-[14px] border border-hairline px-3.5 py-3` + 좌측 아이콘 (메일/자물쇠/사람) — 기존 페이지(bg-soft, rounded-12)와 의도적으로 다름. 인증 페이지는 카페 느낌 강조
- 에러 상태: `border-warn` + `shadow-[0_0_0_3px_rgba(199,126,71,0.13)]` + `text-warn text-xs`
- 제출 버튼: Primary 버튼 lg (`bg-primary text-white rounded-[14px] h-[48px] text-[15px]`)
- 소셜 로그인 (Google/카카오): 이번 Phase 미구현, 향후 추가 가능

### 로그인 페이지 (/login)
- 상단 일러스트: 창가 SVG (카페 분위기, 세이지 그린 나무 + 커피잔)
- 카피: "같은 자리, 같은 잔으로 다시 시작해요" (serif italic)
- 이메일 입력 (메일 아이콘)
- 비밀번호 입력 (자물쇠 아이콘 + 눈 토글)
- "이메일 저장" 체크박스 — `text-[13px] text-ink-muted`, 체크박스 `w-4 h-4 accent-primary`
- "로그인" 버튼 (Primary lg)
- 하단: "계정이 없나요? 회원가입 →" → /signup

### 회원가입 페이지 (/signup)
- TopBar: "회원가입" + 뒤로가기 버튼 (→ /login)
- 카피: "처음 오신 걸 환영해요" (serif italic) + "기본 정보만 알려주세요"
- 닉네임 입력 (라벨 + 사람 아이콘)
- 이메일 입력 (라벨 + 메일 아이콘)
- 비밀번호 입력 (라벨 + 자물쇠 아이콘 + 눈 토글, 최소 8글자 힌트)
- 비밀번호 확인 (라벨 + 자물쇠 아이콘 + 일치 시 세이지 체크 뱃지)
- "가입하기" 버튼 (Primary lg)
- 하단: "이미 회원이신가요? 로그인" → /login

### 홈 페이지 인사 영역 변경
- 기존: "Cozy Cafe"
- 변경: "{닉네임}님 안녕하세요"
- 부제: "오늘도 한 모금 천천히" 유지
