package za.co.kwarhadebe.fisheries

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class PasswordHashTest {
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    @Test fun passwordIsHashedRatherThanStoredAsPlainText() {
        assertNotEquals("fish123", hash("fish123"))
        assertEquals(hash("fish123"), hash("fish123"))
    }
}
