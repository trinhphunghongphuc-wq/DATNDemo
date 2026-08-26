package com.util;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;

public class HashUtil {

    public static String keccak256(String input) {
        byte[] hash = Hash.sha3(input.getBytes(StandardCharsets.UTF_8));
        return Numeric.toHexStringNoPrefix(hash);
    }
}