import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/auth";

export const registerApi = (data) => {
  return axios.post(`${API_BASE_URL}/register`, data);
};

export const loginApi = (data) => {
  return axios.post(`${API_BASE_URL}/login`, data);
};