import axiosClient from "./axiosClient";

export const getAdminBatches = () => {
  return axiosClient.get("/admin/batches");
};

export const getAdminRecords = () => {
  return axiosClient.get("/admin/records");
};

export const getAdminUsers = () => {
  return axiosClient.get("/admin/users");
};