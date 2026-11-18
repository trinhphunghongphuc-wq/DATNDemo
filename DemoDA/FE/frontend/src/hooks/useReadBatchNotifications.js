import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useNotifications } from "../contexts/notificationsContext";

export default function useReadBatchNotifications(batchId, loadedBatchId) {
  const { key, state } = useLocation();
  const { markBatchRead } = useNotifications();
  const fromNotification = state?.fromNotification;

  useEffect(() => {
    // The inbox already marked notifications through the selected item.
    if (fromNotification || loadedBatchId == null || String(loadedBatchId) !== batchId) return;
    markBatchRead(batchId).catch(() => {});
  }, [batchId, loadedBatchId, fromNotification, key, markBatchRead]);
}
