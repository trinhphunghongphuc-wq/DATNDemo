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

  const [storageForm, setStorageForm] = useState({
  location: "",
  temperature: "",
  humidity: "",
  note: "",
});

const [withInitialStorage, setWithInitialStorage] = useState(false);

  const [retailUnits, setRetailUnits] = useState([]);
  const [unitForm, setUnitForm] = useState({
    productName: "",
    type: "BULK",
    allocatedWeight: "",
    pricePerKg: "",
    packageWeight: "",
    packageQuantity: "",
    pricePerPackage: "",
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

  useEffect(() => {
    let active = true;

    axiosClient.get(`/retailer/batches/${batchId}/retail-units`)
      .then((response) => {
        if (active) setRetailUnits(response.data ?? []);
      })
      .catch((err) => {
        if (active) setError(errorMessage(err));
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

  async function addStorageReading(event) {
  event.preventDefault();

  const temperature = Number(storageForm.temperature);
  const humidity = Number(storageForm.humidity);

  if (
    storageForm.temperature === "" ||
    storageForm.humidity === "" ||
    !Number.isFinite(temperature) ||
    !Number.isFinite(humidity) ||
    humidity < 0 ||
    humidity > 100
  ) {
    setError("Nhập nhiệt độ hợp lệ và độ ẩm từ 0 đến 100%.");
    return;
  }

  setBusy(true);
  setError("");
  setNotice("");

  try {
    await axiosClient.post(
      `/retailer/batches/${batchId}/storage-readings`,
      {
        location: storageForm.location.trim(),
        temperature,
        humidity,
        note: storageForm.note.trim(),
      }
    );

    await loadBatch();
    setStorageForm({
      location: "",
      temperature: "",
      humidity: "",
      note: "",
    });
    setNotice("Đã ghi nhận điều kiện bảo quản tại Retailer.");
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


async function createRetailUnit(event) {
  event.preventDefault();
  setBusy(true);
  setError("");
  setNotice("");

  try {
    const productName = unitForm.productName.trim();

    const payload =
      unitForm.type === "PACKAGED"
        ? {
            productName,
            type: "PACKAGED",
            packageWeight: Number(unitForm.packageWeight),
            packageQuantity: Number(unitForm.packageQuantity),
            pricePerPackage: Number(unitForm.pricePerPackage),
            allocatedWeight:
              Number(unitForm.packageWeight) *
              Number(unitForm.packageQuantity),
          }
        : {
            productName,
            type: "BULK",
            allocatedWeight: Number(unitForm.allocatedWeight),
            pricePerKg: Number(unitForm.pricePerKg),
          };

    if (
      !productName ||
      !Number.isFinite(payload.allocatedWeight) ||
      payload.allocatedWeight <= 0 ||
      payload.allocatedWeight > Number(batch?.remainingWeight)
    ) {
      throw new Error("Tên sản phẩm hoặc khối lượng phân bổ không hợp lệ.");
    }

    if (withInitialStorage) {
      const temperature = Number(storageForm.temperature);
      const humidity = Number(storageForm.humidity);

      if (
        !storageForm.location.trim() ||
        storageForm.temperature === "" ||
        storageForm.humidity === "" ||
        !Number.isFinite(temperature) ||
        !Number.isFinite(humidity) ||
        humidity < 0 ||
        humidity > 100
      ) {
        throw new Error(
          "Nhập nơi bảo quản, nhiệt độ và độ ẩm ban đầu hợp lệ."
        );
      }

      payload.initialStorageReading = {
        location: storageForm.location.trim(),
        temperature,
        humidity,
        note: storageForm.note.trim(),
      };
    }

    await axiosClient.post(
      `/retailer/batches/${batchId}/retail-units`,
      payload
    );

    const [unitsResponse] = await Promise.all([
      axiosClient.get(`/retailer/batches/${batchId}/retail-units`),
      loadBatch(),
    ]);

    setRetailUnits(unitsResponse.data ?? []);

    setUnitForm({
      productName: "",
      type: "BULK",
      allocatedWeight: "",
      pricePerKg: "",
      packageWeight: "",
      packageQuantity: "",
      pricePerPackage: "",
    });

    setWithInitialStorage(false);
    setStorageForm({
      location: "",
      temperature: "",
      humidity: "",
      note: "",
    });

    setNotice("Đã tạo đơn vị bán lẻ.");
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



{batch?.status === "AT_RETAIL" &&
  Number(batch.remainingWeight) > 0 && (
    <>
      <form
        onSubmit={createRetailUnit}
        className="space-y-4 rounded-xl border border-slate-800 bg-[#111827] p-4"
      >
        <div>
          <h2 className="font-semibold">Tạo đơn vị bán lẻ</h2>
          <p className="mt-1 text-sm text-slate-400">
            Batch còn {batch.remainingWeight} kg chưa phân bổ.
            Mỗi đơn vị tạo ra có mã QR riêng.
          </p>
        </div>

        <input
          required
          placeholder="Tên sản phẩm"
          value={unitForm.productName}
          onChange={(e) =>
            setUnitForm((old) => ({
              ...old,
              productName: e.target.value,
            }))
          }
          className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
        />

        <select
          value={unitForm.type}
          onChange={(e) =>
            setUnitForm((old) => ({
              ...old,
              type: e.target.value,
            }))
          }
          className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
        >
          <option value="BULK">Bán theo kg</option>
          <option value="PACKAGED">Đóng gói</option>
        </select>

        {unitForm.type === "BULK" ? (
          <>
            <input
              required
              type="number"
              min="0.001"
              step="any"
              placeholder="Khối lượng phân bổ (kg)"
              value={unitForm.allocatedWeight}
              onChange={(e) =>
                setUnitForm((old) => ({
                  ...old,
                  allocatedWeight: e.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
            <input
              required
              type="number"
              min="0.01"
              step="any"
              placeholder="Giá mỗi kg"
              value={unitForm.pricePerKg}
              onChange={(e) =>
                setUnitForm((old) => ({
                  ...old,
                  pricePerKg: e.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
          </>
        ) : (
          <>
            <input
              required
              type="number"
              min="0.001"
              step="any"
              placeholder="Khối lượng mỗi gói (kg)"
              value={unitForm.packageWeight}
              onChange={(e) =>
                setUnitForm((old) => ({
                  ...old,
                  packageWeight: e.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
            <input
              required
              type="number"
              min="1"
              step="1"
              placeholder="Số gói"
              value={unitForm.packageQuantity}
              onChange={(e) =>
                setUnitForm((old) => ({
                  ...old,
                  packageQuantity: e.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
            <input
              required
              type="number"
              min="0.01"
              step="any"
              placeholder="Giá mỗi gói"
              value={unitForm.pricePerPackage}
              onChange={(e) =>
                setUnitForm((old) => ({
                  ...old,
                  pricePerPackage: e.target.value,
                }))
              }
              className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
            <p className="text-sm text-slate-400">
              Sẽ phân bổ:{" "}
              {Number(unitForm.packageWeight || 0) *
                Number(unitForm.packageQuantity || 0)}{" "}
              kg
            </p>
          </>
        )}

        <div className="rounded-xl border border-slate-700 bg-[#0b1020] p-4">
          <label className="flex items-center gap-2 font-medium">
            <input
              type="checkbox"
              checked={withInitialStorage}
              onChange={(e) => setWithInitialStorage(e.target.checked)}
            />
            Ghi số đo bảo quản ban đầu cho đơn vị này
          </label>
          <p className="mt-1 text-xs text-slate-400">
            Tùy chọn. Chỉ nhập số đo thực tế tại nơi giữ đơn vị bán lẻ
            này. Bạn có thể ghi thêm các lần đo sau ở trang QR của nó.
          </p>

          {withInitialStorage && (
            <div className="mt-4 space-y-3">
              <div className="grid gap-3 md:grid-cols-3">
                <label className="text-sm">
                  Nơi bảo quản
                  <input
                    required
                    value={storageForm.location}
                    onChange={(e) =>
                      setStorageForm((old) => ({
                        ...old,
                        location: e.target.value,
                      }))
                    }
                    placeholder="Ví dụ: Tủ mát khu A"
                    className="mt-1 w-full rounded-lg border border-slate-600 bg-[#111827] p-2"
                  />
                </label>

                <label className="text-sm">
                  Nhiệt độ (°C)
                  <input
                    required
                    type="number"
                    step="any"
                    value={storageForm.temperature}
                    onChange={(e) =>
                      setStorageForm((old) => ({
                        ...old,
                        temperature: e.target.value,
                      }))
                    }
                    className="mt-1 w-full rounded-lg border border-slate-600 bg-[#111827] p-2"
                  />
                </label>

                <label className="text-sm">
                  Độ ẩm (%)
                  <input
                    required
                    type="number"
                    min="0"
                    max="100"
                    step="any"
                    value={storageForm.humidity}
                    onChange={(e) =>
                      setStorageForm((old) => ({
                        ...old,
                        humidity: e.target.value,
                      }))
                    }
                    className="mt-1 w-full rounded-lg border border-slate-600 bg-[#111827] p-2"
                  />
                </label>
              </div>

              <label className="block text-sm">
                Ghi chú
                <textarea
                  rows={2}
                  value={storageForm.note}
                  onChange={(e) =>
                    setStorageForm((old) => ({
                      ...old,
                      note: e.target.value,
                    }))
                  }
                  className="mt-1 w-full rounded-lg border border-slate-600 bg-[#111827] p-2"
                />
              </label>
            </div>
          )}
        </div>

        <button
          type="submit"
          disabled={busy}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40"
        >
          {busy ? "Đang tạo..." : "Tạo đơn vị bán lẻ"}
        </button>
      </form>

      <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
        <h2 className="font-semibold">Đơn vị bán lẻ đã tạo</h2>
        {retailUnits.length === 0 ? (
          <p className="mt-3 text-sm text-slate-400">Chưa có đơn vị nào.</p>
        ) : (
          <ul className="mt-3 space-y-2 text-sm">
            {retailUnits.map((unit) => (
              <li
                key={unit.id}
                className="rounded-lg border border-slate-700 p-3"
              >
                {unit.productName} · {unit.retailCode} ·{" "}
                {unit.type === "PACKAGED"
                  ? `${unit.packageQuantity} gói × ${unit.packageWeight} kg`
                  : `${unit.allocatedWeight} kg`}{" "}
                · {unit.status}
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
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
