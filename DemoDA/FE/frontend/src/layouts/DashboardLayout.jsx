import { Outlet } from "react-router-dom";
import Sidebar from "../components/dashboard/Sidebar";
import Footer from "../components/dashboard/Footer";
import NotificationsProvider from "../contexts/NotificationsProvider";

export default function DashboardLayout() {
  return (
    <NotificationsProvider key={`${localStorage.getItem("username")}:${localStorage.getItem("role")}`}>
    <div className="flex min-h-screen bg-[#0f131d] text-white">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col">
        <main className="flex-1 p-6">
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
    </NotificationsProvider>
  );
}
