"use client";

import CoffeeSpinner from "../components/CoffeeSpinner";
import CremaLoader from "../components/CremaLoader";

export default function DevPreviewPage() {
  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-lg font-semibold text-ink tracking-tight">
        로딩 컴포넌트 미리보기
      </h1>

      {/* CoffeeSpinner */}
      <section>
        <p className="text-xs font-semibold text-ink-soft uppercase mb-3">
          CoffeeSpinner
        </p>
        <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm flex flex-col gap-4">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <CoffeeSpinner size={16} />
              <span className="text-xs text-ink-muted">size=16</span>
            </div>
            <div className="flex items-center gap-2">
              <CoffeeSpinner />
              <span className="text-sm text-ink-muted">size=20 (기본)</span>
            </div>
            <div className="flex items-center gap-2">
              <CoffeeSpinner size={32} />
              <span className="text-sm text-ink-muted">size=32</span>
            </div>
          </div>

          {/* 실제 사용 예시 */}
          <div className="border-t border-hairline pt-4">
            <p className="text-[11px] text-ink-muted mb-2">실제 사용 예시 (py-20)</p>
            <div className="flex items-center justify-center gap-2 py-20 bg-cream rounded-[14px]">
              <CoffeeSpinner />
              <span className="text-sm text-ink-muted">불러오는 중...</span>
            </div>
          </div>

          <div>
            <p className="text-[11px] text-ink-muted mb-2">실제 사용 예시 (py-8)</p>
            <div className="flex items-center justify-center gap-2 py-8 bg-cream rounded-[14px]">
              <CoffeeSpinner />
              <span className="text-sm text-ink-muted">불러오는 중...</span>
            </div>
          </div>
        </div>
      </section>

      {/* CremaLoader */}
      <section>
        <p className="text-xs font-semibold text-ink-soft uppercase mb-3">
          CremaLoader
        </p>
        <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm flex flex-col gap-6">
          <div>
            <p className="text-[11px] text-ink-muted mb-2">기본 메시지</p>
            <div className="bg-cream rounded-[14px] py-8">
              <CremaLoader />
            </div>
          </div>

          <div>
            <p className="text-[11px] text-ink-muted mb-2">예문 생성</p>
            <div className="bg-cream rounded-[14px] py-8">
              <CremaLoader message="예문을 만들고 있어요..." />
            </div>
          </div>

          <div>
            <p className="text-[11px] text-ink-muted mb-2">단어 등록</p>
            <div className="bg-cream rounded-[14px] py-8">
              <CremaLoader message="단어를 등록하고 있어요..." />
            </div>
          </div>

          <div>
            <p className="text-[11px] text-ink-muted mb-2">이미지 분석</p>
            <div className="bg-cream rounded-[14px] py-8">
              <CremaLoader message="이미지를 분석하고 있어요..." />
            </div>
          </div>
        </div>
      </section>

      {/* 모달 내 배치 시뮬레이션 */}
      <section>
        <p className="text-xs font-semibold text-ink-soft uppercase mb-3">
          모달 내 배치 시뮬레이션
        </p>
        <div className="bg-raised w-full rounded-t-[20px] p-5 pb-10 border border-hairline shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-ink">단어 등록</h2>
            <span className="text-ink-muted text-sm">닫기</span>
          </div>
          <div className="py-8">
            <CremaLoader message="단어를 등록하고 있어요..." />
          </div>
        </div>
      </section>
    </div>
  );
}
