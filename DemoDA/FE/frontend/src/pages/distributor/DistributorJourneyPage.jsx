import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  CircleMarker,
  MapContainer,
  Polyline,
  Popup,
  TileLayer,
  useMap,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";

import {
  getDistributorBatchDetail,
  getTransportJourney,
  planTransportRoute,
} from "../../api/distributorApi";
import axiosClient from "../../api/axiosClient";

const mapTilerKey = import.meta.env.VITE_MAPTILER_KEY;

function javaDouble(value) {
  return Number.isInteger(value) ? `${value}.0` : String(value);
}

function signaturePayload(batchId, vehicle, sequence, gps, temperature, humidity, timestamp) {
  // Must match DistributorServiceImpl.buildUnsignedTransportPayload field order and
  // Jackson's serialization of Java Double values (including the .0 for integers).
  return `{"batchId":${Number(batchId)},"vehicleId":${vehicle.id},"sequence":${sequence},` +
    `"vehiclePlate":${JSON.stringify(vehicle.vehiclePlate)},` +
    `"deviceId":${JSON.stringify(vehicle.deviceId)},` +
    `"sensorFirmware":${JSON.stringify(vehicle.sensorFirmware)},` +
    `"gps":${JSON.stringify(gps)},"temperature":${javaDouble(temperature)},` +
    `"humidity":${javaDouble(humidity)},"timestamp":${JSON.stringify(timestamp)}}`;
}

function toBase64(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)));
}

