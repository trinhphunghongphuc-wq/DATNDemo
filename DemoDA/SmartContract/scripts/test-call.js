import { network } from "hardhat";

async function main() {

  const { ethers } = await network.connect("localhost");

  const contract = await ethers.getContractAt(
    "TraceabilityRegistry",
    "0x5FbDB2315678afecb367f032d93F642f64180aa3"
  );

  const tx = await contract.storeBatchRoot(1, "abc123");

  await tx.wait();

  console.log("Stored successfully");

}

main().catch(console.error);