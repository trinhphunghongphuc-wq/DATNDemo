import { Layout } from "@/components/Layout";
import {
  Bell,
  Settings,
  Search,
  Package,
  AlertTriangle,
  Leaf,
  Link2,
  Truck,
  Wifi,
  ClipboardCheck,
} from "lucide-react";

const STATS = [
  {
    label: "Active Shipments",
    value: "1,284",
    trend: "+5.2%",
    trendLabel: "from last month",
    positive: true,
    Icon: Truck,
    iconColor: "text-blue-400",
    iconBg: "bg-blue-500/10",
  },
  {
    label: "Verified Blocks",
    value: "45.2k",
    trend: "+12.8%",
    trendLabel: "uptime 99.9%",
    positive: true,
    Icon: Link2,
    iconColor: "text-blue-400",
    iconBg: "bg-blue-500/10",
  },
  {
    label: "System Alerts",
    value: "12",
    trend: "-3.1%",
    trendLabel: "resolved today",
    positive: false,
    Icon: AlertTriangle,
    iconColor: "text-orange-400",
    iconBg: "bg-orange-500/10",
  },
  {
    label: "CO2 Savings",
    value: "2.4k tons",
    trend: "-0.5%",
    trendLabel: "vs benchmark",
    positive: false,
    Icon: Leaf,
    iconColor: "text-green-400",
    iconBg: "bg-green-500/10",
  },
];

const TRANSACTIONS = [
  {
    timestamp: ["2023-10-24", "14:22:01"],
    txHash: "0x88f....e922",
    eventType: "Batch Picked Up",
    EventIcon: Package,
    iconColor: "text-slate-400",
    origin: "Rotterdam Port",
    destination: "Munich Center",
    status: "VERIFIED",
  },
  {
    timestamp: ["2023-10-24", "13:58:44"],
    txHash: "0x2a1....440c",
    eventType: "Sensor Update",
    EventIcon: Wifi,
    iconColor: "text-slate-400",
    origin: "In Transit (Sea)",
    destination: "N/A",
    status: "PROCESSING",
  },
  {
    timestamp: ["2023-10-24", "12:15:10"],
    txHash: "0xbc3....98dd",
    eventType: "Customs Clearance",
    EventIcon: ClipboardCheck,
    iconColor: "text-slate-400",
    origin: "Singapore Terminal",
    destination: "Port of LA",
    status: "VERIFIED",
  },
  {
    timestamp: ["2023-10-24", "11:30:00"],
    txHash: "0x442....12a1",
    eventType: "Seal Tampered",
    EventIcon: AlertTriangle,
    iconColor: "text-orange-400",
    origin: "Checkpoint 4",
    destination: "N/A",
    status: "REVIEW NEEDED",
  },
];

