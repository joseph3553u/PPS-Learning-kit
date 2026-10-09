package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CodeLinter
import com.example.engine.OfflineInterpreter
import com.example.model.DiagnosticSeverity
import com.example.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("C & C++ Studio", appName)
  }

  @Test
  fun `linter detects missing semicolon in C`() {
    val code = """
      #include <stdio.h>
      int main() {
          int x = 42
          return 0;
      }
    """.trimIndent()

    val diagnostics = CodeLinter.lint(code, Language.C)
    assertTrue("Should detect missing semicolon", diagnostics.any { it.rule == "semicolon" })
  }

  @Test
  fun `linter detects unclosed bracket`() {
    val code = """
      #include <stdio.h>
      int main() {
          printf("Hello");
    """.trimIndent()

    val diagnostics = CodeLinter.lint(code, Language.C)
    assertTrue("Should detect unclosed brace", diagnostics.any { it.rule == "bracket" })
  }

  @Test
  fun `offline interpreter executes printf`() {
    val code = """
      #include <stdio.h>
      int main() {
          printf("Hello from test %d\n", 99);
          return 0;
      }
    """.trimIndent()

    val interpreter = OfflineInterpreter()
    val result = interpreter.execute(code, Language.C, "")
    assertEquals(0, result.exitCode)
    assertTrue(result.stdout.contains("Hello from test 99"))
  }
}
