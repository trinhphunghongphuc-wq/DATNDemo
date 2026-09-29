import {
  Routes,
  Route,
  Navigate,
  Outlet,
} from "react-router-dom";

import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import TraceBatchPage from "../pages/trace/TraceBatchPage";

import DashboardLayout from "../layouts/DashboardLayout";
import BatchQrPage from "../pages/batches/BatchQrPage";

import ProducerBatchesPage from "../pages/producer/ProducerBatchesPage";
import ProducerPartnersPage from "../pages/producer/ProducerPartnersPage";

import DistributorBatchesPage from "../pages/distributor/DistributorBatchesPage";
import DistributorJourneyPage from "../pages/distributor/DistributorJourneyPage";

import CompanyProfilePage from "../pages/company/CompanyProfilePage";

import AdminOverviewPage from "../pages/admin/AdminOverviewPage";
import AdminBatchesPage from "../pages/admin/AdminBatchesPage";
import AdminRecordsPage from "../pages/admin/AdminRecordsPage";
import AdminUsersPage from "../pages/admin/AdminUsersPage";
import AdminEvaluationsPage from "../pages/admin/AdminEvaluationsPage";

import RetailerBatchesPage from "../pages/retailer/RetailerBatchesPage";
import RetailerRecordsPage from "../pages/retailer/RetailerRecordsPage";


function getRole() {
  return (localStorage.getItem("role") || "").replace(/^ROLE_/, "");
}

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("token");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function RoleRoute({ allowedRoles }) {
  if (!allowedRoles.includes(getRole())) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

function DashboardHome() {
  const homeByRole = {
    ADMIN: "/admin",
    PRODUCER: "/producer/batches",
    DISTRIBUTOR: "/distributor/batches",
    RETAILER: "/company-profile",
  };

  return (
    <Navigate
      to={homeByRole[getRole()] ?? "/login"}
      replace
    />
  );
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/"
        element={<Navigate to="/dashboard" replace />}
      />

      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Trang công khai mở từ QR */}
      <Route
        path="/trace/batch/:batchCode"
        element={<TraceBatchPage />}
      />

      {/* Các trang cần đăng nhập */}
      <Route
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route
          path="/dashboard"
          element={<DashboardHome />}
        />

        {/* Admin */}
        <Route element={<RoleRoute allowedRoles={["ADMIN"]} />}>
          <Route
            path="/admin"
            element={<AdminOverviewPage />}
          />
          <Route
            path="/admin/batches"
            element={<AdminBatchesPage />}
          />
          <Route
            path="/admin/records"
            element={<AdminRecordsPage />}
          />
          <Route
            path="/admin/users"
            element={<AdminUsersPage />}
          />
          <Route
            path="/admin/evaluations"
            element={<AdminEvaluationsPage />}
          />
        </Route>

        {/* Producer */}
        <Route element={<RoleRoute allowedRoles={["PRODUCER"]} />}>
          <Route
            path="/producer/batches"
            element={<ProducerBatchesPage />}
          />
          <Route
            path="/producer/batches/:batchId/qr"
            element={<BatchQrPage />}
          />
          <Route
            path="/producer/partners"
            element={<ProducerPartnersPage />}
          />
        </Route>

        {/* Distributor */}
        <Route element={<RoleRoute allowedRoles={["DISTRIBUTOR"]} />}>
          <Route
            path="/distributor/batches"
            element={<DistributorBatchesPage />}
          />
          <Route
            path="/distributor/batches/:batchId"
            element={<DistributorJourneyPage />}
          />
        </Route>

        {/* Distributor và Retailer tự cập nhật hồ sơ công ty */}
        <Route
          element={
            <RoleRoute
              allowedRoles={["DISTRIBUTOR", "RETAILER"]}
            />
          }
        >
          <Route
            path="/company-profile"
            element={<CompanyProfilePage />}
          />
        </Route>

        <Route path="/retailer/batches" element={<RetailerBatchesPage />} />
        <Route path="/retailer/batches/:batchId/records" element={<RetailerRecordsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}