import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, ChevronLeft, ChevronRight, ExternalLink } from "lucide-react";
import axiosClient from "../../api/axiosClient";
import { useNotifications } from "../../contexts/notificationsContext";

function formatDate(value) {
  return new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" });
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const { unreadCount, revision, connected, markRead } = useNotifications();
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get("/notifications", {
      params: { page, size: 20 }, signal: controller.signal,
    }).then(({ data }) => {
      setResult(data);
      setError("");
    }).catch((error) => {
      if (!controller.signal.aborted) {
        setError(error.response?.data?.message || "Không tải được thông báo. Hãy thử lại.");
      }
    });
    return () => controller.abort();
  }, [page, revision, retry]);

  async function openNotification(item) {
    setBusyId(item.id);
    try {
      const read = await markRead(item.id);
      navigate(read.targetUrl);
    } catch (error) {
      setError(error.response?.data?.message || "Chưa đánh dấu đã đọc được. Hãy thử lại.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold"><Bell />Thông báo</h1>
          <p className="mt-2 text-sm text-slate-400">
            {unreadCount > 0 ? `${unreadCount} thông báo chưa đọc` : "Bạn đã xem hết thông báo"}
            {" · "}{connected ? "Đang cập nhật trực tiếp" : "Đang kết nối lại…"}
          </p>
        </div>
        <button onClick={() => setRetry((value) => value + 1)}
          className="rounded-lg border border-slate-700 px-4 py-2 text-sm hover:bg-slate-800">
          Tải lại
        </button>
      </header>

      {error && <p role="alert" className="rounded-xl bg-red-500/10 p-4 text-red-300">{error}</p>}
      {!result && !error && <p className="text-slate-400">Đang tải thông báo…</p>}
      {result?.items.length === 0 && (
        <div className="rounded-2xl border border-slate-800 bg-[#111827] px-6 py-14 text-center">
          <Bell className="mx-auto mb-4 text-slate-500" size={36} />
          <h2 className="font-semibold">Chưa có thông báo</h2>
          <p className="mt-2 text-sm text-slate-400">Lô hàng mới được giao đến bạn sẽ xuất hiện ở đây.</p>
        </div>
      )}
      <div className="space-y-3">
        {result?.items.map((item) => (
          <button key={item.id} onClick={() => openNotification(item)} disabled={busyId !== null}
            className={`flex w-full items-start gap-4 rounded-2xl border p-5 text-left transition disabled:opacity-60 ${
              item.readAt ? "border-slate-800 bg-[#111827] hover:bg-slate-800/70"
                : "border-blue-500/40 bg-blue-500/10 hover:bg-blue-500/20"
            }`}>
            <div className="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-800">
              <Bell size={20} className={item.readAt ? "text-slate-400" : "text-blue-400"} />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <h2 className="font-semibold">{item.title}</h2>
                {!item.readAt && <span className="h-2 w-2 shrink-0 rounded-full bg-blue-400" aria-label="Chưa đọc" />}
              </div>
              <p className="mt-1 break-words text-sm text-slate-300">{item.message}</p>
              <p className="mt-3 text-xs text-slate-400">{formatDate(item.createdAt)} · {item.readAt ? "Đã đọc" : "Chưa đọc"}</p>
            </div>
            <ExternalLink size={17} className="mt-1 shrink-0 text-slate-400" />
          </button>
        ))}
      </div>
      {result && (page > 0 || result.hasNext) && (
        <div className="flex items-center justify-between text-sm">
          <button disabled={page === 0} onClick={() => setPage((value) => value - 1)}
            className="flex items-center gap-1 rounded-lg border border-slate-700 px-3 py-2 disabled:opacity-40">
            <ChevronLeft size={16} />Trước
          </button>
          <span className="text-slate-400">Trang {page + 1}</span>
          <button disabled={!result.hasNext} onClick={() => setPage((value) => value + 1)}
            className="flex items-center gap-1 rounded-lg border border-slate-700 px-3 py-2 disabled:opacity-40">
            Sau<ChevronRight size={16} />
          </button>
        </div>
      )}
    </section>
  );
}
