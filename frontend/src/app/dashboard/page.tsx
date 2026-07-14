"use client";

import { AuthProvider } from "@/contexts/AuthContext";
import Layout from "@/components/Layout";

export default function DashboardPage() {
  return (
    <AuthProvider>
      <Layout>
        <div className="max-w-4xl">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            ダッシュボード
          </h2>
          <p className="text-gray-600">
            勤怠管理システムへようこそ。左のメニューから操作を選択してください。
          </p>
        </div>
      </Layout>
    </AuthProvider>
  );
}
