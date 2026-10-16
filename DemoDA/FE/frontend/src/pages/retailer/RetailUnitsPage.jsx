import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { QRCodeCanvas } from "qrcode.react";
import axiosClient from "../../api/axiosClient";
import Pagination from "../../components/common/Pagination";

const publicBaseUrl = (
  import.meta.env.VITE_PUBLIC_APP_URL || window.location.origin
).replace(/\/$/, "");

const emptyReading = () => ({
  location: "",
  temperature: "",
  humidity: "",
  note: "",
});

function errorMessage(err) {
  return err.response?.data?.message ?? err.message ?? "Không lưu được số đo.";
}

export default function RetailUnitsPage() {
  const [units, setUnits] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [page, setPage] = useState(1);

  const [editingUnitId, setEditingUnitId] = useState(null);
  const [reading, setReading] = useState(emptyReading);
  const [saving, setSaving] = useState(false);

  const pageSize = 4;
  const totalPages = Math.ceil(units.length / pageSize);
  const currentPage = Math.min(page, Math.max(totalPages, 1));
  const visibleUnits = units.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );


  useEffect(() => {
    let active = true;

    axiosClient
      .get("/retailer/retail-units")
      .then((response) => {
        if (active) setUnits(response.data ?? []);
      })
      .catch((err) => {
        if (active) {
          setError(
            err.response?.data?.message ??
            "Không tải được đơn vị bán lẻ."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  function downloadQr(unit) {
    const canvas = document.getElementById(`retail-qr-${unit.id}`);
    if (!canvas) return;

    const link = document.createElement("a");
    link.href = canvas.toDataURL("image/png");
    link.download = `${unit.retailCode}-QR.png`;
    link.click();
  }

  function openReadingForm(unitId) {
    setError("");
    setNotice("");
    setReading(emptyReading());
    setEditingUnitId(unitId);
  }

  async function saveReading(event, unit) {
    event.preventDefault();
    if (unit.retailerRecordingClosed) {
      setError("Batch đã hoàn tất ghi nhận; không thể thêm số đo.");
      setEditingUnitId(null);
      return;
    }
    setError("");
    setNotice("");

    const temperature = Number(reading.temperature);
    const humidity = Number(reading.humidity);

    if (
      !reading.location.trim() ||
      reading.temperature === "" ||
      reading.humidity === "" ||
      !Number.isFinite(temperature) ||
      !Number.isFinite(humidity) ||
      humidity < 0 ||
      humidity > 100
    ) {
      setError("Nhập vị trí, nhiệt độ và độ ẩm hợp lệ (0–100%).");
      return;
    }

    try {
      setSaving(true);

      await axiosClient.post(
        `/retailer/retail-units/${unit.id}/storage-readings`,
        {
          location: reading.location.trim(),
          temperature,
          humidity,
          note: reading.note.trim(),
        }
      );

      setEditingUnitId(null);
      setReading(emptyReading());
      setNotice(
        `Đã ghi số đo bảo quản cho lô tách bán ${unit.retailCode}.`
      );
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Đơn vị bán lẻ</h1>
        <p className="text-sm text-slate-400">
          Mỗi lô tách bán có mã QR và lịch sử bảo quản riêng.
        </p>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}
      {notice && <p className="text-sm text-emerald-400">{notice}</p>}

      {loading ? (
        <p>Đang tải...</p>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            {visibleUnits.map((unit) => {
              const traceUrl =
                `${publicBaseUrl}/trace/retail/` +
                encodeURIComponent(unit.retailCode);

              return (
                <div
                  key={unit.id}
                  className="rounded-xl border border-slate-800 bg-[#111827] p-4"
                >
                  <h2 className="font-semibold">{unit.productName}</h2>

                  <p className="mt-2 text-sm text-slate-400">
                    Mã: {unit.retailCode} · Lô: {unit.batchName}
                  </p>

                  <p className="text-sm text-slate-400">
                    {unit.type === "PACKAGED"
                      ? `${unit.packageQuantity} gói × ${unit.packageWeight} kg`
                      : `Bán theo kg · phân bổ ${unit.allocatedWeight} kg`}
                  </p>

                  <p className="text-sm text-slate-400">
                    Trạng thái: {unit.status}
                  </p>

                  {unit.retailRecordKey && (
                    <p className="mt-1 break-all text-xs text-slate-500">
                      Record tách lô: {unit.retailRecordKey}
                    </p>
                  )}

                  <div className="mt-4 inline-block rounded-lg bg-white p-3">
                    <QRCodeCanvas
                      id={`retail-qr-${unit.id}`}
                      value={traceUrl}
                      size={150}
                      includeMargin
                    />
                  </div>

                  <p className="mt-2 break-all text-xs text-slate-400">
                    {traceUrl}
                  </p>

                  <div className="mt-3 flex flex-wrap gap-4 text-sm">
                    <Link
                      to={`/trace/retail/${encodeURIComponent(
                        unit.retailCode
                      )}`}
                      className="text-blue-400 hover:underline"
                    >
                      Mở trang truy xuất
                    </Link>

                    <Link
                      to={`/retailer/batches/${unit.batchId}/records`}
                      className="text-blue-400 hover:underline"
                    >
                      Xem lô gốc
                    </Link>

                    <button
                      type="button"
                      onClick={() => downloadQr(unit)}
                      className="text-blue-400 hover:underline"
                    >
                      Tải QR
                    </button>
                  </div>

                  <div className="mt-5 border-t border-slate-700 pt-4">
                    {unit.retailerRecordingClosed ? (
                      <p className="text-sm text-emerald-300">
                        Đã hoàn tất ghi nhận Retailer. Không thể thêm số đo cho
                        lô tách bán này.
                      </p>
                    ) : editingUnitId !== unit.id ? (
                      <button
                        type="button"
                        onClick={() => openReadingForm(unit.id)}
                        className="rounded-lg border border-emerald-600 px-3 py-2 text-sm text-emerald-300 hover:bg-emerald-600/10"
                      >
                        Ghi số đo bảo quản
                      </button>
                    ) : (
                      <form
                        onSubmit={(event) => saveReading(event, unit)}
                        className="space-y-3"
                      >
                        <h3 className="font-medium">
                          Số đo bảo quản · {unit.retailCode}
                        </h3>
                        <p className="text-xs text-slate-400">
                          Số đo nhập thủ công cho riêng lô tách bán này.
                        </p>

                        <label className="block text-sm">
                          Vị trí bảo quản
                          <input
                            required
                            value={reading.location}
                            onChange={(event) =>
                              setReading((prev) => ({
                                ...prev,
                                location: event.target.value,
                              }))
                            }
                            placeholder="Ví dụ: Tủ mát khu A"
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2"
                          />
                        </label>

                        <div className="grid grid-cols-2 gap-3">
                          <label className="block text-sm">
                            Nhiệt độ (°C)
                            <input
                              required
                              type="number"
                              step="any"
                              value={reading.temperature}
                              onChange={(event) =>
                                setReading((prev) => ({
                                  ...prev,
                                  temperature: event.target.value,
                                }))
                              }
                              className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2"
                            />
                          </label>

                          <label className="block text-sm">
                            Độ ẩm (%)
                            <input
                              required
                              type="number"
                              min="0"
                              max="100"
                              step="any"
                              value={reading.humidity}
                              onChange={(event) =>
                                setReading((prev) => ({
                                  ...prev,
                                  humidity: event.target.value,
                                }))
                              }
                              className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2"
                            />
                          </label>
                        </div>

                        <label className="block text-sm">
                          Ghi chú
                          <textarea
                            value={reading.note}
                            onChange={(event) =>
                              setReading((prev) => ({
                                ...prev,
                                note: event.target.value,
                              }))
                            }
                            rows={2}
                            className="mt-1 w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2"
                          />
                        </label>

                        <div className="flex gap-2">
                          <button
                            type="submit"
                            disabled={saving}
                            className="rounded-lg bg-emerald-600 px-3 py-2 text-sm font-medium hover:bg-emerald-500 disabled:opacity-50"
                          >
                            {saving ? "Đang lưu..." : "Lưu số đo"}
                          </button>
                          <button
                            type="button"
                            disabled={saving}
                            onClick={() => setEditingUnitId(null)}
                            className="rounded-lg border border-slate-600 px-3 py-2 text-sm"
                          >
                            Hủy
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                </div>
              );
            })}

            {units.length === 0 && (
              <div>
                <p>Chưa có đơn vị bán lẻ.</p>
                <Link
                  to="/retailer/batches"
                  className="mt-3 inline-block text-blue-400 hover:underline"
                >
                  Chọn lô hàng để tạo
                </Link>
              </div>
            )}
          </div>

          {units.length > 0 && (
            <Pagination
              page={currentPage}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          )}
        </>
      )}
    </div>
  );
}