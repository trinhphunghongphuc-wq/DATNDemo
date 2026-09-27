import { useEffect, useMemo, useState } from "react";
import { RefreshCw, Search, ShieldCheck } from "lucide-react";
import axiosClient from "../../api/axiosClient";

const STAGES = ["PRODUCER", "DISTRIBUTOR", "RETAILER"];

function errorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.message ||
    "Thao tác không thành công"
  );
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString("vi-VN") : "—";
}

function shortHash(value) {
  if (!value) return "—";
  return value.length > 24
    ? `${value.slice(0, 12)}...${value.slice(-8)}`
    : value;
}

export default function AdminBatchesPage() {
  const [batches, setBatches] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [anchors, setAnchors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [anchoringStage, setAnchoringStage] = useState(null);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");

  const filteredBatches = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    if (!search) return batches;

    return batches.filter(
      (batch) =>
        batch.name?.toLowerCase().includes(search) ||
        String(batch.id).includes(search)
    );
  }, [batches, keyword]);

  async function loadBatches() {
    setLoading(true);
    setError("");

    try {
      const response = await axiosClient.get("/admin/batches");
      setBatches(response.data ?? []);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function loadBatchDetail(batchId) {
    setSelectedId(batchId);
    setDetail(null);
    setAnchors([]);
    setDetailLoading(true);
    setActionError("");
    setNotice("");

    try {
      const [detailResponse, anchorsResponse] = await Promise.all([
        axiosClient.get(`/admin/batches/${batchId}`),
        axiosClient.get(`/admin/batches/${batchId}/anchors`),
      ]);

      setDetail(detailResponse.data);
      setAnchors(anchorsResponse.data ?? []);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setDetailLoading(false);
    }
  }

  useEffect(() => {
    loadBatches();
  }, []);

  async function handleAnchor(stage) {
    if (!selectedId || anchoringStage) return;

    const accepted = window.confirm(
      `Ghi Merkle root của stage ${stage} cho batch #${selectedId} lên blockchain?\n\nGiao dịch này có thể tốn gas và không thể hoàn tác.`
    );
    if (!accepted) return;

    setAnchoringStage(stage);
    setActionError("");
    setNotice("");

    try {
      await axiosClient.post(
        `/admin/batches/${selectedId}/anchor-root/${stage}`
      );

      const [batchResponse, anchorsResponse, listResponse] =
        await Promise.all([
          axiosClient.get(`/admin/batches/${selectedId}`),
          axiosClient.get(`/admin/batches/${selectedId}/anchors`),
          axiosClient.get("/admin/batches"),
        ]);

      setDetail(batchResponse.data);
      setAnchors(anchorsResponse.data ?? []);
      setBatches(listResponse.data ?? []);
      setNotice(`Đã anchor stage ${stage} của batch #${selectedId}.`);
    } catch (err) {
      setActionError(errorMessage(err));

      // Có thể giao dịch đã được gửi trước khi bước lưu dữ liệu báo lỗi.
      // Tải lại lịch sử để Admin thấy trạng thái thực tế.
      try {
        const response = await axiosClient.get(
          `/admin/batches/${selectedId}/anchors`
        );
        setAnchors(response.data ?? []);
      } catch {
        // Giữ nguyên lỗi thao tác ở trên.
      }
    } finally {
      setAnchoringStage(null);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-blue-400">
            ADMIN / LÔ HÀNG
          </p>
          <h1 className="mt-1 text-2xl font-bold">Quản lý Anchor</h1>
          <p className="mt-1 text-sm text-slate-400">
            Kiểm tra giao dịch riêng cho Producer, Distributor và Retailer.
          </p>
        </div>

        <button
          type="button"
          onClick={loadBatches}
          disabled={loading}
          className="flex items-center gap-2 rounded-xl border border-slate-700 px-4 py-2 text-sm hover:bg-slate-800 disabled:opacity-50"
        >
          <RefreshCw size={16} />
          Làm mới danh sách
        </button>
      </header>

      {error && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
          {error}
        </div>
      )}

      <div className="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
        {/* Danh sách lô */}
        <section className="overflow-hidden rounded-2xl border border-slate-800 bg-[#111827]">
          <div className="border-b border-slate-800 p-4">
            <h2 className="font-semibold">Danh sách lô hàng</h2>
            <p className="mt-1 text-xs text-slate-400">
              {filteredBatches.length} lô
            </p>

            <div className="mt-4 flex items-center gap-2 rounded-xl border border-slate-700 bg-[#0b1020] px-3">
              <Search size={17} className="text-slate-400" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm theo tên hoặc Batch ID"
                className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-slate-500"
              />
            </div>
          </div>

          <div className="max-h-[650px] overflow-y-auto p-2">
            {loading ? (
              <p className="p-4 text-sm text-slate-400">Đang tải...</p>
            ) : filteredBatches.length === 0 ? (
              <p className="p-4 text-sm text-slate-400">
                Không tìm thấy lô hàng.
              </p>
            ) : (
              filteredBatches.map((batch) => (
                <button
                  key={batch.id}
                  type="button"
                  onClick={() => loadBatchDetail(batch.id)}
                  className={`mb-1 w-full rounded-xl border p-4 text-left transition-colors ${
                    selectedId === batch.id
                      ? "border-blue-500/60 bg-blue-500/10"
                      : "border-transparent hover:bg-slate-800/70"
                  }`}
                >
                  <strong className="block truncate text-sm">
                    {batch.name || `Batch #${batch.id}`}
                  </strong>
                  <span className="mt-1 block text-xs text-slate-400">
                    #{batch.id} · {batch.status || "Chưa có trạng thái"}
                  </span>
                  <span className="mt-2 block text-xs text-slate-500">
                    {batch.recordCount ?? 0} bản ghi ·{" "}
                    {formatDate(batch.createdAt)}
                  </span>
                </button>
              ))
            )}
          </div>
        </section>

        {/* Chi tiết và thao tác anchor */}
        <section className="min-h-[400px] rounded-2xl border border-slate-800 bg-[#111827] p-5">
          {!selectedId ? (
            <p className="text-sm text-slate-400">
              Chọn một lô hàng bên trái để xem các stage.
            </p>
          ) : detailLoading ? (
            <p className="text-sm text-slate-400">
              Đang tải batch #{selectedId}...
            </p>
          ) : (
            <div className="space-y-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs text-slate-400">
                    BATCH #{selectedId}
                  </p>
                  <h2 className="mt-1 text-xl font-bold">
                    {detail?.name ?? "Chi tiết lô hàng"}
                  </h2>
                  <p className="mt-1 text-sm text-slate-400">
                    Mã lô: {detail?.batchCode ?? "—"} ·{" "}
                    {detail?.recordCount ?? 0} bản ghi
                  </p>
                </div>
                <span className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300">
                  {detail?.status ?? "—"}
                </span>
              </div>

              {notice && (
                <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-300">
                  {notice}
                </p>
              )}
              {actionError && (
                <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-300">
                  {actionError}
                </p>
              )}

              <div className="grid gap-4 lg:grid-cols-3">
                {STAGES.map((stage) => {
                  const anchor = anchors.find(
                    (item) => item.recordStage === stage
                  );
                  const busy = anchoringStage !== null;

                  return (
                    <div
                      key={stage}
                      className="rounded-xl border border-slate-700 bg-[#0b1020] p-4"
                    >
                      <div className="flex items-center gap-2">
                        <ShieldCheck
                          size={18}
                          className={
                            anchor
                              ? "text-emerald-400"
                              : "text-slate-500"
                          }
                        />
                        <h3 className="font-semibold">{stage}</h3>
                      </div>

                      <p
                        className={`mt-3 text-sm ${
                          anchor
                            ? "text-emerald-400"
                            : "text-amber-400"
                        }`}
                      >
                        {anchor
                          ? "Đã có giao dịch anchor"
                          : "Chưa anchor"}
                      </p>

                      {anchor ? (
                        <div className="mt-4 space-y-2 text-xs text-slate-400">
                          <p title={anchor.transactionHash}>
                            TX: {shortHash(anchor.transactionHash)}
                          </p>
                          <p>Block: {anchor.blockNumber ?? "—"}</p>
                          <p>Gas: {anchor.gasUsed ?? "—"}</p>
                          <p>{formatDate(anchor.anchoredAt)}</p>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleAnchor(stage)}
                          disabled={busy || !detail}
                          className="mt-4 w-full rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {anchoringStage === stage
                            ? "Đang gửi giao dịch..."
                            : `Anchor ${stage}`}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>

              <p className="text-xs text-slate-500">
                Stage chưa có Merkle root sẽ bị BE từ chối và hiện thông báo
                lỗi ở phía trên. Không dùng anchorStatus chung để suy ra
                trạng thái của từng stage.
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}