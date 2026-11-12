package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.scanner.SsrfGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("CyberShield", appName)
  }

  @Test
  fun `ssrf guard blocks localhost`() {
    val result = SsrfGuard.validateUrl("http://localhost:8080")
    assertTrue(result is SsrfGuard.ValidationResult.Blocked)
  }
}
