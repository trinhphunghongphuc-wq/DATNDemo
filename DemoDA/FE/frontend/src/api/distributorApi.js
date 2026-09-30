import axiosClient from "./axiosClient";

export const getDistributorBatches = () =>
  axiosClient.get("/distributor/batches");

export const receiveDistributorBatch = (batchId) =>
  axiosClient.post(`/distributor/batches/${batchId}/receive`);

export const getTransportJourney = (batchId) =>
  axiosClient.get(`/transport/batches/${batchId}/journey`);

export const planTransportRoute = (batchId, coordinates) =>
  axiosClient.post(`/transport/batches/${batchId}/route`, coordinates);

export const getDistributorBatchDetail = (batchId) =>
  axiosClient.get(`/distributor/batches/${batchId}`);

export const getVehicles = () =>
  axiosClient.get("/distributor/vehicles");

export const createVehicle = (data) =>
  axiosClient.post("/distributor/vehicles", data);