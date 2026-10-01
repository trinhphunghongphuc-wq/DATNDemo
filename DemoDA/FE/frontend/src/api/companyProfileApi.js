import axiosClient from "./axiosClient";

export function getMyCompanyProfile() {
  return axiosClient.get("/company-profile");
}

export function updateMyCompanyProfile(data) {
  return axiosClient.put("/company-profile", data);
}