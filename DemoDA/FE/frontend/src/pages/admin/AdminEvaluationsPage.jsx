import { useEffect, useMemo, useState } from "react";
import axiosClient from "../../api/axiosClient";

const MODEL_NAMES = {
  FULL_ON_CHAIN: "Full on-chain",
  HASH_PER_RECORD: "Hash mỗi record",
  MERKLE_BATCHING: "Merkle batching",
};

function errorMessage(error) {
  return error?.response?.data?.message || "Không thực hiện được yêu cầu.";
}

function formatNumber(value) {
  if (value === null || value === undefined) return "—";
  return Number(value).toLocaleString("vi-VN");
}

function formatUsd(value) {
  if (value === null || value === undefined) return "—";
  return `$${Number(value).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 6,
  })}`;
}

export default function AdminEvaluationsPage() {
  const [history, setHistory] = useState([]);
  const [selectedRunId, setSelectedRunId] = useState(null);
  const [costs, setCosts] = useState([]);
  const [stageResult, setStageResult] = useState(null);

  const [recordCount, setRecordCount] = useState(5);
  const [repetitions, setRepetitions] = useState(1);
  const [l1GasPriceGwei, setL1GasPriceGwei] = useState("2");
  const [l2GasPriceGwei, setL2GasPriceGwei] = useState("0.05");
  const [ethPriceUsd, setEthPriceUsd] = useState("3000");
  const [batchId, setBatchId] = useState("");

  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [costLoading, setCostLoading] = useState(false);
  const [stageLoading, setStageLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadHistory() {
    setLoading(true);
    setError("");

    try {
      const response = await axiosClient.get("/admin/evaluations");
      setHistory(response.data ?? []);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadHistory();
  }, []);

  const runs = useMemo(() => {
    const grouped = new Map();

    for (const result of history) {
      if (!grouped.has(result.runId)) {
        grouped.set(result.runId, {
          runId: result.runId,
          recordCount: result.recordCount,
          repetitions: result.repetitions,
          createdAt: result.createdAt,
        });
      }
    }

    return [...grouped.values()];
  }, [history]);

  const selectedResults = history.filter(
    (result) => result.runId === selectedRunId
  );

  async function runEvaluation() {
    const count = Number(recordCount);
    const repeat = Number(repetitions);

    if (
      !Number.isInteger(count) ||
      count < 1 ||
      count > 1000 ||
      !Number.isInteger(repeat) ||
      repeat < 1 ||
      repeat > 10
    ) {
      setError("Số record phải từ 1–1000; số lần lặp từ 1–10.");
      return;
    }

    const accepted = window.confirm(
      `Chạy đánh giá ${count} records × ${repeat} lần?\n\n` +
        "BE sẽ gửi giao dịch blockchain thật cho cả ba mô hình. " +
        "Việc này có thể tốn gas và mất thời gian."
    );
    if (!accepted) return;

    setRunning(true);
    setError("");
    setNotice("");

    try {
      const response = await axiosClient.post(
        "/admin/evaluations/run",
        null,
        { params: { recordCount: count, repetitions: repeat } }
      );

      const results = response.data ?? [];
      const runId = results[0]?.runId;

      await loadHistory();
      setSelectedRunId(runId ?? null);
      setCosts([]);
      setNotice(
        runId
          ? `Đã hoàn tất lần chạy ${runId}.`
          : "Đã chạy xong nhưng BE không trả runId."
      );
    } catch (err) {
      setError(errorMessage(err));
      // Không tự chạy lại: request trước có thể đã gửi giao dịch.
      await loadHistory();
    } finally {
      setRunning(false);
    }
  }

  async function calculateCosts() {
    if (!selectedRunId) return;

    const values = [
      l1GasPriceGwei,
      l2GasPriceGwei,
      ethPriceUsd,
    ].map(Number);

    if (values.some((value) => !Number.isFinite(value) || value < 0)) {
      setError("Giá gas và giá ETH phải là số không âm.");
      return;
    }

    setCostLoading(true);
    setError("");

    try {
      const response = await axiosClient.get(
        `/admin/evaluations/${selectedRunId}/cost`,
        {
          params: {
            l1GasPriceGwei,
            l2GasPriceGwei,
            ethPriceUsd,
          },
        }
      );
      setCosts(response.data ?? []);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setCostLoading(false);
    }
  }

  async function evaluateBatchStage() {
    const id = Number(batchId);

    if (!Number.isInteger(id) || id <= 0) {
      setError("Nhập Batch ID hợp lệ.");
      return;
    }

    setStageLoading(true);
    setStageResult(null);
    setError("");

    try {
      const response = await axiosClient.get(
        `/admin/evaluations/batches/${id}/stage-model`
      );
      setStageResult(response.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setStageLoading(false);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header>
        <p className="text-sm font-semibold text-blue-400">
          ADMIN / EVALUATION
        </p>
        <h1 className="mt-1 text-2xl font-bold">Đánh giá chi phí lưu trữ</h1>
        <p className="mt-1 text-sm text-slate-400">
          So sánh số giao dịch, gas và dữ liệu on-chain của ba mô hình.
        </p>
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

      <div className="grid gap-5 lg:grid-cols-2">
        <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="font-semibold">Chạy đánh giá mới</h2>
          <p className="mt-1 text-xs text-amber-300">
            Thao tác này gửi giao dịch blockchain thật và có thể tốn gas.
          </p>

          <div className="mt-5 grid grid-cols-2 gap-4">
            <label className="text-sm text-slate-400">
              Số record
              <input
                type="number"
                min="1"
                max="1000"
                step="1"
                value={recordCount}
                onChange={(event) => setRecordCount(event.target.value)}
                className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1020] px-3 py-2 text-white"
              />
            </label>
            <label className="text-sm text-slate-400">
              Số lần lặp
              <input
                type="number"
                min="1"
                max="10"
                step="1"
                value={repetitions}
                onChange={(event) => setRepetitions(event.target.value)}
                className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1020] px-3 py-2 text-white"
              />
            </label>
          </div>

          <button
            type="button"
            onClick={runEvaluation}
            disabled={running}
            className="mt-5 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50"
          >
            {running ? "Đang chạy, vui lòng chờ..." : "Chạy đánh giá"}
          </button>
        </section>

        <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="font-semibold">Mô hình 3 stage của batch thật</h2>
          <p className="mt-1 text-xs text-slate-400">
            Đọc record và giao dịch anchor đã lưu; không gửi giao dịch mới.
          </p>

          <div className="mt-5 flex gap-3">
            <input
              type="number"
              min="1"
              step="1"
              value={batchId}
              onChange={(event) => setBatchId(event.target.value)}
              placeholder="Batch ID, ví dụ 10"
              className="min-w-0 flex-1 rounded-xl border border-slate-700 bg-[#0b1020] px-3 py-2 text-sm"
            />
            <button
              type="button"
              onClick={evaluateBatchStage}
              disabled={stageLoading}
              className="rounded-xl border border-blue-500 px-4 py-2 text-sm text-blue-300 hover:bg-blue-500/10 disabled:opacity-50"
            >
              {stageLoading ? "Đang tính..." : "Xem"}
            </button>
          </div>

          {stageResult && (
            <div className="mt-5 grid grid-cols-2 gap-3 text-sm">
              <div>Tổng record: {stageResult.totalRecords}</div>
              <div>Stage có record: {stageResult.stagesWithRecords}</div>
              <div>
                TX nếu hash/record:{" "}
                {stageResult.hashPerRecordTransactions}
              </div>
              <div>
                TX nếu root/stage:{" "}
                {stageResult.stageMerkleTransactions}
              </div>
              <div>Stage đã anchor: {stageResult.anchoredStages}</div>
              <div>
                Gas stage thực tế:{" "}
                {String(stageResult.actualStageGasUsed ?? "—")}
              </div>
              <div>
                Giảm số TX:{" "}
                {Number(
                  stageResult.transactionReductionPercent ?? 0
                ).toFixed(2)}
                %
              </div>
              <div>
                Anchor đủ stage có record:{" "}
                {stageResult.fullyAnchored ? "Có" : "Chưa"}
              </div>
            </div>
          )}
        </section>
      </div>

      <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">Lịch sử đánh giá</h2>
          <button
            type="button"
            onClick={loadHistory}
            className="text-sm text-blue-400 hover:text-blue-300"
          >
            Làm mới
          </button>
        </div>

        {loading ? (
          <p className="mt-4 text-sm text-slate-400">Đang tải...</p>
        ) : runs.length === 0 ? (
          <p className="mt-4 text-sm text-slate-400">Chưa có lần chạy nào.</p>
        ) : (
          <div className="mt-4 flex flex-wrap gap-2">
            {runs.map((run) => (
              <button
                key={run.runId}
                type="button"
                onClick={() => {
                  setSelectedRunId(run.runId);
                  setCosts([]);
                }}
                className={`rounded-xl border px-3 py-2 text-left text-xs ${
                  selectedRunId === run.runId
                    ? "border-blue-500 bg-blue-500/10 text-blue-300"
                    : "border-slate-700 text-slate-300 hover:bg-slate-800"
                }`}
              >
                <span className="block font-mono">
                  {run.runId.slice(0, 8)}...
                </span>
                <span className="mt-1 block text-slate-400">
                  {run.recordCount} records × {run.repetitions} lần
                </span>
              </button>
            ))}
          </div>
        )}

        {selectedRunId && (
          <>
            <p className="mt-5 break-all text-xs text-slate-400">
              Run ID: {selectedRunId}
            </p>

            <div className="mt-3 overflow-x-auto">
              <table className="w-full min-w-[850px] text-left text-sm">
                <thead className="bg-slate-800/60 text-slate-400">
                  <tr>
                    <th className="p-3">Mô hình</th>
                    <th className="p-3">TX</th>
                    <th className="p-3">Gas thực tế</th>
                    <th className="p-3">On-chain bytes</th>
                    <th className="p-3">Off-chain bytes</th>
                    <th className="p-3">Proof bytes</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedResults.map((result) => (
                    <tr key={result.id} className="border-t border-slate-800">
                      <td className="p-3">
                        {MODEL_NAMES[result.evaluationModel] ??
                          result.evaluationModel}
                      </td>
                      <td className="p-3">
                        {formatNumber(result.transactionCount)}
                      </td>
                      <td className="p-3 font-mono text-xs">
                        {String(result.totalGasUsed ?? "—")}
                      </td>
                      <td className="p-3">
                        {formatNumber(result.onChainBytes)}
                      </td>
                      <td className="p-3">
                        {formatNumber(result.offChainBytes)}
                      </td>
                      <td className="p-3">
                        {formatNumber(result.proofSizeBytes)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      {selectedRunId && (
        <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
          <h2 className="font-semibold">Ước tính chi phí cho run đã chọn</h2>
          <p className="mt-1 text-xs text-slate-400">
            Đây là giá giả định bạn nhập, không phải giá thị trường tự động.
          </p>

          <div className="mt-4 grid gap-3 md:grid-cols-3">
            {[
              ["L1 gas price (gwei)", l1GasPriceGwei, setL1GasPriceGwei],
              ["L2 gas price (gwei)", l2GasPriceGwei, setL2GasPriceGwei],
              ["ETH price (USD)", ethPriceUsd, setEthPriceUsd],
            ].map(([label, value, setter]) => (
              <label key={label} className="text-sm text-slate-400">
                {label}
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={value}
                  onChange={(event) => setter(event.target.value)}
                  className="mt-2 w-full rounded-xl border border-slate-700 bg-[#0b1020] px-3 py-2 text-white"
                />
              </label>
            ))}
          </div>

          <button
            type="button"
            onClick={calculateCosts}
            disabled={costLoading}
            className="mt-4 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50"
          >
            {costLoading ? "Đang tính..." : "Tính chi phí"}
          </button>

          {costs.length > 0 && (
            <>
              <div className="mt-5 overflow-x-auto">
                <table className="w-full min-w-[650px] text-left text-sm">
                  <thead className="bg-slate-800/60 text-slate-400">
                    <tr>
                      <th className="p-3">Mô hình</th>
                      <th className="p-3">L1 ước tính</th>
                      <th className="p-3">L2 execution ước tính</th>
                    </tr>
                  </thead>
                  <tbody>
                    {costs.map((cost) => (
                      <tr
                        key={cost.evaluationModel}
                        className="border-t border-slate-800"
                      >
                        <td className="p-3">
                          {MODEL_NAMES[cost.evaluationModel] ??
                            cost.evaluationModel}
                        </td>
                        <td className="p-3">
                          {formatUsd(cost.l1CostUsd)}
                        </td>
                        <td className="p-3">
                          {formatUsd(cost.l2ExecutionCostUsd)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <p className="mt-3 text-xs text-amber-300">
                L2 chỉ tính execution fee; BE chưa tính phí đăng dữ liệu
                rollup lên L1.
              </p>
            </>
          )}
        </section>
      )}
    </div>
  );
}