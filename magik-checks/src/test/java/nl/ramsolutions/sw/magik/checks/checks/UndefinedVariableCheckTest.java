package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test UndefinedVariableCheck. */
class UndefinedVariableCheckTest extends MagikCheckTestBase {

  @Test
  void testDefinedLocalUsed() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _method a.b
            _local l_a << 10
            write(l_a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDefinedDefinitionUsed() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _method a.b
            l_a << 10
            write(l_a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testUndefinedLocalUsed() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _method a.b
            write(l_a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testDefinedLocalUsedProcedure() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _proc()
            _local l_a
            write(l_a)
        _endproc""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testUndefinedLocalUsedProcedure() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _proc()
            write(l_a)
        _endproc""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testParameter() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _method a.b(p_a)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSerialAssigment() {
    final MagikCheck check = new UndefinedVariableCheck();
    final String code =
        """
        _method a.b()
            l_a << l_b << 10
            show(l_a, l_b)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
