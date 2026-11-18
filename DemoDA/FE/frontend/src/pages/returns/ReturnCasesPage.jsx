import { useEffect, useState, useRef } from "react";
import { Link, useParams } from "react-router-dom";
import axiosClient from "../../api/axiosClient";

const panel = "rounded-xl border border-slate-800 bg-[#111827] p-5";
const input = "mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2";
const button = "rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium disabled:opacity-40";
const statuses = {
  REQUESTED: "Chờ Producer duyệt",
  APPROVED: "Chờ tạo chuyến về",
  IN_TRANSIT: "Đang hoàn trả",
  QUARANTINED: "Đã cách ly · Chờ kiểm chứng",
  INSPECTED: "Chờ quyết định xử lý",
  DECIDED: "Đang thực hiện phương án",
  CLOSED: "Đã đóng hồ sơ",
};
const titles = {
  REJECTED: "Retailer từ chối nhận",
  APPROVED: "Producer duyệt hoàn trả",
  TRIP_STARTED: "Distributor tạo chuyến về",
  RECEIVED: "Nhập kho cách ly",
  INSPECTED: "Biên bản kiểm chứng",
  DECIDED: "Quyết định xử lý",
  EXECUTED: "Xác nhận thực hiện",
  CLOSED: "Đóng hồ sơ",
};
const outcomes = [
  { id: "TO_PRODUCER", field: "toProducer", role: "PRODUCER", label: "Chuyển về Producer" },
  { id: "DESTROY", field: "destroy", role: "DISTRIBUTOR", label: "Tiêu hủy" },
  { id: "REDISTRIBUTE", field: "redistribute", role: "DISTRIBUTOR", label: "Giao lại bằng lô con" },
];
const role = () => (localStorage.getItem("role") ?? "").replace(/^ROLE_/, "");
const errorMessage = (error) => error.response?.data?.message ?? error.message;
const formatDate = (date) => new Date(date).toLocaleString("vi-VN");
const weight = (value) => `${value} kg`;
const gateway = (import.meta.env.VITE_RETURN_IPFS_GATEWAY || "http://127.0.0.1:8081/ipfs").replace(/\/$/, "");

function Pager({ page, setPage, hasNext }) {
  return (page > 0 || hasNext) && (
    <div className="flex items-center gap-4 text-sm">
      <button disabled={!page} onClick={() => setPage(page - 1)} className={button}>Trước</button>
      <span>Trang {page + 1}</span>
      <button disabled={!hasNext} onClick={() => setPage(page + 1)} className={button}>Sau</button>
    </div>
  );
}

export function ReturnCaseList() {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get("/return-cases", { params: { page }, signal: controller.signal })
      .then(({ data }) => { setResult(data); setError(""); })
      .catch((e) => { if (!controller.signal.aborted) setError(errorMessage(e)); });
    return () => controller.abort();
  }, [page, revision]);

  return (
    <section className="mx-auto max-w-5xl space-y-4">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Hồ sơ hoàn trả</h1>
        <button className={button} onClick={() => setRevision((n) => n + 1)}>Tải lại</button>
      </header>
      {error && <p role="alert" className="text-red-300">{error}</p>}
      {!error && !result && <p>Đang tải…</p>}
      {result?.items.length === 0 && <p className={panel}>Chưa có hồ sơ hoàn trả.</p>}
      {result?.items.map((item) => (
        <Link key={item.batchId} to={`/return-cases/${item.batchId}`}
          className={`${panel} flex flex-wrap items-center justify-between gap-3 hover:border-blue-500`}>
          <div>
            <h2 className="font-semibold">{item.batchCode} · {item.name}</h2>
            <p className="mt-1 text-sm text-slate-400">{weight(item.requestedWeight)} · {formatDate(item.updatedAt)}</p>
          </div>
          <span className="rounded-full bg-slate-800 px-3 py-1 text-sm">{statuses[item.status]}</span>
        </Link>
      ))}
      <Pager page={page} setPage={setPage} hasNext={result?.hasNext} />
    </section>
  );
}

function Field({ name, label, type = "text", initial, min, max, maxLength = 2000 }) {
  return (
    <label className="block text-sm">{label}
      <input name={name} type={type} defaultValue={initial} min={min} max={max}
        step={type === "number" ? "any" : undefined} maxLength={type === "text" ? maxLength : undefined}
        className={input} required />
    </label>
  );
}

