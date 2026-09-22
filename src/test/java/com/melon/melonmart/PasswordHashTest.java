package com.melon.melonmart;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {
    @Test void bcryptRoundTripWorks() {
        String hash = BCrypt.hashpw("review123", BCrypt.gensalt(10));
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(BCrypt.checkpw("review123", hash));
        assertFalse(BCrypt.checkpw("wrong", hash));
    }
}
