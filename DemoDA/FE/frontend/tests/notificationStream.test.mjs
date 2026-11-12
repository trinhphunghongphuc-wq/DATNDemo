import test from "node:test";
import assert from "node:assert/strict";
import { readSseEvents } from "../src/services/sseEvents.js";

function stream(chunks) {
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(new TextEncoder().encode(chunk));
      controller.close();
    },
  });
}

test("reassembles split events and handles multiple events per network chunk", async () => {
  const seen = [];
  await readSseEvents(stream([
    "event: rea", "dy\ndata: refresh\n", "\nevent: notifications-changed\ndata: refresh\n\n",
  ]), new AbortController().signal, (event) => seen.push(event));
  assert.deepEqual(seen, ["ready", "notifications-changed"]);
});

test("handles CRLF split across chunks and ignores SSE comments", async () => {
  const seen = [];
  await readSseEvents(stream([
    ": keepalive\r\n\r\nevent: heartbeat\r", "\ndata: refresh\r\n\r", "\n",
  ]), new AbortController().signal, (event) => seen.push(event));
  assert.deepEqual(seen, ["heartbeat"]);
});

test("does not dispatch a truncated event when the connection ends", async () => {
  const seen = [];
  await readSseEvents(stream(["event: notifications-changed\ndata: ref"]),
    new AbortController().signal, (event) => seen.push(event));
  assert.deepEqual(seen, []);
});

test("an already aborted connection dispatches nothing and releases its reader", async () => {
  const controller = new AbortController();
  controller.abort();
  const body = stream(["event: ready\ndata: refresh\n\n"]);
  await readSseEvents(body, controller.signal, () => assert.fail("Should not receive events"));
  assert.equal(body.locked, false);
});
