package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("InkKey", appName)
  }

  @Test
  fun `load generated ttf into android typeface`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val generator = com.example.font.TtfFontGenerator()
    val testFile = java.io.File(context.cacheDir, "test_android_typeface.ttf")
    val result = generator.compileTtf(
      capturedStrokes = emptyMap(),
      outputFile = testFile,
      fontFamilyName = "InkKey Android Test"
    )
    org.junit.Assert.assertTrue(result.isSuccess)
    val typeface = android.graphics.Typeface.createFromFile(testFile)
    org.junit.Assert.assertNotNull(typeface)
  }
}
