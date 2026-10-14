import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
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

function getGroupType(step) {
  const key = step.recordKey ?? "";
  if (key.startsWith("TRAN")) return "TRANSPORT";
  if (key.startsWith("RETA")) return "RETAIL";
  return null;
}

function groupConsecutiveSteps(steps) {
  const result = [];

  for (const step of steps) {
    const type = getGroupType(step);
    const previous = result[result.length - 1];

    if (type && previous?.groupType === type) {
      previous.steps.push(step);
    } else if (type) {
      result.push({
        groupType: type,
        steps: [step],
      });
    } else {
      result.push({
        groupType: null,
        steps: [step],
      });
    }
  }

  return result;
}

function parseRawJson(rawJson) {
  if (!rawJson) return {};

  try {
    const value = JSON.parse(rawJson);
    return value && typeof value === "object"
      ? value
      : { value };
  } catch {
    return { rawJson };
  }
}

function formatDateTime(value) {
  if (!value) return "Không có dữ liệu";

  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? String(value)
    : date.toLocaleString("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short",
      });
}

function RecordContent({ step, compact = false }) {
  const data = parseRawJson(step.rawJson);

  return (
    <div
      className={
        compact
          ? "rounded-lg border border-slate-200 bg-white p-4"
          : ""
      }
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-bold text-emerald-700">
          {getRecordTitle(step.recordKey)}
        </h3>
        <time className="text-sm text-slate-500">
          {formatDateTime(step.createdAt)}
        </time>
      </div>

      <p className="mt-2 break-all text-sm text-slate-500">
        Record: {step.recordKey}
      </p>

      {Object.keys(data).length > 0 && (
        <dl className="mt-4 grid gap-3 sm:grid-cols-2">
          {Object.entries(data).map(([key, value]) => (
            <div key={key}>
              <dt className="text-xs font-medium uppercase text-slate-500">
                {key}
              </dt>
              <dd className="mt-1 break-words text-sm text-slate-800">
                {value !== null && typeof value === "object"
                  ? JSON.stringify(value)
                  : String(value ?? "—")}
              </dd>
            </div>
          ))}
        </dl>
      )}

      {step.leafHash && (
        <details className="mt-4 text-sm text-slate-600">
          <summary className="cursor-pointer font-medium">
            Xem mã băm Merkle
          </summary>
          <p className="mt-2 break-all rounded bg-slate-50 p-3 font-mono text-xs">
            {step.leafHash}
          </p>
        </details>
      )}
    </div>
  );
}

export default function TraceBatchPage() {
  const { batchCode } = useParams();
  const navigate = useNavigate();

  const [traceability, setTraceability] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    setLoading(true);
    setError("");

    axiosClient
      .get(`/traceability/batch/${encodeURIComponent(batchCode)}`)
      .then((response) => {
        if (active) setTraceability(response.data);
      })
      .catch((err) => {
        if (active) {
          setError(
            err.response?.data?.message ??
              "Không thể tải thông tin truy xuất của lô hàng."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [batchCode]);

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-100 p-6">
        <p className="text-slate-600">Đang tải dữ liệu truy xuất...</p>
      </main>
    );
  }

  if (error || !traceability) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-100 p-6">
        <section className="w-full max-w-lg rounded-2xl bg-white p-8 shadow">
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

  const timeline = groupConsecutiveSteps(traceability.steps ?? []);

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-10">
      <div className="mx-auto max-w-4xl">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="mb-4 text-sm font-semibold text-emerald-700 hover:underline"
        >
          ← Quay lại trang trước
        </button>

        <header className="rounded-2xl bg-emerald-700 p-7 text-white shadow-lg">
          <p className="text-sm text-emerald-100">
            TRUY XUẤT NGUỒN GỐC
          </p>
          <h1 className="mt-2 text-2xl font-bold">
            {traceability.batchName}
          </h1>
          <p className="mt-3 text-sm">
            Mã lô: <span className="font-semibold">{batchCode}</span>
          </p>
          <p className="mt-1 text-sm">
            Trạng thái:{" "}
            <span className="font-semibold">
              {traceability.status}
            </span>
          </p>
        </header>

        <section className="mt-6 rounded-2xl bg-white p-6 shadow">
          <h2 className="text-lg font-bold text-slate-800">
            Hành trình lô hàng
          </h2>

          <div className="mt-5 space-y-5 border-l-2 border-emerald-300 pl-5">
            {timeline.map((item, index) => {
              const first = item.steps[0];
              const last = item.steps[item.steps.length - 1];
              const isGroup = item.groupType !== null;

              return (
                <article
                  key={`${first.recordKey}-${index}`}
                  className="relative rounded-xl border border-slate-200 bg-slate-50 p-5"
                >
                  <span className="absolute -left-[31px] top-6 h-4 w-4 rounded-full bg-emerald-600 ring-4 ring-slate-100" />

                  {isGroup ? (
                    <>
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <h3 className="font-bold text-emerald-700">
                          {item.groupType === "TRANSPORT"
                            ? "Chặng vận chuyển"
                            : "Bán lẻ"}
                        </h3>
                        <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-800">
                          {item.steps.length}{" "}
                          {item.groupType === "TRANSPORT"
                            ? "điểm vận chuyển"
                            : "sự kiện bán lẻ"}
                        </span>
                      </div>

                      <p className="mt-2 text-sm text-slate-600">
                        Ghi nhận từ {formatDateTime(first.createdAt)}
                        {item.steps.length > 1 &&
                          ` đến ${formatDateTime(last.createdAt)}`}
                      </p>

                      <p className="mt-1 text-xs text-slate-500">
                        Mỗi sự kiện có record và mã băm Merkle riêng.
                      </p>

                      <details className="mt-4">
                        <summary className="cursor-pointer text-sm font-medium text-slate-700">
                          Xem {item.steps.length}{" "}
                          {item.groupType === "TRANSPORT"
                            ? "điểm vận chuyển"
                            : "sự kiện bán lẻ"}
                        </summary>

                        <div className="mt-4 space-y-3">
                          {item.steps.map((step) => (
                            <RecordContent
                              key={step.recordKey}
                              step={step}
                              compact
                            />
                          ))}
                        </div>
                      </details>
                    </>
                  ) : (
                    <RecordContent step={first} />
                  )}
                </article>
              );
            })}

            {timeline.length === 0 && (
              <p className="text-sm text-slate-500">
                Lô hàng chưa có sự kiện truy xuất.
              </p>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}