package com.pfh.user.config;

import java.util.Arrays;
import java.util.List;

public final class AppConstant {
    // JWT configuration
    public static final long JWT_EXPIRATION_TIME = 900_000; // 15 

    // Argon2id parameters following OWASP recommendations
    public static final int ARGON2_SALT_LENGTH = 16;
    public static final int ARGON2_HASH_LENGTH = 32;
    public static final int ARGON2_PARALLELISM = 2;
    public static final int ARGON2_MEMORY = 1 << 16; // 65536 KB
    public static final int ARGON2_ITERATIONS = 3;
    
    // Password policy
    public static final int MINIMUM_PASSWORD_LENGTH = 12;
    public static final List<String> COMMON_PASSWORDS = Arrays.asList(
        "password1234",        // 12 chars: predictable word + digits
        "iloveyou2020!!",      // 14 chars: common phrase + popular year + symbols
        "welcome12345!",       // 14 chars: greeting + digits + symbol
        "qwertyuiop123",       // 13 chars: extended keyboard sequence + digits
        "abc123abc123"        // 12 chars: repeated basic pattern
    );

}
