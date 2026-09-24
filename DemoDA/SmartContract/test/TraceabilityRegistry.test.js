import assert from "node:assert/strict";
import hre from "hardhat";

const { ethers } = await hre.network.create();

const PRODUCER = 0;
const DISTRIBUTOR = 1;
const RETAILER = 2;

function createHash(value) {
  return ethers.keccak256(
    ethers.toUtf8Bytes(value)
  );
}

async function waitTransaction(transactionPromise) {
  const transaction = await transactionPromise;
  return transaction.wait();
}

async function expectRevert(
  transactionPromise,
  expectedMessage
) {
  await assert.rejects(
    async () => {
      const transaction = await transactionPromise;
      await transaction.wait();
    },
    (error) => {
      assert.match(
        error.message,
        new RegExp(expectedMessage)
      );

      return true;
    }
  );
}

describe("TraceabilityRegistry", function () {

  let registry;
  let owner;
  let writer;
  let outsider;

  beforeEach(async function () {
    [owner, writer, outsider] =
      await ethers.getSigners();

    registry = await ethers.deployContract(
      "TraceabilityRegistry"
    );

    await registry.waitForDeployment();
  });

  it("anchor Producer root thành công", async function () {
    const producerRoot =
      createHash("producer-root");

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        PRODUCER,
        producerRoot
      )
    );

    const storedRoot =
      await registry.getStageRoot(
        1,
        PRODUCER
      );

    assert.equal(
      storedRoot,
      producerRoot
    );

    const information =
      await registry.getStageInfo(
        1,
        PRODUCER
      );

    assert.equal(
      information.merkleRoot,
      producerRoot
    );

    assert.equal(
      information.anchoredBy,
      await owner.getAddress()
    );

    assert.equal(
      information.exists,
      true
    );
  });

  it("không cho anchor lại cùng stage", async function () {
    const firstRoot =
      createHash("producer-root-1");

    const secondRoot =
      createHash("producer-root-2");

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        PRODUCER,
        firstRoot
      )
    );

    await expectRevert(
      registry.anchorStageRoot(
        1,
        PRODUCER,
        secondRoot
      ),
      "Stage root already anchored"
    );
  });

  it("bắt buộc anchor đúng thứ tự stage", async function () {
    const producerRoot =
      createHash("producer-root");

    const distributorRoot =
      createHash("distributor-root");

    const retailerRoot =
      createHash("retailer-root");

    await expectRevert(
      registry.anchorStageRoot(
        1,
        DISTRIBUTOR,
        distributorRoot
      ),
      "Producer root is not anchored"
    );

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        PRODUCER,
        producerRoot
      )
    );

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        DISTRIBUTOR,
        distributorRoot
      )
    );

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        RETAILER,
        retailerRoot
      )
    );

    assert.equal(
      await registry.getStageRoot(
        1,
        DISTRIBUTOR
      ),
      distributorRoot
    );

    assert.equal(
      await registry.getStageRoot(
        1,
        RETAILER
      ),
      retailerRoot
    );
  });

  it("chỉ ví được cấp quyền mới được anchor", async function () {
    const producerRoot =
      createHash("producer-root");

    await expectRevert(
      registry
        .connect(outsider)
        .anchorStageRoot(
          1,
          PRODUCER,
          producerRoot
        ),
      "Writer is not authorized"
    );

    await waitTransaction(
      registry.setAuthorizedWriter(
        await writer.getAddress(),
        true
      )
    );

    await waitTransaction(
      registry
        .connect(writer)
        .anchorStageRoot(
          1,
          PRODUCER,
          producerRoot
        )
    );

    const information =
      await registry.getStageInfo(
        1,
        PRODUCER
      );

    assert.equal(
      information.anchoredBy,
      await writer.getAddress()
    );
  });

  it("xác minh Merkle proof hợp lệ", async function () {
    const firstLeaf =
      createHash("record-1");

    const secondLeaf =
      createHash("record-2");

    /*
     * Root = keccak256(firstLeaf || secondLeaf)
     */
    const producerRoot =
      ethers.keccak256(
        ethers.concat([
          firstLeaf,
          secondLeaf,
        ])
      );

    await waitTransaction(
      registry.anchorStageRoot(
        1,
        PRODUCER,
        producerRoot
      )
    );

    const valid =
      await registry.verifyStageRecord(
        1,
        PRODUCER,
        firstLeaf,
        [secondLeaf],
        0
      );

    assert.equal(valid, true);

    const invalidLeaf =
      createHash("altered-record");

    const invalid =
      await registry.verifyStageRecord(
        1,
        PRODUCER,
        invalidLeaf,
        [secondLeaf],
        0
      );

    assert.equal(invalid, false);
  });
});