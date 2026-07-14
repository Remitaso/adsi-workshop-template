"use client";

import { AuthProvider } from "@/contexts/AuthContext";
import Layout from "@/components/Layout";
import AttendanceDashboard from "./AttendanceDashboard";

export default function DashboardPage() {
  return (
    <AuthProvider>
      <Layout>
        <AttendanceDashboard />
      </Layout>
    </AuthProvider>
  );
}
