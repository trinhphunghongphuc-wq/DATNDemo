import fs from "node:fs";
import path from "node:path";

const contractNames = [
  "TraceabilityRegistry",
  "CertificationBenchmark"
];

const projectRoot = process.cwd();
const outputDirectory = path.join(
  projectRoot,
  "web3j-artifacts"
);

fs.mkdirSync(outputDirectory, {
  recursive: true
});

for (const contractName of contractNames) {
  const artifactPath = path.join(
    projectRoot,
    "artifacts",
    "contracts",
    `${contractName}.sol`,
    `${contractName}.json`
  );

  const artifact = JSON.parse(
    fs.readFileSync(artifactPath, "utf8")
  );

  const abiPath = path.join(
    outputDirectory,
    `${contractName}.abi`
  );

  const binPath = path.join(
    outputDirectory,
    `${contractName}.bin`
  );

  fs.writeFileSync(
    abiPath,
    JSON.stringify(artifact.abi)
  );

  fs.writeFileSync(
    binPath,
    artifact.bytecode.replace(/^0x/, "")
  );

  console.log(`Exported ${contractName}`);
}