package io.github.jayfunc.carpelines.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoStartSettingsTest {
    @Test
    fun `Xiaomi family devices need the auto-start switch`() {
        assertTrue(isMiuiFamilyDevice(manufacturer = "Xiaomi", brand = "Xiaomi"))
        assertTrue(isMiuiFamilyDevice(manufacturer = "Xiaomi", brand = "Redmi"))
        assertTrue(isMiuiFamilyDevice(manufacturer = "Xiaomi", brand = "POCO"))
    }

    @Test
    fun `brand matching ignores case and surrounding whitespace`() {
        assertTrue(isMiuiFamilyDevice(manufacturer = " xiaomi ", brand = " redmi "))
    }

    @Test
    fun `other manufacturers are not treated as MIUI`() {
        assertFalse(isMiuiFamilyDevice(manufacturer = "Google", brand = "google"))
        assertFalse(isMiuiFamilyDevice(manufacturer = "samsung", brand = "samsung"))
    }
}
