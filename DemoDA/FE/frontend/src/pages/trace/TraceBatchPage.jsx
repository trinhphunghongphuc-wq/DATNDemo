import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import axiosClient from "../../api/axiosClient";

function getRecordTitle(recordKey = "") {
  if (recordKey.startsWith("PROD")) return "Điều kiện bảo quản";
  if (recordKey.startsWith("HARV")) return "Thu hoạch";
  if (recordKey.startsWith("PACK")) return "Đóng gói";
  if (recordKey.startsWith("TRAN")) return "Vận chuyển";
  if (recordKey.startsWith("WARE")) return "Kho vận";
  if (recordKey.startsWith("RETA")) return "Bán lẻ";
  return "Thông tin truy xuất";
}

function parseRawJson(rawJson) {
  try {
    return JSON.parse(rawJson);
  } catch {
    return { rawJson };
  }
}

function formatDateTime(value) {
  if (!value) return "Không có dữ liệu";

  return new Date(value).toLocaleString("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export default function TraceBatchPage() {
  const { batchCode } = useParams();

  const [traceability, setTraceability] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadTraceability() {
      try {
        setLoading(true);
        setError("");

        const response = await axiosClient.get(
          `/traceability/batch/${encodeURIComponent(batchCode)}`
        );

        setTraceability(response.data);
      } catch (err) {
        setError(
          err.response?.data?.message ||
            "Không thể tải thông tin truy xuất của lô hàng."
        );
      } finally {
        setLoading(false);
      }
    }

    loadTraceability();
  }, [batchCode]);

  if (loading) {
    return (
      <main className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
        <p className="text-slate-600">Đang tải dữ liệu truy xuất...</p>
      </main>
    );
  }

  if (error) {
    return (
      <main className="min-h-screen bg-slate-100 flex items-center justify-center p-6">
        <section className="max-w-lg w-full rounded-2xl bg-white p-8 shadow">
          <h1 className="text-xl font-bold text-red-600">
            Không tìm thấy lô hàng
          </h1>
          <p className="mt-3 text-slate-600">{error}</p>
          <p className="mt-4 text-sm text-slate-500">
            Mã lô: {batchCode}
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-100 py-10 px-4">
      <div className="mx-auto max-w-4xl">
        <header className="rounded-2xl bg-emerald-700 p-7 text-white shadow-lg">
          <p className="text-sm text-emerald-100">TRUY XUẤT NGUỒN GỐC</p>
          <h1 className="mt-2 text-2xl font-bold">
            {traceability.batchName}
          </h1>
          <p className="mt-3 text-sm">
            Mã lô: <span className="font-semibold">{batchCode}</span>
          </p>
          <p className="mt-1 text-sm">
            Trạng thái: <span className="font-semibold">{traceability.status}</span>
          </p>
        </header>

        <section className="mt-6 rounded-2xl bg-white p-6 shadow">
          <h2 className="text-lg font-bold text-slate-800">
            Hành trình lô hàng
          </h2>

          <div className="mt-5 space-y-5 border-l-2 border-emerald-300 pl-5">
            {traceability.steps?.map((step) => {
              const data = parseRawJson(step.rawJson);

              return (
                <article
                  key={step.recordKey}
                  className="relative rounded-xl border border-slate-200 bg-slate-50 p-5"
                >
                  <span className="absolute -left-[31px] top-6 h-4 w-4 rounded-full bg-emerald-600 ring-4 ring-slate-100" />

                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h3 className="font-bold text-emerald-700">
                      {getRecordTitle(step.recordKey)}
                    </h3>
                    <time className="text-sm text-slate-500">
                      {formatDateTime(step.createdAt)}
                    </time>
                  </div>

                  <p className="mt-2 text-sm text-slate-500">
                    Record: {step.recordKey}
                  </p>

                  <dl className="mt-4 grid gap-3 sm:grid-cols-2">
                    {Object.entries(data).map(([key, value]) => (
                      <div key={key}>
                        <dt className="text-xs font-medium uppercase text-slate-500">
                          {key}
                        </dt>
                        <dd className="mt-1 break-words text-sm text-slate-800">
                          {typeof value === "object"
                            ? JSON.stringify(value)
                            : String(value)}
                        </dd>
                      </div>
                    ))}
                  </dl>

                  <details className="mt-4 text-sm text-slate-600">
                    <summary className="cursor-pointer font-medium">
                      Xem mã băm Merkle
                    </summary>
                    <p className="mt-2 break-all rounded bg-white p-3 font-mono text-xs">
                      {step.leafHash}
                    </p>
                  </details>
                </article>
              );
            })}
          </div>
        </section>
      </div>
    </main>
  );
}