import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axiosClient from "../../api/axiosClient";

function errorMessage(error) {
  return error.response?.data?.message ?? error.message ?? "Đã xảy ra lỗi.";
}

export default function RetailerRecordsPage() {
  const { batchId } = useParams();
  const [batch, setBatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [rejectForm, setRejectForm] = useState({ reason: "" });
  const [evidenceFile, setEvidenceFile] = useState(null);
  const [form, setForm] = useState({
    location: "",
    condition: "GOOD",
    note: "",
  });

  const loadBatch = useCallback(async () => {
    const response = await axiosClient.get(`/retailer/batches/${batchId}`);
    setBatch(response.data);
  }, [batchId]);

  useEffect(() => {
    let active = true;
    axiosClient.get(`/retailer/batches/${batchId}`)
      .then((response) => {
        if (active) setBatch(response.data);
      })
      .catch((err) => {
        if (active) setError(errorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [batchId]);

  async function confirmDelivery() {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await axiosClient.post(`/retailer/batches/${batchId}/confirm-delivery`);
      await loadBatch();
      setNotice("Đã xác nhận nhận lô. Bây giờ hãy ghi nhận sự kiện bán lẻ.");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

async function rejectDelivery(event) {
  event.preventDefault();
  setBusy(true);
  setError("");
  setNotice("");

  try {
    const reason = rejectForm.reason.trim();
    const returnedWeight = Number(batch?.remainingWeight);

    if (!reason) {
      throw new Error("Vui lòng nhập lý do trả hàng.");
    }
    if (!evidenceFile) {
      throw new Error("Hãy chọn ảnh minh chứng.");
    }
    if (!Number.isFinite(returnedWeight) || returnedWeight <= 0) {
      throw new Error("Batch chưa có khối lượng trả hợp lệ.");
    }

    const uploadForm = new FormData();
    uploadForm.append("file", evidenceFile);

    const uploadResponse = await axiosClient.post(
      `/retailer/batches/${batchId}/return-evidence`,
      uploadForm
    );

    const evidenceCid = uploadResponse.data?.cid;
    if (typeof evidenceCid !== "string" || !evidenceCid.trim()) {
      throw new Error(
        `API upload ảnh không trả cid. Response: ${JSON.stringify(uploadResponse.data)}`
      );
    }

    await axiosClient.post(`/retailer/batches/${batchId}/reject-delivery`, {
      reason,
      returnedWeight,
      evidenceCid,
    });

    await loadBatch();
    setNotice("Đã từ chối lô hàng và lưu record minh chứng.");
    setRejectForm({ reason: "" });
    setEvidenceFile(null);
  } catch (err) {
    setError(errorMessage(err));
  } finally {
    setBusy(false);
  }
}
  async function addRetailRecord(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await axiosClient.post(`/retailer/batches/${batchId}/retail-record`, {
        recordType: "RETAIL",
        privateData: false,
        rawJson: JSON.stringify({
          eventType: "RETAIL_RECEIPT_INSPECTION",
          location: form.location.trim(),
          condition: form.condition,
          note: form.note.trim(),
          timestamp: new Date().toISOString(),
        }),
      });
      await loadBatch();
      setForm({ location: "", condition: "GOOD", note: "" });
      setNotice("Đã lưu RETAIL record. BE đã tính Merkle root cho stage Retailer.");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <p className="text-slate-400">Đang tải lô hàng...</p>;

  return (
    <div className="space-y-5">
      <Link to="/retailer/batches" className="text-sm text-blue-400 hover:underline">
        ← Danh sách lô Retailer
      </Link>
      <div>
        <h1 className="text-2xl font-bold">{batch?.name ?? `Batch #${batchId}`}</h1>
        <p className="mt-1 text-sm text-slate-400">
          Batch #{batchId} · {batch?.status ?? "Không rõ trạng thái"} · {batch?.recordCount ?? batch?.records?.length ?? 0} record
        </p>
      </div>

      {error && <p className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-red-300">{error}</p>}
      {notice && <p className="rounded-lg border border-green-500/30 bg-green-500/10 p-3 text-green-300">{notice}</p>}

      {batch?.status === "DELIVERED_TO_RETAILER" && (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="mb-3 text-sm text-slate-300">Kiểm tra hàng thực tế trước khi xác nhận nhận lô.</p>
          <button type="button" disabled={busy} onClick={confirmDelivery} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {busy ? "Đang xử lý..." : "Xác nhận nhận hàng"}
          </button>
        </div>
      )}

      {batch?.status === "DELIVERED_TO_RETAILER" && (
        <form onSubmit={rejectDelivery} className="space-y-3 rounded-xl border border-red-500/30 bg-[#111827] p-4">
          <h2 className="font-semibold">Từ chối nhận và trả toàn bộ lô</h2>
          <p className="text-sm text-slate-400">
            Khối lượng trả: {batch.remainingWeight ?? "Chưa có dữ liệu"} kg
          </p>

          <label className="block text-sm">
            Lý do trả hàng
            <textarea
              required
              value={rejectForm.reason}
              onChange={(e) => setRejectForm({ reason: e.target.value })}
              className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
          </label>

          <label className="block text-sm">
            Ảnh minh chứng
            <input
              required
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => setEvidenceFile(e.target.files?.[0] ?? null)}
              className="mt-1 block w-full"
            />
          </label>

          <button type="submit" disabled={busy || batch.remainingWeight == null}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {busy ? "Đang xử lý..." : "Từ chối nhận hàng"}
          </button>
        </form>
      )}

      {batch?.status === "AT_RETAIL" && (
        <form onSubmit={addRetailRecord} className="space-y-3 rounded-xl border border-slate-800 bg-[#111827] p-4">
          <h2 className="font-semibold">Ghi RETAIL record</h2>
          <p className="text-sm text-slate-400">Ghi nhận kiểm tra lô tại điểm bán. Hoàn tất mọi record Retailer trước khi Admin anchor stage này.</p>
          <div className="grid gap-3 md:grid-cols-2">
            <label className="text-sm">Điểm bán / nơi nhận
              <input required value={form.location} onChange={(e) => setForm((old) => ({ ...old, location: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
            <label className="text-sm">Tình trạng nhận hàng
              <select value={form.condition} onChange={(e) => setForm((old) => ({ ...old, condition: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2">
                <option value="GOOD">Đạt</option>
                <option value="OBSERVATION">Cần theo dõi</option>
              </select>
            </label>
          </div>
          <label className="block text-sm">Ghi chú
            <textarea rows={3} value={form.note} onChange={(e) => setForm((old) => ({ ...old, note: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
          </label>
          <button type="submit" disabled={busy} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {busy ? "Đang lưu..." : "Lưu RETAIL record"}
          </button>
        </form>
      )}

      {batch?.status !== "DELIVERED_TO_RETAILER" && batch?.status !== "AT_RETAIL" && (
        <p className="rounded-xl border border-slate-800 bg-[#111827] p-4 text-sm text-slate-400">
          Chỉ có thể ghi RETAIL record sau khi Distributor giao hàng và Retailer xác nhận nhận lô.
        </p>
      )}

      <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
        <h2 className="font-semibold">Record của lô</h2>
        <ul className="mt-3 space-y-2 text-sm">
          {(batch?.records ?? []).map((record) => (
            <li key={record.recordId} className="rounded-lg border border-slate-700 p-2">
              {record.recordType} · {record.recordKey}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
