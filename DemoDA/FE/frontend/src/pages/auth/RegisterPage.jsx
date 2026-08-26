import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerApi } from "../../api/authApi";

function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    role: "",
  });

  const [agreed, setAgreed] = useState(false);
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

  const handleRegister = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");

    if (!formData.role) {
      setError("Vui lòng chọn role.");
      return;
    }

    if (!agreed) {
      setError("Bạn cần xác nhận điều khoản trước khi đăng ký.");
      return;
    }

    try {
      setLoading(true);
      const response = await registerApi(formData);

      setMessage(
        response?.data?.message || response?.data || "Register successful!"
      );

      setTimeout(() => {
        navigate("/login");
      }, 1000);
    } catch (err) {
      console.error("Register error:", err);
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Register failed. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen overflow-x-hidden bg-background text-on-surface flex flex-col">
      <header className="fixed top-0 z-50 flex h-16 w-full items-center justify-between border-b border-slate-800/50 bg-[#101622]/50 px-6 backdrop-blur-sm">
        <div className="flex items-center gap-2">
          <span className="font-headline text-xl font-bold tracking-tighter text-blue-500">
            Shapphire
          </span>
        </div>

        <div className="flex items-center gap-4">
          {/* <a
            href="#"
            className="rounded-lg px-3 py-1 text-sm font-medium text-slate-400 transition-colors hover:bg-blue-500/10"
          >
            Documentation
          </a> */}
        </div>
      </header>

      <main className="relative flex flex-1 items-center justify-center overflow-hidden px-4 pt-16">
        <div className="absolute left-1/2 top-1/2 -z-10 h-[800px] w-[800px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/5 blur-[120px]" />

        <div className="grid w-full max-w-5xl overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-2xl lg:grid-cols-2">
          <div className="relative hidden flex-col justify-between bg-surface-container-low p-12 lg:flex">
            <div className="relative z-10 space-y-6">
              <div className="inline-flex items-center gap-2 rounded-full border border-tertiary/20 bg-tertiary/10 px-3 py-1">
                <span className="h-2 w-2 rounded-full bg-tertiary shadow-[0_0_8px_rgba(16,185,129,0.2)]" />
                <span className="text-[10px] font-bold uppercase tracking-widest text-tertiary">
                  System Online
                </span>
              </div>

              <h1 className="text-4xl font-bold tracking-tight leading-tight text-on-surface">
                The Infrastructure of <br />
                <span className="text-primary">Immutable Trust.</span>
              </h1>

              <p className="max-w-sm text-sm leading-relaxed text-on-surface-variant">
                Join the global network of verified suppliers and logistics nodes.
                Sapphire provides industrial-grade transparency for the
                modern supply chain.
              </p>
            </div>

            <div className="relative z-10">
              <div className="grid grid-cols-2 gap-4">
                <div className="rounded-lg border border-outline-variant/20 bg-surface-container-high p-4">
                  <span className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                    Nodes Active
                  </span>
                  <span className="font-headline text-2xl font-bold">14,802</span>
                </div>

                <div className="rounded-lg border border-outline-variant/20 bg-surface-container-high p-4">
                  <span className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">
                    Daily Txns
                  </span>
                  <span className="font-headline text-2xl font-bold">2.4M</span>
                </div>
              </div>
            </div>

            <div
              className="pointer-events-none absolute inset-0 opacity-10"
              style={{
                backgroundImage:
                  "radial-gradient(circle, #334155 1px, transparent 1px)",
                backgroundSize: "24px 24px",
              }}
            />

            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDty0uldGi6lJfxrgbgZ0WYtgRUNK5Y96XG1ZQwPfG_wl3JZ4yZD42m0OAOhFmQAv3OiFXTww-D3r3IOWxR5GVtSS8thQFTM3rV85kT6d_DAugi6TShnrosHTFmzVGAjQCk6MWkWMswLJzn3LVBGhB4oC2d23nPTQnNq3xV3waPHYXjSF0CajC3YQ6p_NUuGoyySRpXl9D5iuyovirO3fuVoSuZ8Ni3SkBJ7qblvt_59LF6z3H9n5oX-dpjZ-OEM6mrp610O8GFSbcI"
              alt="Blockchain visual"
              className="pointer-events-none absolute bottom-0 right-0 w-2/3 grayscale opacity-20"
            />
          </div>

          <div className="flex flex-col justify-center bg-surface p-8 lg:p-12">
            <div className="mb-8">
              <h2 className="mb-2 text-2xl font-bold tracking-tight text-on-surface">
                Initialize Account
              </h2>
              <p className="text-sm text-on-surface-variant">
                Enter your credentials to access the Sovereign Ledger.
              </p>
            </div>

            <form onSubmit={handleRegister} className="space-y-5">
              <div className="space-y-1.5">
                <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                  Username
                </label>
                <div className="relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-on-surface-variant">
                    person
                  </span>
                  <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    placeholder="e.g. node_operator_01"
                    className="w-full rounded-lg border-none bg-surface-container-low py-3 pl-10 pr-4 text-sm text-on-surface outline-none transition-all placeholder:text-slate-600 focus:ring-1 focus:ring-primary/50"
                    required
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                  Email Address
                </label>
                <div className="relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-on-surface-variant">
                    alternate_email
                  </span>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="customer@example.com"
                    className="w-full rounded-lg border-none bg-surface-container-low py-3 pl-10 pr-4 text-sm text-on-surface outline-none transition-all placeholder:text-slate-600 focus:ring-1 focus:ring-primary/50"
                    required
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                  Password
                </label>
                <div className="relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-on-surface-variant">
                    lock
                  </span>
                  <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="••••••••••••"
                    className="w-full rounded-lg border-none bg-surface-container-low py-3 pl-10 pr-4 text-sm text-on-surface outline-none transition-all placeholder:text-slate-600 focus:ring-1 focus:ring-primary/50"
                    required
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="px-1 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                  System Role
                </label>
                <div className="relative">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg text-on-surface-variant">
                    account_tree
                  </span>

                  <select
                    name="role"
                    value={formData.role}
                    onChange={handleChange}
                    className="w-full appearance-none cursor-pointer rounded-lg border-none bg-surface-container-low py-3 pl-10 pr-10 text-sm text-on-surface outline-none transition-all focus:ring-1 focus:ring-primary/50"
                    required
                  >
                    <option value="" disabled>
                      Select your entity role...
                    </option>
                    <option value="PRODUCER">Producer</option>
                    <option value="DISTRIBUTOR">Distributor</option>
                    <option value="RETAILER">Retailer</option>
                    <option value="CONSUMER">Consumer</option>
                  </select>

                  <span className="material-symbols-outlined pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant">
                    expand_more
                  </span>
                </div>
              </div>

              <div className="flex items-start gap-3 py-2">
                <div className="flex h-5 items-center">
                  <input
                    type="checkbox"
                    checked={agreed}
                    onChange={(e) => setAgreed(e.target.checked)}
                    className="h-4 w-4 rounded border-outline-variant bg-surface-container-low text-primary focus:ring-primary/20"
                  />
                </div>

                <label className="text-[11px] leading-relaxed text-on-surface-variant">
                  I acknowledge that all transactions and identity registrations
                  are recorded immutably on the Sapphire ledger.
                </label>
              </div>

              {message && (
                <div className="rounded-lg border border-tertiary/30 bg-tertiary/10 px-4 py-3 text-sm text-tertiary">
                  {message}
                </div>
              )}

              {error && (
                <div className="rounded-lg border border-error/30 bg-error/10 px-4 py-3 text-sm text-red-300">
                  {error}
                </div>
              )}

              <div className="space-y-4 pt-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-3.5 font-bold text-on-primary transition-all hover:bg-primary/90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <span>{loading ? "Registering..." : "Register Node"}</span>
                  <span className="material-symbols-outlined text-lg">
                    arrow_forward
                  </span>
                </button>

                <div className="flex items-center justify-center gap-2 text-sm">
                  <span className="text-on-surface-variant">
                    Already an operator?
                  </span>
                  <Link to="/login" className="font-bold text-primary hover:underline">
                    Login to Access
                  </Link>
                </div>
              </div>
            </form>

  
          </div>
        </div>
      </main>

      <footer className="p-6 text-center text-[10px] uppercase tracking-widest text-on-surface-variant opacity-50">
        © 2026 Sapphire Logistics Core. All Hashes Verified.
      </footer>
    </div>
  );
}

export default RegisterPage;