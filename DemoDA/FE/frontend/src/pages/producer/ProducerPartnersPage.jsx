import { useEffect, useState } from "react";
import {
  addPartner,
  getMyPartners,
  getPartnerDirectory,
  removePartner,
} from "../../api/partnerApi";

const ROLES = [
  { value: "DISTRIBUTOR", label: "Đơn vị vận chuyển" },
  { value: "RETAILER", label: "Đơn vị bán lẻ" },
];

function errorMessage(error) {
  return (
    error?.response?.data?.message ||
    "Không thể thực hiện thao tác. Vui lòng thử lại."
  );
}

export default function ProducerPartnersPage() {
  const [role, setRole] = useState("DISTRIBUTOR");
  const [directory, setDirectory] = useState([]);
  const [myPartners, setMyPartners] = useState([]);
  const [keyword, setKeyword] = useState("");

  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");
      setDirectory([]);
      setMyPartners([]);

      try {
        const [directoryResponse, myPartnersResponse] =
          await Promise.all([
            getPartnerDirectory(role),
            getMyPartners(role),
          ]);

        if (!cancelled) {
          setDirectory(directoryResponse.data ?? []);
          setMyPartners(myPartnersResponse.data ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          setError(errorMessage(err));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [role]);

  async function refreshPartners() {
    const [directoryResponse, myPartnersResponse] =
      await Promise.all([
        getPartnerDirectory(role),
        getMyPartners(role),
      ]);

    setDirectory(directoryResponse.data ?? []);
    setMyPartners(myPartnersResponse.data ?? []);
  }

  async function handleAdd(partner) {
    setBusyId(partner.id);
    setError("");
    setNotice("");

    try {
      await addPartner(partner.id);
      await refreshPartners();
      setNotice(`Đã thêm ${partner.companyName} vào danh sách đối tác.`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleRemove(partner) {
    setBusyId(partner.id);
    setError("");
    setNotice("");

    try {
      await removePartner(partner.id);
      await refreshPartners();
      setNotice(`Đã bỏ ${partner.companyName} khỏi danh sách đối tác.`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  const activeIds = new Set(
    myPartners.map((partner) => partner.id)
  );

  const normalizedKeyword = keyword.trim().toLowerCase();

  const availablePartners = directory.filter((partner) => {
    if (activeIds.has(partner.id)) {
      return false;
    }

    if (!normalizedKeyword) {
      return true;
    }

    return (
      partner.companyName
        ?.toLowerCase()
        .includes(normalizedKeyword) ||
      partner.companyAddress
        ?.toLowerCase()
        .includes(normalizedKeyword)
    );
  });

  return (
    <div className="space-y-6 text-slate-100">
      <header>
        <p className="text-sm font-semibold text-blue-400">
          PRODUCER / ĐỐI TÁC
        </p>
        <h1 className="mt-1 text-2xl font-bold">
          Quản lý đối tác
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Thêm đơn vị vận chuyển và bán lẻ trước khi chỉ định
          họ nhận lô hàng.
        </p>
      </header>

      <div className="flex flex-wrap gap-2">
        {ROLES.map((item) => (
          <button
            key={item.value}
            type="button"
            disabled={busyId !== null}
            onClick={() => {
              setRole(item.value);
              setKeyword("");
              setNotice("");
            }}
            className={`rounded-xl px-4 py-2 text-sm font-semibold disabled:opacity-50 ${
              role === item.value
                ? "bg-blue-600 text-white"
                : "border border-slate-700 text-slate-300 hover:bg-slate-800"
            }`}
          >
            {item.label}
          </button>
        ))}
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

      {loading ? (
        <p className="text-sm text-slate-400">
          Đang tải danh sách đối tác...
        </p>
      ) : (
        <div className="grid gap-5 xl:grid-cols-2">
          <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
            <h2 className="text-lg font-semibold">
              Đối tác của tôi ({myPartners.length})
            </h2>

            {myPartners.length === 0 ? (
              <p className="mt-4 text-sm text-slate-400">
                Chưa có đối tác thuộc nhóm này.
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {myPartners.map((partner) => (
                  <article
                    key={partner.id}
                    className="rounded-xl border border-slate-700 p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h3 className="font-semibold">
                          {partner.companyName}
                        </h3>
                        <p className="mt-1 text-sm text-slate-400">
                          {partner.companyAddress}
                        </p>
                        <p className="mt-2 text-xs text-slate-500">
                          Tài khoản #{partner.id}
                        </p>
                      </div>

                      <button
                        type="button"
                        disabled={busyId !== null}
                        onClick={() => handleRemove(partner)}
                        className="rounded-lg border border-red-500/50 px-3 py-2 text-xs font-semibold text-red-300 hover:bg-red-500/10 disabled:opacity-50"
                      >
                        {busyId === partner.id
                          ? "Đang xử lý..."
                          : "Bỏ đối tác"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-2xl border border-slate-800 bg-[#111827] p-5">
            <h2 className="text-lg font-semibold">
              Tìm đối tác đã đăng ký
            </h2>

            <input
              type="search"
              value={keyword}
              onChange={(event) =>
                setKeyword(event.target.value)
              }
              placeholder="Tìm theo tên công ty hoặc địa chỉ..."
              className="mt-4 w-full rounded-lg border border-slate-700 bg-[#0b1020] p-3 text-sm"
            />

            {availablePartners.length === 0 ? (
              <p className="mt-4 text-sm text-slate-400">
                Không có tài khoản phù hợp. Distributor hoặc
                Retailer cần cập nhật tên công ty và địa chỉ
                trước khi xuất hiện tại đây.
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {availablePartners.map((partner) => (
                  <article
                    key={partner.id}
                    className="rounded-xl border border-slate-700 p-4"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <h3 className="font-semibold">
                          {partner.companyName}
                        </h3>
                        <p className="mt-1 text-sm text-slate-400">
                          {partner.companyAddress}
                        </p>
                        <p className="mt-2 text-xs text-slate-500">
                          Tài khoản #{partner.id}
                        </p>
                      </div>

                      <button
                        type="button"
                        disabled={busyId !== null}
                        onClick={() => handleAdd(partner)}
                        className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-semibold hover:bg-blue-500 disabled:opacity-50"
                      >
                        {busyId === partner.id
                          ? "Đang xử lý..."
                          : "Thêm đối tác"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}