import { useEffect, useState } from "react";
import {
  Boxes,
  Database,
  Link2,
  Users,
  RefreshCw,
  ChevronDown,
} from "lucide-react";
import axiosClient from "../../api/axiosClient";

const stages = ["PRODUCER", "DISTRIBUTOR", "RETAILER"];

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString("vi-VN");
}

function Metric({ label, value, Icon, color }) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
      <div className="flex items-center justify-between">
        <span className="text-sm text-slate-400">{label}</span>
        <Icon size={19} className={color} />
      </div>
      <strong className="mt-4 block text-3xl text-white">
        {value ?? "—"}
      </strong>
    </div>
  );
}

export default function AdminOverviewPage() {
  const [summary, setSummary] = useState(null);
  const [batches, setBatches] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [anchors, setAnchors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [anchorLoading, setAnchorLoading] = useState(false);
  const [error, setError] = useState("");
  const [anchorError, setAnchorError] = useState("");

  async function loadOverview() {
    setLoading(true);
    setError("");

    try {
      const [summaryResponse, batchesResponse] = await Promise.all([
        axiosClient.get("/dashboard/summary"),
        axiosClient.get("/admin/batches"),
      ]);

      setSummary(summaryResponse.data);
      setBatches(batchesResponse.data ?? []);
    } catch (err) {
      setError(
        err.response?.data?.message ??
          "Không tải được dữ liệu Admin. Kiểm tra tài khoản và BE."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview();
  }, []);

  async function toggleAnchors(batchId) {
    if (selectedId === batchId) {
      setSelectedId(null);
      return;
    }

    setSelectedId(batchId);
    setAnchors([]);
    setAnchorError("");
    setAnchorLoading(true);

    try {
      const response = await axiosClient.get(
        `/admin/batches/${batchId}/anchors`
      );
      setAnchors(response.data ?? []);
    } catch (err) {
      setAnchorError(
        err.response?.data?.message ?? "Không tải được lịch sử anchor."
      );
    } finally {
      setAnchorLoading(false);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-blue-400">
            QUẢN TRỊ HỆ THỐNG
          </p>
          <h1 className="mt-1 text-2xl font-bold">Tổng quan</h1>
          <p className="mt-1 text-sm text-slate-400">
            Theo dõi lô hàng, bản ghi và trạng thái anchor theo từng giai đoạn.
          </p>
        </div>

        <button
          type="button"
          onClick={loadOverview}
          disabled={loading}
          className="flex items-center gap-2 rounded-xl border border-slate-700 px-4 py-2 text-sm hover:bg-slate-800 disabled:opacity-50"
        >
          <RefreshCw size={16} />
          Làm mới
        </button>
      </header>

      {error && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
          {error}
        </div>
      )}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric
          label="Lô hàng"
          value={summary?.totalBatches}
          Icon={Boxes}
          color="text-blue-400"
        />
        <Metric
          label="Bản ghi"
          value={summary?.totalRecords}
          Icon={Database}
          color="text-cyan-400"
        />
        <Metric
          label="Người dùng"
          value={summary?.totalUsers}
          Icon={Users}
          color="text-violet-400"
        />
        <Metric
          label="Lô đã anchor"
          value={summary?.anchoredBatches}
          Icon={Link2}
          color="text-emerald-400"
        />
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-800 bg-[#111827]">
        <div className="border-b border-slate-800 p-5">
          <h2 className="font-semibold">Lô hàng và anchor theo stage</h2>
          <p className="mt-1 text-sm text-slate-400">
            Bấm vào một lô để xem giao dịch của Producer, Distributor, Retailer.
          </p>
        </div>

        {loading ? (
          <p className="p-6 text-sm text-slate-400">Đang tải dữ liệu...</p>
        ) : batches.length === 0 ? (
          <p className="p-6 text-sm text-slate-400">Chưa có lô hàng.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[750px] text-left text-sm">
              <thead className="bg-slate-800/60 text-slate-400">
                <tr>
                  <th className="px-5 py-3">Lô hàng</th>
                  <th className="px-5 py-3">Trạng thái</th>
                  <th className="px-5 py-3">Bản ghi</th>
                  <th className="px-5 py-3">Ngày tạo</th>
                  <th className="px-5 py-3">Anchor</th>
                </tr>
              </thead>

              <tbody>
                {batches.map((batch) => (
                  <tr key={batch.id} className="border-t border-slate-800">
                    <td colSpan={5} className="p-0">
                      <button
                        type="button"
                        onClick={() => toggleAnchors(batch.id)}
                        className="grid w-full min-w-[750px] grid-cols-[2fr_1.3fr_0.7fr_1.4fr_0.8fr] items-center gap-2 px-5 py-4 text-left hover:bg-slate-800/40"
                      >
                        <span>
                          <strong className="block text-white">
                            {batch.name}
                          </strong>
                          <small className="text-slate-500">
                            Batch #{batch.id}
                          </small>
                        </span>
                        <span className="text-slate-300">
                          {batch.status ?? "—"}
                        </span>
                        <span className="text-slate-300">
                          {batch.recordCount ?? 0}
                        </span>
                        <span className="text-slate-400">
                          {formatDate(batch.createdAt)}
                        </span>
                        <span className="flex items-center gap-1 text-blue-400">
                          Chi tiết <ChevronDown size={15} />
                        </span>
                      </button>

                      {selectedId === batch.id && (
                        <div className="border-t border-slate-800 bg-[#0d1423] px-5 py-5">
                          {anchorLoading ? (
                            <p className="text-slate-400">Đang tải anchor...</p>
                          ) : anchorError ? (
                            <p className="text-red-400">{anchorError}</p>
                          ) : (
                            <div className="grid gap-3 md:grid-cols-3">
                              {stages.map((stage) => {
                                const anchor = anchors.find(
                                  (item) => item.recordStage === stage
                                );

                                return (
                                  <div
                                    key={stage}
                                    className="rounded-xl border border-slate-700 bg-[#111827] p-4"
                                  >
                                    <p className="font-semibold">{stage}</p>
                                    <p
                                      className={`mt-2 text-sm ${
                                        anchor
                                          ? "text-emerald-400"
                                          : "text-slate-400"
                                      }`}
                                    >
                                      {anchor
                                        ? "Có giao dịch anchor"
                                        : "Chưa có giao dịch anchor"}
                                    </p>
                                    {anchor && (
                                      <>
                                        <p
                                          className="mt-2 break-all font-mono text-xs text-slate-400"
                                          title={anchor.transactionHash}
                                        >
                                          TX: {anchor.transactionHash ?? "—"}
                                        </p>
                                        <p className="mt-1 text-xs text-slate-400">
                                          Block: {anchor.blockNumber ?? "—"}
                                        </p>
                                        <p className="mt-1 text-xs text-slate-400">
                                          {formatDate(anchor.anchoredAt)}
                                        </p>
                                      </>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}