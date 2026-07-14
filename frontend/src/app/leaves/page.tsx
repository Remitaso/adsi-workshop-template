"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiClient } from "@/lib/api-client";
import {
  LeaveResponse,
  LeaveBalanceResponse,
  LEAVE_TYPE_LABELS,
  STATUS_LABELS,
} from "@/lib/leave-types";

export default function LeavesPage() {
  const router = useRouter();
  const [leaves, setLeaves] = useState<LeaveResponse[]>([]);
  const [balance, setBalance] = useState<LeaveBalanceResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    try {
      const [leavesRes, balanceRes] = await Promise.all([
        apiClient.get<{ items: LeaveResponse[] }>("/leaves"),
        apiClient.get<LeaveBalanceResponse>("/leaves/balance"),
      ]);
      setLeaves(leavesRes.items);
      setBalance(balanceRes);
    } catch (e) {
      setError("データの取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(id: number) {
    if (!confirm("この申請を取り消しますか？")) return;
    try {
      await apiClient.delete(`/leaves/${id}`);
      setLeaves(leaves.filter((l) => l.id !== id));
    } catch (e) {
      alert("取消に失敗しました");
    }
  }

  if (loading) return <div className="p-8">読み込み中...</div>;
  if (error) return <div className="p-8 text-red-600">{error}</div>;

  const totalGranted = balance?.details.reduce((sum, d) => sum + d.granted, 0) ?? 0;
  const usagePercent = totalGranted > 0
    ? Math.round(((totalGranted - (balance?.totalRemaining ?? 0)) / totalGranted) * 100)
    : 0;

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">休暇管理</h1>
        <button
          onClick={() => router.push("/leaves/new")}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          休暇を申請する
        </button>
      </div>

      {balance && (
        <div className="bg-white border rounded-lg p-6 mb-6">
          <div className="flex justify-between items-center mb-2">
            <span className="text-lg font-medium">
              有給残高: {balance.totalRemaining}日 / {totalGranted}日
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3 mb-4">
            <div
              className="bg-blue-600 h-3 rounded-full"
              style={{ width: `${usagePercent}%` }}
            />
          </div>
          {balance.details.length > 0 && (
            <div className="text-sm text-gray-600">
              {balance.details.map((d, i) => (
                <div key={i}>
                  付与日 {d.grantDate}（期限 {d.expiryDate}）: 残 {d.remaining}日
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="bg-white border rounded-lg overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">申請日</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">種別</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">期間</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">状態</th>
              <th className="px-4 py-3 text-left text-sm font-medium text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody>
            {leaves.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                  休暇申請はありません
                </td>
              </tr>
            ) : (
              leaves.map((leave) => (
                <tr key={leave.id} className="border-t">
                  <td className="px-4 py-3 text-sm">
                    {new Date(leave.createdAt).toLocaleDateString("ja-JP")}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {LEAVE_TYPE_LABELS[leave.leaveType]}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {leave.startDate === leave.endDate
                      ? leave.startDate
                      : `${leave.startDate} 〜 ${leave.endDate}`}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <StatusBadge status={leave.status} />
                  </td>
                  <td className="px-4 py-3 text-sm">
                    {leave.status === "PENDING" && (
                      <button
                        onClick={() => handleCancel(leave.id)}
                        className="text-red-600 hover:text-red-800"
                      >
                        取消
                      </button>
                    )}
                    {leave.status === "REJECTED" && leave.rejectReason && (
                      <span className="text-gray-500" title={leave.rejectReason}>
                        理由: {leave.rejectReason}
                      </span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    PENDING: "bg-yellow-100 text-yellow-800",
    APPROVED: "bg-green-100 text-green-800",
    REJECTED: "bg-red-100 text-red-800",
  };
  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${styles[status] ?? ""}`}>
      {STATUS_LABELS[status as keyof typeof STATUS_LABELS] ?? status}
    </span>
  );
}
