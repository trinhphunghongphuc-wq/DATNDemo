import assert from "node:assert/strict";
import { network } from "hardhat";

describe("CertificationBenchmark", function () {

  async function deployContract() {
    const { ethers } = await network.connect();

    const contract =
      await ethers.deployContract("CertificationBenchmark");

    await contract.waitForDeployment();

    return { ethers, contract };
  }

  it("chứng nhận một record hash", async function () {
    const { ethers, contract } = await deployContract();

    const hash = ethers.keccak256(
      ethers.toUtf8Bytes("record-1")
    );

    const transaction =
      await contract.certifyCommitment(hash);

    const receipt = await transaction.wait();

    assert.equal(receipt.status, 1);
    assert.ok(receipt.logs.length > 0);
  });

  it("chứng nhận một Merkle root", async function () {
    const { ethers, contract } = await deployContract();

    const root = ethers.keccak256(
      ethers.toUtf8Bytes("merkle-root")
    );

    const transaction =
      await contract.certifyCommitment(root);

    const receipt = await transaction.wait();

    assert.equal(receipt.status, 1);
    assert.ok(receipt.logs.length > 0);
  });

  it("từ chối commitment rỗng", async function () {
    const { ethers, contract } = await deployContract();

    await assert.rejects(
      contract.certifyCommitment(ethers.ZeroHash),
      /Empty commitment/
    );
  });

  it("chứng nhận JSON full on-chain làm baseline", async function () {
    const { ethers, contract } = await deployContract();

    const jsonBytes = ethers.toUtf8Bytes(
      '{"product":"Tomato","quantity":100}'
    );

    const transaction =
      await contract.certifyData(jsonBytes);

    const receipt = await transaction.wait();

    assert.equal(receipt.status, 1);
    assert.ok(receipt.logs.length > 0);
  });
});