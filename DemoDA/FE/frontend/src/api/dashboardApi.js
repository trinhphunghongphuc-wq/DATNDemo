import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/dashboard";

export const getDashboardSummary = () => {
  const token = localStorage.getItem("token");

  return axios.get(`${API_BASE_URL}/summary`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};