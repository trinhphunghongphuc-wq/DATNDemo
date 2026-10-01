import axiosClient from "./axiosClient";

export function getPartnerDirectory(role) {
  return axiosClient.get("/producer/partners/directory", {
    params: { role },
  });
}

export function getMyPartners(role) {
  return axiosClient.get("/producer/partners", {
    params: { role },
  });
}

export function addPartner(partnerId) {
  return axiosClient.post("/producer/partners", {
    partnerId,
  });
}

export function removePartner(partnerId) {
  return axiosClient.delete(`/producer/partners/${partnerId}`);
}