function Action({ title, action, endpoint, onSaved, busy, setBusy, evidence, children }) {
  const [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const data = Object.fromEntries(values);
    delete data.file;
    setError("");
    setBusy(true);
    try {
      if (evidence) {
        const upload = new FormData();
        upload.append("file", values.get("file"));
        const result = await axiosClient.post(`${endpoint}/evidence`, upload);
        data.evidenceCid = result.data.cid;
      }
      const updated = await axiosClient.post(`${endpoint}/${action}`, data);
      onSaved(updated.data);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className={`${panel} space-y-3`}>
      <h2 className="font-semibold">{title}</h2>
      <fieldset disabled={busy} className="space-y-3">
        {children}
        {evidence && <label className="block text-sm">Ảnh bằng chứng
          <input name="file" type="file" accept="image/jpeg,image/png,image/webp" className={input} required />
        </label>}
        <button className={button} type="submit">{busy ? "Đang lưu…" : title}</button>
      </fieldset>
      {error && <p role="alert" className="text-sm text-red-300">{error}</p>}
    </form>
  );
}

function TripFields({ requestedWeight }) {
  const [vehicles, setVehicles] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get("/distributor/vehicles", { signal: controller.signal })
      .then(({ data }) => setVehicles(data.filter((v) => v.active && !v.demoBusy)))
      .catch((e) => { if (!controller.signal.aborted) setError(errorMessage(e)); });
    return () => controller.abort();
  }, []);
  return (
    <>
      <label className="block text-sm">Xe có thiết bị Ed25519
        <select className={input} name="vehicleId" required defaultValue="">
          <option value="">{vehicles ? "Chọn xe" : "Đang tải danh sách xe…"}</option>
          {vehicles?.map((v) => <option key={v.id} value={v.id}>{v.vehiclePlate} · {v.deviceId}</option>)}
        </select>
      </label>
      {error && <p role="alert" className="text-red-300">{error}</p>}
      <div className="grid gap-3 sm:grid-cols-2">
        <Field name="warehouseName" label="Tên kho cách ly" maxLength={200} />
        <Field name="warehouseAddress" label="Địa chỉ kho" maxLength={300} />
      </div>
      <Field name="loadedWeight" label="Khối lượng thực giao lên xe (kg)"
        type="number" initial={requestedWeight} min={0} max={requestedWeight} />
    </>
  );
}

function CaseActions({ detail, endpoint, onSaved, busy, setBusy }) {
  const { status, requestedWeight, receivedWeight } = detail.summary;
  const currentRole = role();
  const props = { endpoint, onSaved, busy, setBusy };
  if (currentRole === "PRODUCER") {
    if (status === "REQUESTED") return (
      <Action {...props} action="approve" title="Duyệt chuyến trả toàn bộ lô">
        <input type="hidden" name="weight" value={requestedWeight} />
        <p className="text-sm text-slate-300">Khối lượng duyệt {weight(requestedWeight)}.</p>
        <label className="block text-sm">Ghi chú (nếu có)
          <input name="note" maxLength={2000} className={input} />
        </label>
      </Action>
    );
    if (status === "QUARANTINED") return (
      <Action {...props} action="inspect" title="Lưu biên bản kiểm chứng" evidence>
        <Field name="inspectorName" label="Họ tên người trực tiếp kiểm chứng" maxLength={150} />
        <Field name="conclusion" label="Kết luận kiểm tra và đối chiếu dữ liệu vận chuyển" />
      </Action>
    );
    if (status === "INSPECTED") return (
      <Action {...props} action="decision" title="Duyệt phương án xử lý">
        <p className="text-sm text-slate-300">Tổng ba phương án phải bằng khối lượng thực nhận {weight(receivedWeight)}.</p>
        <div className="grid gap-3 sm:grid-cols-3">
          {outcomes.map((option) => (
            <Field key={option.id} name={option.field} label={`${option.label} (kg)`} type="number"
              initial={0} min={0} max={receivedWeight} />
          ))}
        </div>
        <Field name="reason" label="Căn cứ quyết định" />
      </Action>
    );
  }

  if (currentRole === "DISTRIBUTOR") {
    if (status === "APPROVED") return (
      <Action {...props} action="trip" title="Tạo chuyến hoàn trả">
        <TripFields requestedWeight={requestedWeight} />
      </Action>
    );
    if (status === "IN_TRANSIT") return (
      <Action {...props} action="receive" title="Xác nhận nhập kho cách ly" evidence>
        <Field name="receivedWeight" label="Khối lượng cân thực nhận (kg)" type="number" min={0}
          max={requestedWeight} initial={requestedWeight} />
        <Field name="note" label="Tình trạng lô và lý do chênh lệch nếu có" />
      </Action>
    );
  }

  if (status !== "DECIDED") return null;
  const decision = detail.events.find((event) => event.type === "DECIDED").details;
  const done = new Set(detail.events.filter((event) => event.type === "EXECUTED").map((event) => event.details.outcome));
  const pending = outcomes.filter((option) => Number(decision[option.field]) > 0 && !done.has(option.id));
  return (
    <div className="grid gap-4">
      {pending.filter((option) => option.role === currentRole).map((option) => (
        <Action key={option.id} {...props} action="execute" title={`Xác nhận: ${option.label}`} evidence>
          <input type="hidden" name="outcome" value={option.id} />
          <Field name="weight" label="Khối lượng đã xử lý thực tế (kg)"
            type="number" initial={decision[option.field]} min={0} />
          <Field name="note" label={option.id === "REDISTRIBUTE"
            ? "Ghi nhận tách hàng sang lô con để giao lại"
            : "Ghi nhận kết quả thực hiện"} />
        </Action>
      ))}
      {currentRole === "PRODUCER" && pending.length === 0 && (
        <Action {...props} action="close" title="Đối soát và đóng hồ sơ" />
      )}
    </div>
  );
}

