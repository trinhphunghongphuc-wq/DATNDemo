import axios from "axios";

const axiosClient = axios.create({
  baseURL: "http://localhost:8080/api",
});

export function expireSession() {
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("role");

  if (window.location.pathname !== "/login") {
    window.location.replace("/login?reason=expired");
  }
}

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  const isPublic =
    /^\/(traceability|products)(\/|$)/.test(config.url ?? "");

  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isPublic =
      /^\/(traceability|products)(\/|$)/.test(
        error.config?.url ?? ""
      );

    if (
      error.response?.status === 401 &&
      localStorage.getItem("token") &&
      !isPublic
    ) {
      expireSession();
    }

    return Promise.reject(error);
  }
);

export default axiosClient;