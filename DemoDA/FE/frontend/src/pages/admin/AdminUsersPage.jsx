import { useEffect, useMemo, useState } from "react";
import { Search, Users } from "lucide-react";
import axiosClient from "../../api/axiosClient";

const ROLES = [
  "ADMIN",
  "PRODUCER",
  "DISTRIBUTOR",
  "RETAILER",
  "CONSUMER",
];

const formatDate = (value) =>
  value ? new Date(value).toLocaleString("vi-VN") : "—";

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [roleDrafts, setRoleDrafts] = useState({});
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const currentUsername = localStorage.getItem("username");

  useEffect(() => {
    async function load() {
      try {
        const response = await axiosClient.get("/admin/users");
        const items = response.data ?? [];
        setUsers(items);
        setRoleDrafts(
          Object.fromEntries(items.map((user) => [user.id, user.role]))
        );
      } catch (err) {
        setError(
          err.response?.data?.message ??
            "Không tải được danh sách người dùng."
        );
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return users;

    return users.filter(
      (user) =>
        user.username?.toLowerCase().includes(q) ||
        user.role?.toLowerCase().includes(q) ||
        String(user.id).includes(q)
    );
  }, [users, keyword]);

  function updateRow(updated) {
    setUsers((current) =>
      current.map((user) => (user.id === updated.id ? updated : user))
    );
    setRoleDrafts((current) => ({
      ...current,
      [updated.id]: updated.role,
    }));
  }

  async function saveRole(user) {
    const role = roleDrafts[user.id];
    if (!role || role === user.role) return;

    if (
      !window.confirm(
        `Đổi vai trò của ${user.username} từ ${user.role} sang ${role}?`
      )
    ) {
      return;
    }

    setSavingId(user.id);
    setError("");
    setNotice("");

    try {
      const response = await axiosClient.put(
        `/admin/users/${user.id}/role`,
        { role }
      );
      updateRow(response.data);
      setNotice(`Đã đổi vai trò của ${user.username}.`);
    } catch (err) {
      setError(err.response?.data?.message ?? "Không đổi được vai trò.");
    } finally {
      setSavingId(null);
    }
  }

  async function toggleStatus(user) {
    const nextEnabled = !user.enabled;
    const action = nextEnabled ? "kích hoạt" : "khóa";

    if (!window.confirm(`${action} tài khoản ${user.username}?`)) {
      return;
    }

    setSavingId(user.id);
    setError("");
    setNotice("");

    try {
      const response = await axiosClient.put(
        `/admin/users/${user.id}/status`,
        { enabled: nextEnabled }
      );
      updateRow(response.data);
      setNotice(`Đã ${action} tài khoản ${user.username}.`);
    } catch (err) {
      setError(err.response?.data?.message ?? "Không đổi được trạng thái.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <div className="space-y-6 text-slate-100">
      <header>
        <p className="text-sm font-semibold text-blue-400">
          ADMIN / NGƯỜI DÙNG
        </p>
        <h1 className="mt-1 text-2xl font-bold">Quản lý người dùng</h1>
        <p className="mt-1 text-sm text-slate-400">
          Xem tài khoản, phân quyền và trạng thái hoạt động.
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

      <section className="overflow-hidden rounded-2xl border border-slate-800 bg-[#111827]">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 p-5">
          <div className="flex items-center gap-2">
            <Users size={19} className="text-blue-400" />
            <h2 className="font-semibold">Tài khoản ({filtered.length})</h2>
          </div>

          <div className="flex items-center gap-2 rounded-xl border border-slate-700 bg-[#0b1020] px-3">
            <Search size={16} className="text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm username, role hoặc ID"
              className="w-64 max-w-full bg-transparent py-2 text-sm outline-none placeholder:text-slate-500"
            />
          </div>
        </div>

        {loading ? (
          <p className="p-5 text-sm text-slate-400">Đang tải...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="bg-slate-800/60 text-slate-400">
                <tr>
                  <th className="px-5 py-4">Tài khoản</th>
                  <th className="px-5 py-4">Vai trò</th>
                  <th className="px-5 py-4">Trạng thái</th>
                  <th className="px-5 py-4">Ngày tạo</th>
                  <th className="px-5 py-4">Thao tác</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-800">
                {filtered.map((user) => {
                  const isSelf = user.username === currentUsername;
                  const busy = savingId !== null;
                  const roleChanged = roleDrafts[user.id] !== user.role;

                  return (
                    <tr key={user.id}>
                      <td className="px-5 py-4">
                        <strong>{user.username}</strong>
                        {isSelf && (
                          <span className="ml-2 text-xs text-blue-400">
                            (Bạn)
                          </span>
                        )}
                        <p className="mt-1 text-xs text-slate-500">
                          ID #{user.id}
                        </p>
                      </td>

                      <td className="px-5 py-4">
                        <select
                          value={roleDrafts[user.id] ?? user.role}
                          onChange={(event) =>
                            setRoleDrafts((current) => ({
                              ...current,
                              [user.id]: event.target.value,
                            }))
                          }
                          disabled={isSelf || busy}
                          className="rounded-lg border border-slate-700 bg-[#0b1020] px-3 py-2 disabled:opacity-50"
                        >
                          {ROLES.map((role) => (
                            <option key={role} value={role}>
                              {role}
                            </option>
                          ))}
                        </select>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={
                            user.enabled
                              ? "text-emerald-400"
                              : "text-red-400"
                          }
                        >
                          {user.enabled ? "Hoạt động" : "Đã khóa"}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-slate-400">
                        {formatDate(user.createdAt)}
                      </td>

                      <td className="space-x-2 px-5 py-4">
                        <button
                          type="button"
                          onClick={() => saveRole(user)}
                          disabled={isSelf || busy || !roleChanged}
                          className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-medium hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          Lưu role
                        </button>
                        <button
                          type="button"
                          onClick={() => toggleStatus(user)}
                          disabled={isSelf || busy}
                          className="rounded-lg border border-slate-700 px-3 py-2 text-xs hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          {user.enabled ? "Khóa" : "Kích hoạt"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {filtered.length === 0 && (
              <p className="p-5 text-sm text-slate-400">
                Không có tài khoản phù hợp.
              </p>
            )}
          </div>
        )}
      </section>
    </div>
  );
}