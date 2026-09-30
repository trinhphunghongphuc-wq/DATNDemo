import { useEffect, useState } from "react";
import { createVehicle, getVehicles } from "../../api/distributorApi";

export default function DistributorVehiclesPage() {
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState({
    vehiclePlate: "",
    sensorFirmware: "v1.0.0",
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadVehicles() {
    const response = await getVehicles();
    setVehicles(response.data ?? []);
  }

  useEffect(() => {
    loadVehicles()
      .catch((err) =>
        setError(err.response?.data?.message ?? "Không tải được danh sách xe.")
      )
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    try {
      setSaving(true);
      await createVehicle({
        vehiclePlate: form.vehiclePlate.trim(),
        sensorFirmware: form.sensorFirmware.trim(),
      });

      await loadVehicles();
      setForm({ vehiclePlate: "", sensorFirmware: "v1.0.0" });
      setNotice("Đã thêm xe lạnh.");
    } catch (err) {
      setError(err.response?.data?.message ?? "Không thêm được xe.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6 p-6 text-white">
      <div>
        <h1 className="text-2xl font-bold">Danh sách xe lạnh</h1>
        <p className="mt-1 text-sm text-slate-400">
          Mã thiết bị demo được tạo tự động khi thêm xe.
        </p>
      </div>

      {error && <p className="rounded-lg bg-red-950 p-3 text-red-300">{error}</p>}
      {notice && <p className="rounded-lg bg-green-950 p-3 text-green-300">{notice}</p>}

      <form
        onSubmit={handleSubmit}
        className="space-y-4 rounded-xl border border-slate-700 bg-[#111827] p-5"
      >
        <h2 className="font-semibold">Thêm xe mới</h2>

        <div className="grid gap-4 md:grid-cols-2">
          <label className="text-sm">
            Biển số xe
            <input
              required
              maxLength={50}
              value={form.vehiclePlate}
              onChange={(e) =>
                setForm((current) => ({
                  ...current,
                  vehiclePlate: e.target.value,
                }))
              }
              placeholder="Ví dụ: 51C-678.90"
              className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
          </label>

          <label className="text-sm">
            Phiên bản firmware cảm biến
            <input
              required
              maxLength={100}
              value={form.sensorFirmware}
              onChange={(e) =>
                setForm((current) => ({
                  ...current,
                  sensorFirmware: e.target.value,
                }))
              }
              className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
            />
          </label>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-blue-600 px-4 py-2 font-semibold disabled:opacity-50"
        >
          {saving ? "Đang thêm..." : "Thêm xe"}
        </button>
      </form>

      <div className="overflow-x-auto rounded-xl border border-slate-700 bg-[#111827]">
        {loading ? (
          <p className="p-5 text-slate-400">Đang tải danh sách xe...</p>
        ) : vehicles.length === 0 ? (
          <p className="p-5 text-slate-400">Chưa có xe nào đang hoạt động.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-700 text-slate-400">
              <tr>
                <th className="p-4">Biển số</th>
                <th className="p-4">Mã thiết bị</th>
                <th className="p-4">Firmware</th>
                <th className="p-4">Sequence cuối</th>
              </tr>
            </thead>
            <tbody>
              {vehicles.map((vehicle) => (
                <tr key={vehicle.id} className="border-b border-slate-800">
                  <td className="p-4 font-medium">{vehicle.vehiclePlate}</td>
                  <td className="p-4">{vehicle.deviceId}</td>
                  <td className="p-4">{vehicle.sensorFirmware}</td>
                  <td className="p-4">{vehicle.lastSequence ?? 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}