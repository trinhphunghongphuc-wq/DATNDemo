package com.enums;

public enum EvaluationModel {

    // Toàn bộ nội dung record được ghi lên blockchain.
    FULL_ON_CHAIN,

    // Mỗi record được ghi một hash riêng lên blockchain.
    HASH_PER_RECORD,

    // Nhiều record dùng chung một Merkle root.
    MERKLE_BATCHING
}