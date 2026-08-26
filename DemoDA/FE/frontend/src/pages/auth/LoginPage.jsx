import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginApi } from "../../api/authApi";
function LoginPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");

    try {
      setLoading(true);

      const response = await loginApi(formData);
      const data = response?.data;

      const token = data?.token;
      const username = data?.username;
      const role = data?.role;

      if (token) {
        localStorage.setItem("token", token);
      }

      if (username) {
        localStorage.setItem("username", username);
      }

      if (role) {
        localStorage.setItem("role", role);
      }

      setMessage(data?.message || "Login successful!");

      setTimeout(() => {
        navigate("/dashboard");
      }, 800);
    } catch (err) {
      console.error("Login error:", err);

      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Login failed. Please check your credentials."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#101622] text-slate-100 flex flex-col overflow-x-hidden">
      <header className="fixed top-0 z-50 w-full h-16 px-6 flex items-center justify-between border-b border-slate-800/50 bg-[#101622]/50 backdrop-blur-sm">
        <div className="flex items-center gap-2">
          <span className="text-xl font-bold tracking-tighter text-blue-500">
            Shapphire
          </span>
        </div>
      </header>

      <main className="relative flex flex-1 items-center justify-center pt-16 px-4">
        <div className="absolute left-1/2 top-1/2 -z-10 h-[700px] w-[700px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-blue-600/5 blur-[120px]" />

        <div className="w-full max-w-md rounded-xl border border-slate-800/40 bg-[#101622] p-8 shadow-2xl">
          <div className="mb-8 text-center">
            <h2 className="mb-2 text-2xl font-bold tracking-tight">
              Login to Access
            </h2>
            <p className="text-sm text-slate-400">
              Enter your credentials to continue to the dashboard.
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-5">
            <div className="space-y-1.5">
              <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">
                Username
              </label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-slate-400">
                  person
                </span>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="Enter username"
                  className="w-full rounded-lg bg-[#141b29] py-3 pl-10 pr-4 text-sm text-white placeholder:text-slate-500 outline-none ring-1 ring-transparent transition-all focus:ring-blue-500/50"
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-slate-400">
                Password
              </label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-slate-400">
                  lock
                </span>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Enter password"
                  className="w-full rounded-lg bg-[#141b29] py-3 pl-10 pr-4 text-sm text-white placeholder:text-slate-500 outline-none ring-1 ring-transparent transition-all focus:ring-blue-500/50"
                  required
                />
              </div>
            </div>

            {message && (
              <div className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-400">
                {message}
              </div>
            )}

            {error && (
              <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 py-3.5 font-bold text-white transition-all hover:bg-blue-600/90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
            >
              <span>{loading ? "Logging in..." : "Login"}</span>
              <span className="material-symbols-outlined text-lg">
                login
              </span>
            </button>

            <div className="flex items-center justify-center gap-2 text-sm">
              <span className="text-slate-400">Don&apos;t have an account?</span>
              <Link to="/register" className="font-bold text-blue-500 hover:underline">
                Register now
              </Link>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}

export default LoginPage;