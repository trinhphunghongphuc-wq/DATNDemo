import { network } from "hardhat";

async function main() {
  console.log("Starting deployment...");

  const { ethers } = await network.connect("localhost");

  const contract = await ethers.deployContract("TraceabilityRegistry");
  await contract.waitForDeployment();

  console.log("Contract deployed to:", await contract.getAddress());

  
}

main().catch((error) => {
  console.error("Failed to deploy contract:", error);
  process.exitCode = 1;
});