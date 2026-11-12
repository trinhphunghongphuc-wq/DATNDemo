import { useCallback, useEffect, useState } from "react";
import axiosClient from "../api/axiosClient";

export default function useBatchSearch(endpoint) {
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState({ keyword: "", status: "", page: 1 });
  const [refreshKey, setRefreshKey] = useState(0);
  const [snapshot, setSnapshot] = useState({ key: null, items: [], totalPages: 0, totalElements: 0, error: "" });
  const requestKey = JSON.stringify([endpoint, query, refreshKey]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQuery((current) => current.keyword === keyword.trim()
        ? current : { ...current, keyword: keyword.trim(), page: 1 });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  useEffect(() => {
    const controller = new AbortController();
    axiosClient.get(endpoint, {
      params: { keyword: query.keyword, status: query.status, page: query.page - 1, size: 10 },
      signal: controller.signal,
    }).then(({ data }) => {
      if (controller.signal.aborted) return;
      if (!Array.isArray(data.content)) throw new Error("Invalid batch page");
      // A receive/status change may remove the last item from a filtered page.
      if (query.page > Math.max(data.totalPages, 1)) {
        setQuery((current) => current === query
          ? { ...current, page: Math.max(data.totalPages, 1) } : current);
        return;
      }
      setSnapshot({ key: requestKey, items: data.content,
        totalPages: data.totalPages, totalElements: data.totalElements, error: "" });
    }).catch((error) => {
      if (!controller.signal.aborted) {
        setSnapshot({ key: requestKey, items: [], totalPages: 0, totalElements: 0,
          error: error.response?.data?.message || "Không tải được danh sách lô hàng. Vui lòng thử lại." });
      }
    });
    return () => controller.abort();
  }, [endpoint, query, requestKey]);

  const setStatus = useCallback((status) => setQuery((current) => ({ ...current, status, page: 1 })), []);
  const setPage = useCallback((page) => setQuery((current) => ({ ...current, page })), []);
  const reload = useCallback(({ firstPage = false } = {}) => {
    if (firstPage) setQuery((current) => ({ ...current, page: 1 }));
    setRefreshKey((current) => current + 1);
  }, []);
  const clear = useCallback(() => {
    setKeyword("");
    setQuery({ keyword: "", status: "", page: 1 });
  }, []);

  return { ...snapshot, loading: snapshot.key !== requestKey,
    keyword, status: query.status, page: query.page,
    setKeyword, setStatus, setPage, reload, clear };
}
