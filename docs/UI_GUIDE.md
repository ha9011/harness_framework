# UI 디자인 가이드

## 디자인 원칙
1. 모바일 학습 도구처럼 보여야 한다. 랜딩 페이지가 아니라 매일 열어 복습하는 앱이다.
2. Cozy Cafe & Coffee Tree 컨셉을 유지하되, 학습 흐름을 방해하는 장식은 줄인다.
3. 단어/패턴/문장 복습의 상태와 다음 행동이 한눈에 보여야 한다.
4. 손가락으로 누르기 쉬운 버튼, 탭, 카드 크기를 유지한다.
5. 비어 있는 상태에서도 다음 행동이 명확해야 한다.

## AI 슬롭 안티패턴 — 하지 마라
| 금지 사항 | 이유 |
|-----------|------|
| 과한 backdrop-filter: blur() | glass morphism은 템플릿 느낌이 강하고 목업 컨셉과 맞지 않음 |
| gradient-text | 영어 학습 앱보다 SaaS 랜딩처럼 보임 |
| "Powered by AI" 배지 | 기능 가치가 아니라 장식임 |
| box-shadow 글로우 애니메이션 | 카페/학습 컨셉보다 네온 AI 앱처럼 보임 |
| 보라/인디고 중심 브랜드 색상 | Cozy Cafe 팔레트와 충돌함 |
| 모든 카드에 같은 큰 radius | 화면 밀도가 낮고 템플릿처럼 보임 |
| 배경 gradient orb | 학습 화면에서 시선 분산이 큼 |
| 카드 안에 카드 중첩 | 모바일에서 정보 위계가 흐려짐 |

## 색상

### 기본 라이트 테마
| 토큰 | 값 | 용도 |
|------|------|------|
| bg | #FAF6F0 | 페이지 배경, 크림 화이트 |
| bgRaised | #FFFCF7 | 카드/상단 표면 |
| bgKraft | #E8DCC8 | 메뉴판/영수증/복습 카드 앞면 |
| bgSoft | #F2EADD | 입력 필드, 탭 배경, 보조 표면 |
| ink | #3D2E22 | 주 텍스트, 모카 브라운 |
| inkSoft | #6B5644 | 본문 텍스트 |
| inkMuted | #9A8676 | 보조 텍스트 |
| hairline | rgba(61,46,34,0.10) | 카드 border |
| divider | rgba(61,46,34,0.06) | 목록 구분선 |

### 포인트 팔레트
| 이름 | primary | primaryDeep | primarySoft | 용도 |
|------|---------|-------------|-------------|------|
| 라떼 | #A67C52 | #8C6440 | #E8D5BE | 기본 CTA, 주요 선택 상태 |
| 모카 | #6F4E37 | #523926 | #D9C5B0 | 진한 브라운 대체 테마 |
| 세이지 | #7A8F6B | #5A6E4F | #D4DCC4 | 식물, 복습 성공, 상황 말풍선 |

### 시맨틱 색상
| 용도 | 값 | 사용 위치 |
|------|------|----------|
| 성공/EASY | #7A8F6B | 기억남, 복습 완료, 커피나무 건강 상태 |
| 경고/HARD | #C77E47 | 모름, Gemini 실패, 보강 실패 |
| 중요 | #E8C97B | 단어 중요 체크 별 |
| 기본 CTA 텍스트 | #FFFCF7 | primary/sage 버튼 위 텍스트 |

## 컴포넌트

### 앱 Shell
- 모바일 우선으로 구현하고, 데스크톱에서는 중앙에 최대 너비 컨테이너를 둔다.
- 하단 내비게이션은 모바일 고정이며 `홈`, `단어`, `패턴`, `생성`, `복습` 5개 항목을 가진다.
- 학습 기록과 설정은 홈 또는 상단 메뉴에서 접근한다.
- 상단 바는 제목, 뒤로가기, 우측 액션을 제공한다.

### 카드/Surface
```
rounded-[18px] border border-[rgba(61,46,34,0.10)] bg-[#FFFCF7] shadow-[0_2px_6px_rgba(120,90,60,0.08),0_12px_28px_-8px_rgba(120,90,60,0.12)]
```
- 일반 목록 카드는 `rounded-[14px]` 또는 `rounded-[16px]`를 사용한다.
- 핵심 정보 카드와 복습 카드는 `rounded-[20px]` 이상을 허용한다.
- 카드 안에 또 다른 카드처럼 보이는 큰 표면을 넣지 않는다.

### 버튼
```
Primary:   rounded-[14px] bg-[#A67C52] text-[#FFFCF7] shadow-[0_6px_14px_-4px_rgba(166,124,82,0.4)]
Secondary: rounded-[14px] border border-[rgba(61,46,34,0.10)] bg-[#FFFCF7] text-[#3D2E22]
Ghost:     rounded-[14px] border border-[rgba(61,46,34,0.10)] bg-transparent text-[#3D2E22]
Sage:      rounded-[14px] bg-[#7A8F6B] text-[#FFFCF7]
Danger:    rounded-[14px] bg-[#C77E47] text-[#FFFCF7]
```
- 버튼 높이는 작은 버튼 34px, 기본 42px, 큰 CTA 52px를 기준으로 한다.
- 아이콘은 lucide-react를 우선 사용한다.
- 텍스트 버튼보다 아이콘+텍스트 버튼을 우선한다.