const STATUS_STYLES: Record<string, string> = {
  VERIFIED: "bg-green-500/10 text-green-400 border border-green-500/20",
  PROCESSING: "bg-blue-500/10 text-blue-400 border border-blue-500/20",
  "REVIEW NEEDED": "bg-orange-500/10 text-orange-500 border border-orange-500/20",
};

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex flex-col items-center justify-center px-2 py-1 rounded text-[10px] font-bold tracking-wider text-center leading-tight ${STATUS_STYLES[status] ?? "bg-slate-500/10 text-slate-400"}`}
    >
      {status === "REVIEW NEEDED" ? (
        <>
          <span>REVIEW</span>
          <span>NEEDED</span>
        </>
      ) : (
        status
      )}
    </span>
  );
}

function WorldMap() {
  return (
    <svg
      viewBox="0 0 1000 500"
      className="w-full h-full"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect width="1000" height="500" fill="#111A2B" />

      {/* Subtle grid */}
      {[100, 200, 300, 400].map((y) => (
        <line key={y} x1="0" y1={y} x2="1000" y2={y} stroke="#1E2D42" strokeWidth="0.8" />
      ))}
      {[100, 200, 300, 400, 500, 600, 700, 800, 900].map((x) => (
        <line key={x} x1={x} y1="0" x2={x} y2="500" stroke="#1E2D42" strokeWidth="0.8" />
      ))}

      {/* Continents */}
      <g fill="#1A2B42" stroke="#2A3D58" strokeWidth="1.2" strokeLinejoin="round">
        {/* Greenland */}
        <polygon points="268,8 330,4 360,26 354,56 318,66 280,60 256,40" />
        {/* North America */}
        <polygon points="62,55 355,28 375,98 358,200 296,256 244,275 216,262 198,245 220,210 238,180 232,160 188,152 138,168 86,196 62,180 46,145 46,90" />
        {/* Central America */}
        <polygon points="235,262 250,290 244,312 232,305 228,278" />
        {/* South America */}
        <polygon points="240,310 285,285 336,294 365,335 370,392 350,442 310,470 270,472 244,447 228,407 232,354 246,318" />
        {/* Iceland */}
        <polygon points="356,68 374,62 386,72 384,86 370,92 354,84" />
        {/* UK */}
        <polygon points="404,84 418,76 434,80 440,96 430,112 416,116 402,104" />
        {/* Scandinavia */}
        <polygon points="468,46 494,26 520,30 528,52 518,68 498,72 474,68" />
        {/* Europe */}
        <polygon points="430,52 484,42 530,56 542,80 534,118 512,148 482,165 456,168 432,155 420,132 422,98" />
        {/* Iberian Peninsula */}
        <polygon points="416,132 440,125 452,148 442,172 418,175 404,158" />
        {/* Africa */}
        <polygon points="428,170 492,158 538,180 560,218 564,274 550,336 526,382 500,422 466,434 432,418 406,374 396,310 398,248 412,206" />
        {/* Middle East */}
        <polygon points="532,148 574,140 612,150 624,172 618,200 596,212 562,214 538,202 526,182" />
        {/* Asia main */}
        <polygon points="538,50 650,34 764,38 864,56 926,96 954,148 942,202 912,234 860,258 800,270 740,268 680,250 626,226 597,202 576,180 563,156 549,126 543,96 539,74" />
        {/* India */}
        <polygon points="608,208 644,202 660,220 662,254 650,282 624,300 602,290 588,264 594,232" />
        {/* Indochina / SE Asia */}
        <polygon points="700,262 732,255 758,272 754,316 738,340 714,336 696,316 692,286" />
        {/* Japan */}
        <polygon points="862,115 878,106 894,110 900,128 892,147 874,153 858,143 852,127" />
        {/* Australia */}
        <polygon points="708,318 786,304 850,320 890,348 900,396 880,440 838,462 788,464 744,444 712,416 697,374 700,338" />
        {/* New Zealand */}
        <polygon points="914,378 926,364 940,368 944,386 936,406 920,412 908,397" />
      </g>

      {/* Route lines */}
      <path
        d="M 430 207 Q 534 165 638 260"
        stroke="#3B82F6"
        strokeWidth="2"
        strokeDasharray="9,5"
        fill="none"
        opacity="0.75"
      />
      <path
        d="M 638 260 Q 718 238 800 298"
        stroke="#3B82F6"
        strokeWidth="1.8"
        strokeDasharray="9,5"
        fill="none"
        opacity="0.45"
      />

      {/* Blue dot – Active Shipment */}
      <circle cx="430" cy="207" r="14" fill="#3B82F6" opacity="0.12" />
      <circle cx="430" cy="207" r="7" fill="#3B82F6" />
      <circle cx="430" cy="207" r="3" fill="white" opacity="0.85" />

      {/* Green dot – Verified & Stored */}
      <circle cx="638" cy="260" r="14" fill="#22C55E" opacity="0.12" />
      <circle cx="638" cy="260" r="7" fill="#22C55E" />
      <circle cx="638" cy="260" r="3" fill="white" opacity="0.85" />

      {/* Orange dot – Security Alert */}
      <circle cx="800" cy="298" r="14" fill="#F59E0B" opacity="0.12" />
      <circle cx="800" cy="298" r="7" fill="#F59E0B" />
      <circle cx="800" cy="298" r="3" fill="white" opacity="0.85" />

      {/* Legend box */}
      <rect x="16" y="415" width="168" height="78" rx="8" fill="#0D1624" opacity="0.96" />
      <rect x="16" y="415" width="168" height="78" rx="8" fill="none" stroke="#1E2D42" strokeWidth="1" />
      <circle cx="36" cy="437" r="5" fill="#3B82F6" />
      <text x="48" y="442" fill="#94A3B8" fontSize="11.5" fontFamily="Inter, system-ui, sans-serif">Active Shipment</text>
      <circle cx="36" cy="460" r="5" fill="#22C55E" />
      <text x="48" y="465" fill="#94A3B8" fontSize="11.5" fontFamily="Inter, system-ui, sans-serif">Verified &amp; Stored</text>
      <circle cx="36" cy="483" r="5" fill="#F59E0B" />
      <text x="48" y="488" fill="#94A3B8" fontSize="11.5" fontFamily="Inter, system-ui, sans-serif">Security Alert</text>
    </svg>
  );
}

export default function Index() {
  return (
    <Layout>
      {/* Top header */}
      <header className="hidden lg:flex items-center gap-4 px-6 py-4 border-b border-white/[0.06] flex-shrink-0">
        <h1 className="text-white font-bold text-xl flex-shrink-0">Supply Chain Overview</h1>

        {/* Search */}
        <div className="flex-1 max-w-lg mx-6">
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
            <input
              type="text"
              placeholder="Search Shipment ID, Batch ID, or Tx Hash..."
              className="w-full bg-[#1A2538] border border-white/[0.08] rounded-lg pl-9 pr-4 py-2 text-sm text-slate-300 placeholder-slate-500 focus:outline-none focus:border-blue-500/50 focus:ring-1 focus:ring-blue-500/20 transition-colors"
            />
          </div>
        </div>

        {/* Right actions */}
        <div className="flex items-center gap-2 ml-auto flex-shrink-0">
          <button className="w-9 h-9 flex items-center justify-center rounded-lg border border-white/[0.08] text-slate-400 hover:text-white hover:border-white/20 transition-colors">
            <Bell size={16} />
          </button>
          <button className="w-9 h-9 flex items-center justify-center rounded-lg border border-white/[0.08] text-slate-400 hover:text-white hover:border-white/20 transition-colors">
            <Settings size={16} />
          </button>
          <div className="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/[0.08] ml-1">
            <span className="w-2 h-2 rounded-full bg-green-400 flex-shrink-0 animate-pulse" />
            <div className="text-right">
              <div className="text-green-400 text-[11px] font-semibold leading-none mb-0.5">Node Connected</div>
              <div className="text-blue-400 text-[10px] font-mono leading-none">0x4a2...9b1c</div>
            </div>
          </div>
        </div>
      </header>

      {/* Main scrollable content */}
      <main className="flex-1 overflow-y-auto p-4 md:p-6 space-y-4 md:space-y-5">
        {/* Mobile search */}
        <div className="lg:hidden relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            placeholder="Search Shipment ID, Batch ID, or Tx Hash..."
            className="w-full bg-[#1A2538] border border-white/[0.08] rounded-lg pl-9 pr-4 py-2.5 text-sm text-slate-300 placeholder-slate-500 focus:outline-none focus:border-blue-500/50"
          />
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 md:gap-4">
          {STATS.map(({ label, value, trend, trendLabel, positive, Icon, iconColor, iconBg }) => (
            <div
              key={label}
              className="bg-[#162030] border border-white/[0.06] rounded-xl p-4 md:p-5"
            >
              <div className="flex items-start justify-between mb-3 gap-2">
                <span className="text-slate-400 text-xs md:text-sm leading-tight">{label}</span>
                <div className={`w-8 h-8 rounded-lg ${iconBg} flex items-center justify-center flex-shrink-0`}>
                  <Icon size={15} className={iconColor} />
                </div>
              </div>
              <div className="text-white font-extrabold text-xl md:text-2xl mb-1 leading-none">
                {value}
              </div>
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className={`text-xs font-semibold ${positive ? "text-green-400" : "text-red-400"}`}>
                  {trend}
                </span>
                <span className="text-slate-500 text-xs">{trendLabel}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Global Logistics Tracking */}
        <div className="bg-[#162030] border border-white/[0.06] rounded-xl overflow-hidden">
          <div className="flex items-start justify-between px-5 py-4 gap-3">
            <div className="min-w-0">
              <h2 className="text-white font-semibold text-base">Global Logistics Tracking</h2>
              <p className="text-slate-500 text-xs mt-0.5">Real-time blockchain-verified shipment nodes</p>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
              <button className="px-3 md:px-4 py-1.5 rounded-lg border border-white/[0.12] text-slate-300 text-sm hover:border-white/25 hover:text-white transition-colors">
                Filter
              </button>
              <button className="px-3 md:px-4 py-1.5 rounded-lg bg-blue-600 text-white text-sm font-medium hover:bg-blue-500 transition-colors">
                Live Feed
              </button>
            </div>
          </div>
          <div className="h-56 sm:h-72 md:h-80 lg:h-[340px]">
            <WorldMap />
          </div>
        </div>

        {/* Transaction Ledger */}
        <div className="bg-[#162030] border border-white/[0.06] rounded-xl overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-white/[0.06]">
            <h2 className="text-white font-semibold text-base">Immutable Transaction Ledger</h2>
            <button className="text-blue-400 text-sm hover:text-blue-300 transition-colors whitespace-nowrap">
              View All →
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px]">
              <thead>
                <tr className="border-b border-white/[0.06]">
                  {["Timestamp", "Tx Hash", "Event Type", "Origin", "Destination", "Status"].map(
                    (h) => (
                      <th
                        key={h}
                        className="text-left px-5 py-3 text-slate-500 text-[11px] font-medium uppercase tracking-wider"
                      >
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {TRANSACTIONS.map((tx, i) => (
                  <tr
                    key={i}
                    className="border-b border-white/[0.04] last:border-0 hover:bg-white/[0.02] transition-colors"
                  >
                    <td className="px-5 py-4 text-slate-400 text-xs font-mono leading-relaxed">
                      <div>{tx.timestamp[0]}</div>
                      <div>{tx.timestamp[1]}</div>
                    </td>
                    <td className="px-5 py-4">
                      <span className="text-blue-400 text-xs font-mono">{tx.txHash}</span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <tx.EventIcon size={14} className={tx.iconColor} />
                        <span className="text-slate-300 text-sm whitespace-nowrap">{tx.eventType}</span>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-slate-400 text-sm whitespace-nowrap">
                      {tx.origin}
                    </td>
                    <td className="px-5 py-4 text-slate-400 text-sm whitespace-nowrap">
                      {tx.destination}
                    </td>
                    <td className="px-5 py-4">
                      <StatusBadge status={tx.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </Layout>
  );
}
