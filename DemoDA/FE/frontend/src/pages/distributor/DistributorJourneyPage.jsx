import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  CircleMarker,
  MapContainer,
  Polyline,
  Popup,
  TileLayer,
  useMap,
  useMapEvents,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";

import {
  getDistributorBatchDetail,
  getTransportJourney,
  planTransportRoute,
} from "../../api/distributorApi";

function SelectRoutePoints({ enabled, onSelect }) {
  useMapEvents({
    click(event) {
      if (enabled) onSelect(event.latlng);
    },
  });

  return null;
}

function FitJourney({ routePositions, points }) {
  const map = useMap();

  useEffect(() => {
    const positions = routePositions.length
      ? routePositions
      : points.map((point) => [point.latitude, point.longitude]);

    if (positions.length > 1) {
      map.fitBounds(positions, { padding: [30, 30] });
    } else if (positions.length === 1) {
      map.setView(positions[0], 15);
    }
  }, [map, routePositions, points]);

  return null;
}

export default function DistributorJourneyPage() {
  const { batchId } = useParams();

  const [batch, setBatch] = useState(null);
  const [journey, setJourney] = useState(null);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
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

        const [batchResponse, journeyResponse] = await Promise.all([
          getDistributorBatchDetail(batchId),
          getTransportJourney(batchId),
        ]);

        if (active) {
          setBatch(batchResponse.data);
          setJourney(journeyResponse.data);
        }
      } catch (err) {
        if (active) {
          setError(err.response?.data?.message ?? "Không tải được hành trình.");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadInitial();

    return () => {
      active = false;
    };
  }, [batchId]);

  const routePositions = useMemo(() => {
    const coordinates = journey?.plannedRoute?.coordinates;

    if (!Array.isArray(coordinates)) return [];

    return coordinates
      .filter(
        (coordinate) =>
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

  function handleMapClick(latlng) {
    const selected = {
      lat: latlng.lat,
      lon: latlng.lng,
    };

    if (!origin || destination) {
      setOrigin(selected);
      setDestination(null);
    } else {
      setDestination(selected);
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

  if (loading) {
    return <p className="text-slate-400">Đang tải hành trình...</p>;
  }

  return (
    <div className="space-y-5">
      <Link
        to="/distributor/batches"
        className="text-sm text-blue-400 hover:underline"
      >
        ← Danh sách lô hàng
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">
            {batch?.name ?? `Batch #${batchId}`}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Batch #{batchId} · {batch?.status ?? "Không rõ trạng thái"}
          </p>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          className="rounded-lg border border-slate-600 px-4 py-2 text-sm hover:bg-slate-800"
        >
          Cập nhật GPS
        </button>
      </div>

      {error && (
        <p className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-red-300">
          {error}
        </p>
      )}

      {canPlanRoute && (
        <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
          <p className="text-sm text-slate-300">
            Nhấp lên bản đồ để chọn điểm bắt đầu, sau đó nhấp lần nữa để chọn
            điểm kết thúc. Nhấp lần thứ ba sẽ chọn lại từ đầu.
          </p>

          <button
            type="button"
            disabled={!origin || !destination || saving}
            onClick={handlePlanRoute}
            className="mt-3 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40"
          >
            {saving ? "Đang lập tuyến..." : "Xác nhận tuyến đường"}
          </button>
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-slate-800">
        <MapContainer
          center={[11.9404, 108.4583]}
          zoom={13}
          scrollWheelZoom
          style={{ height: 500, width: "100%" }}
        >
       <TileLayer
  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
/>

          <SelectRoutePoints
            enabled={canPlanRoute}
            onSelect={handleMapClick}
          />

          <FitJourney routePositions={routePositions} points={points} />

          {routePositions.length > 1 && (
            <Polyline
              positions={routePositions}
              pathOptions={{ color: "#3b82f6", weight: 5 }}
            />
          )}

          {origin && canPlanRoute && (
            <CircleMarker center={[origin.lat, origin.lon]} radius={8}>
              <Popup>Điểm bắt đầu</Popup>
            </CircleMarker>
          )}

          {destination && canPlanRoute && (
            <CircleMarker
              center={[destination.lat, destination.lon]}
              radius={8}
              pathOptions={{ color: "#f97316" }}
            >
              <Popup>Điểm kết thúc</Popup>
            </CircleMarker>
          )}

          {points.map((point, index) => (
            <CircleMarker
              key={point.recordKey}
              center={[point.latitude, point.longitude]}
              radius={index === points.length - 1 ? 9 : 5}
              pathOptions={{
                color: index === points.length - 1 ? "#22c55e" : "#eab308",
              }}
            >
              <Popup>
                <strong>Điểm GPS #{index + 1}</strong>
                <br />
                Xe: {point.vehicleId} · Sequence: {point.sequence}
                <br />
                Nhiệt độ: {point.temperature}°C
                <br />
                Độ ẩm: {point.humidity}%
                <br />
                {point.timestamp}
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>

      <div className="rounded-xl border border-slate-800 bg-[#111827] p-4">
        <h2 className="font-semibold">Dữ liệu vận chuyển</h2>
        <p className="mt-2 text-sm text-slate-400">
          Tuyến dự kiến: {routePositions.length ? "Đã lập" : "Chưa lập"} ·
          Điểm GPS đã nhận: {points.length}
        </p>

        {latestPoint && (
          <p className="mt-2 text-sm text-slate-300">
            Vị trí mới nhất: {latestPoint.latitude}, {latestPoint.longitude}
            {" · "}Nhiệt độ {latestPoint.temperature}°C
            {" · "}Độ ẩm {latestPoint.humidity}%
          </p>
        )}
      </div>
    </div>
  );
}