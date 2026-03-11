package data.writer

import data.model.EcuPlatform
import data.preferences.platform.EcuPlatformPreference
import kotlin.test.*

/**
 * Tests for [BinWriter] MED17 platform guard.
 */
class BinWriterTest {

    private lateinit var savedPlatform: EcuPlatform

    @BeforeTest
    fun savePlatform() {
        savedPlatform = EcuPlatformPreference.platform
    }

    @AfterTest
    fun restorePlatform() {
        EcuPlatformPreference.platform = savedPlatform
    }

    @Test
    fun `isBinWriteSupported returns true for ME7`() {
        EcuPlatformPreference.platform = EcuPlatform.ME7
        assertTrue(BinWriter.isBinWriteSupported(), "ME7 should support BIN writing")
    }

    @Test
    fun `isBinWriteSupported returns false for MED17`() {
        EcuPlatformPreference.platform = EcuPlatform.MED17
        assertFalse(BinWriter.isBinWriteSupported(), "MED17 should NOT support BIN writing (CRC32 not implemented)")
    }
}
