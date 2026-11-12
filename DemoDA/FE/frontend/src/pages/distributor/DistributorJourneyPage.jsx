import { useCallback, useEffect, useMemo, useState } from "react";
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
  receiveDistributorBatch,
} from "../../api/distributorApi";
import axiosClient from "../../api/axiosClient";
import Pagination from "../../components/common/Pagination";

const mapTilerKey = import.meta.env.VITE_MAPTILER_KEY;

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
  const [receivingBatch, setReceivingBatch] = useState(false);
  const [vehicles, setVehicles] = useState([]);
  const [vehicleId, setVehicleId] = useState("");
  const [demoCount, setDemoCount] = useState(8);

  const [demoTemperature, setDemoTemperature] = useState("");
  const [demoHumidity, setDemoHumidity] = useState("");

  const [demoRun, setDemoRun] = useState(null);
  const [demoStatusLoaded, setDemoStatusLoaded] = useState(false);
  const [demoStarting, setDemoStarting] = useState(false);
  const demoRunning = demoRun?.status === "RUNNING" ||
    demoRun?.status === "STOPPING";

  const demoCompleted =
    Number(demoRun?.totalPoints) > 0 &&
    Number(demoRun?.sentPoints) >= Number(demoRun?.totalPoints);
  const [warehouse, setWarehouse] = useState({
    location: "",
    temperature: "",
    humidity: "",
    note: "",
  });

  const [returnWarehouse, setReturnWarehouse] = useState({
    warehouseName: "",
    warehouseAddress: "",
  });

  const [savingReturn, setSavingReturn] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const [evidence, setEvidence] = useState(null);
  const [selectedEvidenceKey, setSelectedEvidenceKey] = useState(null);
  const [evidenceLoading, setEvidenceLoading] = useState(false);
  const [evidenceError, setEvidenceError] = useState("");
  const [evidencePage, setEvidencePage] = useState(1);
  const [signedRequestText, setSignedRequestText] = useState("");
  const [changedTemperature, setChangedTemperature] = useState("");
  const [attackBusy, setAttackBusy] = useState(false);
  const [preparingSignedRecord, setPreparingSignedRecord] = useState(false);
  const [attackStep, setAttackStep] = useState("idle");
  const [attackResults, setAttackResults] = useState([]);
  const [attackError, setAttackError] = useState("");

  const [selectedAttackRecordKey, setSelectedAttackRecordKey] = useState(null);

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
        const [batchResponse, journeyResponse, vehiclesResponse, runResponse] = await Promise.all([
          getDistributorBatchDetail(batchId),
          getTransportJourney(batchId),
          axiosClient.get("/distributor/vehicles"),
          axiosClient.get(`/distributor/batches/${batchId}/demo-run`),
        ]);
        if (active) {
          setBatch(batchResponse.data);
          setJourney(journeyResponse.data);
          setVehicles(vehiclesResponse.data ?? []);
          // Phiên BE là nguồn sự thật khi quay lại trang.
          setDemoRun(runResponse.data);
          setDemoStatusLoaded(true);
          if (Number(runResponse.data?.totalPoints) > 0) {
            setDemoCount(Number(runResponse.data.totalPoints));
          }
          setVehicleId((current) => String(
            runResponse.data?.vehicleId ??
            (current || vehiclesResponse.data?.find((vehicle) => !vehicle.demoBusy)?.id ||
              vehiclesResponse.data?.[0]?.id || "")
          ));
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
    };
  }, [batchId]);

  useEffect(() => {
    let active = true;
    let checking = false;
    let previousSent = null;
    let previousStatus = null;
    let hydrated = false;

    async function refreshDemo() {
      if (checking) return;
      checking = true;
      try {
        const { data: run } = await axiosClient.get(
          `/distributor/batches/${batchId}/demo-run`
        );
        if (!active) return;
        setDemoRun(run);
        setDemoStatusLoaded(true);
        if (!hydrated) {
          hydrated = true;
          if (Number(run.totalPoints) > 0) {
            setDemoCount(Number(run.totalPoints));
          }
          if (run.vehicleId != null) setVehicleId(String(run.vehicleId));
        }

        if (run.sentPoints !== previousSent || run.status !== previousStatus) {
          previousSent = run.sentPoints;
          previousStatus = run.status;
          const [journeyResponse, batchResponse, vehiclesResponse] = await Promise.all([
            getTransportJourney(batchId),
            getDistributorBatchDetail(batchId),
            axiosClient.get("/distributor/vehicles"),
          ]);
          if (!active) return;
          setJourney(journeyResponse.data);
          setBatch(batchResponse.data);
          setVehicles(vehiclesResponse.data ?? []);
        }
      } catch (err) {
        if (active) setError(
          err.response?.data?.message ?? "Không đọc được tiến độ demo từ BE."
        );
      } finally {
        checking = false;
      }
    }

    refreshDemo();
    const pollId = window.setInterval(refreshDemo, 3000);
    function refreshWhenVisible() {
      if (document.visibilityState === "visible") refreshDemo();
    }
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      active = false;
      window.clearInterval(pollId);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [batchId]);

  async function openEvidence(recordKey) {
    if (!recordKey) return;
    setSelectedEvidenceKey(recordKey);
    setEvidence(null);
    setEvidenceError("");
    setEvidenceLoading(true);
    try {
      const response = await axiosClient.get(
        `/transport/batches/${batchId}/journey/points/${encodeURIComponent(recordKey)}/signature-evidence`
      );
      setEvidence(response.data);
    } catch (err) {
      setEvidenceError(
        err.response?.data?.message ?? "Không tải được bằng chứng điểm đo."
      );
    } finally {
      setEvidenceLoading(false);
    }
  }

  function closeEvidence() {
    setSelectedEvidenceKey(null);
    setEvidence(null);
    setEvidenceError("");
  }

  async function selectSignedPoint(point) {
    if (!point?.recordKey) return;

    setAttackError("");
    setNotice("");
    setAttackResults([]);
    setAttackStep("idle");
    setSignedRequestText("");
    setSelectedAttackRecordKey(null);
    setPreparingSignedRecord(true);

    try {
      const { data } = await axiosClient.get(
        `/distributor/batches/${batchId}/transport-records/` +
        `${encodeURIComponent(point.recordKey)}/signed-request`
      );

      if (
        !data?.deviceSignature ||
        data.vehicleId == null ||
        data.sequence == null
      ) {
        throw new Error("BE trả về bản tin thiếu chữ ký, xe hoặc sequence.");
      }

      setSignedRequestText(JSON.stringify(data, null, 2));
      setSelectedAttackRecordKey(point.recordKey);
      setVehicleId(String(data.vehicleId));
      setNotice(
        `Đã chọn điểm ${point.recordKey}. Xuống phần “Thử giả mạo bản tin IoT” để kiểm tra.`
      );
    } catch (err) {
      const message =
        err.response?.data?.message ??
        err.message ??
        "Không lấy được bản tin đã ký của điểm này.";

      setAttackError(message);
      setNotice("");
    } finally {
      setPreparingSignedRecord(false);
    }
  }

  async function testSignedRequest(mode) {
    setAttackError("");

    let original;
    try {
      original = JSON.parse(signedRequestText);
    } catch {
      setAttackError("Dán JSON request do lệnh sign của simulator in ra.");
      return;
    }

    if (!original || typeof original !== "object" ||
      !Number.isInteger(Number(original.vehicleId)) ||
      !Number.isInteger(Number(original.sequence)) ||
      typeof original.deviceSignature !== "string" ||
      !original.deviceSignature.trim() ||
      !original.gps || !original.timestamp ||
      original.temperature == null || original.humidity == null) {
      setAttackError("JSON thiếu trường của transport record đã ký.");
      return;
    }
    if (mode === "replay" && attackStep !== "forgedRejected") {
      setAttackError("Hãy thử sửa nhiệt độ trước khi phát lại bản gốc.");
      return;
    }


    const newTemperature = Number(changedTemperature);
    if (mode === "forged" &&
      (changedTemperature.trim() === "" ||
        !Number.isFinite(newTemperature) ||
        newTemperature === Number(original.temperature))) {
      setAttackError("Nhiệt độ sửa phải khác nhiệt độ trong JSON đã ký.");
      return;
    }

    setAttackBusy(true);
    try {
      const { data: beforeVehicles } = await axiosClient.get("/distributor/vehicles");
      const vehicle = (beforeVehicles ?? []).find(
        (item) => Number(item.id) === Number(original.vehicleId)
      );
      if (!vehicle) throw new Error("Xe trong JSON không thuộc Distributor hiện tại.");
      if (vehicle.demoBusy) throw new Error("Xe đang bận chạy demo. Hãy đợi hoàn tất.");

      const beforeSequence = Number(vehicle.lastSequence ?? 0);


      const body = mode === "forged"
        ? { ...original, temperature: newTemperature }
        : original;
      let status;
      let message;
      try {
        const response = await axiosClient.post(
          `/distributor/batches/${batchId}/transport-record`, body
        );
        status = response.status;
        message = "BE đã chấp nhận bản tin.";
      } catch (err) {
        if (!err.response) throw err;
        status = err.response.status;
        message = err.response.data?.message ?? `BE trả HTTP ${status}.`;
      }

      const { data: afterVehicles } = await axiosClient.get("/distributor/vehicles");
      setVehicles(afterVehicles ?? []);
      const afterSequence = Number(
        (afterVehicles ?? []).find((item) => Number(item.id) === Number(original.vehicleId))
          ?.lastSequence ?? 0
      );
      const expected = mode === "forged"
        ? /invalid device signature/i.test(message) &&
        afterSequence === beforeSequence
        : /invalid device sequence/i.test(message) &&
        afterSequence === beforeSequence;

      setAttackResults((previous) => [
        ...previous,
        { mode, expected, status, message, beforeSequence, afterSequence },
      ]);

      if (expected) {
        setAttackStep(
          mode === "forged" ? "forgedRejected" : "replayRejected"
        );
      }
    } catch (err) {
      setAttackError(err.response?.data?.message ?? err.message ?? "Không chạy được phép thử.");
    } finally {
      setAttackBusy(false);
    }
  }

  useEffect(() => {
    if (!selectedEvidenceKey) return;
    function onEscape(event) {
      if (event.key === "Escape" && !evidenceLoading) {
        setSelectedEvidenceKey(null);
        setEvidence(null);
        setEvidenceError("");
      }
    }
    document.addEventListener("keydown", onEscape);
    return () => document.removeEventListener("keydown", onEscape);
  }, [selectedEvidenceKey, evidenceLoading]);


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
  const evidenceRows = points.map((point, index) => ({
    point,
    pointNumber: index + 1,
  })).reverse();
  const evidencePageSize = 5;
  const evidenceTotalPages = Math.ceil(evidenceRows.length / evidencePageSize);
  const currentEvidencePage = Math.min(
    evidencePage,
    Math.max(evidenceTotalPages, 1)
  );
  const visibleEvidenceRows = evidenceRows.slice(
    (currentEvidencePage - 1) * evidencePageSize,
    currentEvidencePage * evidencePageSize
  );
  const latestPoint = points.at(-1);
  const selectedVehicle = vehicles.find(
    (vehicle) => Number(vehicle.id) === Number(vehicleId)
  );
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
    if (demoRunning || demoStarting || demoCompleted) return;
    const count = Number(demoCount);
    const temperature = Number(demoTemperature);
    const humidity = Number(demoHumidity);

    if (!vehicleId) {
      setError("Chọn xe trước khi chạy demo.");
      return;
    }
    if (
      !Number.isInteger(count) || count < 2 || count > 100 ||
      count > routePositions.length ||
      demoTemperature.trim() === "" || demoHumidity.trim() === "" ||
      !Number.isFinite(temperature) || !Number.isFinite(humidity) ||
      humidity < 0 || humidity > 100
    ) {
      setError("Kiểm tra số điểm, nhiệt độ và độ ẩm demo.");
      return;
    }

    setError("");
    setNotice("");
    setDemoStarting(true);
    try {
      const { data } = await axiosClient.post(
        `/distributor/batches/${batchId}/demo-run`,
        { vehicleId: Number(vehicleId), pointCount: count, temperature, humidity }
      );
      setDemoRun(data);
      setDemoStatusLoaded(true);
      if (Number(data.totalPoints) > 0) setDemoCount(Number(data.totalPoints));
      setVehicleId(String(data.vehicleId));
      setNotice("BE đã nhận phiên demo và sẽ gửi một điểm mỗi 15 giây.");
      const { data: updatedVehicles } = await axiosClient.get("/distributor/vehicles");
      setVehicles(updatedVehicles ?? []);
    } catch (err) {
      setError(err.response?.data?.message ?? err.message ?? "Không bắt đầu được demo.");
    } finally {
      setDemoStarting(false);
    }
  }

  async function handleStopDemo() {
    setError("");
    try {
      const { data } = await axiosClient.post(
        `/distributor/batches/${batchId}/demo-run/stop`
      );
      setDemoRun(data);
      setDemoStatusLoaded(true);
      setNotice(data.status === "STOPPING"
        ? "Đang hoàn tất điểm hiện tại rồi dừng demo."
        : "Đã dừng demo.");
      const { data: updatedVehicles } = await axiosClient.get("/distributor/vehicles");
      setVehicles(updatedVehicles ?? []);
    } catch (err) {
      setError(err.response?.data?.message ?? "Không dừng được demo.");
    }
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

  async function handleReceiveBatch() {
    if (receivingBatch || batch?.status !== "ASSIGNED_TO_DISTRIBUTOR") return;
    setReceivingBatch(true);
    setError("");
    setNotice("");
    try {
      const { data } = await receiveDistributorBatch(batchId);
      // Receive returns the list DTO; retain detail fields such as productCategory.
      setBatch((current) => ({ ...current, ...data }));
      setNotice("Đã xác nhận nhận hàng. Bạn có thể lập tuyến và bắt đầu vận chuyển.");
    } catch (err) {
      setError(err.response?.data?.message ?? "Không thể xác nhận nhận hàng. Hãy thử lại.");
    } finally {
      setReceivingBatch(false);
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


  async function handleReceiveReturn(event) {
    event.preventDefault();
    setSavingReturn(true);
    setError("");
    setNotice("");

    try {
      await axiosClient.post(
        `/distributor/batches/${batchId}/receive-return`,
        {
          warehouseName: returnWarehouse.warehouseName.trim(),
          warehouseAddress: returnWarehouse.warehouseAddress.trim(),
        }
      );

      const response = await getDistributorBatchDetail(batchId);
      setBatch(response.data);
      setReturnWarehouse({ warehouseName: "", warehouseAddress: "" });
      setNotice("Đã xác nhận hàng trả về kho và lưu RETURN_RECEIPT record.");
    } catch (err) {
      setError(err.response?.data?.message ?? "Không nhận được hàng trả về.");
    } finally {
      setSavingReturn(false);
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
        <div className="flex flex-wrap items-center gap-2">
          {batch?.status === "ASSIGNED_TO_DISTRIBUTOR" && (
            <button type="button" onClick={handleReceiveBatch} disabled={receivingBatch}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50">
              {receivingBatch ? "Đang xác nhận..." : "Xác nhận nhận hàng"}
            </button>
          )}
          <button type="button" onClick={handleRefresh} className="rounded-lg border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800">
            Cập nhật GPS
          </button>
        </div>
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
            Chế độ demo: BE tạo khóa Ed25519 mô phỏng và gửi từng điểm trên tuyến đã lập.
            Xe được đánh dấu đang bận cho tới khi phiên hoàn tất hoặc dừng.
          </p>
          <div className="grid gap-3 md:grid-cols-2">
            <label className="text-sm">Xe dùng cho demo
              <select value={vehicleId} disabled={!demoStatusLoaded || demoRunning || demoStarting}
                onChange={(e) => {
                  setVehicleId(e.target.value);
                  setSignedRequestText("");
                  setAttackResults([]);
                  setAttackStep("idle");
                }}
                className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2">
                {vehicles.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}
                    disabled={vehicle.demoBusy && Number(demoRun?.vehicleId) !== vehicle.id}>
                    {vehicle.vehiclePlate} · {vehicle.deviceId}{vehicle.demoBusy ? " · Đang bận" : ""}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-sm">Số điểm (2–{Math.min(100, routePositions.length)})
              <input type="number" min="2" max={Math.min(100, routePositions.length)}
                value={demoCount} disabled={!demoStatusLoaded || demoRunning || demoStarting}
                onChange={(e) => setDemoCount(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
            </label>
            <div className="grid grid-cols-2 gap-3">
              <label className="text-sm">Nhiệt độ (°C)
                <input type="number" step="any" value={demoTemperature}
                  disabled={!demoStatusLoaded || demoRunning || demoStarting}
                  onChange={(e) => setDemoTemperature(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
              </label>
              <label className="text-sm">Độ ẩm (%)
                <input type="number" min="0" max="100" step="any" value={demoHumidity}
                  disabled={!demoStatusLoaded || demoRunning || demoStarting}
                  onChange={(e) => setDemoHumidity(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2" />
              </label>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {demoRunning ? (
              <button type="button" onClick={handleStopDemo}
                disabled={demoRun?.status === "STOPPING"}
                className="rounded-lg border border-red-500 px-4 py-2 text-sm disabled:opacity-40">
                {demoRun?.status === "STOPPING" ? "Đang dừng..." : "Dừng demo"}
              </button>
            ) : (
              <button
                type="button"
                onClick={handleStartDemo}
                disabled={!demoStatusLoaded || demoStarting || demoCompleted}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
              >
                {!demoStatusLoaded
                  ? "Đang kiểm tra phiên..."
                  : demoStarting
                    ? "Đang bắt đầu..."
                    : demoCompleted
                      ? "Đã hoàn tất demo"
                      : "Bắt đầu demo"}
              </button>
            )}
            <span className="text-sm text-slate-400">
              {demoStatusLoaded
                ? `Đã gửi ${demoRun?.sentPoints ?? 0}/${demoRun?.totalPoints ?? 0} điểm`
                : "Đang tải tiến độ..."}
            </span>
          </div>
          {demoRun?.status === "FAILED" && (
            <p className="text-sm text-red-400">
              Demo thất bại: {demoRun.error || "Không rõ nguyên nhân"}
            </p>
          )}
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

      {batch?.status === "DELIVERY_REJECTED" && (
        <form onSubmit={handleReceiveReturn}
          className="space-y-3 rounded-xl border border-amber-500/30 bg-[#111827] p-4">
          <h2 className="font-semibold">Nhận hàng Retailer trả về</h2>

          <input
            required
            placeholder="Tên kho Distributor"
            value={returnWarehouse.warehouseName}
            onChange={(e) => setReturnWarehouse((old) => ({
              ...old, warehouseName: e.target.value
            }))}
            className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
          />

          <input
            required
            placeholder="Địa chỉ kho"
            value={returnWarehouse.warehouseAddress}
            onChange={(e) => setReturnWarehouse((old) => ({
              ...old, warehouseAddress: e.target.value
            }))}
            className="w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2"
          />

          <button type="submit" disabled={savingReturn}
            className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold disabled:opacity-40">
            {savingReturn ? "Đang lưu..." : "Xác nhận hàng đã về kho"}
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

      <section className="rounded-xl border border-slate-800 bg-[#111827] p-4">
        <h2 className="font-semibold">Bằng chứng điểm đo IoT</h2>
        <p className="mt-2 text-sm text-slate-400">
          Chọn một điểm để BE đọc dữ liệu đã lưu và xác minh chữ ký Ed25519
          bằng khóa công khai gắn với record lúc ghi nhận.
        </p>

        {points.length === 0 ? (
          <p className="mt-4 text-sm text-slate-400">Chưa có điểm đo để kiểm tra.</p>
        ) : (
          <div className="mt-4 space-y-2">
            {visibleEvidenceRows.map(({ point, pointNumber }) => (
              <div key={point.recordKey ?? `${point.sequence}-${pointNumber}`}
                className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-700 bg-[#0b1020] p-3">
                <div className="min-w-0 text-sm">
                  <p className="font-medium">
                    Điểm #{pointNumber} · Xe #{point.vehicleId ?? "—"} · Sequence {point.sequence ?? "—"}
                  </p>
                  <p className="mt-1 text-xs text-slate-400">
                    {point.timestamp || "Chưa có thời điểm"} · {point.temperature ?? "—"}°C · {point.humidity ?? "—"}%
                  </p>
                  <p className="mt-1 break-all font-mono text-xs text-slate-500">
                    {point.recordKey ?? "Chưa có record key"}
                  </p>
                </div>
                <button type="button"
                  disabled={!point.recordKey || evidenceLoading}
                  onClick={() => openEvidence(point.recordKey)}
                  className="rounded-lg border border-blue-500 px-3 py-2 text-sm text-blue-300 hover:bg-blue-500/10 disabled:opacity-40">
                  Xem bằng chứng
                </button>
                <button
                  type="button"
                  disabled={!point.recordKey || preparingSignedRecord || demoRunning}
                  onClick={() => selectSignedPoint(point)}
                  className="rounded-lg border border-indigo-500 px-3 py-2 text-sm text-indigo-300"
                >
                  Dùng điểm này để thử tấn công
                </button>
              </div>
            ))}
            <Pagination
              page={currentEvidencePage}
              totalPages={evidenceTotalPages}
              onPageChange={setEvidencePage}
            />
          </div>
        )}
      </section>

      {(batch?.status === "RECEIVED_BY_DISTRIBUTOR" ||
        batch?.status === "IN_DISTRIBUTION") && (

          <section className="rounded-xl border border-slate-800 bg-[#111827] p-4">
            <h2 className="font-semibold">Thử giả mạo bản tin IoT</h2>
            <p className="mt-2 text-sm text-slate-400">
              BE tạo bản tin mẫu đã ký cho xe đang chọn. FE chỉ đổi nhiệt độ,
              giữ nguyên chữ ký rồi gửi vào endpoint nhận TRANSPORT record để BE quyết định.
              Khóa demo nằm ở BE; đây là dữ liệu mô phỏng, không chứng minh cảm biến vật lý đã đo.
            </p>
            <label className="mt-4 block max-w-lg text-sm text-slate-300">
              Xe dùng để thử
              <select value={vehicleId} disabled={preparingSignedRecord || attackBusy || demoRunning}
                onChange={(event) => {
                  setVehicleId(event.target.value);
                  setSignedRequestText("");
                  setAttackStep("idle");
                  setAttackResults([]);
                  setAttackError("");
                }}
                className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-2">
                <option value="">Chọn xe</option>
                {vehicles.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id} disabled={vehicle.demoBusy}>
                    Xe #{vehicle.id} · {vehicle.vehiclePlate} · sequence cuối {vehicle.lastSequence ?? 0}
                    {vehicle.demoBusy ? " · Đang bận" : ""}
                  </option>
                ))}
              </select>
            </label>
            {selectedVehicle && (
              <p className="mt-2 text-sm text-cyan-300">
                Đang chọn xe #{selectedVehicle.id}: lastSequence = {selectedVehicle.lastSequence ?? 0};
                bản tin mẫu kế tiếp sẽ dùng sequence {Number(selectedVehicle.lastSequence ?? 0) + 1}.
                Sequence được tính riêng cho từng xe, kể cả khi xe đã chạy ở batch khác.
              </p>
            )}
            <p className="mt-4 rounded-lg border border-indigo-500/30 bg-indigo-500/10 p-3 text-sm text-indigo-200">
              Chọn “Dùng điểm này để thử tấn công” trong danh sách Bằng chứng điểm đo
              IoT phía trên. Hệ thống sẽ lấy bản tin đã ký của điểm đó; không tạo thêm
              điểm vận chuyển.
            </p>
            {selectedAttackRecordKey ? (
              <p className="mt-4 rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-200">
                Đang dùng điểm: <strong>{selectedAttackRecordKey}</strong>
              </p>
            ) : (
              <p className="mt-4 text-sm text-amber-300">
                Chưa chọn điểm đo. Bấm “Dùng điểm này để thử tấn công” ở danh sách phía trên.
              </p>
            )}
            <label className="mt-4 block text-sm text-slate-300">
              Bản tin gốc do BE tạo (không cần dán JSON)
              <textarea
                rows={8}
                spellCheck={false}
                readOnly
                value={signedRequestText}
                placeholder="Chưa chọn điểm đo nào để thử tấn công"
                className="mt-1 w-full rounded-lg border border-slate-600 bg-[#0b1020] p-3 font-mono text-xs text-slate-100 placeholder:text-slate-600"
              />
            </label>
            <label className="mt-3 block text-sm text-slate-300">
              Nhiệt độ giả mạo (°C)
              <input
                type="number"
                step="any"
                value={changedTemperature}
                onChange={(event) => {
                  setChangedTemperature(event.target.value);
                  setAttackStep("idle");
                  setAttackResults([]);
                  setAttackError("");
                }}
                placeholder="Ví dụ: 25"
                className="mt-1 w-full max-w-52 rounded-lg border border-slate-600 bg-[#0b1020] p-2"
              />
            </label>
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => testSignedRequest("forged")}
                disabled={
                  attackBusy ||
                  preparingSignedRecord ||
                  demoRunning ||
                  !signedRequestText ||
                  attackStep !== "idle"
                }
                className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold disabled:opacity-40"
              >
                1. Thử sửa nhiệt độ
              </button>

              <button
                type="button"
                onClick={() => testSignedRequest("replay")}
                disabled={
                  attackBusy ||
                  preparingSignedRecord ||
                  demoRunning ||
                  !signedRequestText ||
                  attackStep !== "forgedRejected"
                }
                className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold disabled:opacity-40"
              >
                2. Phát lại bản gốc
              </button>
            </div>
            <p className="mt-2 text-xs text-amber-300">
              Bước 2 tạo một TRANSPORT record thật và tăng sequence của xe. Chỉ thử trên lô demo.
            </p>
            {attackBusy && <p className="mt-3 text-sm text-slate-400">Đang đợi phản hồi từ BE...</p>}
            {attackError && (
              <p className="mt-3 rounded-lg bg-red-500/10 p-3 text-sm text-red-300">{attackError}</p>
            )}
            {attackResults.length > 0 && (
              <ol className="mt-4 space-y-2 text-sm">
                {attackResults.map((result, index) => (
                  <li key={index} className={`rounded-lg p-3 ${result.expected
                    ? "bg-emerald-500/10 text-emerald-200"
                    : "bg-red-500/10 text-red-200"}`}>
                    <strong>{result.mode === "forged" ? "Bản sửa nhiệt độ"
                      : result.mode === "original" ? "Bản gốc" : "Phát lại"}: HTTP {result.status}</strong>
                    <p className="mt-1 break-words">BE: {result.message}</p>
                    <p>Sequence xe: {result.beforeSequence} → {result.afterSequence}</p>
                    {!result.expected && <p>Chưa đạt kết quả mong đợi; kiểm tra phản hồi BE.</p>}
                  </li>
                ))}
              </ol>
            )}
          </section>
        )}

      {selectedEvidenceKey && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/75 p-4"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !evidenceLoading) closeEvidence();
          }}>
          <section role="dialog" aria-modal="true" aria-label="Bằng chứng điểm đo IoT"
            className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl border border-slate-700 bg-[#111827] p-5 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="font-semibold">Bằng chứng của record</h3>
                <p className="mt-1 break-all font-mono text-xs text-slate-400">
                  {selectedEvidenceKey}
                </p>
              </div>
              <button type="button" onClick={closeEvidence}
                disabled={evidenceLoading}
                className="text-sm text-slate-400 hover:text-white disabled:opacity-40">
                Đóng
              </button>
            </div>

            {evidenceLoading && (
              <p className="mt-4 text-sm text-slate-400">Đang tải và xác minh chữ ký...</p>
            )}
            {evidenceError && (
              <p className="mt-4 rounded-lg bg-red-500/10 p-3 text-sm text-red-300">
                {evidenceError}
              </p>
            )}
            {evidence && (
              <div className="mt-4 space-y-4 text-sm">
                <div className={`rounded-lg p-4 ${evidence.signatureValid === true
                  ? "bg-emerald-500/10 text-emerald-300"
                  : evidence.signatureValid === false
                    ? "bg-red-500/10 text-red-300"
                    : "bg-amber-500/10 text-amber-300"
                  }`}>
                  <strong>
                    {evidence.signatureValid === true
                      ? "Chữ ký Ed25519 hợp lệ"
                      : evidence.signatureValid === false
                        ? "Chữ ký Ed25519 không hợp lệ"
                        : "BE chưa trả kết quả xác minh chữ ký"}
                  </strong>
                  <p className="mt-1 text-xs">
                    Kết quả này xác minh dữ liệu khớp với khóa demo đã đăng ký;
                    không chứng minh cảm biến vật lý đã tự tạo số đo.
                  </p>
                </div>

                <dl className="grid gap-3 sm:grid-cols-2">
                  <div><dt className="text-slate-400">Xe / thiết bị</dt>
                    <dd>{evidence.vehicleId ?? "—"} / {evidence.deviceId ?? "—"}</dd></div>
                  <div><dt className="text-slate-400">Sequence</dt>
                    <dd>{evidence.sequence ?? "—"}</dd></div>
                  <div><dt className="text-slate-400">Thời điểm đo</dt>
                    <dd>{evidence.timestamp ?? "—"}</dd></div>
                  <div><dt className="text-slate-400">Vân tay khóa công khai</dt>
                    <dd className="break-all font-mono text-xs">
                      {evidence.publicKeyFingerprint ?? evidence.devicePublicKeyFingerprint ?? "—"}
                    </dd></div>
                </dl>

                {(evidence.devicePublicKeyAtSigning || evidence.deviceSignature) && (
                  <div className="space-y-2 rounded-lg border border-slate-700 p-3 text-xs">
                    {evidence.devicePublicKeyAtSigning && (
                      <p>
                        <span className="text-slate-400">Public key tại thời điểm ký: </span>
                        <span className="break-all font-mono">
                          {evidence.devicePublicKeyAtSigning}
                        </span>
                      </p>
                    )}
                    {evidence.deviceSignature && (
                      <p>
                        <span className="text-slate-400">Chữ ký (Base64): </span>
                        <span className="break-all font-mono">
                          {evidence.deviceSignature}
                        </span>
                      </p>
                    )}
                  </div>
                )}

                <details className="rounded-lg border border-slate-700 p-3">
                  <summary className="cursor-pointer font-medium">Dữ liệu bằng chứng BE trả về</summary>
                  <pre className="mt-3 max-h-80 overflow-auto whitespace-pre-wrap break-all text-xs text-slate-300">
                    {JSON.stringify(evidence, null, 2)}
                  </pre>
                </details>

              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
