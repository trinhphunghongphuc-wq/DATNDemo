import axiosClient from "../api/axiosClient";
import { readSseEvents } from "./sseEvents";

// Native EventSource cannot attach our Bearer header. Fetch keeps JWT out of URLs.
export async function consumeNotificationStream(token, signal, onEvent) {
  const response = await fetch(
    `${axiosClient.defaults.baseURL.replace(/\/$/, "")}/notifications/stream`,
    { headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" }, signal }
  );
  if (!response.ok) {
    const error = new Error(`Notification stream: HTTP ${response.status}`);
    error.status = response.status;
    throw error;
  }
  if (!response.body) throw new Error("Trình duyệt không hỗ trợ thông báo trực tiếp.");
  await readSseEvents(response.body, signal, onEvent);
}
