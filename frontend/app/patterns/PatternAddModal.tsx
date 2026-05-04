"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { PatternResponse, PatternExtractResponse } from "@/lib/types";

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

interface ExampleInput {
  sentence: string;
  translation: string;
}

export default function PatternAddModal({ onClose, onSuccess }: Props) {
  const [mode, setMode] = useState<"manual" | "image">("manual");
  const [template, setTemplate] = useState("");
  const [description, setDescription] = useState("");
  const [examples, setExamples] = useState<ExampleInput[]>([
    { sentence: "", translation: "" },
  ]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleAddExample = () => {
    setExamples([...examples, { sentence: "", translation: "" }]);
  };

  const handleRemoveExample = (index: number) => {
    setExamples(examples.filter((_, i) => i !== index));
  };

  const handleExampleChange = (
    index: number,
    field: "sentence" | "translation",
    value: string
  ) => {
    const updated = [...examples];
    updated[index] = { ...updated[index], [field]: value };
    setExamples(updated);
  };

  const handleSubmit = async () => {
    if (!template.trim()) {
      setError("패턴을 입력해주세요");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const filteredExamples = examples.filter((e) => e.sentence.trim());
      await api.post<PatternResponse>("/patterns", {
        template: template.trim(),
        description: description.trim() || null,
        examples: filteredExamples,
      });
      onSuccess();
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleImageExtract = async (file: File) => {
    setLoading(true);
    setError("");

    try {
      const formData = new FormData();
      formData.append("image", file);
      const result = await api.upload<PatternExtractResponse>(
        "/patterns/extract",
        formData
      );

      if (result.template) {
        setTemplate(result.template);
        setDescription(result.description || "");
        if (result.examples && result.examples.length > 0) {
          setExamples(
            result.examples.map((e) => ({
              sentence: e.sentence,
              translation: e.translation,
            }))
          );
        }
        setMode("manual");
      } else {
        setError("이미지에서 패턴을 추출할 수 없습니다");
      }
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30">
      <div className="bg-raised w-full max-w-md rounded-t-[24px] p-5 max-h-[85vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-ink">패턴 등록</h2>
          <button onClick={onClose} className="text-ink-muted text-sm">
            닫기
          </button>
        </div>

        {/* 모드 전환 탭 */}
        <div className="bg-soft rounded-[14px] border border-hairline p-1 flex gap-1.5 mb-4">
          <button
            onClick={() => setMode("manual")}
            className={`flex-1 py-2 rounded-[10px] text-xs font-medium ${
              mode === "manual"
                ? "bg-raised text-ink shadow-sm"
                : "text-ink-muted"
            }`}
          >
            직접 입력
          </button>
          <button
            onClick={() => setMode("image")}
            className={`flex-1 py-2 rounded-[10px] text-xs font-medium ${
              mode === "image"
                ? "bg-raised text-ink shadow-sm"
                : "text-ink-muted"
            }`}
          >
            이미지 추출
          </button>
        </div>

        {mode === "image" && (
          <div className="mb-4">
            <label className="block">
              <span className="text-xs text-ink-muted">
                교재 이미지를 업로드하세요
              </span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleImageExtract(file);
                }}
                className="mt-2 block w-full text-xs text-ink-soft"
              />
            </label>
          </div>
        )}

        {/* 직접 입력 폼 */}
        <div className="flex flex-col gap-3">
          <input
            type="text"
            placeholder="패턴 (예: I want to ~)"
            value={template}
            onChange={(e) => setTemplate(e.target.value)}
            className="bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted"
          />
          <input
            type="text"
            placeholder="설명 (예: ~하고 싶다)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted"
          />

          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-ink-muted">교재 예문</span>
              <button
                onClick={handleAddExample}
                className="text-xs text-primary font-medium"
              >
                + 추가
              </button>
            </div>
            {examples.map((ex, i) => (
              <div key={i} className="flex flex-col gap-1 mb-2">
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="영어 예문"
                    value={ex.sentence}
                    onChange={(e) =>
                      handleExampleChange(i, "sentence", e.target.value)
                    }
                    className="flex-1 bg-soft rounded-[12px] border border-hairline px-3 py-2 text-xs text-ink placeholder:text-ink-muted"
                  />
                  {examples.length > 1 && (
                    <button
                      onClick={() => handleRemoveExample(i)}
                      className="text-xs text-warn px-1"
                    >
                      ✕
                    </button>
                  )}
                </div>
                <input
                  type="text"
                  placeholder="해석"
                  value={ex.translation}
                  onChange={(e) =>
                    handleExampleChange(i, "translation", e.target.value)
                  }
                  className="bg-soft rounded-[12px] border border-hairline px-3 py-2 text-xs text-ink placeholder:text-ink-muted"
                />
              </div>
            ))}
          </div>
        </div>

        {error && (
          <p className="text-xs text-warn mt-2">{error}</p>
        )}

        <button
          onClick={handleSubmit}
          disabled={loading}
          className="w-full mt-4 bg-primary text-white rounded-[14px] h-[42px] text-sm font-semibold disabled:opacity-50"
        >
          {loading ? "처리 중..." : "등록"}
        </button>
      </div>
    </div>
  );
}
