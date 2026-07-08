package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link GetGlobalValueUnsetCheck}. */
class GetGlobalValueUnsetCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if (foo << bar()) _isnt _unset
        _then
            foo.run()
        _endif
        """,
        """
        _if foo _isnt _unset
        _then
            foo.run()
        _endif
        """,
        "get_global_value(:var).run()",
        """
        _if (foo << get_global_value(:var)) _is _unset
        _then
            condition.raise(:error, :format_args, {"missing"})
        _endif
        foo.run()
        """,
        """
        _if (foo << sw:get_global_value(:var)) _is _unset
        _then
            _return
        _endif
        foo.run()
        """,
        """
        exemplar_global << get_global_value(:var)
        _if exemplar_global _is _unset
        _then
            condition.raise(:no_spec_loaded, :spec, :var.print_string)
        _endif
        >> exemplar_global
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new GetGlobalValueUnsetCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if (foo << get_global_value(:var)) _isnt _unset
        _then
            foo.run()
        _endif
        """,
        """
        _if _unset _isnt (foo << get_global_value(:var))
        _then
            foo.run()
        _endif
        """,
        "_if get_global_value(:var) _isnt _unset _then get_global_value(:var).run() _endif",
        """
        foo << get_global_value(:var)
        _if foo _isnt _unset
        _then
            foo.run()
        _endif
        """,
        """
        _local foo << get_global_value(:var)
        _if foo _isnt _unset
        _then
            foo.run()
        _endif
        """,
        """
        _if (foo << get_global_value(:var)) _isnt _unset
        _then
            foo.run()
        _else
            condition.raise(:error, :format_args, {"missing"})
        _endif
        """,
        """
        exemplar_global << get_global_value(:var)
        _if exemplar_global _isnt _unset
        _then
            condition.raise(:no_spec_loaded, :spec, :var.print_string)
        _endif
        >> exemplar_global
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new GetGlobalValueUnsetCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
