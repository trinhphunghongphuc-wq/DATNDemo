import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import DashboardPage from "../pages/dashboard/DashboardPage";
import TraceBatchPage from "../pages/trace/TraceBatchPage";
import DashboardLayout from "../layouts/DashboardLayout";
import BatchQrPage from "../pages/batches/BatchQrPage";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Trang consumer mở từ QR của Producer */}
      <Route path="/trace/batch/:batchCode" element={<TraceBatchPage />} />

      <Route
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />}/>
        <Route path="/producer/batches/:batchId/qr" element={<BatchQrPage />}/>
      </Route>
      

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}