import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axiosClient from "../../api/axiosClient";

export default function RetailUnitsPage() {
  const [units, setUnits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    axiosClient.get("/retailer/retail-units")
      .then((response) => setUnits(response.data ?? []))
      .catch((err) => setError(
        err.response?.data?.message ?? "Không tải được đơn vị bán lẻ."
      ))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Đơn vị bán lẻ</h1>
        <p className="text-sm text-slate-400">
          Các đơn vị đã tách từ lô hàng Retailer nhận.
        </p>
      </div>

      {error && <p className="text-red-400">{error}</p>}
      {loading ? <p>Đang tải...</p> : (
        <div className="grid gap-4 md:grid-cols-2">
          {units.map((unit) => (
            <div key={unit.id}
              className="rounded-xl border border-slate-800 bg-[#111827] p-4">
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
              <Link
                to={`/retailer/batches/${unit.batchId}/records`}
                className="mt-3 inline-block text-sm text-blue-400 hover:underline"
              >
                Xem lô gốc
              </Link>
            </div>
          ))}
          {units.length === 0 && <p>Chưa có đơn vị bán lẻ.</p>}
        </div>
      )}
    </div>
  );
}