import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  addProducerRecords,
  createProducerBatch,
  getBatchRecords,
  getProducerBatch,
  getProducerBatches,
  getProductCategories,
  updateProducerExpiry,
  verifyProducerBatch,
} from "../../api/producerApi";
import { getMyPartners } from "../../api/partnerApi";




const INPUT_CLASS =
  "mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] p-2 text-slate-100";

const RECORD_FIELDS = {
  PRODUCTION: {
    label: "Sản xuất",
    fields: [
      {
        key: "farmName",
        label: "Tên trang trại / cơ sở sản xuất",
        type: "text",
        required: true,
      },
      {
        key: "farmLocation",
        label: "Địa điểm sản xuất",
        type: "text",
      },
      {
        key: "productionDate",
        label: "Ngày sản xuất",
        type: "date",
        required: true,
      },
      {
        key: "farmingMethod",
        label: "Phương pháp sản xuất",
        type: "text",
      },
    ],
  },
  HARVEST: {
    label: "Thu hoạch",
    fields: [
      {
        key: "harvestDate",
        label: "Ngày thu hoạch",
        type: "date",
        required: true,
      },
      {
        key: "fieldLocation",
        label: "Vị trí thu hoạch",
        type: "text",
      },
      {
        key: "harvestedWeightKg",
        label: "Khối lượng thu hoạch (kg)",
        type: "number",
      },
      {
        key: "harvestNote",
        label: "Ghi chú thu hoạch",
        type: "text",
      },
    ],
  },
  PACKAGING: {
    label: "Đóng gói",
    fields: [
      {
        key: "packagingDate",
        label: "Ngày đóng gói",
        type: "date",
        required: true,
      },
      {
        key: "packagingType",
        label: "Hình thức đóng gói",
        type: "text",
      },
      {
        key: "packageCount",
        label: "Số kiện / hộp",
        type: "number",
      },
      {
        key: "batchLabel",
        label: "Nhãn trên bao bì",
        type: "text",
      },
    ],
  },
};

const RECORD_TYPES = Object.keys(RECORD_FIELDS);

function emptyFields(recordType) {
  return Object.fromEntries(
    RECORD_FIELDS[recordType].fields.map(({ key }) => [key, ""])
  );
}



function emptyCreateForm() {
  return {
    name: "",
    expiryDate: "",
    productCategoryId: "",
    totalWeight: "",
    distributorId: "",
    retailerId: "",
    recordType: "HARVEST",
    fields: emptyFields("HARVEST"),
    privateData: false,
  };
}

function emptyNewRecord() {
  return {
    recordType: "PACKAGING",
    fields: emptyFields("PACKAGING"),
    privateData: false,
  };
}

function buildRawJson(recordType, fields) {
  const data = {};

  for (const field of RECORD_FIELDS[recordType].fields) {
    const value = fields[field.key];

    if (value === "" || value === undefined || value === null) {
      continue;
    }

    data[field.key] =
      field.type === "number" ? Number(value) : String(value).trim();
  }

  return JSON.stringify(data);
}

function buildRecordRequest(record) {
  return {
    recordType: record.recordType,
    rawJson: buildRawJson(record.recordType, record.fields),
    privateData: record.privateData,
  };
}

function messageOf(error) {
  return error?.response?.data?.message || "Thao tác không thành công.";
}

function RecordFieldInputs({ recordType, values, onChange, idPrefix }) {
  const definition = RECORD_FIELDS[recordType];

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {definition.fields.map((field) => {
        const id = `${idPrefix}-${field.key}`;

        return (
          <div
            key={field.key}
            className={field.key.toLowerCase().includes("note") ? "md:col-span-2" : ""}
          >
            <label htmlFor={id} className="text-sm text-slate-200">
              {field.label}
              {field.required && <span className="text-red-400"> *</span>}
            </label>

            <input
              id={id}
              name={field.key}
              type={field.type}
              required={Boolean(field.required)}
              min={field.type === "number" ? "0" : undefined}
              step={field.type === "number" ? "any" : undefined}
              value={values[field.key] ?? ""}
              onChange={(event) =>
                onChange(field.key, event.target.value)
              }
              className={INPUT_CLASS}
            />
          </div>
        );
      })}
    </div>
  );
}

