package test;

import org.junit.jupiter.api.Test;
import util.PasswordUtil;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPasswordUtilTest {

    @Test
    void hashProducesBcryptHash() {

        String plaintext =
                "Synthetic@Test123";

        String hash =
                PasswordUtil.hash(
                        plaintext
                );

        assertNotNull(hash);

        assertNotEquals(
                plaintext,
                hash
        );

        assertTrue(
                PasswordUtil.isHash(hash)
        );
    }

    @Test
    void correctPasswordVerifiesSuccessfully() {

        String plaintext =
                "Synthetic@Test123";

        String hash =
                PasswordUtil.hash(
                        plaintext
                );

        assertTrue(
                PasswordUtil.verify(
                        plaintext,
                        hash
                )
        );
    }

    @Test
    void incorrectPasswordFailsVerification() {

        String hash =
                PasswordUtil.hash(
                        "Synthetic@Test123"
                );

        assertFalse(
                PasswordUtil.verify(
                        "WrongPassword",
                        hash
                )
        );
    }

    @Test
    void samePasswordProducesDifferentHashes() {

        String plaintext =
                "Synthetic@Test123";

        String firstHash =
                PasswordUtil.hash(
                        plaintext
                );

        String secondHash =
                PasswordUtil.hash(
                        plaintext
                );

        /*
         * BCrypt automatically generates a new salt
         * for every password hash.
         */
        assertNotEquals(
                firstHash,
                secondHash
        );

        assertTrue(
                PasswordUtil.verify(
                        plaintext,
                        firstHash
                )
        );

        assertTrue(
                PasswordUtil.verify(
                        plaintext,
                        secondHash
                )
        );
    }

    @Test
    void malformedHashFailsSafely() {

        assertFalse(
                PasswordUtil.verify(
                        "password",
                        "not-a-valid-bcrypt-hash"
                )
        );

        assertFalse(
                PasswordUtil.verify(
                        null,
                        null
                )
        );
    }
}