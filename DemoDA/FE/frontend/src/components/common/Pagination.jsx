export default function Pagination({
  page,
  totalPages,
  onPageChange,
}) {
  if (totalPages <= 1) return null;

  return (
    <nav
      aria-label="Phân trang"
      className="mt-6 flex items-center justify-center gap-3"
    >
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        className="rounded-lg border px-3 py-2 disabled:opacity-40"
      >
        Trước
      </button>

      <span className="text-sm">
        Trang {page} / {totalPages}
      </span>

      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onPageChange(page + 1)}
        className="rounded-lg border px-3 py-2 disabled:opacity-40"
      >
        Sau
      </button>
    </nav>
  );
}