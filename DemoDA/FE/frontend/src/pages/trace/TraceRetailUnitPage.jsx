import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

const API_BASE = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"
).replace(/\/$/, "");

async function getJson(url) {
  const response = await fetch(url);
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      data?.message || `Không tải được dữ liệu (HTTP ${response.status}).`
    );
  }

  return data;
}

export default function TraceRetailUnitPage() {
  const { retailCode } = useParams();

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

    return () => { active = false; };
  }, [retailCode]);

  async function verifyOnChain() {
    if (!unit?.batchId || !unit?.retailRecordKey) return;

    setVerifying(true);
    setVerifyError("");
    setVerification(null);

    try {
      const result = await getJson(
        `${API_BASE}/verify/batches/${unit.batchId}` +
        `/records/${encodeURIComponent(unit.retailRecordKey)}` +
        `/verify-on-chain`
      );

      setVerification(result);
    } catch (err) {
      setVerifyError(err.message);
    } finally {
      setVerifying(false);
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
          <p className="mt-2 text-slate-600">{error}</p>
          <p className="mt-2 text-sm text-slate-500">
            Mã: {retailCode}
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-10 text-slate-800">
      <div className="mx-auto max-w-3xl space-y-6">
        <header className="rounded-2xl bg-emerald-700 p-7 text-white shadow">
          <p className="text-sm text-emerald-100">
            TRUY XUẤT ĐƠN VỊ BÁN LẺ
          </p>
          <h1 className="mt-2 text-2xl font-bold">
            {unit.productName}
          </h1>
          <p className="mt-2 font-mono text-sm">
            {unit.retailCode}
          </p>
        </header>

        <section className="rounded-2xl bg-white p-6 shadow">
          <h2 className="text-lg font-bold">Thông tin đơn vị</h2>

          <dl className="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <dt className="text-sm text-slate-500">Hình thức bán</dt>
              <dd className="font-medium">
                {unit.type === "PACKAGED"
                  ? "Đóng gói"
                  : "Bán theo khối lượng"}
              </dd>
            </div>

            <div>
              <dt className="text-sm text-slate-500">
                Khối lượng phân bổ
              </dt>
              <dd className="font-medium">
                {unit.allocatedWeight} kg
              </dd>
            </div>

            {unit.type === "PACKAGED" && (
              <>
                <div>
                  <dt className="text-sm text-slate-500">
                    Khối lượng mỗi gói
                  </dt>
                  <dd className="font-medium">
                    {unit.packageWeight} kg
                  </dd>
                </div>

                <div>
                  <dt className="text-sm text-slate-500">Số gói</dt>
                  <dd className="font-medium">
                    {unit.packageQuantity}
                  </dd>
                </div>
              </>
            )}

            <div>
              <dt className="text-sm text-slate-500">Hạn sử dụng</dt>
              <dd className="font-medium">
                {unit.expiryDate || "Chưa có dữ liệu"}
              </dd>
            </div>

            <div>
              <dt className="text-sm text-slate-500">Batch gốc</dt>
              <dd className="font-medium">{unit.batchCode}</dd>
            </div>
          </dl>

          <Link
            to={`/trace/batch/${encodeURIComponent(unit.batchCode)}`}
            className="mt-5 inline-block font-semibold text-emerald-700 hover:underline"
          >
            Xem toàn bộ hành trình batch gốc →
          </Link>
        </section>

        <section className="rounded-2xl bg-white p-6 shadow">
          <h2 className="text-lg font-bold">
            Kiểm chứng sự kiện tách lô
          </h2>

          {unit.retailRecordKey ? (
            <>
              <p className="mt-3 break-all text-sm text-slate-600">
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
            <p className="mt-3 text-sm text-amber-700">
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
              className={`mt-4 rounded-lg p-4 ${
                verification.valid
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
                  <li>Dữ liệu khớp leaf: {String(verification.dataValid)}</li>
                  <li>Merkle proof hợp lệ: {String(verification.localProofValid)}</li>
                  <li>Root khớp blockchain: {String(verification.rootMatchesBlockchain)}</li>
                  <li>Contract xác minh: {String(verification.contractProofValid)}</li>
                </ul>
              </details>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}