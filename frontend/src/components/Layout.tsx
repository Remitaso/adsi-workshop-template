"use client";

import { useAuth } from "@/contexts/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const { user, isLoading, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace("/login");
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-gray-500">読み込み中...</p>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  const handleLogout = async () => {
    await logout();
    router.replace("/login");
  };

  const navItems = getNavItems(user.role);

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <h1 className="text-lg font-semibold text-gray-900">勤怠管理システム</h1>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">
                {user.name}（{roleLabel(user.role)}）
              </span>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                ログアウト
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="flex flex-1">
        <nav className="w-56 bg-white border-r p-4">
          <ul className="space-y-1">
            {navItems.map((item) => (
              <li key={item.href}>
                <a
                  href={item.href}
                  className="block px-3 py-2 rounded-md text-sm text-gray-700 hover:bg-gray-100"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}

function roleLabel(role: string): string {
  switch (role) {
    case "HR":
      return "人事";
    case "MANAGER":
      return "管理者";
    default:
      return "一般";
  }
}

function getNavItems(role: string) {
  const items = [
    { href: "/dashboard", label: "ダッシュボード" },
    { href: "/attendance", label: "勤怠一覧" },
  ];

  if (role === "MANAGER" || role === "HR") {
    items.push({ href: "/approval", label: "承認" });
  }
  if (role === "HR") {
    items.push({ href: "/employees", label: "社員管理" });
  }

  return items;
}
