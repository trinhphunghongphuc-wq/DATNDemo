import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import DashboardPage from "../pages/dashboard/DashboardPage";
import TraceBatchPage from "../pages/trace/TraceBatchPage";
import DashboardLayout from "../layouts/DashboardLayout";
import BatchQrPage from "../pages/batches/BatchQrPage";
import DistributorBatchesPage from "../pages/distributor/DistributorBatchesPage";
import DistributorJourneyPage from "../pages/distributor/DistributorJourneyPage";
import AdminOverviewPage from "../pages/admin/AdminOverviewPage";
import AdminBatchesPage from "../pages/admin/AdminBatchesPage";
import AdminRecordsPage from "../pages/admin/AdminRecordsPage";
import AdminUsersPage from "../pages/admin/AdminUsersPage";
import AdminEvaluationsPage from "../pages/admin/AdminEvaluationsPage";
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

      <Route
        path="/trace/batch/:batchCode"
        element={<TraceBatchPage />}
      />
      <Route
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />

        <Route
          path="/producer/batches/:batchId/qr"
          element={<BatchQrPage />}
        />

        <Route
          path="/distributor/batches"
          element={<DistributorBatchesPage />}
        />
        <Route
          path="/distributor/batches/:batchId"
          element={<DistributorJourneyPage />}
        />

        <Route
          path="/admin"
          element={
            localStorage.getItem("role")?.replace("ROLE_", "") === "ADMIN"
              ? <AdminOverviewPage />
              : <Navigate to="/dashboard" replace />
          }
        />
        <Route
          path="/admin/batches"
          element={
            localStorage.getItem("role")?.replace("ROLE_", "") === "ADMIN"
              ? <AdminBatchesPage />
              : <Navigate to="/dashboard" replace />
          }
        />
        <Route
          path="/admin/records"
          element={
            localStorage.getItem("role")?.replace("ROLE_", "") === "ADMIN"
              ? <AdminRecordsPage />
              : <Navigate to="/dashboard" replace />
          }
        />
        <Route
          path="/admin/users"
          element={
            localStorage.getItem("role")?.replace("ROLE_", "") === "ADMIN"
              ? <AdminUsersPage />
              : <Navigate to="/dashboard" replace />
          }
        />
        <Route
          path="/admin/evaluations"
          element={
            localStorage.getItem("role")?.replace("ROLE_", "") === "ADMIN"
              ? <AdminEvaluationsPage />
              : <Navigate to="/dashboard" replace />
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}