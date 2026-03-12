package domain.model.presets

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetsTest {

    // ── Engine presets ──────────────────────────────────────────

    @Test
    fun `engine presets list is non-empty`() {
        assertTrue(EnginePresets.all.isNotEmpty())
    }

    @Test
    fun `all engine presets have positive displacement`() {
        EnginePresets.all.forEach {
            assertTrue(it.displacementCc > 0, "${it.name} displacement must be > 0")
        }
    }

    @Test
    fun `all engine presets have valid cylinder count`() {
        EnginePresets.all.forEach {
            assertTrue(it.cylinderCount in 1..16, "${it.name} cylinders must be 1–16")
        }
    }

    @Test
    fun `engine preset names are unique`() {
        val names = EnginePresets.all.map { it.name }
        assertEquals(names.size, names.distinct().size)
    }

    // ── Fuel presets ────────────────────────────────────────────

    @Test
    fun `fuel presets list is non-empty`() {
        assertTrue(FuelPresets.all.isNotEmpty())
    }

    @Test
    fun `all fuel presets have positive density`() {
        FuelPresets.all.forEach {
            assertTrue(it.densityGPerCc > 0, "${it.name} density must be > 0")
        }
    }

    @Test
    fun `all fuel presets have positive stoich AFR`() {
        FuelPresets.all.forEach {
            assertTrue(it.stoichAfr > 0, "${it.name} AFR must be > 0")
        }
    }

    @Test
    fun `fuel blend E0 matches pure gasoline`() {
        val e0Blend = FuelPresets.blend(0.0)
        assertEquals(FuelPresets.E0.densityGPerCc, e0Blend.densityGPerCc, 1e-9)
        assertEquals(FuelPresets.E0.stoichAfr, e0Blend.stoichAfr, 1e-9)
    }

    @Test
    fun `fuel blend E100 matches pure ethanol`() {
        val e100Blend = FuelPresets.blend(100.0)
        assertEquals(FuelPresets.E100.densityGPerCc, e100Blend.densityGPerCc, 1e-9)
        assertEquals(FuelPresets.E100.stoichAfr, e100Blend.stoichAfr, 1e-9)
    }

    @Test
    fun `fuel blend E50 is between E0 and E100`() {
        val e50 = FuelPresets.blend(50.0)
        assertTrue(e50.densityGPerCc > FuelPresets.E0.densityGPerCc)
        assertTrue(e50.densityGPerCc < FuelPresets.E100.densityGPerCc)
        assertTrue(e50.stoichAfr < FuelPresets.E0.stoichAfr)
        assertTrue(e50.stoichAfr > FuelPresets.E100.stoichAfr)
        assertEquals("E50", e50.name)
    }

    // ── Injector presets ────────────────────────────────────────

    @Test
    fun `injector presets list is non-empty`() {
        assertTrue(InjectorPresets.all.isNotEmpty())
    }

    @Test
    fun `all injector presets have positive flow rate`() {
        InjectorPresets.all.forEach {
            assertTrue(it.flowRateCcPerMin > 0, "${it.name} flow rate must be > 0")
        }
    }

    @Test
    fun `all injector presets have positive fuel pressure`() {
        InjectorPresets.all.forEach {
            assertTrue(it.fuelPressureBar > 0, "${it.name} fuel pressure must be > 0")
        }
    }

    @Test
    fun `GDI injectors have higher pressure than PFI`() {
        val gdiMin = InjectorPresets.all.filter { "GDI" in it.name }.minOf { it.fuelPressureBar }
        val pfiMax = InjectorPresets.all.filter { "PFI" in it.name }.maxOf { it.fuelPressureBar }
        assertTrue(gdiMin > pfiMax, "GDI pressure ($gdiMin) must exceed PFI pressure ($pfiMax)")
    }
}
