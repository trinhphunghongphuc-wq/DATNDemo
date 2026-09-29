import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axiosClient from "../../api/axiosClient";

export default function RetailerBatchesPage() {
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    axiosClient.get("/retailer/batches")
      .then((response) => {
        if (active) setBatches(response.data ?? []);
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message ?? "Không tải được danh sách lô.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Lô hàng của Retailer</h1>
        <p className="text-sm text-slate-400">Xác nhận nhận hàng và ghi dữ liệu bán lẻ.</p>
      </div>
      {error && <p className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-red-300">{error}</p>}
      {loading ? <p className="text-slate-400">Đang tải...</p> : (
        <div className="grid gap-4 md:grid-cols-2">
          {batches.map((batch) => (
            <article key={batch.id} className="rounded-xl border border-slate-800 bg-[#111827] p-4">
              <h2 className="font-semibold">{batch.name}</h2>
              <p className="mt-1 text-sm text-slate-400">
                Batch #{batch.id} · {batch.status} · {batch.recordCount ?? 0} record
              </p>
              <Link to={`/retailer/batches/${batch.id}/records`} className="mt-3 inline-block rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold">
                Xem và ghi record
              </Link>
            </article>
          ))}
          {batches.length === 0 && <p className="text-sm text-slate-400">Chưa có lô được gán.</p>}
        </div>
      )}
    </div>
  );
}