### 입력 필드
```
rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-[#F2EADD] px-4 py-3 text-[#3D2E22] placeholder:text-[#9A8676]
```
- 벌크 JSON textarea는 monospace를 사용하고 예시 placeholder를 반드시 제공한다.
- 이미지 업로드는 dashed border dropzone으로 표시한다.
- 추출 결과는 저장 전 편집 가능한 폼으로 보여준다.

### Chip/Tab
```
Chip: rounded-full bg-[#F2EADD] px-2.5 py-1 text-[11px] font-medium text-[#6B5644]
Warm: rounded-full bg-[#E8D5BE] px-2.5 py-1 text-[11px] font-medium text-[#8C6440]
Sage: rounded-full bg-[#EBF0E2] px-2.5 py-1 text-[11px] font-medium text-[#7A8F6B]
```
- 탭은 pill segmented control 형태로 구현한다.
- 복습 탭은 남은 카드 수 badge를 함께 표시한다.

### 복습 플립 카드
- 카드 비율은 `aspect-[3/4]`를 기본으로 한다.
- 앞면은 `bgKraft` 또는 메뉴판 질감, 뒷면은 `bgRaised` 또는 `primarySoft`를 사용한다.
- flip은 `transform-style: preserve-3d`, `backface-visibility: hidden`, `rotateY(180deg)`로 구현한다.
- 문장 카드는 앞면에 영어 예문과 상황 말풍선을 함께 보여준다.
- 액션 버튼은 `모름`, `애매`, `기억남` 3개를 하단 고정으로 배치한다.

### 상황 말풍선
```
rounded-[18px] border border-[#7A8F6B22] bg-[#EBF0E2] px-3.5 py-2 text-[12px] font-medium text-[#7A8F6B]
```
- 예문 위 또는 카드 상단에 배치한다.
- "이 상황을 상상하며 읽어보세요" 같은 설명 문구를 반복 노출하지 말고, 시각 요소로 의미를 전달한다.

## 레이아웃
- 모바일 기준 콘텐츠 좌우 padding은 16px을 기본으로 한다.
- 목록 간격은 6px~8px, 큰 섹션 간격은 16px~22px를 기준으로 한다.
- 페이지 하단은 고정 하단 내비게이션 높이만큼 padding을 둔다.
- CTA는 학습 흐름의 마지막 또는 가장 중요한 위치에 한 개를 크게 둔다.
- 데스크톱에서는 모바일 화면을 그대로 늘리지 말고 `max-w-md` 또는 `max-w-lg` 중심 컨테이너로 제한한다.

## 타이포그래피
| 용도 | 스타일 |
|------|--------|
| 기본 heading | Pretendard 또는 system-ui, font-semibold |
| 감성/카드 영어 문장 | Fraunces 또는 serif fallback |
| 본문 | Pretendard 또는 system-ui, text-sm, leading-relaxed |
| 보조 텍스트 | text-xs 또는 text-[12px], inkMuted |
| JSON textarea | JetBrains Mono 또는 monospace |

## 화면별 핵심 구조
- 홈: 인사말, 커피나무/복습 현황, 큰 복습 시작 CTA, 누적 통계, 최근 학습 목록.
- 단어: 등록 방식 탭, 검색/필터, 단어 목록, 중요 토글.
- 단어 상세: 단어 정보 카드, AI 보강 정보, 예문 목록, 예문 추가 액션.
- 패턴: 직접 입력/이미지 등록, 패턴 목록, 예문 미리보기.
- 패턴 상세: 패턴 정보, 교재 예문, AI 생성 예문, 패턴 예문 생성 액션.
- 예문 생성: 난이도 선택, 개수 선택, Gemini 로딩 상태, 생성 결과 아코디언.
- 복습: 탭, 진행률, 플립 카드, 3단계 응답 버튼, 완료 후 다시보기/추가복습.
- 학습 기록: 날짜 역순, Day N, 등록한 패턴/단어 chip.
- 설정: 하루 복습 개수 선택, 저장 상태.

## 애니메이션
- 허용: 버튼 press `translateY(1px)`, 카드 flip 0.6s, progress width 0.4s, 화면/카드 fade-in 0.2s.
- Gemini 로딩은 커피 추출 느낌의 progress 또는 잔 채움 애니메이션을 사용한다.
- 과도한 loop animation은 금지한다.

## 아이콘
- lucide-react 아이콘을 우선 사용한다.
- 하단 내비게이션은 `Home`, `BookOpen`, `Rows3`, `Sparkles`, `PanelTop` 또는 의미가 가까운 아이콘을 사용한다.
- 별 중요 표시는 `Star` 아이콘을 사용하고 active 상태는 #E8C97B로 채운다.
- 삭제/수정은 상세 우측 `MoreHorizontal` 메뉴 안에 둔다.
