import assert from "node:assert/strict";
import hre from "hardhat";

const { ethers } = await hre.network.create();
const types = {
  StageApproval: [
    { name: "batchId", type: "uint256" },
    { name: "stage", type: "uint8" },
    { name: "root", type: "bytes32" },
    { name: "deadline", type: "uint256" },
  ],
};

describe("WalletTraceabilityRegistry", function () {
  let registry, owner, producer, distributor, retailer, outsider, domain, deadline;
  const producerRoot = ethers.id("producer root");
  const distributorRoot = ethers.id("distributor root");
  const retailerRoot = ethers.id("retailer root");

  beforeEach(async function () {
    [owner, producer, distributor, retailer, outsider] = await ethers.getSigners();
    registry = await ethers.deployContract("WalletTraceabilityRegistry");
    await registry.waitForDeployment();
    domain = {
      name: "ChainTrack",
      version: "2",
      chainId: (await ethers.provider.getNetwork()).chainId,
      verifyingContract: await registry.getAddress(),
    };
    deadline = (await ethers.provider.getBlock("latest")).timestamp + 3600;
    await (await registry.bindBatch(1, producer.address, distributor.address, retailer.address)).wait();
  });

  function sign(signer, stage, root, options = {}) {
    return signer.signTypedData({ ...domain, ...options.domain }, types,
      { batchId: 1, stage, root, deadline, ...options.message });
  }

  it("only the owner can bind three distinct wallets, once per batch", async function () {
    await assert.rejects(
      registry.connect(outsider).bindBatch(2, producer.address, distributor.address, retailer.address),
      /Only owner/,
    );
    await assert.rejects(registry.bindBatch(2, producer.address, producer.address, retailer.address), /distinct/);
    await assert.rejects(registry.bindBatch(1, outsider.address, distributor.address, retailer.address), /already bound/);
    assert.equal(await registry.stageWallet(1, 0), producer.address);
  });

  it("rejects the legacy bypass, an unapproved relayer and signatures by unassigned wallets", async function () {
    await assert.rejects(registry.anchorStageRoot(1, 0, producerRoot), /signature required/);
    const signature = await sign(producer, 0, producerRoot);
    await assert.rejects(
      registry.connect(producer).anchorApprovedStage(1, 0, producerRoot, deadline, signature), /not authorized/,
    );
    const wrong = await sign(distributor, 0, producerRoot);
    await assert.rejects(registry.anchorApprovedStage(1, 0, producerRoot, deadline, wrong), /Invalid stage approval/);
  });

  it("binds approval to the root, batch, stage, contract, network and deadline", async function () {
    await (await registry.bindBatch(2, producer.address, distributor.address, retailer.address)).wait();
    const signature = await sign(producer, 0, producerRoot);
    assert.equal(await registry.verifyStageApproval(1, 0, producerRoot, deadline, signature), true);
    assert.equal(await registry.verifyStageApproval(2, 0, producerRoot, deadline, signature), false);
    assert.equal(await registry.verifyStageApproval(1, 0, distributorRoot, deadline, signature), false);
    const wrongStage = await sign(producer, 1, producerRoot);
    assert.equal(await registry.verifyStageApproval(1, 0, producerRoot, deadline, wrongStage), false);
    const wrongContract = await sign(producer, 0, producerRoot, { domain: { verifyingContract: outsider.address } });
    assert.equal(await registry.verifyStageApproval(1, 0, producerRoot, deadline, wrongContract), false);
    const wrongChain = await sign(producer, 0, producerRoot, { domain: { chainId: domain.chainId + 1n } });
    assert.equal(await registry.verifyStageApproval(1, 0, producerRoot, deadline, wrongChain), false);
    const expired = deadline - 7200;
    const expiredSignature = await sign(producer, 0, producerRoot, { message: { deadline: expired } });
    assert.equal(await registry.verifyStageApproval(1, 0, producerRoot, expired, expiredSignature), false);
  });

  it("preserves anchor order, one root per stage, and the existing proof verification API", async function () {
    const production = await sign(producer, 0, producerRoot);
    const delivery = await sign(distributor, 1, distributorRoot);
    const sales = await sign(retailer, 2, retailerRoot);
    await assert.rejects(
      registry.anchorApprovedStage(1, 1, distributorRoot, deadline, delivery), /Producer root is not anchored/,
    );
    const receipt = await (await registry.anchorApprovedStage(1, 0, producerRoot, deadline, production)).wait();
    const event = receipt.logs.map((log) => registry.interface.parseLog(log))
      .find((log) => log?.name === "StageApprovalUsed");
    assert.equal(event.args.signer, producer.address);
    await assert.rejects(
      registry.anchorApprovedStage(1, 0, producerRoot, deadline, production), /already anchored/,
    );
    await (await registry.anchorApprovedStage(1, 1, distributorRoot, deadline, delivery)).wait();
    await (await registry.anchorApprovedStage(1, 2, retailerRoot, deadline, sales)).wait();
    assert.equal(await registry.getStageRoot(1, 0), producerRoot);
    assert.equal(await registry.verifyStageRecord(1, 2, retailerRoot, [], 0), true);
  });
});
