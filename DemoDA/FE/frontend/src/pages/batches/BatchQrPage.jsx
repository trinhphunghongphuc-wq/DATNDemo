import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { QRCodeCanvas } from "qrcode.react";
import axiosClient from "../../api/axiosClient";

export default function BatchQrPage() {
  const { batchId } = useParams();
  const [batch, setBatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadBatch() {
      try {
        const response = await axiosClient.get(
          `/producer/batches/${batchId}`
        );
        setBatch(response.data);
      } catch (err) {
        setError(
          err.response?.data?.message ||
            "Không thể tải thông tin QR của lô hàng."
        );
      } finally {
        setLoading(false);
      }
    }

    loadBatch();
  }, [batchId]);

  function downloadQr() {
    const canvas = document.getElementById("batch-qr-code");
    if (!canvas || !batch) return;

    const link = document.createElement("a");
    link.href = canvas.toDataURL("image/png");
    link.download = `${batch.batchCode}-QR.png`;
    link.click();
  }

  if (loading) {
    return <p className="text-white">Đang tải QR...</p>;
  }

  if (error || !batch) {
    return (
      <p className="text-red-400">
        {error || "Không tìm thấy lô hàng."}
      </p>
    );
  }

  const tracePath = `/trace/batch/${encodeURIComponent(batch.batchCode)}`;
  const traceUrl = `${window.location.origin}${tracePath}`;

  return (
    <div className="mx-auto max-w-xl rounded-2xl bg-white p-8 text-center shadow">
      <h1 className="text-2xl font-bold text-slate-900">
        QR truy xuất lô hàng
      </h1>

      <p className="mt-2 text-slate-600">{batch.name}</p>
      <p className="mt-1 font-mono text-sm text-slate-500">
        {batch.batchCode}
      </p>

      <div className="mt-6 flex justify-center">
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <QRCodeCanvas
            id="batch-qr-code"
            value={traceUrl}
            size={260}
            level="H"
            includeMargin
          />
        </div>
      </div>

      <Link
        to={tracePath}
        target="_blank"
        rel="noopener noreferrer"
        className="mt-5 inline-block rounded-xl border border-blue-500 px-5 py-2.5 font-semibold text-blue-600 hover:bg-blue-50"
      >
        Thông tin chi tiết
      </Link>

      <div>
        <button
          type="button"
          onClick={downloadQr}
          className="mt-6 rounded-xl bg-emerald-600 px-6 py-3 font-semibold text-white hover:bg-emerald-700"
        >
          Tải QR xuống
        </button>
      </div>
    </div>
  );
}