export default function ProducerBatchesPage() {

  const [distributors, setDistributors] = useState([]);
  const [retailers, setRetailers] = useState([]);
  const [partnersLoading, setPartnersLoading] = useState(true);

  const [batches, setBatches] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyCreateForm);
  const [showCreate, setShowCreate] = useState(false);

  const [selected, setSelected] = useState(null);
  const [records, setRecords] = useState([]);
  const [verification, setVerification] = useState(null);
  const [newRecord, setNewRecord] = useState(emptyNewRecord);
  const [expiryDraft, setExpiryDraft] = useState("");

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadList() {
    const response = await getProducerBatches();
    setBatches(response.data ?? []);
  }

  useEffect(() => {
    async function load() {
      try {
        const [batchResponse, categoryResponse] = await Promise.all([
          getProducerBatches(),
          getProductCategories(),
        ]);

        setBatches(batchResponse.data ?? []);
        setCategories(categoryResponse.data ?? []);
      } catch (err) {
        setError(messageOf(err));
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadPartners() {
      try {
        const [distributorResponse, retailerResponse] =
          await Promise.all([
            getMyPartners("DISTRIBUTOR"),
            getMyPartners("RETAILER"),
          ]);

        if (!cancelled) {
          setDistributors(distributorResponse.data ?? []);
          setRetailers(retailerResponse.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err?.response?.data?.message ||
            "Không tải được danh sách đối tác."
          );
        }
      } finally {
        if (!cancelled) {
          setPartnersLoading(false);
        }
      }
    }

    loadPartners();

    return () => {
      cancelled = true;
    };
  }, []);

  function changeCreateRecordType(recordType) {
    setForm((current) => ({
      ...current,
      recordType,
      fields: emptyFields(recordType),
    }));
  }

  function changeNewRecordType(recordType) {
    setNewRecord((current) => ({
      ...current,
      recordType,
      fields: emptyFields(recordType),
    }));
  }

  function changeCreateField(key, value) {
    setForm((current) => ({
      ...current,
      fields: {
        ...current.fields,
        [key]: value,
      },
    }));
  }

  function changeNewRecordField(key, value) {
    setNewRecord((current) => ({
      ...current,
      fields: {
        ...current.fields,
        [key]: value,
      },
    }));
  }

  async function openBatch(id) {
    setError("");
    setNotice("");
    setVerification(null);
    setSelected(null);
    setRecords([]);
    setBusy(true);

    try {
      // Xác nhận đây là batch của Producer trước khi gọi API batch chung.
      const ownResponse = await getProducerBatch(id);
      const detailResponse = await getBatchRecords(id);

      setSelected(ownResponse.data);
      setExpiryDraft(ownResponse.data.expiryDate ?? "");
      setRecords(detailResponse.data?.records ?? []);
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  async function refreshSelected(id) {
    const ownResponse = await getProducerBatch(id);
    const detailResponse = await getBatchRecords(id);

    setSelected(ownResponse.data);
    setExpiryDraft(ownResponse.data.expiryDate ?? "");
    setRecords(detailResponse.data?.records ?? []);

    await loadList();
  }

async function handleCreate(event) {
  event.preventDefault();
  setError("");
  setNotice("");

  const hasDistributor = form.distributorId !== "";
  const hasRetailer = form.retailerId !== "";

  if (hasDistributor !== hasRetailer) {
    setError(
      "Nếu chỉ định nơi phân phối, hãy nhập cả Distributor ID và Retailer ID."
    );
    return;
  }

  const totalWeight =
    form.totalWeight !== ""
      ? Number(form.totalWeight)
      : form.recordType === "HARVEST"
        ? Number(form.fields.harvestedWeightKg)
        : NaN;

  if (!Number.isFinite(totalWeight) || totalWeight <= 0) {
    setError("Vui lòng nhập tổng khối lượng lô lớn hơn 0.");
    return;
  }

  const body = {
    name: form.name.trim(),
    expiryDate: form.expiryDate,
    records: [buildRecordRequest(form)],
    totalWeight,

    ...(form.productCategoryId !== "" && {
      productCategoryId: Number(form.productCategoryId),
    }),

    ...(hasDistributor && {
      distributorId: Number(form.distributorId),
      retailerId: Number(form.retailerId),
    }),
  };

  setBusy(true);

  try {
    const response = await createProducerBatch(body);

    setForm(emptyCreateForm());
    setShowCreate(false);

    await loadList();
    await openBatch(response.data.id);

    setNotice(`Đã tạo batch #${response.data.id}.`);
  } catch (err) {
    setError(messageOf(err));
  } finally {
    setBusy(false);
  }
}

  async function handleAddRecord(event) {
    event.preventDefault();

    if (!selected) return;

    setBusy(true);
    setError("");
    setNotice("");

    try {
      await addProducerRecords(selected.id, [
        buildRecordRequest(newRecord),
      ]);

      await refreshSelected(selected.id);

      setNewRecord(emptyNewRecord());
      setVerification(null);
      setNotice("Đã thêm record Producer.");
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleExpiry(event) {
    event.preventDefault();

    if (!selected || !expiryDraft) return;

    setBusy(true);
    setError("");
    setNotice("");

    try {
      await updateProducerExpiry(selected.id, expiryDraft);
      await refreshSelected(selected.id);
      setNotice("Đã cập nhật hạn sử dụng.");
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleVerify() {
    if (!selected) return;

    setBusy(true);
    setError("");
    setVerification(null);

    try {
      const response = await verifyProducerBatch(selected.id);
      setVerification(response.data);
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-blue-400">
            PRODUCER / LÔ HÀNG
          </p>
          <h1 className="mt-1 text-2xl font-bold">Lô hàng của tôi</h1>
          <p className="mt-1 text-sm text-slate-400">
            Tạo lô, ghi dữ liệu sản xuất và kiểm tra tính toàn vẹn.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowCreate((value) => !value)}
          className="rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold hover:bg-blue-500"
        >
          {showCreate ? "Đóng form" : "Tạo lô mới"}
        </button>
      </header>

      {error && (
        <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
          {error}
        </p>
      )}

      {notice && (
        <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-300">
          {notice}
        </p>
      )}

      {showCreate && (
        <form
          onSubmit={handleCreate}
          className="space-y-4 rounded-2xl border border-slate-800 bg-[#111827] p-5"
        >
          <h2 className="font-semibold">Thông tin lô mới</h2>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label htmlFor="batch-name" className="text-sm">
                Tên lô <span className="text-red-400">*</span>
              </label>
              <input
                id="batch-name"
                required
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              />
            </div>

            <div>
              <label htmlFor="batch-expiry" className="text-sm">
                Hạn sử dụng <span className="text-red-400">*</span>
              </label>
              <input
                id="batch-expiry"
                required
                type="date"
                value={form.expiryDate}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    expiryDate: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              />
            </div>

            <div>
              <label htmlFor="batch-category" className="text-sm">
                Danh mục sản phẩm
              </label>
              <select
                id="batch-category"
                value={form.productCategoryId}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    productCategoryId: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              >
                <option value="">Không chọn</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="batch-weight" className="text-sm">
                Tổng khối lượng (kg)
              </label>
              <input
                id="batch-weight"
                type="number"
                min="0"
                step="any"
                value={form.totalWeight}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    totalWeight: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              />
            </div>

            <div>
              <label htmlFor="batch-distributor" className="text-sm">
                Đơn vị vận chuyển
              </label>

              <select
                id="batch-distributor"
                value={form.distributorId}
                disabled={partnersLoading}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    distributorId: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              >
                <option value="">
                  {partnersLoading ? "Đang tải..." : "Chưa chỉ định"}
                </option>

                {distributors.map((partner) => (
                  <option key={partner.id} value={partner.id}>
                    {partner.companyName} — {partner.companyAddress}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="batch-retailer" className="text-sm">
                Đơn vị bán lẻ nhận hàng
              </label>

              <select
                id="batch-retailer"
                value={form.retailerId}
                disabled={partnersLoading}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    retailerId: event.target.value,
                  }))
                }
                className={INPUT_CLASS}
              >
                <option value="">
                  {partnersLoading ? "Đang tải..." : "Chưa chỉ định"}
                </option>

                {retailers.map((partner) => (
                  <option key={partner.id} value={partner.id}>
                    {partner.companyName} — {partner.companyAddress}
                  </option>
                ))}
              </select>
            </div>


          </div>

          <p className="text-xs text-slate-400">
            Nếu chỉ định phân phối ngay khi tạo lô, nhập cả hai ID.
            Nếu chưa chỉ định, để trống cả hai.
          </p>

          <div className="space-y-4 border-t border-slate-800 pt-4">
            <h3 className="font-semibold">Record ban đầu</h3>

            <div className="flex flex-wrap items-center gap-4">
              <div>
                <label htmlFor="create-record-type" className="text-sm">
                  Loại record
                </label>
                <select
                  id="create-record-type"
                  value={form.recordType}
                  onChange={(event) =>
                    changeCreateRecordType(event.target.value)
                  }
                  className={INPUT_CLASS}
                >
                  {RECORD_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {RECORD_FIELDS[type].label} ({type})
                    </option>
                  ))}
                </select>
              </div>

              <label className="flex items-center gap-2 self-end pb-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.privateData}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      privateData: event.target.checked,
                    }))
                  }
                />
                Dữ liệu riêng tư (mã hóa)
              </label>
            </div>

            <RecordFieldInputs
              recordType={form.recordType}
              values={form.fields}
              onChange={changeCreateField}
              idPrefix="create"
            />
          </div>

          <button
            type="submit"
            disabled={busy}
            className="rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold hover:bg-emerald-500 disabled:opacity-50"
          >
            {busy ? "Đang tạo..." : "Tạo batch"}
          </button>
        </form>
      )}

      <div className="grid gap-5 xl:grid-cols-[340px_minmax(0,1fr)]">
        <section className="rounded-2xl border border-slate-800 bg-[#111827] p-4">
          <h2 className="mb-3 font-semibold">Danh sách batch</h2>

          {loading ? (
            <p className="text-sm text-slate-400">Đang tải...</p>
          ) : batches.length === 0 ? (
            <p className="text-sm text-slate-400">
              Bạn chưa tạo batch nào.
            </p>
          ) : (
            <div className="max-h-[650px] space-y-2 overflow-y-auto">
              {batches.map((batch) => (
                <button
                  key={batch.id}
                  type="button"
                  onClick={() => openBatch(batch.id)}
                  className={`w-full rounded-xl border p-3 text-left ${selected?.id === batch.id
                    ? "border-blue-500 bg-blue-500/10"
                    : "border-slate-800 hover:bg-slate-800"
                    }`}
                >
                  <strong className="block">{batch.name}</strong>
                  <span className="mt-1 block text-xs text-slate-400">
                    #{batch.id} · {batch.status} ·{" "}
                    {batch.recordCount} records
                  </span>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
          {!selected ? (
            <p className="text-sm text-slate-400">
              Chọn một batch để xem chi tiết.
            </p>
          ) : (
            <div className="space-y-6">
              <div>
                <h2 className="text-xl font-bold">{selected.name}</h2>

                <p className="mt-1 text-sm text-slate-400">
                  {selected.batchCode} · {selected.status}
                </p>

                <p className="mt-2 break-all font-mono text-xs text-cyan-400">
                  Merkle root: {selected.merkleRoot ?? "—"}
                </p>

                <p className="mt-1 text-xs text-slate-400">
                  Anchor status tổng: {selected.anchorStatus ?? "—"}
                </p>

                <Link
                  to={`/producer/batches/${selected.id}/qr`}
                  className="mt-3 inline-block text-sm text-blue-400 hover:underline"
                >
                  Xem và tải QR truy xuất
                </Link>
              </div>

              <form
                onSubmit={handleExpiry}
                className="border-t border-slate-800 pt-4"
              >
                <label htmlFor="edit-expiry" className="text-sm">
                  Hạn sử dụng
                </label>

                <div className="mt-2 flex flex-wrap gap-3">
                  <input
                    id="edit-expiry"
                    type="date"
                    required
                    value={expiryDraft}
                    onChange={(event) =>
                      setExpiryDraft(event.target.value)
                    }
                    className="rounded-lg border border-slate-700 bg-[#0b1020] p-2"
                  />

                  <button
                    type="submit"
                    disabled={
                      busy || expiryDraft === selected.expiryDate
                    }
                    className="rounded-lg border border-slate-700 px-3 py-2 text-sm hover:bg-slate-800 disabled:opacity-50"
                  >
                    Lưu hạn dùng
                  </button>
                </div>
              </form>

              <div className="border-t border-slate-800 pt-4">
                <h3 className="font-semibold">
                  Records ({records.length})
                </h3>

                <div className="mt-3 max-h-48 space-y-2 overflow-y-auto">
                  {records.length === 0 ? (
                    <p className="text-sm text-slate-400">
                      Chưa có record.
                    </p>
                  ) : (
                    records.map((record) => (
                      <div
                        key={record.recordId}
                        className="rounded-lg border border-slate-800 p-3 text-xs"
                      >
                        <strong>{record.recordType}</strong>
                        <p className="mt-1 break-all text-slate-400">
                          {record.recordKey}
                        </p>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <form
                onSubmit={handleAddRecord}
                className="space-y-4 border-t border-slate-800 pt-4"
              >
                <h3 className="font-semibold">
                  Thêm record Producer
                </h3>

                <div className="flex flex-wrap items-center gap-4">
                  <div>
                    <label
                      htmlFor="new-record-type"
                      className="text-sm"
                    >
                      Loại record
                    </label>
                    <select
                      id="new-record-type"
                      value={newRecord.recordType}
                      onChange={(event) =>
                        changeNewRecordType(event.target.value)
                      }
                      className={INPUT_CLASS}
                    >
                      {RECORD_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {RECORD_FIELDS[type].label} ({type})
                        </option>
                      ))}
                    </select>
                  </div>

                  <label className="flex items-center gap-2 self-end pb-2 text-sm">
                    <input
                      type="checkbox"
                      checked={newRecord.privateData}
                      onChange={(event) =>
                        setNewRecord((current) => ({
                          ...current,
                          privateData: event.target.checked,
                        }))
                      }
                    />
                    Dữ liệu riêng tư (mã hóa)
                  </label>
                </div>

                <RecordFieldInputs
                  recordType={newRecord.recordType}
                  values={newRecord.fields}
                  onChange={changeNewRecordField}
                  idPrefix="new-record"
                />

                <button
                  type="submit"
                  disabled={busy}
                  className="rounded-lg bg-blue-600 px-4 py-2 text-sm hover:bg-blue-500 disabled:opacity-50"
                >
                  {busy ? "Đang thêm..." : "Thêm record"}
                </button>

                <p className="text-xs text-slate-500">
                  Sau khi Admin anchor stage PRODUCER, BE sẽ khóa
                  việc thêm record và sửa hạn dùng của Producer.
                </p>
              </form>

              <div className="border-t border-slate-800 pt-4">
                <button
                  type="button"
                  onClick={handleVerify}
                  disabled={busy}
                  className="rounded-lg border border-emerald-500 px-4 py-2 text-sm text-emerald-300 disabled:opacity-50"
                >
                  Xác minh tất cả records
                </button>

                {verification && (
                  <div className="mt-3 rounded-xl bg-[#0b1020] p-4 text-sm">
                    <p
                      className={
                        verification.valid
                          ? "text-emerald-400"
                          : "text-red-400"
                      }
                    >
                      {verification.valid
                        ? "Hợp lệ"
                        : "Có record không hợp lệ"}
                    </p>

                    <p className="mt-1 text-slate-400">
                      {verification.validRecords}/
                      {verification.totalRecords} records hợp lệ
                    </p>

                    {(verification.results ?? []).map((result) => (
                      <p
                        key={result.recordKey}
                        className="mt-2 break-all text-xs"
                      >
                        {result.valid ? "✓" : "✗"}{" "}
                        {result.recordKey}: {result.message}
                      </p>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}