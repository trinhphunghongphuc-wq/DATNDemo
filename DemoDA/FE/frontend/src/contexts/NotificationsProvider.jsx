import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axiosClient, { expireSession } from "../api/axiosClient";
import { consumeNotificationStream } from "../services/notificationStream";
import { NotificationsContext } from "./notificationsContext";

export default function NotificationsProvider({ children }) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [revision, setRevision] = useState(0);
  const [connected, setConnected] = useState(false);
  const countRequest = useRef(0);
  const token = localStorage.getItem("token");
  const role = (localStorage.getItem("role") || "").replace(/^ROLE_/, "");
  const supported = ["ADMIN", "PRODUCER", "DISTRIBUTOR", "RETAILER"].includes(role);

  const refresh = useCallback(async () => {
    const request = ++countRequest.current;
    const response = await axiosClient.get("/notifications/unread-count");
    if (request === countRequest.current) {
      setUnreadCount(response.data.unreadCount);
      setRevision((previous) => previous + 1);
    }
  }, []);

  useEffect(() => {
    if (!token || !supported) return;
    const controller = new AbortController();
    let retry = 1000;
    let wake;
    let retryTimer;
    const abortWait = () => { window.clearTimeout(retryTimer); wake?.(); };
    controller.signal.addEventListener("abort", abortWait);

    async function connect() {
      while (!controller.signal.aborted) {
        try {
          await consumeNotificationStream(token, controller.signal, (event) => {
            if (controller.signal.aborted) return;
            setConnected(true);
            retry = 1000;
            if (event === "ready" || event === "notifications-changed") {
              refresh().catch(() => { /* next reconnect reconciles persisted data */ });
            }
          });
        } catch (error) {
          if (controller.signal.aborted) return;
          if (error.status === 401) { expireSession(); return; }
          if (error.status === 403) { setConnected(false); return; }
        }
        if (controller.signal.aborted) return;
        setConnected(false);
        await new Promise((resolve) => {
          wake = resolve;
          retryTimer = window.setTimeout(resolve, retry);
        });
        retry = Math.min(retry * 2, 15000);
      }
    }

    const reconcile = () => { refresh().catch(() => {}); };
    reconcile();
    connect();
    // Also recover missed messages or a proxy temporarily buffering the stream.
    const reconcileTimer = window.setInterval(reconcile, 30000);
    window.addEventListener("focus", reconcile);
    return () => {
      controller.abort();
      controller.signal.removeEventListener("abort", abortWait);
      window.clearInterval(reconcileTimer);
      window.removeEventListener("focus", reconcile);
    };
  }, [token, supported, refresh]);

  const markRead = useCallback(async (id) => {
    const response = await axiosClient.patch(`/notifications/${id}/read`);
    // A confirmed read must still navigate even if the subsequent counter request fails.
    await refresh().catch(() => {});
    return response.data;
  }, [refresh]);

  const value = useMemo(() => ({ unreadCount, revision, connected, markRead, refresh }),
    [unreadCount, revision, connected, markRead, refresh]);

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}
