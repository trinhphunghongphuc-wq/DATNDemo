import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Boxes,
  Database,
  Users,
  BarChart3,
  Truck,
  Store,
  Package,
  Handshake,
  LogOut,
  Building2,
} from "lucide-react";

const menusByRole = {
  ADMIN: [
    { label: "Tổng quan", to: "/admin", icon: LayoutDashboard, end: true },
    { label: "Lô hàng & Anchor", to: "/admin/batches", icon: Boxes },
    { label: "Bản ghi", to: "/admin/records", icon: Database },
    { label: "Người dùng", to: "/admin/users", icon: Users },
    { label: "Đánh giá chi phí", to: "/admin/evaluations", icon: BarChart3 },
  ],

  PRODUCER: [
    { label: "Lô hàng của tôi", to: "/producer/batches", icon: Boxes },
    { label: "Đối tác", to: "/producer/partners", icon: Handshake },
  ],

DISTRIBUTOR: [
  { label: "Lô hàng được giao", to: "/distributor/batches", icon: Truck },
  { label: "Danh sách xe", to: "/distributor/vehicles", icon: Package },
  { label: "Hồ sơ công ty", to: "/company-profile", icon: Building2 },
],

RETAILER: [
  { label: "Lô hàng nhận", to: "/retailer/batches", icon: Store },
  { label: "Đơn vị bán lẻ", to: "/retailer/retail-units", icon: Package },
  { label: "Hồ sơ công ty", to: "/company-profile", icon: Building2 },
],
};

const roleLabels = {
  ADMIN: "Quản trị viên",
  PRODUCER: "Nhà sản xuất",
  DISTRIBUTOR: "Nhà phân phối",
  RETAILER: "Nhà bán lẻ",
};

export default function Sidebar() {
  const navigate = useNavigate();

  // AuthResponse của BE trả username và role;
  // LoginPage đã lưu hai giá trị này.
  const username = localStorage.getItem("username") || "Tài khoản";
  const role = (localStorage.getItem("role") || "").replace(/^ROLE_/, "");
  const menus = menusByRole[role] ?? [];

  const initial = username.trim().charAt(0).toUpperCase() || "U";

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    navigate("/login", { replace: true });
  }

  return (
    <aside className="flex min-h-screen w-[250px] shrink-0 flex-col border-r border-slate-800 bg-[#0b1020] text-slate-100">
      <div className="border-b border-slate-800 px-6 py-6">
        <p className="text-2xl font-bold tracking-tight text-blue-400">
          ChainTrack
        </p>
        <p className="mt-1 text-xs text-slate-400">
          Truy xuất nguồn gốc nông sản
        </p>
      </div>

      <div className="flex-1 px-3 py-5">
        <p className="px-3 pb-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
          {roleLabels[role] ?? "Điều hướng"}
        </p>

        <nav aria-label="Menu theo vai trò" className="space-y-1">
          {menus.map(({ label, to, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                [
                  "flex items-center gap-3 rounded-xl px-3 py-3 text-sm",
                  "font-medium transition-colors",
                  isActive
                    ? "bg-blue-600 text-white"
                    : "text-slate-400 hover:bg-slate-800 hover:text-white",
                ].join(" ")
              }
            >
              <Icon size={18} aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}

          {menus.length === 0 && (
            <p className="px-3 text-sm text-slate-500">
              Chưa có menu cho vai trò này.
            </p>
          )}
        </nav>
      </div>

      <div className="border-t border-slate-800 p-4">
        <div className="flex items-center gap-3 rounded-xl bg-slate-800/50 p-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-600 font-bold text-white">
            {initial}
          </div>

          <div className="min-w-0">
            <p className="truncate text-sm font-semibold" title={username}>
              {username}
            </p>
            <p className="text-xs text-slate-400">
              {roleLabels[role] ?? role}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleLogout}
          className="mt-3 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-slate-400 transition-colors hover:bg-red-500/10 hover:text-red-400"
        >
          <LogOut size={18} aria-hidden="true" />
          Đăng xuất
        </button>
      </div>
    </aside>
  );
}