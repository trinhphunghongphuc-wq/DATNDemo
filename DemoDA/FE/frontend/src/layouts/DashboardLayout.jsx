import { Outlet } from "react-router-dom";
import Sidebar from "../components/dashboard/Sidebar";
import Header from "../components/dashboard/Header";
import Footer from "../components/dashboard/Footer";

export default function DashboardLayout() {
  return (
    <div className="min-h-screen bg-[#0f131d] text-white flex">
      <Sidebar />

      <div className="flex flex-1 flex-col">
      

        <main className="flex-1 p-6">
          <Outlet />
        </main>

        <Footer />
      </div>
    </div>
  );
}