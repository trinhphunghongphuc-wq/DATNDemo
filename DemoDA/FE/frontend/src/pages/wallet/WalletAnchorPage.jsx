import { useEffect, useState } from "react";
import axiosClient from "../../api/axiosClient";

const panel = "space-y-4 rounded-xl border border-slate-800 bg-[#111827] p-5";
const button = "rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold disabled:opacity-40";
const input = "w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2";

async function connectWallet() {
  const provider = window.ethereum;
  if (!provider?.request) throw new Error("Hãy mở trang bằng trình duyệt có ví Ethereum, ví dụ MetaMask.");
  const [account] = await provider.request({ method: "eth_requestAccounts" });
  if (!account) throw new Error("Chưa chọn tài khoản trong ví.");
  return { provider, account };
}

export default function WalletAnchorPage() {
  const [wallet, setWallet] = useState(null);
  const [batchId, setBatchId] = useState("");
  const [request, setRequest] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get("/wallet-anchors/wallet", { signal: controller.signal })
      .then(({ data }) => setWallet(data.walletAddress ?? ""))
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.response?.data?.message ?? err.message);
      });
    return () => controller.abort();
  }, []);

  async function perform(action) {
    setError("");
    setNotice("");
    setBusy(true);
    try {
      await action();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
    } finally {
      setBusy(false);
    }
  }

  function saveWallet() {
    perform(async () => {
      const { account } = await connectWallet();
      const { data } = await axiosClient.put("/wallet-anchors/wallet", { walletAddress: account });
      setWallet(data.walletAddress);
      setNotice("Đã lưu địa chỉ ví cho tài khoản.");
    });
  }

  function loadRequest(event) {
    event.preventDefault();
    setRequest(null);
    perform(async () => {
      const { data } = await axiosClient.get(`/wallet-anchors/batches/${batchId}/signing-request`);
      setRequest(data);
    });
  }

  function signRoot() {
    perform(async () => {
      const { provider, account } = await connectWallet();
      if (account.toLowerCase() !== request.walletAddress.toLowerCase()) {
        throw new Error(`Hãy chọn đúng ví đã gán cho lô: ${request.walletAddress}`);
      }
      const network = await provider.request({ method: "eth_chainId" });
      if (BigInt(network) !== BigInt(request.chainId)) {
        throw new Error(`Hãy chuyển ví sang mạng chain ID ${request.chainId}.`);
      }
      const signature = await provider.request({
        method: "eth_signTypedData_v4",
        params: [account, JSON.stringify(request.typedData)],
      });
      await axiosClient.post(`/wallet-anchors/batches/${batchId}/approval`, {
        deadline: request.deadline,
        signature,
      });
      setRequest(null);
      setNotice("Đã gửi chữ ký. Admin có thể gửi giao dịch anchor.");
    });
  }

  return (
    <section className="mx-auto max-w-3xl space-y-5">
      <h1 className="text-2xl font-bold">Ví và ký anchor</h1>
      {error && <p className="text-sm text-red-300" role="alert">{error}</p>}
      {notice && <p className="text-sm text-emerald-300" role="status">{notice}</p>}
      <div className={panel}>
        <h2 className="font-semibold">Ví của tài khoản</h2>
        <p className="break-all text-sm text-slate-300">{wallet ?? "Đang tải…"}</p>
        {wallet === "" && <p className="text-sm text-slate-400">Chưa lưu địa chỉ ví.</p>}
        <button type="button" className={button} disabled={busy || wallet === null} onClick={saveWallet}>
          Kết nối và lưu ví
        </button>
        <p className="text-xs text-slate-400">Địa chỉ mới áp dụng cho các lô chưa được Admin gán ví.</p>
      </div>
      <form className={panel} onSubmit={loadRequest}>
        <label className="block space-y-2 text-sm">
          <span>ID lô đã được Admin gán ví</span>
          <input className={input} inputMode="numeric" pattern="[1-9][0-9]*" required value={batchId}
            disabled={busy}
            onChange={(event) => { setBatchId(event.target.value); setRequest(null); }} />
        </label>
        <button type="submit" className={button} disabled={busy}>Tải root cần ký</button>
      </form>
      {request && (
        <div className={panel}>
          <h2 className="font-semibold">Lô #{batchId} · {request.stage}</h2>
          <p className="break-all text-sm text-slate-300">Ví được gán {request.walletAddress}</p>
          <p className="break-all font-mono text-sm text-slate-300">{request.typedData.message.root}</p>
          <p className="text-xs text-slate-400">
            Chữ ký hết hạn lúc {new Date(request.deadline * 1000).toLocaleString("vi-VN")}.
          </p>
          <button type="button" className={button} disabled={busy} onClick={signRoot}>Ký Merkle root</button>
        </div>
      )}
    </section>
  );
}
