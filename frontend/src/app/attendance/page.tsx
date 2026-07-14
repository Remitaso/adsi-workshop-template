"use client";

import { useState, useEffect, useCallback } from "react";
import { AuthProvider } from "@/contexts/AuthContext";
import Layout from "@/components/Layout";
import { apiClient, ApiClientError } from "@/lib/api-client";
import type { MonthlyRecordsResponse } from "@/lib/types";

export default function AttendancePage() {
  return (
    <AuthProvider>
      <Layout>
        <MonthlyRecords />
      </Layout>
    </AuthProvider>
  );
}

function MonthlyRecords() {
  const [yearMonth, setYearMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  });
  const [data, setData] = useState<MonthlyRecordsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRecords = useCallback(async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<MonthlyRecordsResponse>(
        `/attendance/records?yearMonth=${yearMonth}`
      );
      setData(res);
      setError(null);
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.error.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, [yearMonth]);

  useEffect(() => {
    fetchRecords();
  }, [fetchRecords]);

  return (
    <div className="max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">月次勤怠一覧</h2>
        <input
          type="month"
          value={yearMonth}
          onChange={(e) => setYearMonth(e.target.value)}
          className="border rounded-md px-3 py-1.5 text-sm"
        />
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
          {error}
        </div>
      )}

      {isLoading ? (
        <p className="text-gray-500">読み込み中...</p>
      ) : (
        <div className="bg-white border rounded-lg overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-2 font-medium text-gray-500">日付</th>
                <th className="text-left px-4 py-2 font-medium text-gray-500">ステータス</th>
                <th className="text-right px-4 py-2 font-medium text-gray-500">勤務時間</th>
                <th className="text-right px-4 py-2 font-medium text-gray-500">残業</th>
              </tr>
            </thead>
            <tbody>
              {data?.records.map((record) => (
                <tr key={record.workDate} className="border-t">
                  <td className="px-4 py-2">{record.workDate}</td>
                  <td className="px-4 py-2">
                    <span className={statusClassName(record.status)}>
                      {statusLabel(record.status)}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-right">{formatMinutes(record.totalWorkMinutes)}</td>
                  <td className="px-4 py-2 text-right">
                    {record.overtimeMinutes > 0 ? formatMinutes(record.overtimeMinutes) : "—"}
                  </td>
                </tr>
              ))}
              {data?.records.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-gray-400">
                    勤怠記録がありません
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function statusLabel(status: string): string {
  switch (status) {
    case "DRAFT":
      return "下書き";
    case "SUBMITTED":
      return "申請中";
    case "APPROVED":
      return "承認済み";
    default:
      return status;
  }
}

function statusClassName(status: string): string {
  const base = "inline-block px-2 py-0.5 text-xs rounded-full";
  switch (status) {
    case "APPROVED":
      return `${base} bg-green-100 text-green-700`;
    case "SUBMITTED":
      return `${base} bg-yellow-100 text-yellow-700`;
    default:
      return `${base} bg-gray-100 text-gray-600`;
  }
}

function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}:${String(m).padStart(2, "0")}`;
}
