package com.ssafy.dartservice.global.security;

import java.security.MessageDigest;

final class MessageDigestUtil {

	private MessageDigestUtil() {
	}

	static boolean equals(byte[] expected, byte[] actual) {
		return MessageDigest.isEqual(expected, actual);
	}
}
