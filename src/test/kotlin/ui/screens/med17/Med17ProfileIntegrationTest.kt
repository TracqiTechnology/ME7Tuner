package ui.screens.med17

import data.preferences.kfldimx.KfldimxPreferences
import data.preferences.kfldrl.KfldrlPreferences
import data.preferences.kfmiop.KfmiopPreferences
import data.preferences.kfmirl.KfmirlPreferences
import data.preferences.kfzw.KfzwPreferences
import data.preferences.kfzwop.KfzwopPreferences
import data.preferences.krkte.KrktePfiPreferences
import data.preferences.krkte.KrkteGdiPreferences
import data.preferences.rkw.RkwPreferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test: apply the MED17 profile and verify ALL screen-used
 * MapPreferences resolve non-null. This is the catch-all test that would
 * have detected the KRKTE bug immediately.
 *
 * No Compose UI rendering needed — pure data-layer verification.
 */
class Med17ProfileIntegrationTest : Med17ScreenTestBase() {

    @Test
    fun `all screen-used preferences resolve after profile apply`() {
        // Every MapPreference that any screen reads should resolve to a real map.
        // If any of these fail, the corresponding screen will show "Not configured".
        val screenPreferences = mapOf(
            "KRKTE PFI (KrkteScreen on MED17)" to KrktePfiPreferences,
            "KRKTE GDI (KrkteScreen on MED17)" to KrkteGdiPreferences,
            "KFMIOP (KfmiopScreen)" to KfmiopPreferences,
            "KFMIRL (KfmirlScreen)" to KfmirlPreferences,
            "KFZWOP (KfzwopScreen)" to KfzwopPreferences,
            "KFZW (KfzwScreen)" to KfzwPreferences,
            "KFLDRL (LdrpidScreen)" to KfldrlPreferences,
            "KFLDIMX (LdrpidScreen)" to KfldimxPreferences,
            "RKW (FuelTrimScreen)" to RkwPreferences,
        )

        val failures = mutableListOf<String>()
        for ((name, pref) in screenPreferences) {
            val selected = pref.getSelectedMap()
            if (selected == null) {
                failures.add(name)
            }
        }

        assertTrue(
            failures.isEmpty(),
            "Profile should resolve ALL screen-used preferences but ${failures.size} failed: $failures"
        )
    }

    @Test
    fun `KRKTE PFI preference maps to correct table`() {
        val selected = KrktePfiPreferences.getSelectedMap()
        assertNotNull(selected, "KRKTE PFI should resolve")
        assertTrue(
            selected.first.tableName.contains("inj time", ignoreCase = true),
            "KRKTE PFI should map to an injection time table, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `KFMIOP preference maps to correct table`() {
        val selected = KfmiopPreferences.getSelectedMap()
        assertNotNull(selected, "KFMIOP should resolve")
        assertTrue(
            selected.first.tableName == KFMIOP_TITLE,
            "KFMIOP should map to '$KFMIOP_TITLE', got: ${selected.first.tableName}"
        )
        // Bug 1 regression: must be a real 2D table, not a scalar
        assertFalse(
            selected.second.xAxis.isEmpty() && selected.second.yAxis.isEmpty(),
            "KFMIOP must NOT be scalar — wrong map selected if scalar"
        )
        assertTrue(
            selected.second.xAxis.size >= 14,
            "KFMIOP should have 14+ load columns, got ${selected.second.xAxis.size}"
        )
        assertTrue(
            selected.second.yAxis.size >= 14,
            "KFMIOP should have 14+ RPM rows, got ${selected.second.yAxis.size}"
        )
    }

    @Test
    fun `KFMIRL preference maps to correct table`() {
        val selected = KfmirlPreferences.getSelectedMap()
        assertNotNull(selected, "KFMIRL should resolve")
        assertTrue(
            selected.first.tableName.contains("Tgt filling", ignoreCase = true),
            "KFMIRL should map to Tgt filling table, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `KFZWOP preference maps to correct table`() {
        val selected = KfzwopPreferences.getSelectedMap()
        assertNotNull(selected, "KFZWOP should resolve")
        assertTrue(
            selected.first.tableName.contains("Opt model ref ignition", ignoreCase = true),
            "KFZWOP should map to optimal ignition table, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `KFZW preference maps to correct table`() {
        val selected = KfzwPreferences.getSelectedMap()
        assertNotNull(selected, "KFZW should resolve")
        assertTrue(
            selected.first.tableName.contains("Ignition GDI", ignoreCase = true),
            "KFZW should map to DS1 ignition switch map, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `KFLDRL preference maps to correct table`() {
        val selected = KfldrlPreferences.getSelectedMap()
        assertNotNull(selected, "KFLDRL should resolve")
        assertTrue(
            selected.first.tableName.contains("linearize boost", ignoreCase = true),
            "KFLDRL should map to linearize boost table, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `KFLDIMX preference maps to correct table`() {
        val selected = KfldimxPreferences.getSelectedMap()
        assertNotNull(selected, "KFLDIMX should resolve")
        assertTrue(
            selected.first.tableName.contains("LDR I controller", ignoreCase = true),
            "KFLDIMX should map to LDR I controller table, got: ${selected.first.tableName}"
        )
    }

    @Test
    fun `all 2D maps meet minimum dimension contract`() {
        val maps2d = mapOf(
            "KFMIOP" to KfmiopPreferences,
            "KFMIRL" to KfmirlPreferences,
            "KFZWOP" to KfzwopPreferences,
            "KFZW" to KfzwPreferences,
            "KFLDRL" to KfldrlPreferences,
            "KFLDIMX" to KfldimxPreferences,
            "RKW" to RkwPreferences,
        )

        for ((name, pref) in maps2d) {
            val selected = pref.getSelectedMap()
            assertNotNull(selected, "$name should resolve")
            val map = selected.second
            assertTrue(map.xAxis.size > 1, "$name should be 2D (xAxis.size=${map.xAxis.size})")
            assertTrue(map.yAxis.size > 1, "$name should be 2D (yAxis.size=${map.yAxis.size})")
        }
    }

    @Test
    fun `all resolved maps have non-empty data`() {
        val screenPreferences = mapOf(
            "KRKTE PFI" to KrktePfiPreferences,
            "KFMIOP" to KfmiopPreferences,
            "KFMIRL" to KfmirlPreferences,
            "KFZWOP" to KfzwopPreferences,
            "KFZW" to KfzwPreferences,
            "KFLDRL" to KfldrlPreferences,
            "KFLDIMX" to KfldimxPreferences,
            "RKW" to RkwPreferences,
        )

        for ((name, pref) in screenPreferences) {
            val selected = pref.getSelectedMap()
            assertNotNull(selected, "$name should resolve")
            val map = selected.second
            assertTrue(
                map.zAxis.isNotEmpty() && map.zAxis.any { row -> row.isNotEmpty() },
                "$name resolved but z-axis is empty"
            )
        }
    }
}
