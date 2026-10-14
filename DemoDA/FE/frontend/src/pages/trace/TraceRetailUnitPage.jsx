import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

const API_BASE = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"
).replace(/\/$/, "");

function isRetailerLoggedIn() {
  const role = (localStorage.getItem("role") || "").replace(/^ROLE_/, "");
  const token = localStorage.getItem("token");
  return role === "RETAILER" && Boolean(token);
}

async function getJson(url, token = null) {
  const response = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      data?.message || `Không tải được dữ liệu (HTTP ${response.status}).`
    );
  }

  return data;
}





function formatDate(value) {
  if (!value) return "Chưa có dữ liệu";

  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (match) return `${match[3]}/${match[2]}/${match[1]}`;

  return value;
}

function formatDateTime(value) {
  if (!value) return null;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function formatRange(min, max, unit) {
  if (min == null && max == null) return "Chưa có dữ liệu";
  if (min == null) return `Tối đa ${max}${unit}`;
  if (max == null) return `Tối thiểu ${min}${unit}`;
  if (min === max) return `${min}${unit}`;
  return `${min}–${max}${unit}`;
}

function Info({ label, value }) {
  return (
    <div>
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className="mt-1 font-medium text-slate-800">
        {value ?? "Chưa có dữ liệu"}
      </dd>
    </div>
  );
}



function MeasuredConditions({ title, conditions, emptyMessage }) {
  return (
    <section className="rounded-2xl bg-white p-6 shadow">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-lg font-bold">{title}</h2>
        <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">
          Đã đo
        </span>
      </div>

      {!conditions ? (
        <p className="mt-4 text-sm text-slate-500">{emptyMessage}</p>
      ) : (
        <>
          <dl className="mt-4 grid gap-4 sm:grid-cols-2">
            <Info
              label="Nhiệt độ"
              value={formatRange(
                conditions.temperatureMin,
                conditions.temperatureMax,
                "°C"
              )}
            />
            <Info
              label="Độ ẩm"
              value={formatRange(
                conditions.humidityMin,
                conditions.humidityMax,
                "%"
              )}
            />
            <Info
              label="Khoảng thời gian ghi nhận"
              value={
                conditions.startedAt && conditions.endedAt
                  ? `${formatDateTime(conditions.startedAt)} – ${formatDateTime(
                    conditions.endedAt
                  )}`
                  : "Chưa có thời gian đo"
              }
            />
            <Info
              label="Số mẫu đo"
              value={conditions.sampleCount ?? "Chưa có dữ liệu"}
            />
          </dl>

          {conditions.outOfRangeCount != null && (
            <p className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-700">
              Số mẫu vượt ngưỡng khuyến nghị:{" "}
              <strong>{conditions.outOfRangeCount}</strong>
            </p>
          )}
        </>
      )}
    </section>
  );
}

export default function TraceRetailUnitPage() {
  const { retailCode } = useParams();
  const canVerify = isRetailerLoggedIn();
  const navigate = useNavigate();
  const [unit, setUnit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [verification, setVerification] = useState(null);
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState("");

  useEffect(() => {
    let active = true;

    setLoading(true);
    setError("");
    setUnit(null);
    setVerification(null);
    setVerifyError("");

    getJson(
      `${API_BASE}/traceability/retail/${encodeURIComponent(retailCode)}`
    )
      .then((data) => {
        if (active) setUnit(data);
      })
      .catch((err) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [retailCode]);

  async function verifyOnChain() {
    if (!canVerify || !unit?.batchId || !unit?.retailRecordKey) return;

    const token = localStorage.getItem("token");
    if (!token) {
      setVerifyError("Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.");
      return;
    }

    setVerifying(true);
    setVerifyError("");
    setVerification(null);

    try {
      const result = await getJson(
        `${API_BASE}/verify/batches/${unit.batchId}` +
        `/records/${encodeURIComponent(unit.retailRecordKey)}` +
        `/verify-on-chain`,
        token
      );

      setVerification(result);
    } catch (err) {
      setVerifyError(err.message);
    } finally {
      setVerifying(false);
    }
  }

  function goBack() {
    const role = (localStorage.getItem("role") || "").replace(/^ROLE_/, "");
    const isRetailer = role === "RETAILER" && !!localStorage.getItem("token");

    if (isRetailer) {
      navigate("/retailer/retail-units");
    } else {
      navigate(-1);
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-slate-100 p-6 text-slate-700">
        Đang tải thông tin đơn vị bán lẻ...
      </main>
    );
  }

  if (error || !unit) {
    return (
      <main className="min-h-screen bg-slate-100 p-6">
        <div className="mx-auto max-w-2xl rounded-2xl bg-white p-6 shadow">
          <h1 className="text-xl font-bold text-red-600">
            Không tìm thấy đơn vị bán lẻ
          </h1>
          <p className="mt-2 text-slate-600">
            {error || "Không có dữ liệu."}
          </p>
          <p className="mt-2 text-sm text-slate-500">
            Mã: {retailCode}
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-8 text-slate-800">
      <div className="mx-auto max-w-3xl space-y-5">
        <button
          type="button"
          onClick={goBack}
          className="mb-4 inline-flex items-center text-sm font-semibold text-emerald-700 hover:underline"
        >
          ← Quay lại
        </button>
        <header className="rounded-2xl bg-emerald-700 p-7 text-white shadow">
          <p className="text-sm text-emerald-100">
            TRUY XUẤT NGUỒN GỐC
          </p>
          <h1 className="mt-2 text-3xl font-bold">
            {unit.productName}
          </h1>

          <div className="mt-5 rounded-xl bg-white/15 p-4">
            <p className="text-sm text-emerald-100">Hạn sử dụng</p>
            <p className="mt-1 text-xl font-bold">
              {formatDate(unit.expiryDate)}
            </p>
          </div>

          <p className="mt-4 font-mono text-xs text-emerald-100">
            Mã đơn vị: {unit.retailCode}
          </p>
        </header>

        <section className="rounded-2xl bg-white p-6 shadow">
          <h2 className="text-lg font-bold">Nguồn gốc</h2>

          <dl className="mt-4 grid gap-4 sm:grid-cols-2">
            <Info label="Nông trại" value={unit.farmName} />
            <Info
              label="Địa điểm thu hoạch"
              value={unit.harvestLocation}
            />
            <Info
              label="Ngày thu hoạch"
              value={
                unit.harvestDate
                  ? formatDate(unit.harvestDate)
                  : null
              }
            />
          </dl>
        </section>

        <MeasuredConditions
          title="Điều kiện vận chuyển"
          conditions={unit.transportConditions}
          emptyMessage="Chưa có số đo vận chuyển để công bố."
        />

        <MeasuredConditions
          title="Bảo quản tại cửa hàng"
          conditions={unit.retailerStorageConditions}
          emptyMessage="Chưa có dữ liệu bảo quản tại cửa hàng."
        />

        <section className="rounded-2xl bg-white p-6 shadow">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-bold">
              Điều kiện bảo quản khuyến nghị
            </h2>
            <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-800">
              Khuyến nghị
            </span>
          </div>

          {unit.recommendedConditions ? (
            <dl className="mt-4 grid gap-4 sm:grid-cols-2">
              <Info
                label="Nhiệt độ"
                value={formatRange(
                  unit.recommendedConditions.temperatureMin,
                  unit.recommendedConditions.temperatureMax,
                  "°C"
                )}
              />
              <Info
                label="Độ ẩm"
                value={formatRange(
                  unit.recommendedConditions.humidityMin,
                  unit.recommendedConditions.humidityMax,
                  "%"
                )}
              />
            </dl>
          ) : (
            <p className="mt-4 text-sm text-slate-500">
              Chưa có điều kiện khuyến nghị cho sản phẩm này.
            </p>
          )}
        </section>

        <details className="rounded-2xl bg-white p-6 shadow">
          <summary className="cursor-pointer text-lg font-bold text-emerald-800">
            Xem hành trình
          </summary>

          <div className="mt-5 border-t border-slate-200 pt-5">
            <h3 className="font-semibold">Hành trình lô gốc</h3>
            <p className="mt-1 text-sm text-slate-500">
              Mã lô: {unit.batchCode}
            </p>

            {unit.batchCode && (
              <Link
                to={`/trace/batch/${encodeURIComponent(unit.batchCode)}`}
                className="mt-3 inline-block font-semibold text-emerald-700 hover:underline"
              >
                Xem toàn bộ hành trình →
              </Link>
            )}

            {canVerify && (
              <div className="mt-6 border-t border-slate-200 pt-5">
                <h3 className="font-semibold">
                  Kiểm chứng sự kiện tách lô bán lẻ
                </h3>

                {unit.retailRecordKey ? (
                  <>
                    <p className="mt-2 break-all text-sm text-slate-600">
                      RETAIL record: {unit.retailRecordKey}
                    </p>
                    <button
                      type="button"
                      onClick={verifyOnChain}
                      disabled={verifying}
                      className="mt-4 rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
                    >
                      {verifying
                        ? "Đang xác minh..."
                        : "Xác minh Merkle proof trên blockchain"}
                    </button>
                  </>
                ) : (
                  <p className="mt-2 text-sm text-amber-700">
                    Đơn vị này chưa được liên kết với RETAIL record.
                  </p>
                )}

                {verifyError && (
                  <p className="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
                    Chưa xác minh được: {verifyError}
                  </p>
                )}

                {verification && (
                  <div
                    className={`mt-4 rounded-lg p-4 ${verification.valid
                        ? "bg-emerald-50 text-emerald-800"
                        : "bg-red-50 text-red-800"
                      }`}
                  >
                    <p className="font-bold">
                      {verification.valid
                        ? "Đã xác minh trên blockchain"
                        : "Xác minh không thành công"}
                    </p>

                    {verification.message && (
                      <p className="mt-1 text-sm">
                        {verification.message}
                      </p>
                    )}

                    <details className="mt-3 text-sm">
                      <summary className="cursor-pointer">
                        Xem chi tiết kiểm chứng
                      </summary>
                      <ul className="mt-2 space-y-1">
                        <li>
                          Dữ liệu khớp leaf:{" "}
                          {String(verification.dataValid)}
                        </li>
                        <li>
                          Merkle proof hợp lệ:{" "}
                          {String(verification.localProofValid)}
                        </li>
                        <li>
                          Root khớp blockchain:{" "}
                          {String(verification.rootMatchesBlockchain)}
                        </li>
                        <li>
                          Contract xác minh:{" "}
                          {String(verification.contractProofValid)}
                        </li>
                      </ul>
                    </details>
                  </div>
                )}
              </div>
            )}
          </div>
        </details>
      </div>
    </main>
  );
}