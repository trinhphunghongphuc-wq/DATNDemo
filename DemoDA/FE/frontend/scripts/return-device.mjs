// Local demo device. The private key stays in the user's key file.
import { generateKeyPairSync, sign } from "node:crypto";
import { readFile, writeFile } from "node:fs/promises";

const api = (process.env.CHAINTRACK_API ?? "http://localhost:8080/api").replace(/\/$/, "");
const token = process.env.CHAINTRACK_TOKEN;

async function call(path, data) {
  const res = await fetch(api + path, {
    method: data === undefined ? "GET" : "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: data === undefined ? undefined : JSON.stringify(data),
    signal: AbortSignal.timeout(15000),
  });
  const result = await res.json();
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${result.message ?? res.statusText}`);
  return result;
}

function id(value, label) {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed <= 0) throw new Error(`${label} không hợp lệ.`);
  return parsed;
}

function number(value, label) {
  if (!value?.trim() || !Number.isFinite(Number(value))) throw new Error(`${label} không hợp lệ.`);
  return Number(value);
}

async function main() {
  const [command, target, keyFile, ...values] = process.argv.slice(2);
  if (!token || !keyFile || !["register", "send"].includes(command)) {
    throw new Error("Đặt CHAINTRACK_TOKEN. Dùng register <vehicleId> <keyFile> hoặc send <batchId> <keyFile> <latitude> <longitude> <temperature> <humidity>.");
  }
  if (command === "register") {
    const vehicleId = id(target, "Vehicle ID");
    const pair = generateKeyPairSync("ed25519", {
      privateKeyEncoding: { type: "pkcs8", format: "pem" },
      publicKeyEncoding: { type: "spki", format: "der" },
    });
    await writeFile(keyFile, pair.privateKey, { flag: "wx", mode: 0o600 });
    try {
      await call(`/distributor/vehicles/${vehicleId}/demo-public-key`, {
        publicKey: pair.publicKey.toString("base64"),
      });
    } catch (error) {
      throw new Error(`Không đăng ký được public key. File demo đã tạo tại ${keyFile}. ${error.message}`);
    }
    console.log(`Đã đăng ký public key cho xe #${vehicleId}.`);
    return;
  }

  const batchId = id(target, "Batch ID");
  const detail = await call(`/return-cases/${batchId}`);
  if (detail.summary.status !== "IN_TRANSIT") throw new Error("Chuyến hoàn trả chưa chạy hoặc đã kết thúc.");
  const vehicles = await call("/distributor/vehicles");
  const vehicle = vehicles.find((item) => item.id === detail.vehicleId);
  if (!vehicle) throw new Error("Không tìm thấy xe của chuyến hoàn trả.");

  const payload = JSON.stringify({
    batchId,
    tripId: detail.tripId,
    vehicleId: vehicle.id,
    sequence: (vehicle.lastSequence ?? 0) + 1,
    measuredAt: new Date().toISOString(),
    latitude: number(values[0], "Vĩ độ"),
    longitude: number(values[1], "Kinh độ"),
    temperature: number(values[2], "Nhiệt độ"),
    humidity: number(values[3], "Độ ẩm"),
  });
  const privateKey = await readFile(keyFile, "utf8");
  const signature = sign(null, Buffer.from(payload, "utf8"), privateKey).toString("base64");
  const result = await call(`/return-cases/${batchId}/telemetry`, { payload, signature });
  console.log(`Backend đã nhận điểm IoT #${result.id}, sequence ${result.details.reading.sequence}.`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
