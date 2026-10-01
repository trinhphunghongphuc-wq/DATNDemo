import { useEffect, useState } from "react";
import {
  getMyCompanyProfile,
  updateMyCompanyProfile,
} from "../../api/companyProfileApi";

function messageOf(error) {
  return (
    error?.response?.data?.message ||
    "Không thể thực hiện thao tác. Vui lòng thử lại."
  );
}

export default function CompanyProfilePage() {
  const [form, setForm] = useState({
    companyName: "",
    companyAddress: "",
  });
  const [role, setRole] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      try {
        const response = await getMyCompanyProfile();

        if (cancelled) return;

        setForm({
          companyName: response.data?.companyName ?? "",
          companyAddress: response.data?.companyAddress ?? "",
        });
        setRole(response.data?.role ?? "");
      } catch (err) {
        if (!cancelled) {
          setError(messageOf(err));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const companyName = form.companyName.trim();
    const companyAddress = form.companyAddress.trim();

    if (!companyName || !companyAddress) {
      setError("Vui lòng nhập tên công ty và địa chỉ.");
      return;
    }

    setSaving(true);

    try {
      const response = await updateMyCompanyProfile({
        companyName,
        companyAddress,
      });

      setForm({
        companyName: response.data.companyName,
        companyAddress: response.data.companyAddress,
      });
      setRole(response.data.role);
      setNotice(
        "Đã lưu hồ sơ. Producer có thể tìm thấy công ty trong danh bạ đối tác."
      );
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setSaving(false);
    }
  }

  const hasProfile =
    form.companyName.trim() !== "" &&
    form.companyAddress.trim() !== "";

  if (loading) {
    return (
      <p className="text-sm text-slate-400">
        Đang tải hồ sơ công ty...
      </p>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6 text-slate-100">
      <header>
        <p className="text-sm font-semibold text-blue-400">
          {role || "DOANH NGHIỆP"} / HỒ SƠ
        </p>
        <h1 className="mt-1 text-2xl font-bold">
          Hồ sơ công ty
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Tên và địa chỉ này sẽ hiển thị trong danh bạ để
          Producer tìm và thêm bạn làm đối tác.
        </p>
      </header>

      <div
        className={`rounded-xl border p-4 text-sm ${
          hasProfile
            ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
            : "border-amber-500/30 bg-amber-500/10 text-amber-300"
        }`}
      >
        {hasProfile
          ? "Hồ sơ đã có tên công ty và địa chỉ."
          : "Bạn cần điền thông tin trước khi xuất hiện trong danh bạ đối tác."}
      </div>

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

      <form
        onSubmit={handleSubmit}
        className="space-y-5 rounded-2xl border border-slate-800 bg-[#111827] p-6"
      >
        <div>
          <label
            htmlFor="company-name"
            className="block text-sm font-medium"
          >
            Tên công ty <span className="text-red-400">*</span>
          </label>
          <input
            id="company-name"
            type="text"
            required
            maxLength={200}
            value={form.companyName}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                companyName: event.target.value,
              }))
            }
            placeholder="Ví dụ: Công ty Vận tải Nông sản Đà Lạt"
            className="mt-2 w-full rounded-lg border border-slate-700 bg-[#0b1020] p-3"
          />
        </div>

        <div>
          <label
            htmlFor="company-address"
            className="block text-sm font-medium"
          >
            Địa chỉ công ty <span className="text-red-400">*</span>
          </label>
          <textarea
            id="company-address"
            required
            maxLength={500}
            rows={3}
            value={form.companyAddress}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                companyAddress: event.target.value,
              }))
            }
            placeholder="Ví dụ: 25 Trần Hưng Đạo, Đà Lạt, Lâm Đồng"
            className="mt-2 w-full rounded-lg border border-slate-700 bg-[#0b1020] p-3"
          />
        </div>

        <button
          type="submit"
          disabled={saving}
          className="rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold hover:bg-blue-500 disabled:opacity-50"
        >
          {saving ? "Đang lưu..." : "Lưu hồ sơ công ty"}
        </button>
      </form>
    </div>
  );
}