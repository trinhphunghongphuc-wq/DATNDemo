import { useEffect, useMemo, useState } from "react";
import { getAdminBatches } from "../../api/adminApi";

export default function DashboardPage() {
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchName, setSearchName] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const filteredBatches = batches.filter((batch) =>
    batch.name?.toLowerCase().includes(searchName.toLowerCase())
  );

  const totalPages = Math.ceil(filteredBatches.length / itemsPerPage);

  const startIndex = (currentPage - 1) * itemsPerPage;

  const currentBatches = filteredBatches.slice(
    startIndex,
    startIndex + itemsPerPage
  );

  const handleSearchChange = (e) => {
    setSearchName(e.target.value);
    setCurrentPage(1);
  };

  const fetchBatches = async () => {
    try {
      setLoading(true);
      const res = await getAdminBatches();
      setBatches(res.data || []);
    } catch (error) {
      console.error("Load batches error:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBatches();
  }, []);

  const totalBatches = batches.length;

  const anchoredBatches = useMemo(() => {
    return batches.filter(
      (batch) =>
        batch.anchorStatus === "ANCHORED" ||
        batch.chainTxHash ||
        batch.merkleRoot
    ).length;
  }, [batches]);

  const notAnchoredBatches = useMemo(() => {
    return batches.filter(
      (batch) =>
        batch.anchorStatus === "NOT_ANCHORED" &&
        !batch.chainTxHash
    ).length;
  }, [batches]);

  const totalRecords = useMemo(() => {
    return batches.reduce(
      (total, batch) => total + (batch.recordCount || 0),
      0
    );
  }, [batches]);

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";

    return new Date(dateString).toLocaleString("vi-VN", {
      second: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getStatusBadge = (status) => {
    const styles = {
      ASSIGNED_TO_DISTRIBUTOR:
        "bg-blue-500/10 text-blue-400 border-blue-500/20",
      CREATED:
        "bg-slate-500/10 text-slate-300 border-slate-500/20",
      IN_TRANSIT:
        "bg-orange-500/10 text-orange-400 border-orange-500/20",
      COMPLETED:
        "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    };

    return styles[status] || "bg-slate-500/10 text-slate-300 border-slate-500/20";
  };

  const getAnchorBadge = (anchorStatus) => {
    if (anchorStatus === "ANCHORED") {
      return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";
    }

    return "bg-orange-500/10 text-orange-400 border-orange-500/20";
  };

  return (
    <div className="space-y-6">
      <section className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div className="rounded-2xl border border-slate-800 bg-[#111827] p-6">
          <p className="text-sm text-slate-400">Total Batches</p>
          <h2 className="mt-3 text-3xl font-bold text-white">
            {totalBatches}
          </h2>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-[#111827] p-6">
          <p className="text-sm text-slate-400">Anchored Batches</p>
          <h2 className="mt-3 text-3xl font-bold text-emerald-400">
            {totalBatches - notAnchoredBatches}
          </h2>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-[#111827] p-6">
          <p className="text-sm text-slate-400">Not Anchored</p>
          <h2 className="mt-3 text-3xl font-bold text-orange-400">
            {notAnchoredBatches}
          </h2>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-[#111827] p-6">
          <p className="text-sm text-slate-400">Total Records</p>
          <h2 className="mt-3 text-3xl font-bold text-blue-400">
            {totalRecords}
          </h2>
        </div>
      </section>

      <div className="flex items-center justify-between border-b border-slate-800 p-6">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Blockchain Batch Overview
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Danh sách lô hàng và trạng thái ghi nhận blockchain.
          </p>
        </div>

        <input
          type="text"
          value={searchName}
          onChange={handleSearchChange}
          placeholder="Search batch name..."
          className="w-72 rounded-xl border border-slate-700 bg-[#0b1020] px-4 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-blue-500"
        />
      </div>

      <section className="overflow-hidden rounded-2xl border border-slate-800 bg-[#111827]">
  



        {loading ? (
          <div className="p-6 text-slate-400">Đang tải batches...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-[#1a2235] text-slate-400">
                <tr>
                  <th className="px-6 py-4">Name</th>
                  <th className="px-6 py-4">Created At</th>
                  <th className="px-6 py-4">Merkle Root</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Anchor Status</th>
                  <th className="px-6 py-4 text-center">Record Count</th>

                </tr>
              </thead>

              <tbody className="divide-y divide-slate-800">
                {currentBatches.map((batch) => (
                  <tr key={batch.id} className="hover:bg-slate-800/40">
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-semibold text-white">
                          {batch.name || "N/A"}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Batch ID: #{batch.id}
                        </p>
                      </div>
                    </td>

                    <td className="divide-y divide-slate-800">
                      {formatDate(batch.createdAt)}
                    </td>

                    <td className="px-6 py-4">
                      <span
                        title={batch.merkleRoot}
                        className="font-mono text-xs text-cyan-400"
                      >
                        {batch.merkleRoot
                          ? `${batch.merkleRoot.slice(0, 6)} ... ${batch.merkleRoot.slice(-6)}`
                          : "N/A"}
                      </span>
                    </td>

                    <td className="px-6 py-4">
                      <span
                        className={`rounded-full border px-3 py-1 text-xs font-bold ${getStatusBadge(
                          batch.status
                        )}`}
                      >
                        {batch.status || "UNKNOWN"}
                      </span>
                    </td>

                    <td className="px-6 py-4">
                      <span
                        className={`rounded-full border px-3 py-1 text-xs font-bold ${getAnchorBadge(
                          batch.anchorStatus
                        )}`}
                      >
                        {batch.anchorStatus || "UNKNOWN"}
                      </span>
                    </td>

                    <td className="px-6 py-4 text-center">
                      <span className="rounded-lg bg-blue-500/10 px-3 py-1 text-sm font-bold text-blue-400">
                        {batch.recordCount || 0}
                      </span>
                    </td>

                  </tr>
                ))}
              </tbody>
            </table>

            {filteredBatches.length === 0 && (
              <div className="p-6 text-center text-slate-400">
                Không tìm thấy batch phù hợp.
              </div>

            )}
            {filteredBatches.length > 0 && (
              <div className="flex items-center justify-between border-t border-slate-800 px-6 py-4">
                <p className="text-sm text-slate-400">
                  Showing {startIndex + 1} -{" "}
                  {Math.min(startIndex + itemsPerPage, filteredBatches.length)} of{" "}
                  {filteredBatches.length} batches
                </p>

                <div className="flex items-center gap-2">
                  <button
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((prev) => prev - 1)}
                    className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-300 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Prev
                  </button>

                  {Array.from({ length: totalPages }, (_, index) => {
                    const page = index + 1;

                    return (
                      <button
                        key={page}
                        onClick={() => setCurrentPage(page)}
                        className={`rounded-lg px-3 py-1.5 text-sm font-semibold ${currentPage === page
                            ? "bg-blue-600 text-white"
                            : "border border-slate-700 text-slate-300 hover:bg-slate-800"
                          }`}
                      >
                        {page}
                      </button>
                    );
                  })}

                  <button
                    disabled={currentPage === totalPages}
                    onClick={() => setCurrentPage((prev) => prev + 1)}
                    className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-300 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}