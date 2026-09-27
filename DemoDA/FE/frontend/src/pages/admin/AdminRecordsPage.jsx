import { useEffect, useMemo, useState } from "react";
import { Search } from "lucide-react";
import axiosClient from "../../api/axiosClient";

const formatDate = (value) =>
  value ? new Date(value).toLocaleString("vi-VN") : "—";

export default function AdminRecordsPage() {
  const [records, setRecords] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const response = await axiosClient.get("/admin/records");
        setRecords(response.data ?? []);
      } catch (err) {
        setError(
          err.response?.data?.message ?? "Không tải được danh sách bản ghi."
        );
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return records;

    return records.filter(
      (record) =>
        record.recordKey?.toLowerCase().includes(q) ||
        record.batchName?.toLowerCase().includes(q) ||
        String(record.batchId).includes(q)
    );
  }, [records, keyword]);

  async function openRecord(recordId) {
    setSelected(null);
    setDetailLoading(true);
    setError("");

    try {
      const response = await axiosClient.get(`/admin/records/${recordId}`);
      setSelected(response.data);
    } catch (err) {
      setError(
        err.response?.data?.message ?? "Không tải được chi tiết bản ghi."
      );
    } finally {
      setDetailLoading(false);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header>
        <p className="text-sm font-semibold text-blue-400">
          ADMIN / BẢN GHI
        </p>
        <h1 className="mt-1 text-2xl font-bold">Quản lý bản ghi</h1>
        <p className="mt-1 text-sm text-slate-400">
          Xem record key, leaf hash và dữ liệu đang lưu trong hệ thống.
        </p>
      </header>

      {error && (
        <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
          {error}
        </p>
      )}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.3fr)_minmax(330px,1fr)]">
        <section className="overflow-hidden rounded-2xl border border-slate-800 bg-[#111827]">
          <div className="border-b border-slate-800 p-5">
            <h2 className="font-semibold">Danh sách bản ghi</h2>
            <div className="mt-4 flex items-center gap-2 rounded-xl border border-slate-700 bg-[#0b1020] px-3">
              <Search size={17} className="text-slate-400" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm record key, tên lô hoặc batch ID"
                className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-slate-500"
              />
            </div>
          </div>

          {loading ? (
            <p className="p-5 text-sm text-slate-400">Đang tải...</p>
          ) : filtered.length === 0 ? (
            <p className="p-5 text-sm text-slate-400">
              Không có bản ghi phù hợp.
            </p>
          ) : (
            <div className="max-h-[680px] overflow-y-auto">
              {filtered.map((record) => (
                <button
                  key={record.recordId}
                  type="button"
                  onClick={() => openRecord(record.recordId)}
                  className="w-full border-b border-slate-800 px-5 py-4 text-left hover:bg-slate-800/50"
                >
                  <strong className="block break-all text-sm text-white">
                    {record.recordKey}
                  </strong>
                  <span className="mt-1 block text-xs text-slate-400">
                    {record.batchName ?? "Lô không xác định"} · Batch #
                    {record.batchId ?? "—"}
                  </span>
                  <span className="mt-1 block text-xs text-slate-500">
                    Leaf #{record.leafIndex ?? "—"} ·{" "}
                    {formatDate(record.createdAt)}
                  </span>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="min-h-[350px] rounded-2xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="font-semibold">Chi tiết bản ghi</h2>

          {detailLoading ? (
            <p className="mt-5 text-sm text-slate-400">Đang tải chi tiết...</p>
          ) : !selected ? (
            <p className="mt-5 text-sm text-slate-400">
              Chọn một bản ghi trong danh sách.
            </p>
          ) : (
            <div className="mt-5 space-y-5 text-sm">
              <div>
                <p className="text-slate-400">Record key</p>
                <p className="mt-1 break-all font-mono">
                  {selected.recordKey}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-slate-400">Lô hàng</p>
                  <p className="mt-1">
                    {selected.batchName ?? "—"} (#{selected.batchId ?? "—"})
                  </p>
                </div>
                <div>
                  <p className="text-slate-400">Leaf index</p>
                  <p className="mt-1">{selected.leafIndex ?? "—"}</p>
                </div>
              </div>

              <div>
                <p className="text-slate-400">Leaf hash</p>
                <p className="mt-1 break-all rounded-xl bg-[#0b1020] p-3 font-mono text-xs text-cyan-300">
                  {selected.leafHash ?? "—"}
                </p>
              </div>

              <div>
                <p className="text-slate-400">Dữ liệu lưu trong PostgreSQL</p>
                <pre className="mt-1 max-h-60 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-[#0b1020] p-3 text-xs text-slate-300">
                  {selected.rawJson ?? "—"}
                </pre>
                <p className="mt-2 text-xs text-slate-500">
                  Với record riêng tư, BE lưu placeholder tại đây; dữ liệu
                  thật được mã hóa và xử lý qua luồng xác minh riêng.
                </p>
              </div>

              <p className="text-xs text-slate-500">
                Tạo lúc {formatDate(selected.createdAt)}
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}