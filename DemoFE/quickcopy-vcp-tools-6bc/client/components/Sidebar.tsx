import { Link, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Truck,
  ArrowLeftRight,
  Package,
  BarChart2,
  ShieldCheck,
} from "lucide-react";

const NAV_ITEMS = [
  { label: "Dashboard", path: "/", icon: LayoutDashboard },
  { label: "Shipments", path: "/shipments", icon: Truck },
  { label: "Transactions", path: "/transactions", icon: ArrowLeftRight },
  { label: "Inventory", path: "/inventory", icon: Package },
  { label: "Analytics", path: "/analytics", icon: BarChart2 },
  { label: "Certificates", path: "/certificates", icon: ShieldCheck },
];

export function Sidebar() {
  const { pathname } = useLocation();

  return (
    <aside className="w-[220px] flex-shrink-0 flex flex-col bg-[#0E1520] min-h-screen border-r border-white/[0.05]">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 pt-5 pb-5">
        <div className="w-9 h-9 rounded-lg flex-shrink-0 overflow-hidden">
          <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-9 h-9">
            <rect width="36" height="36" rx="8" fill="url(#logoGrad)" />
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="36" y2="36" gradientUnits="userSpaceOnUse">
                <stop stopColor="#60A5FA" />
                <stop offset="1" stopColor="#1D4ED8" />
              </linearGradient>
            </defs>
            <circle cx="12" cy="13" r="2.5" fill="white" />
            <circle cx="24" cy="13" r="2.5" fill="white" />
            <circle cx="12" cy="23" r="2.5" fill="white" />
            <circle cx="24" cy="23" r="2.5" fill="white" />
            <line x1="14.5" y1="13" x2="21.5" y2="13" stroke="white" strokeWidth="1.5" />
            <line x1="14.5" y1="23" x2="21.5" y2="23" stroke="white" strokeWidth="1.5" />
            <line x1="12" y1="15.5" x2="12" y2="20.5" stroke="white" strokeWidth="1.5" />
            <line x1="24" y1="15.5" x2="24" y2="20.5" stroke="white" strokeWidth="1.5" />
            <line x1="14.5" y1="13" x2="21.5" y2="23" stroke="white" strokeWidth="1" opacity="0.4" />
            <line x1="21.5" y1="13" x2="14.5" y2="23" stroke="white" strokeWidth="1" opacity="0.4" />
          </svg>
        </div>
        <div>
          <div className="text-white font-bold text-sm leading-tight">ChainTrack</div>
          <div className="text-green-400 text-[11px] leading-tight mt-0.5">Enterprise Verified</div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-2">
        {NAV_ITEMS.map(({ label, path, icon: Icon }) => {
          const active = pathname === path;
          return (
            <Link
              key={path}
              to={path}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium mb-0.5 transition-colors",
                active
                  ? "bg-blue-500/[0.15] text-blue-400"
                  : "text-slate-400 hover:text-slate-200 hover:bg-white/[0.05]"
              )}
            >
              <Icon size={17} className="flex-shrink-0" />
              <span>{label}</span>
            </Link>
          );
        })}
      </nav>

      {/* User profile */}
      <div className="px-4 py-4 border-t border-white/[0.06]">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-slate-600 flex items-center justify-center text-xs text-white font-semibold flex-shrink-0">
            AR
          </div>
          <div className="min-w-0">
            <div className="text-white text-sm font-medium truncate">Alex Rivera</div>
            <div className="text-slate-400 text-xs truncate">Logistics Lead</div>
          </div>
        </div>
      </div>
    </aside>
  );
}
