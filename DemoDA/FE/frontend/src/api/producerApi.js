import axiosClient from "./axiosClient";

export const getProducerBatches = () =>
  axiosClient.get("/producer/batches");

export const getProducerBatch = (id) =>
  axiosClient.get(`/producer/batches/${id}`);

export const createProducerBatch = (body) =>
  axiosClient.post("/producer/batches", body);

export const addProducerRecords = (id, records) =>
  axiosClient.post(`/producer/batches/${id}/records`, records);

export const updateProducerExpiry = (id, expiryDate) =>
  axiosClient.patch(`/producer/batches/${id}/expiry-date`, {
    expiryDate,
  });

export const verifyProducerBatch = (id) =>
  axiosClient.get(`/producer/batches/${id}/verify-all`);

export const getBatchRecords = (id) =>
  axiosClient.get(`/batches/${id}`);

export const getProductCategories = () =>
  axiosClient.get("/product-categories");