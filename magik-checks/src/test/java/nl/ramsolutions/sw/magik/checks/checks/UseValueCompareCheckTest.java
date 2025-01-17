package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test UseValueCompareCheck. */
class UseValueCompareCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a _is b",
        "a = \"b\"",
        "a _is 1",
      })
  void testValid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"a\" _is b",
        "a _is \"b\"",
        "a _is 0.0",
        "536870913 _is a",
        "a _is 16rffffffffffff",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
