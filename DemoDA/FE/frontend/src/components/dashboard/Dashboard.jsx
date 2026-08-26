const stats = [
  {
    title: "Active Shipments",
    value: "1,284",
    status: "+5.2%",
  },
  {
    title: "Verified Blocks",
    value: "45.2k",
    status: "+12.8%",
  },
  {
    title: "System Alerts",
    value: "12",
    status: "-3.1%",
  },
  {
    title: "CO2 Savings",
    value: "2.4k tons",
    status: "-0.5%",
  },
];

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Stats */}
      <div className="grid grid-cols-4 gap-5">
        {stats.map((item, index) => (
          <div
            key={index}
            className="bg-[#141b2d] border border-[#1f2937]
            rounded-2xl p-6"
          >
            <p className="text-gray-400 text-sm">
              {item.title}
            </p>

            <h2 className="text-3xl font-bold mt-3">
              {item.value}
            </h2>

            <p className="text-green-400 text-sm mt-2">
              {item.status}
            </p>
          </div>
        ))}
      </div>

      {/* Tracking */}
      <div className="bg-[#141b2d] border border-[#1f2937]
      rounded-2xl p-6 h-[400px]">
        <h2 className="text-2xl font-bold">
          Global Logistics Tracking
        </h2>

        <p className="text-gray-400 mt-2">
          Real-time blockchain-verified shipment nodes
        </p>

        <div className="h-[300px] mt-6 rounded-xl bg-[#2a3142] flex items-center justify-center text-gray-400">
          Blockchain Map Here
        </div>
      </div>

      {/* Table */}
      <div className="bg-[#141b2d] border border-[#1f2937]
      rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-[#1f2937]">
          <h2 className="text-2xl font-bold">
            Immutable Transaction Ledger
          </h2>
        </div>

        <table className="w-full">
          <thead className="bg-[#1a2235] text-gray-400">
            <tr>
              <th className="text-left px-6 py-4">Timestamp</th>
              <th className="text-left px-6 py-4">Tx Hash</th>
              <th className="text-left px-6 py-4">Event</th>
              <th className="text-left px-6 py-4">Origin</th>
              <th className="text-left px-6 py-4">Destination</th>
              <th className="text-left px-6 py-4">Status</th>
            </tr>
          </thead>

          <tbody>
            <tr className="border-t border-[#1f2937]">
              <td className="px-6 py-4">
                2025-01-01
              </td>

              <td className="px-6 py-4 text-blue-400">
                0x21a...440c
              </td>

              <td className="px-6 py-4">
                Batch Picked Up
              </td>

              <td className="px-6 py-4">
                Rotterdam Port
              </td>

              <td className="px-6 py-4">
                Munich Center
              </td>

              <td className="px-6 py-4">
                <span className="bg-green-500/20 text-green-400 px-3 py-1 rounded-full text-sm">
                  VERIFIED
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}