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
    
    @Test
void strongPasswordMeetsCentralizedPolicy() {

    assertTrue(
            PasswordUtil.meetsPolicy(
                    "StrongSecure@Test2026"
            )
    );
    }

    @Test
    void passwordBelowMinimumLengthFailsPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(
                        "Aa1!Short"
                )
        );
    }

    @Test
    void passwordWithoutUppercaseFailsPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(
                        "strongsecure@test2026"
                )
        );
    }

    @Test
    void passwordWithoutLowercaseFailsPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(
                        "STRONGSECURE@TEST2026"
                )
        );
    }

    @Test
    void passwordWithoutDigitFailsPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(
                        "StrongSecure@Testing"
                )
        );
    }

    @Test
    void passwordWithoutSpecialCharacterFailsPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(
                        "StrongSecureTest2026"
                )
        );
    }

    @Test
    void nullAndBlankPasswordsFailPolicy() {

        assertFalse(
                PasswordUtil.meetsPolicy(null)
        );

        assertFalse(
                PasswordUtil.meetsPolicy("")
        );

        assertFalse(
                PasswordUtil.meetsPolicy("              ")
        );
    }

    @Test
    void validatePolicyRejectsWeakPassword() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PasswordUtil.validatePolicy(
                                "Weak1!"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("at least 15")
        );
    }
}