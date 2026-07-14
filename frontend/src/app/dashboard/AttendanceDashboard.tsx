"use client";

import { useState, useEffect, useCallback } from "react";
import { apiClient, ApiClientError } from "@/lib/api-client";
import type { TodayAttendanceResponse, TimeEntryResponse } from "@/lib/types";

export default function AttendanceDashboard() {
  const [today, setToday] = useState<TodayAttendanceResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const fetchToday = useCallback(async () => {
    try {
      const data = await apiClient.get<TodayAttendanceResponse>("/attendance/today");
      setToday(data);
      setError(null);
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.error.message);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchToday();
  }, [fetchToday]);

  const handleClockIn = async () => {
    setIsProcessing(true);
    setError(null);
    try {
      await apiClient.post<TimeEntryResponse>("/attendance/clock-in");
      await fetchToday();
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.error.message);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClockOut = async () => {
    setIsProcessing(true);
    setError(null);
    try {
      await apiClient.post<TimeEntryResponse>("/attendance/clock-out");
      await fetchToday();
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.error.message);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return <p className="text-gray-500">読み込み中...</p>;
  }

  const isClockedIn = today?.entries.some((e) => e.clockOut === null) ?? false;

  return (
    <div className="max-w-2xl">
      <h2 className="text-xl font-semibold text-gray-900 mb-6">ダッシュボード</h2>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="bg-white border rounded-lg p-6 mb-6">
        <h3 className="text-sm font-medium text-gray-500 mb-2">本日の勤怠</h3>
        <p className="text-sm text-gray-600 mb-4">
          {today?.workDate ?? "—"}{" "}
          {today?.status && (
            <span className="inline-block ml-2 px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600">
              {statusLabel(today.status)}
            </span>
          )}
        </p>

        <div className="flex gap-3 mb-6">
          <button
            onClick={handleClockIn}
            disabled={isProcessing || isClockedIn}
            className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            出勤
          </button>
          <button
            onClick={handleClockOut}
            disabled={isProcessing || !isClockedIn}
            className="px-4 py-2 bg-gray-600 text-white text-sm font-medium rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            退勤
          </button>
        </div>

        {today && today.entries.length > 0 && (
          <div>
            <h4 className="text-sm font-medium text-gray-500 mb-2">打刻記録</h4>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-1 text-gray-500 font-medium">出勤</th>
                  <th className="text-left py-1 text-gray-500 font-medium">退勤</th>
                </tr>
              </thead>
              <tbody>
                {today.entries.map((entry) => (
                  <tr key={entry.id} className="border-b last:border-0">
                    <td className="py-1">{formatTime(entry.clockIn)}</td>
                    <td className="py-1">{entry.clockOut ? formatTime(entry.clockOut) : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="mt-3 text-sm text-gray-600">
              合計勤務時間: {formatMinutes(today.totalWorkMinutes)}
            </p>
          </div>
        )}
      </div>
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

function formatTime(isoString: string): string {
  return isoString.substring(11, 16);
}

function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}時間${m > 0 ? `${m}分` : ""}`;
}
