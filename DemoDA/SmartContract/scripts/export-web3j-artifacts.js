import fs from "node:fs";
import path from "node:path";

const artifactPath = path.resolve(
  "artifacts/contracts/TraceabilityRegistry.sol/TraceabilityRegistry.json"
);

const outputDirectory = path.resolve(
  "build/web3j"
);

const artifact = JSON.parse(
  fs.readFileSync(artifactPath, "utf8")
);

fs.mkdirSync(outputDirectory, {
  recursive: true,
});

fs.writeFileSync(
  path.join(
    outputDirectory,
    "TraceabilityRegistry.abi"
  ),
  JSON.stringify(artifact.abi),
  "utf8"
);

fs.writeFileSync(
  path.join(
    outputDirectory,
    "TraceabilityRegistry.bin"
  ),
  artifact.bytecode.replace(/^0x/, ""),
  "utf8"
);

console.log(
  "Exported ABI and BIN to:",
  outputDirectory
);