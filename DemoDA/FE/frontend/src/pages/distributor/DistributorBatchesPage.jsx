import { useEffect, useState } from "react";
import { getDistributorBatches, receiveDistributorBatch } from "../../api/distributorApi";
import { Link } from "react-router-dom";
export default function DistributorBatchesPage() {
    const [batches, setBatches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [error, setError] = useState("");

    async function loadBatches() {
        try {
            setLoading(true);
            setError("");
            const response = await getDistributorBatches();
            setBatches(response.data ?? []);
        } catch (err) {
            setError(err.response?.data?.message ?? "Không tải được danh sách lô hàng.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadBatches();
    }, []);

    async function handleReceive(batchId) {
        try {
            setBusyId(batchId);
            setError("");
            await receiveDistributorBatch(batchId);
            await loadBatches();
        } catch (err) {
            setError(err.response?.data?.message ?? "Không thể nhận lô hàng.");
        } finally {
            setBusyId(null);
        }
    }

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold">Lô hàng được phân công</h1>
                <p className="mt-1 text-sm text-slate-400">
                    Nhận lô hàng trước khi lập tuyến vận chuyển.
                </p>
            </div>

            {error && (
                <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
                    {error}
                </p>
            )}

            {loading ? (
                <p className="text-slate-400">Đang tải lô hàng...</p>
            ) : batches.length === 0 ? (
                <p className="text-slate-400">Chưa có lô hàng được phân công.</p>
            ) : (
                <div className="grid gap-4 md:grid-cols-2">
                    {batches.map((batch) => (
                        <article

                            key={batch.id}
                            className="rounded-2xl border border-slate-800 bg-[#111827] p-5"
                        >
                            <Link
                                to={`/distributor/batches/${batch.id}`}
                                className="mt-4 inline-block rounded-lg border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800"
                            >
                                Xem hành trình
                            </Link>
                            <h2 className="text-lg font-semibold">{batch.name}</h2>
                            <p className="mt-2 text-sm text-slate-400">Batch ID: {batch.id}</p>
                            <p className="mt-1 text-sm text-slate-400">
                                Trạng thái: {batch.status}
                            </p>
                            <p className="mt-1 text-sm text-slate-400">
                                Số record: {batch.recordCount}
                            </p>

                            {batch.status === "ASSIGNED_TO_DISTRIBUTOR" && (
                                <button
                                    type="button"
                                    disabled={busyId === batch.id}
                                    onClick={() => handleReceive(batch.id)}
                                    className="mt-5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50"
                                >
                                    {busyId === batch.id ? "Đang xử lý..." : "Xác nhận nhận hàng"}
                                </button>
                            )}
                        </article>
                    ))}
                </div>
            )}
        </div>
    );
}