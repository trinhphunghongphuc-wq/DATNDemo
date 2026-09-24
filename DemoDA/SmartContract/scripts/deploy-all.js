import { network } from "hardhat";

async function main() {
  console.log("Starting deployment...");

  const { ethers } = await network.connect("localhost");

  const registry = await ethers.deployContract(
    "TraceabilityRegistry"
  );
  await registry.waitForDeployment();

  const benchmark = await ethers.deployContract(
    "CertificationBenchmark"
  );
  await benchmark.waitForDeployment();

  console.log(
    "TraceabilityRegistry:",
    await registry.getAddress()
  );

  console.log(
    "CertificationBenchmark:",
    await benchmark.getAddress()
  );
}

main().catch((error) => {
  console.error("Deployment failed:", error);
  process.exitCode = 1;
});