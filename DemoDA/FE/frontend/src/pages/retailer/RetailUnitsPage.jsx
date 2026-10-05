import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axiosClient from "../../api/axiosClient";
import { QRCodeCanvas } from "qrcode.react";

const publicBaseUrl = (
  import.meta.env.VITE_PUBLIC_APP_URL || window.location.origin
).replace(/\/$/, "");

export default function RetailUnitsPage() {
  const [units, setUnits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    axiosClient.get("/retailer/retail-units")
      .then((response) => {
        if (active) setUnits(response.data ?? []);
      })
      .catch((err) => {
        if (active) {
          setError(
            err.response?.data?.message ??
            "Không tải được đơn vị bán lẻ."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, []);

  function downloadQr(unit) {
    const canvas = document.getElementById(`retail-qr-${unit.id}`);
    if (!canvas) return;

    const link = document.createElement("a");
    link.href = canvas.toDataURL("image/png");
    link.download = `${unit.retailCode}-QR.png`;
    link.click();
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Đơn vị bán lẻ</h1>
        <p className="text-sm text-slate-400">
          Các đơn vị đã tách từ lô hàng Retailer nhận.
        </p>
      </div>

      {error && <p className="text-red-400">{error}</p>}

      {loading ? (
        <p>Đang tải...</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {units.map((unit) => {
            const traceUrl =
              `${publicBaseUrl}/trace/retail/${encodeURIComponent(unit.retailCode)}`;

            return (
              <div
                key={unit.id}
                className="rounded-xl border border-slate-800 bg-[#111827] p-4"
              >
                <h2 className="font-semibold">{unit.productName}</h2>

                <p className="mt-2 text-sm text-slate-400">
                  Mã: {unit.retailCode} · Lô: {unit.batchName}
                </p>

                <p className="text-sm text-slate-400">
                  {unit.type === "PACKAGED"
                    ? `${unit.packageQuantity} gói × ${unit.packageWeight} kg`
                    : `Bán theo kg · phân bổ ${unit.allocatedWeight} kg`}
                </p>

                <p className="text-sm text-slate-400">
                  Trạng thái: {unit.status}
                </p>

                {unit.retailRecordKey && (
                  <p className="mt-1 break-all text-xs text-slate-500">
                    Record tách lô: {unit.retailRecordKey}
                  </p>
                )}

                <div className="mt-4 inline-block rounded-lg bg-white p-3">
                  <QRCodeCanvas
                    id={`retail-qr-${unit.id}`}
                    value={traceUrl}
                    size={150}
                    includeMargin
                  />
                </div>

                <p className="mt-2 break-all text-xs text-slate-400">
                  {traceUrl}
                </p>

                <div className="mt-3 flex flex-wrap gap-4 text-sm">
                  <Link
                    to={`/trace/retail/${encodeURIComponent(unit.retailCode)}`}
                    className="text-blue-400 hover:underline"
                  >
                    Mở trang truy xuất
                  </Link>

                  <Link
                    to={`/retailer/batches/${unit.batchId}/records`}
                    className="text-blue-400 hover:underline"
                  >
                    Xem lô gốc
                  </Link>

                  <button
                    type="button"
                    onClick={() => downloadQr(unit)}
                    className="text-blue-400 hover:underline"
                  >
                    Tải QR
                  </button>
                </div>
              </div>
            );
          })}

          {units.length === 0 && (
            <div>
              <p>Chưa có đơn vị bán lẻ.</p>
              <Link
                to="/retailer/batches"
                className="mt-3 inline-block text-blue-400 hover:underline"
              >
                Chọn lô hàng để tạo
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  );
}