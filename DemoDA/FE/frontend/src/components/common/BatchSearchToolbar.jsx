import { useId } from "react";
import { Search } from "lucide-react";

const states = [
  ["CREATED", "Mới tạo"],
  ["IN_PRODUCTION", "Đang sản xuất"],
  ["ASSIGNED_TO_DISTRIBUTOR", "Đã phân công nhà phân phối"],
  ["RECEIVED_BY_DISTRIBUTOR", "Nhà phân phối đã nhận"],
  ["IN_DISTRIBUTION", "Đang vận chuyển"],
  ["DELIVERED_TO_RETAILER", "Chờ nhà bán lẻ nhận"],
  ["AT_RETAIL", "Đang bán lẻ"],
  ["SOLD_OUT", "Đã bán hết"],
  ["DELIVERY_REJECTED", "Nhà bán lẻ từ chối"],
  ["RETURNED_TO_DISTRIBUTOR", "Đã nhận hàng hoàn trả"],
];

export default function BatchSearchToolbar({ list, role }) {
  const id = useId();
  const filtered = Boolean(list.keyword.trim() || list.status);
  const available = role === "PRODUCER" ? states
    : states.filter(([value]) => !["CREATED", "IN_PRODUCTION"].includes(value));
  return (
    <div className="mb-4 space-y-3">
      <div className="flex flex-wrap gap-3">
        <div className="relative min-w-[180px] flex-1">
          <label htmlFor={`${id}-keyword`} className="sr-only">Tìm theo mã hoặc tên lô hàng</label>
          <Search size={17} aria-hidden="true" className="pointer-events-none absolute left-3 top-3 text-slate-400" />
          <input id={`${id}-keyword`} type="search" value={list.keyword} maxLength={200}
            onChange={(event) => list.setKeyword(event.target.value)}
            placeholder="Tìm mã hoặc tên lô…"
            className="w-full rounded-lg border border-slate-700 bg-[#0b1020] py-2.5 pl-10 pr-3 text-sm text-slate-100 placeholder:text-slate-500 focus:border-blue-500 focus:outline-none" />
        </div>
        <div className="min-w-[180px] flex-1">
          <label htmlFor={`${id}-status`} className="sr-only">Lọc theo trạng thái</label>
          <select id={`${id}-status`} value={list.status} onChange={(event) => list.setStatus(event.target.value)}
            className="w-full rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2.5 text-sm text-slate-100">
            <option value="">Tất cả trạng thái</option>
            {available.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400">
        <p role="status" aria-live="polite">
          {list.loading ? "Đang tải…" : `${list.totalElements} lô hàng · Mới nhất trước`}
        </p>
        <div className="flex gap-3">
          {filtered && <button type="button" onClick={list.clear} className="text-blue-400 hover:underline">Xóa bộ lọc</button>}
          <button type="button" onClick={() => list.reload()} className="text-blue-400 hover:underline">Tải lại</button>
        </div>
      </div>
    </div>
  );
}
