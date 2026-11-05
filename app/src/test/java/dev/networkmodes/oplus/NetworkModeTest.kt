package dev.networkmodes.oplus

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NetworkModeTest {
    @Test
    fun combinationsWithout2gKeepTheirExactRadioTypes() {
        assertEquals("5G/4G/3G", NetworkMode.labelForValue(28, Locale.ENGLISH))
        assertEquals("5G/4G", NetworkMode.labelForValue(24, Locale.ENGLISH))
        assertEquals("4G/3G", NetworkMode.labelForValue(12, Locale.ENGLISH))
        assertEquals("4G only", NetworkMode.labelForValue(11, Locale.ENGLISH))
    }

    @Test
    fun unknownModeKeepsStockSummary() {
        for (mode in listOf(-1, 3, 10, 22, 34, Int.MAX_VALUE)) {
            assertNull(NetworkMode.labelForValue(mode, Locale.ENGLISH))
        }
    }

    @Test
    fun labelsAndRadioTypesStayAlignedInEveryLocale() {
        val types = NetworkMode.networkTypes()
        assertArrayEquals(arrayOf(33, 28, 24, 23, 9, 12, 11, 0, 2, 1), types)
        assertEquals(types.size, types.toSet().size)
        for (locale in listOf(Locale.ENGLISH, Locale.forLanguageTag("ru-RU"), Locale.GERMAN, Locale.ROOT)) {
            val labels = NetworkMode.labels(locale)
            assertEquals(types.size, labels.size)
            for (i in types.indices) {
                assertEquals(labels[i], NetworkMode.labelForValue(types[i], locale))
                assertTrue(labels[i].isNotEmpty())
            }
        }
    }

    @Test
    fun russianUsesTranslatedLabelsAndOtherLanguagesFallBackToEnglish() {
        assertEquals("Только 4G", NetworkMode.labelForValue(11, Locale.forLanguageTag("ru")))
        assertEquals("Авто: 5G/4G/3G/2G", NetworkMode.labelForValue(33, Locale.forLanguageTag("ru-RU")))
        assertArrayEquals(NetworkMode.labels(Locale.ENGLISH), NetworkMode.labels(Locale.GERMAN))
    }

    @Test
    fun hostChangesCannotMutateFutureLists() {
        val types = NetworkMode.networkTypes()
        val labels = NetworkMode.labels(Locale.ENGLISH)
        types[0] = -1
        labels[0] = "changed"
        assertEquals(33, NetworkMode.networkTypes()[0])
        assertEquals("Auto: 5G/4G/3G/2G", NetworkMode.labels(Locale.ENGLISH)[0])
    }

    @Test
    fun networkTypesUseTheBoxedArrayRequiredByOemReflection() {
        assertEquals(java.lang.Integer::class.java, NetworkMode.networkTypes().javaClass.componentType)
    }
}
