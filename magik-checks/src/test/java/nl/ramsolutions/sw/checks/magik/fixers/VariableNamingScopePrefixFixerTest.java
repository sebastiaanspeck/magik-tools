package nl.ramsolutions.sw.checks.magik.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import org.junit.jupiter.api.Test;

/** Tests for {@link VariableNamingScopePrefixFixer}. */
class VariableNamingScopePrefixFixerTest {

  private static final Range FULL_RANGE =
      new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, 0));

  private List<CodeAction> getCodeActions(final String code, final boolean forbidScopePrefixes) {
    final MagikToolsProperties properties =
        new MagikToolsProperties(
            Map.of("variable-naming.forbid-scope-prefixes", Boolean.toString(forbidScopePrefixes)));
    final MagikFile magikFile = new MagikFile(properties, MagikFile.DEFAULT_URI, code);
    final VariableNamingScopePrefixFixer fixer = new VariableNamingScopePrefixFixer();
    return fixer.provideCodeActions(magikFile, FULL_RANGE);
  }

  @Test
  void testNoCodeActionsWhenDisabled() {
    final String code =
        """
        _block
          _local l_count << 1
          write(l_count)
        _endblock
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, false);
    assertThat(codeActions).isEmpty();
  }

  @Test
  void testRenamesLocalAndItsUsage() {
    final String code =
        """
        _block
          _local l_count << 1
          write(l_count)
        _endblock
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).hasSize(1);

    final CodeAction codeAction = codeActions.get(0);
    assertThat(codeAction.getTitle()).isEqualTo("Remove scope prefix from \"l_count\"");
    assertThat(codeAction.getEdits()).hasSize(2);
    assertThat(codeAction.getEdits())
        .allSatisfy(edit -> assertThat(edit.getNewText()).isEqualTo("count"));
  }

  @Test
  void testRenamesParameter() {
    final String code =
        """
        _method a.b(p_value)
          >> p_value
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).hasSize(1);

    final CodeAction codeAction = codeActions.get(0);
    assertThat(codeAction.getTitle()).isEqualTo("Remove scope prefix from \"p_value\"");
    assertThat(codeAction.getEdits()).hasSize(2);
    assertThat(codeAction.getEdits())
        .allSatisfy(edit -> assertThat(edit.getNewText()).isEqualTo("value"));
  }

  @Test
  void testDoesNotRenamePlainName() {
    final String code =
        """
        _block
          _local count << 1
          write(count)
        _endblock
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).isEmpty();
  }

  @Test
  void testRenamesUsageInNestedProcedureImport() {
    // A variable which is `_import`-ed into a nested procedure gets a separate ScopeEntry for
    // that import; usages inside the nested procedure's body must also be renamed.
    final String code =
        """
        _method a.b()
          _local l_count << 1
          _local q << _proc()
            _import l_count
            write(l_count)
          _endproc
          q()
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).hasSize(1);

    final CodeAction codeAction = codeActions.get(0);
    assertThat(codeAction.getTitle()).isEqualTo("Remove scope prefix from \"l_count\"");
    assertThat(codeAction.getEdits()).hasSize(3);
    assertThat(codeAction.getEdits())
        .allSatisfy(edit -> assertThat(edit.getNewText()).isEqualTo("count"));
  }

  @Test
  void testDoesNotOfferFixOnNameCollision() {
    // Stripping the prefix from `l_count` would collide with the already-declared `count` in the
    // same scope, so no (potentially behavior-changing) fix should be offered.
    final String code =
        """
        _block
          _local count << 0
          _local l_count << 1
          write(l_count)
        _endblock
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).isEmpty();
  }

  @Test
  void testDoesNotOfferFixForBarePrefixIdentifier() {
    // An identifier which is exactly a bare prefix would strip to an empty string; no fix should
    // be offered for that.
    final String code =
        """
        _method a.b(p_)
          write(p_)
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code, true);
    assertThat(codeActions).isEmpty();
  }
}
