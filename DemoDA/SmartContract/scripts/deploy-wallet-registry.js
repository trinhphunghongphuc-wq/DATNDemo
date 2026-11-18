import { network } from "hardhat";

const { ethers } = await network.connect("localhost");
const registry = await ethers.deployContract("WalletTraceabilityRegistry");
await registry.waitForDeployment();
console.log("Wallet registry owner (configure this account in Spring Boot):", await registry.owner());
console.log("BLOCKCHAIN_WALLET_CONTRACT_ADDRESS=" + await registry.getAddress());
