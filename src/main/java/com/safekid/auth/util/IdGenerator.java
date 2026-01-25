package com.safekid.auth.util;

public final class IdGenerator {
    private static final de.huxhorn.sulky.ulid.ULID ulid = new de.huxhorn.sulky.ulid.ULID();
    public static String newId() { return ulid.nextULID(); }
    private IdGenerator() {}
}