function EventDetails({ entry }) {
  const data = entry.details;
  const [showEvidence, setShowEvidence] = useState(false);
  const imageDialog = useRef(null);
  const outcome = outcomes.find((option) => option.id === data.outcome);
  return (
    <div className="mt-2 space-y-1 text-sm text-slate-300">
      {data.weight != null && <p>{outcome?.label ?? "Khối lượng"} · {weight(data.weight)}</p>}
      {data.loadedWeight != null && <p>Đã giao lên xe · {weight(data.loadedWeight)}</p>}
      {data.receivedWeight != null && <p>Thực nhận · {weight(data.receivedWeight)}</p>}
      {data.toProducer != null && outcomes.map((option) => (
        <p key={option.id}>{option.label} · {weight(data[option.field])}</p>
      ))}
      {[data.reason, data.note, data.conclusion].filter(Boolean).map((value, index) => (
        <p key={index} className="whitespace-pre-wrap break-words">{value}</p>
      ))}
      {data.inspectorName && <p>Người kiểm chứng · {data.inspectorName}</p>}
      {data.recordKey && <p>Bản ghi từ chối · {data.recordKey}</p>}
      {data.childBatchId && <p>Lô con #{data.childBatchId}</p>}
      {data.evidenceCid && (
        <div>
          <button type="button" onClick={() => setShowEvidence(!showEvidence)} className="text-blue-400 hover:underline">
            {showEvidence ? "Ẩn ảnh bằng chứng" : "Xem ảnh bằng chứng"}
          </button>
          {showEvidence && (
            <>
              <img
                src={`${gateway}/${encodeURIComponent(data.evidenceCid)}`}
                alt="Ảnh bằng chứng"
                className="my-2 max-h-64 max-w-full rounded-lg object-contain"
                loading="lazy"
                referrerPolicy="no-referrer"
              />

              <button
                type="button"
                onClick={() => imageDialog.current?.showModal()}
                className="rounded-lg border border-slate-600 px-3 py-1.5 text-blue-300 hover:bg-slate-800"
              >
                Phóng to ảnh
              </button>

              <dialog
                ref={imageDialog}
                aria-label="Ảnh bằng chứng phóng to"
                className="m-auto max-h-[95dvh] w-[min(96vw,1200px)] max-w-none rounded-xl bg-slate-950 p-4 text-white backdrop:bg-black/80"
              >
                <form method="dialog" className="mb-3 flex justify-end">
                  <button className="rounded-lg border border-slate-600 px-3 py-1.5">
                    Đóng ảnh
                  </button>
                </form>

                <img
                  src={`${gateway}/${encodeURIComponent(data.evidenceCid)}`}
                  alt="Ảnh bằng chứng phóng to"
                  className="mx-auto max-h-[80dvh] max-w-full object-contain"
                  loading="lazy"
                  referrerPolicy="no-referrer"
                />
              </dialog>

              <p className="break-all text-xs text-slate-400">CID {data.evidenceCid}</p>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function Telemetry({ endpoint, detail }) {
  const [result, setResult] = useState(null);
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get(`${endpoint}/telemetry`, { params: { page }, signal: controller.signal })
      .then(({ data }) => { setResult(data); setError(""); })
      .catch((e) => { if (!controller.signal.aborted) setError(errorMessage(e)); });
    return () => controller.abort();
  }, [endpoint, page, revision, detail.summary.status]);

  return (
    <section className={`${panel} space-y-3`}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-semibold">Dữ liệu IoT chuyến về #{detail.tripId}</h2>
        <button className={button} onClick={() => setRevision((n) => n + 1)}>Tải điểm mới</button>
      </div>
      <p className="text-sm text-slate-300">Xe #{detail.vehicleId} · {detail.warehouseName} · {detail.warehouseAddress}</p>
      {error && <p role="alert" className="text-red-300">{error}</p>}
      {!result?.items.length && !error && <p className="text-sm text-slate-400">
        {result ? "Chưa có điểm đo đã xác thực." : "Đang tải điểm đo…"}
      </p>}
      {result?.items.map((entry) => {
        const point = entry.details.reading;
        return (
          <div key={entry.id} className="border-t border-slate-800 pt-2 text-sm">
            <p>{formatDate(point.measuredAt)} · {point.temperature}°C · {point.humidity}% RH</p>
            <p className="text-slate-400">GPS {point.latitude}, {point.longitude} · Sequence {point.sequence} · Chữ ký hợp lệ lúc nhận</p>
          </div>
        );
      })}
      <Pager page={page} setPage={setPage} hasNext={result?.hasNext} />
    </section>
  );
}

export function ReturnCaseDetail() {
  const { batchId } = useParams();
  return <ReturnCaseContent key={batchId} batchId={batchId} />;
}

function ReturnCaseContent({ batchId }) {
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const endpoint = `/return-cases/${batchId}`;

  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get(endpoint, { signal: controller.signal })
      .then(({ data }) => { setDetail(data); setError(""); })
      .catch((e) => { if (!controller.signal.aborted) setError(errorMessage(e)); });
    return () => controller.abort();
  }, [endpoint]);

  return (
    <section className="mx-auto max-w-5xl space-y-4">
      <Link to="/return-cases" className="text-sm text-blue-400 hover:underline">← Hồ sơ hoàn trả</Link>
      {error && <p role="alert" className="text-red-300">{error}</p>}
      {!detail && !error && <p>Đang tải…</p>}
      {detail && (
        <>
          <header className={panel}>
            <h1 className="text-2xl font-bold">{detail.summary.batchCode} · {detail.summary.name}</h1>
            <p className="mt-2 text-blue-300">{statuses[detail.summary.status]}</p>
            <p className="mt-1 text-sm text-slate-300">
              Đề nghị trả {weight(detail.summary.requestedWeight)}
              {detail.summary.receivedWeight != null && ` · Thực nhận ${weight(detail.summary.receivedWeight)}`}
            </p>
          </header>
          <CaseActions key={detail.summary.status} detail={detail} endpoint={endpoint}
            onSaved={setDetail} busy={busy} setBusy={setBusy} />
          {detail.tripId && <Telemetry endpoint={endpoint} detail={detail} />}
          <section className={`${panel} space-y-4`}>
            <h2 className="font-semibold">Lịch sử xử lý</h2>
            <ol className="space-y-4">
              {detail.events.map((entry) => (
                <li key={entry.id} className="border-l-2 border-slate-700 pl-4">
                  <h3 className="font-medium">{titles[entry.type]}</h3>
                  <p className="text-xs text-slate-400">{entry.actorRole} #{entry.actorId} · {formatDate(entry.occurredAt)}</p>
                  <EventDetails entry={entry} />
                </li>
              ))}
            </ol>
          </section>
        </>
      )}
    </section>
  );
}
