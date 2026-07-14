"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider } from "@/contexts/AuthContext";
import Layout from "@/components/Layout";
import { apiClient, ApiClientError, withBasePath } from "@/lib/api-client";
import {
  LeaveType,
  LeaveBalanceResponse,
  CreateLeaveRequest,
  LEAVE_TYPE_LABELS,
} from "@/lib/leave-types";

export default function NewLeavePageWrapper() {
  return (
    <AuthProvider>
      <Layout>
        <NewLeaveContent />
      </Layout>
    </AuthProvider>
  );
}

function NewLeaveContent() {
  const router = useRouter();
  const [leaveType, setLeaveType] = useState<LeaveType>("PAID");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [balance, setBalance] = useState<LeaveBalanceResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient
      .get<LeaveBalanceResponse>("/leaves/balance")
      .then(setBalance)
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (leaveType === "HALF_DAY" && startDate) {
      setEndDate(startDate);
    }
  }, [leaveType, startDate]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!startDate || !endDate) {
      setError("日付を入力してください");
      return;
    }
    if (startDate > endDate) {
      setError("開始日は終了日以前である必要があります");
      return;
    }

    setSubmitting(true);
    try {
      const body: CreateLeaveRequest = {
        leaveType,
        startDate,
        endDate,
        reason,
      };
      await apiClient.post("/leaves", body);
      router.push(withBasePath("/leaves"));
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.error.message);
      } else {
        setError("申請に失敗しました");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="p-8 max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-6">休暇申請</h1>

      <form onSubmit={handleSubmit} className="bg-white border rounded-lg p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            休暇種類
          </label>
          <select
            value={leaveType}
            onChange={(e) => setLeaveType(e.target.value as LeaveType)}
            className="w-full border rounded px-3 py-2"
          >
            {Object.entries(LEAVE_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            開始日
          </label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="w-full border rounded px-3 py-2"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            終了日
          </label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            disabled={leaveType === "HALF_DAY"}
            className="w-full border rounded px-3 py-2 disabled:bg-gray-100"
            required
          />
          {leaveType === "HALF_DAY" && (
            <p className="text-xs text-gray-500 mt-1">半休は1日のみ指定可能です</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            理由
          </label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full border rounded px-3 py-2"
            rows={3}
            placeholder="申請理由を入力してください"
          />
        </div>

        {(leaveType === "PAID" || leaveType === "HALF_DAY") && balance && (
          <div className="bg-blue-50 border border-blue-200 rounded p-3">
            <p className="text-sm font-medium text-blue-800">
              有給残日数: {balance.totalRemaining}日
            </p>
            {balance.details.map((d, i) => (
              <p key={i} className="text-xs text-blue-600 mt-1">
                付与 {d.grantDate}（期限 {d.expiryDate}）: 残 {d.remaining}日
              </p>
            ))}
          </div>
        )}

        {error && (
          <div className="bg-red-50 border border-red-200 rounded p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={() => router.push(withBasePath("/leaves"))}
            className="flex-1 border rounded px-4 py-2 hover:bg-gray-50"
          >
            キャンセル
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 bg-blue-600 text-white rounded px-4 py-2 hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? "送信中..." : "申請する"}
          </button>
        </div>
      </form>
    </div>
  );
}