function PlaceInput({ label, placeholder, selected, onSelect }) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  useEffect(() => {
    if (selected || query.trim().length < 3 || !mapTilerKey) {
      return;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        setSearching(true);
        setSearchError("");
        const params = new URLSearchParams({
          key: mapTilerKey,
          language: "vi",
          country: "vn",
          limit: "5",
          autocomplete: "true",
        });
        const response = await fetch(
          `https://api.maptiler.com/geocoding/${encodeURIComponent(query.trim())}.json?${params}`,
          { signal: controller.signal }
        );
        if (!response.ok) throw new Error(`MapTiler trả về HTTP ${response.status}`);
        const data = await response.json();
        setSuggestions(
          (data.features ?? []).filter((item) =>
            Array.isArray(item.center) &&
            Number.isFinite(item.center[0]) &&
            Number.isFinite(item.center[1])
          )
        );
      } catch (err) {
        if (err.name !== "AbortError") {
          setSuggestions([]);
          setSearchError("Không tìm được địa điểm. Kiểm tra key hoặc thử lại.");
        }
      } finally {
        if (!controller.signal.aborted) setSearching(false);
      }
    }, 400);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query, selected]);

  function handleChange(event) {
    setQuery(event.target.value);
    setSuggestions([]);
    setSearchError("");
    onSelect(null);
  }

  function choose(item) {
    setQuery(item.place_name ?? item.text ?? "");
    setSuggestions([]);
    setSearchError("");
    onSelect({ lat: item.center[1], lon: item.center[0] });
  }

  return (
    <div className="relative min-w-0 flex-1">
      <label className="mb-1 block text-sm text-slate-300">{label}</label>
      <input
        type="text"
        value={query}
        onChange={handleChange}
        placeholder={placeholder}
        autoComplete="off"
        className="w-full rounded-lg border border-slate-600 bg-[#0b1020] px-3 py-2 text-slate-100 placeholder:text-slate-500"
      />
      {searching && <p className="mt-1 text-xs text-slate-400">Đang tìm...</p>}
      {searchError && <p className="mt-1 text-xs text-red-300">{searchError}</p>}
      {!selected && suggestions.length > 0 && (
        <ul className="absolute z-[1000] mt-1 max-h-60 w-full overflow-y-auto rounded-lg border border-slate-600 bg-[#111827] shadow-xl">
          {suggestions.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                onClick={() => choose(item)}
                className="w-full px-3 py-2 text-left text-sm text-slate-100 hover:bg-slate-700"
              >
                {item.place_name ?? item.text}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function FitJourney({ routePositions, points, origin, destination }) {
  const map = useMap();

  useEffect(() => {
    const positions = routePositions.length
      ? routePositions
      : [
        ...points.map((point) => [Number(point.latitude), Number(point.longitude)]),
        ...(origin ? [[origin.lat, origin.lon]] : []),
        ...(destination ? [[destination.lat, destination.lon]] : []),
      ];

    const valid = positions.filter(
      ([lat, lon]) => Number.isFinite(lat) && Number.isFinite(lon)
    );
    if (valid.length === 1) {
      map.setView(valid[0], 14);
    } else if (valid.length > 1) {
      map.fitBounds(valid, { padding: [40, 40], maxZoom: 15 });
    }
  }, [map, routePositions, points, origin, destination]);

  return null;
}

function JourneyMap({ routePositions, points, origin, destination, canSelect }) {
  if (!mapTilerKey) {
    return (
      <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-red-300">
        Thiếu VITE_MAPTILER_KEY trong .env.local. Hãy thêm key và khởi động lại Vite.
      </p>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-slate-800">
      <MapContainer
        center={[11.9404, 108.4583]}
        zoom={13}
        scrollWheelZoom
        style={{ height: 700, width: "100%" }}
      >
        <TileLayer
          url={`https://api.maptiler.com/maps/streets-v4/256/{z}/{x}/{y}.png?key=${mapTilerKey}`}
          attribution='&copy; <a href="https://www.maptiler.com/copyright/">MapTiler</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
        />
        <FitJourney
          routePositions={routePositions}
          points={points}
          origin={origin}
          destination={destination}
        />

        {routePositions.length > 1 && (
          <Polyline positions={routePositions} pathOptions={{ color: "#2563eb", weight: 5 }} />
        )}

        {canSelect && origin && (
          <CircleMarker center={[origin.lat, origin.lon]} radius={9} pathOptions={{ color: "#2563eb", fillColor: "#2563eb", fillOpacity: 0.9 }}>
            <Popup>Điểm bắt đầu</Popup>
          </CircleMarker>
        )}
        {canSelect && destination && (
          <CircleMarker center={[destination.lat, destination.lon]} radius={9} pathOptions={{ color: "#f97316", fillColor: "#f97316", fillOpacity: 0.9 }}>
            <Popup>Điểm kết thúc</Popup>
          </CircleMarker>
        )}

        {points.map((point, index) => {
          const lat = Number(point.latitude);
          const lon = Number(point.longitude);
          if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
          const latest = index === points.length - 1;
          const color = latest ? "#16a34a" : "#eab308";
          return (
            <CircleMarker
              key={`${point.sequence ?? index}-${index}`}
              center={[lat, lon]}
              radius={latest ? 9 : 6}
              pathOptions={{ color, fillColor: color, fillOpacity: 0.85 }}
            >
              <Popup>
                <strong>Điểm GPS #{index + 1}</strong><br />
                Xe: {point.vehicleId ?? "—"} · Sequence: {point.sequence ?? "—"}<br />
                Nhiệt độ: {point.temperature ?? "—"}°C<br />
                Độ ẩm: {point.humidity ?? "—"}%<br />
                {point.timestamp ?? ""}
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>
    </div>
  );
}

export default function DistributorJourneyPage() {
  const { batchId } = useParams();
  const [batch, setBatch] = useState(null);
  const [journey, setJourney] = useState(null);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingWarehouse, setSavingWarehouse] = useState(false);
  const [savingDelivery, setSavingDelivery] = useState(false);
  const [vehicles, setVehicles] = useState([]);
  const [vehicleId, setVehicleId] = useState("");
  const [demoCount, setDemoCount] = useState(8);

  const [demoTemperature, setDemoTemperature] = useState("");
  const [demoHumidity, setDemoHumidity] = useState("");

  const [demoRunning, setDemoRunning] = useState(false);
  const [demoProgress, setDemoProgress] = useState(0);
  const demoActive = useRef(false);
  const demoStarting = useRef(false);
  const demoTimer = useRef(null);
  const [warehouse, setWarehouse] = useState({
    location: "",
    temperature: "",
    humidity: "",
    note: "",
  });
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const loadJourney = useCallback(async () => {
    const response = await getTransportJourney(batchId);
    setJourney(response.data);
  }, [batchId]);

  useEffect(() => {
    let active = true;
    async function loadInitial() {
      try {
        setLoading(true);
        setError("");
        const [batchResponse, journeyResponse, vehiclesResponse] = await Promise.all([
          getDistributorBatchDetail(batchId),
          getTransportJourney(batchId),
          axiosClient.get("/distributor/vehicles"),
        ]);
        if (active) {
          setBatch(batchResponse.data);
          setJourney(journeyResponse.data);
          setVehicles(vehiclesResponse.data ?? []);
          setVehicleId((current) => current || String(vehiclesResponse.data?.[0]?.id ?? ""));
        }
      } catch (err) {
        if (active) setError(err.response?.data?.message ?? "Không tải được hành trình.");
      } finally {
        if (active) setLoading(false);
      }
    }
    loadInitial();
    return () => {
      active = false;
      demoActive.current = false;
      clearTimeout(demoTimer.current);
    };
  }, [batchId]);


  useEffect(() => {
    const category = batch?.productCategory;
    if (
      category?.temperatureMin == null ||
      category?.temperatureMax == null ||
      category?.humidityMin == null ||
      category?.humidityMax == null
    ) return;

    setDemoTemperature(String(
      (category.temperatureMin + category.temperatureMax) / 2
    ));
    setDemoHumidity(String(
      (category.humidityMin + category.humidityMax) / 2
    ));
  }, [batch?.productCategory?.id]);

  const routePositions = useMemo(() => {
    const coordinates = journey?.plannedRoute?.coordinates;
    if (!Array.isArray(coordinates)) return [];
    return coordinates
      .filter((coordinate) =>
        Array.isArray(coordinate) &&
        Number.isFinite(coordinate[0]) &&
        Number.isFinite(coordinate[1])
      )
      .map(([longitude, latitude]) => [latitude, longitude]);
  }, [journey?.plannedRoute]);

  const points = journey?.points ?? [];
  const latestPoint = points.at(-1);
  const canPlanRoute =
    batch?.status === "RECEIVED_BY_DISTRIBUTOR" &&
    !journey?.plannedRoute &&
    points.length === 0;
  const canDemo = routePositions.length > 1 &&
    (batch?.status === "RECEIVED_BY_DISTRIBUTOR" || batch?.status === "IN_DISTRIBUTION");
  const destinationPosition = routePositions.at(-1);
  const arrived = Boolean(latestPoint && destinationPosition &&
    Math.abs(Number(latestPoint.latitude) - destinationPosition[0]) < 0.003 &&
    Math.abs(Number(latestPoint.longitude) - destinationPosition[1]) < 0.003);

  async function handleStartDemo() {
    if (demoActive.current || demoStarting.current) return;
    const count = Number(demoCount);
    if (demoTemperature.trim() === "" || demoHumidity.trim() === "") {
      setError("Batch chưa có ngưỡng khuyến nghị; hãy nhập nhiệt độ và độ ẩm demo.");
      return;
    }
    const temperature = Number(demoTemperature);
    const humidity = Number(demoHumidity);
    if (!vehicleId) {
      setError("Chọn xe trước khi chạy demo.");
      return;
    }
    if (!Number.isInteger(count) || count < 2 || count > 100 || count > routePositions.length ||
      !Number.isFinite(temperature) || !Number.isFinite(humidity) || humidity < 0 || humidity > 100) {
      setError(`Chọn 2–${Math.min(100, routePositions.length)} điểm; kiểm tra nhiệt độ và độ ẩm (0–100%).`);
      return;
    }

    setError("");
    setNotice("");
    demoStarting.current = true;
    try {
      const { data: currentVehicles } = await axiosClient.get("/distributor/vehicles");
      const vehicle = currentVehicles.find((item) => String(item.id) === vehicleId);
      if (!vehicle) {
        throw new Error("Không tìm được xe đang hoạt động của Distributor.");
      }
      // Private key is non-extractable and remains in this browser session.
      const keyPair = await crypto.subtle.generateKey("Ed25519", false, ["sign", "verify"]);
      const publicKey = toBase64(await crypto.subtle.exportKey("spki", keyPair.publicKey));
      const { data: registration } = await axiosClient.post(
        `/distributor/vehicles/${vehicle.id}/demo-public-key`,
        { publicKey }
      );
      const firstSequence = Number(registration.lastSequence) + 1;
      if (!Number.isSafeInteger(firstSequence) || firstSequence < 1) {
        throw new Error("BE không trả về lastSequence hợp lệ.");
      }
      demoActive.current = true;
      setDemoRunning(true);
      setDemoProgress(0);

      async function sendPoint(index) {
        if (!demoActive.current) return;
        try {
          const position = routePositions[Math.round(index * (routePositions.length - 1) / (count - 1))];
          const gps = `${position[0].toFixed(6)},${position[1].toFixed(6)}`;
          const timestamp = new Date().toISOString();
          const sequence = firstSequence + index;
          const payload = signaturePayload(batchId, vehicle, sequence, gps, temperature, humidity, timestamp);
          const signature = toBase64(await crypto.subtle.sign("Ed25519", keyPair.privateKey, new TextEncoder().encode(payload)));
          await axiosClient.post(`/distributor/batches/${batchId}/transport-record`, {
            vehicleId: vehicle.id,
            sequence,
            gps,
            temperature,
            humidity,
            timestamp,
            deviceSignature: signature,
          });
          const [journeyResponse, batchResponse] = await Promise.all([
            getTransportJourney(batchId),
            getDistributorBatchDetail(batchId),
          ]);
          setJourney(journeyResponse.data);
          setBatch(batchResponse.data);
          setDemoProgress(index + 1);
          if (!demoActive.current) return;
          if (index + 1 < count) {
            demoTimer.current = setTimeout(() => sendPoint(index + 1), 15000);
          } else {
            demoActive.current = false;
            setDemoRunning(false);
            setNotice("Demo hoàn tất. Kiểm tra các điểm GPS trước khi xác nhận giao hàng.");
          }
        } catch (err) {
          demoActive.current = false;
          setDemoRunning(false);
          setError(err.response?.data?.message ?? err.message ?? "Không gửi được điểm GPS demo.");
        }
      }

      await sendPoint(0);
    } catch (err) {
      demoActive.current = false;
      setDemoRunning(false);
      setError(err.response?.status === 404
        ? "API demo chưa bật. Đặt APP_DEMO_DEVICE_ENABLED=true và chạy lại BE."
        : err.response?.data?.message ?? err.message ?? "Không bắt đầu được demo.");
    } finally {
      demoStarting.current = false;
    }
  }

  function handleStopDemo() {
    demoActive.current = false;
    clearTimeout(demoTimer.current);
    setDemoRunning(false);
    setNotice("Đã dừng demo. Lần chạy mới bắt đầu lại từ đầu tuyến với sequence tiếp theo.");
  }

  async function handleConfirmDelivery() {
    try {
      setSavingDelivery(true);
      setError("");
      await axiosClient.post(`/distributor/batches/${batchId}/delivery-to-retailer`);
      const response = await getDistributorBatchDetail(batchId);
      setBatch(response.data);
      setNotice("Đã giao lô hàng. Retailer có thể xác nhận nhận hàng.");
    } catch (err) {
      setError(err.response?.data?.message ?? "Không xác nhận được giao hàng.");
    } finally {
      setSavingDelivery(false);
    }
  }

  async function handlePlanRoute() {
    if (!origin || !destination) return;
    try {
      setSaving(true);
      setError("");
      const response = await planTransportRoute(batchId, {
        originLat: origin.lat,
        originLon: origin.lon,
        destinationLat: destination.lat,
        destinationLon: destination.lon,
      });
      setJourney(response.data);
      setOrigin(null);
      setDestination(null);
    } catch (err) {
      setError(err.response?.data?.message ?? "Không lập được tuyến đường.");
    } finally {
      setSaving(false);
    }
  }

  async function handleRefresh() {
    try {
      setError("");
      await loadJourney();
    } catch (err) {
      setError(err.response?.data?.message ?? "Không cập nhật được GPS.");
    }
  }

  async function handleWarehouse(event) {
    event.preventDefault();
    try {
      setSavingWarehouse(true);
      setError("");
      setNotice("");
      await axiosClient.post(`/distributor/batches/${batchId}/warehouse-record`, {
        recordType: "WAREHOUSE",
        privateData: false,
        rawJson: JSON.stringify({
          location: warehouse.location.trim(),
          temperature: Number(warehouse.temperature),
          humidity: Number(warehouse.humidity),
          note: warehouse.note.trim(),
          timestamp: new Date().toISOString(),
        }),
      });
      const [batchResponse, journeyResponse] = await Promise.all([
        getDistributorBatchDetail(batchId),
        getTransportJourney(batchId),
      ]);
      setBatch(batchResponse.data);
      setJourney(journeyResponse.data);
      setWarehouse({ location: "", temperature: "", humidity: "", note: "" });
      setNotice("Đã lưu WAREHOUSE record cho lô hàng.");
    } catch (err) {
      setError(err.response?.data?.message ?? "Không lưu được WAREHOUSE record.");
    } finally {
      setSavingWarehouse(false);
    }
  }

  if (loading) return <p className="text-slate-400">Đang tải hành trình...</p>;

  return (
    <div className="space-y-5">
      <Link to="/distributor/batches" className="text-sm text-blue-400 hover:underline">
        ← Danh sách lô hàng
      </Link>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{batch?.name ?? `Batch #${batchId}`}</h1>
          <p className="mt-1 text-sm text-slate-400">
            Batch #{batchId} · {batch?.status ?? "Không rõ trạng thái"}
          </p>
        </div>
        <button type="button" onClick={handleRefresh} className="rounded-lg border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800">
          Cập nhật GPS
        </button>
      </div>
      {error && (
        <p className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-red-300">{error}</p>
      )}
      {notice && <p className="rounded-lg border border-green-500/30 bg-green-500/10 p-3 text-green-300">{notice}</p>}
      {canPlanRoute && (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <div className="flex flex-col gap-3 md:flex-row">
            <PlaceInput
              label="Điểm bắt đầu"
              placeholder="Nhập địa chỉ hoặc tên địa điểm..."
              selected={origin}
              onSelect={setOrigin}
            />
            <PlaceInput
              label="Điểm đến"
              placeholder="Nhập địa chỉ hoặc tên địa điểm..."
              selected={destination}
              onSelect={setDestination}
            />
          </div>
          <p className="mt-2 text-xs text-slate-400">
            Chọn một địa điểm trong danh sách gợi ý cho mỗi ô trước khi xác nhận.
          </p>
          <button type="button" disabled={!origin || !destination || saving} onClick={handlePlanRoute} className="mt-3 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {saving ? "Đang lập tuyến..." : "Xác nhận tuyến đường"}
          </button>
        </div>
      )}
      <JourneyMap
        routePositions={routePositions}
        points={points}
        origin={origin}
        destination={destination}
        canSelect={canPlanRoute}
      />
      {canDemo && (
        <div className="space-y-3 rounded-xl border border-slate-800 bg-[#111827] p-4">
          <h2 className="font-semibold">Demo GPS có chữ ký · 15 giây/điểm</h2>
          <p className="text-sm text-slate-400">
            Chế độ demo: trình duyệt tự tạo khóa mô phỏng cho xe đã chọn. Private key không gửi lên BE; BE chỉ lưu public key và nhận các điểm GPS đã ký.
          </p>
          <div className="grid gap-3 md:grid-cols-2">
            <label className="text-sm">Xe dùng cho demo
              <select value={vehicleId} disabled={demoRunning} onChange={(e) => setVehicleId(e.target.value)} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2">
                {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.vehiclePlate} · {vehicle.deviceId}</option>)}
              </select>
            </label>
            <label className="text-sm">Số điểm (2–{Math.min(100, routePositions.length)})
              <input type="number" min="2" max={Math.min(100, routePositions.length)} value={demoCount} disabled={demoRunning} onChange={(e) => setDemoCount(e.target.value)} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
            <div className="grid grid-cols-2 gap-3">
              <label className="text-sm">Nhiệt độ (°C)
                <input type="number" step="any" value={demoTemperature} disabled={demoRunning} onChange={(e) => setDemoTemperature(e.target.value)} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
              </label>
              <label className="text-sm">Độ ẩm (%)
                <input type="number" min="0" max="100" step="any" value={demoHumidity} disabled={demoRunning} onChange={(e) => setDemoHumidity(e.target.value)} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
              </label>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {demoRunning ? (
              <button type="button" onClick={handleStopDemo} className="rounded-lg border border-red-500 px-4 py-2 text-sm">Dừng demo</button>
            ) : (
              <button type="button" onClick={handleStartDemo} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold">Bắt đầu demo</button>
            )}
            <span className="text-sm text-slate-400">Đã gửi {demoProgress}/{demoCount} điểm</span>
          </div>
        </div>
      )}
      {batch?.status === "IN_DISTRIBUTION" && (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <button type="button" disabled={demoRunning || savingDelivery || !arrived} onClick={handleConfirmDelivery} className="rounded-lg bg-green-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {savingDelivery ? "Đang xác nhận..." : "Xác nhận đã giao cho Retailer"}
          </button>
          {!arrived && <p className="mt-2 text-xs text-slate-400">Cần có điểm GPS cuối gần điểm đến trước khi xác nhận giao.</p>}
        </div>
      )}
      {(batch?.status === "RECEIVED_BY_DISTRIBUTOR" || batch?.status === "IN_DISTRIBUTION") && (
        <form onSubmit={handleWarehouse} className="space-y-3 rounded-xl border border-slate-800 bg-[#111827] p-4">
          <h2 className="font-semibold">Ghi nhận lưu kho (WAREHOUSE)</h2>
          <p className="text-sm text-slate-400">
            Chỉ ghi khi lô thực sự vào kho. Dữ liệu sẽ thành record của stage Distributor.
          </p>
          <div className="grid gap-3 md:grid-cols-3">
            <label className="text-sm">Địa điểm kho
              <input required value={warehouse.location} onChange={(e) => setWarehouse((w) => ({ ...w, location: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
            <label className="text-sm">Nhiệt độ (°C)
              <input required type="number" step="any" value={warehouse.temperature} onChange={(e) => setWarehouse((w) => ({ ...w, temperature: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
            <label className="text-sm">Độ ẩm (%)
              <input required type="number" min="0" max="100" step="any" value={warehouse.humidity} onChange={(e) => setWarehouse((w) => ({ ...w, humidity: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
          </div>
          <label className="block text-sm">Ghi chú
            <input value={warehouse.note} onChange={(e) => setWarehouse((w) => ({ ...w, note: e.target.value }))} className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
          </label>
          <button type="submit" disabled={savingWarehouse} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {savingWarehouse ? "Đang lưu..." : "Lưu WAREHOUSE record"}
          </button>
        </form>
      )}
      <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
        <h2 className="font-semibold">Dữ liệu vận chuyển</h2>
        <p className="mt-2 text-sm text-slate-400">
          TRANSPORT record và điểm GPS phải được gửi từ thiết bị/simulator có chữ ký Ed25519.
        </p>
        <p className="mt-2 text-sm text-slate-400">
          Tuyến dự kiến: {routePositions.length ? "Đã lập" : "Chưa lập"} · Điểm GPS đã nhận: {points.length}
        </p>
        {latestPoint && (
          <p className="mt-2 text-sm text-slate-300">
            Vị trí mới nhất: {latestPoint.latitude}, {latestPoint.longitude} · Nhiệt độ {latestPoint.temperature}°C · Độ ẩm {latestPoint.humidity}%
          </p>
        )}
      </div>
    </div>
  );
}
