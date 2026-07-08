package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.MagikCheckList;
import org.junit.jupiter.api.Test;

class RcFileGeneratorTest {

  private static Set<String> disabledKeys(final String rcFile) {
    return rcFile
        .lines()
        .filter(line -> line.startsWith("disabled="))
        .findFirst()
        .map(line -> line.substring("disabled=".length()))
        .map(csv -> Set.copyOf(Arrays.asList(csv.split(","))))
        .orElse(Set.of());
  }

  @Test
  void testChecksNotInAnyProfileAreDisabled() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules = List.of(new ActiveRule("SyntaxError", Map.of()));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);
    final Set<String> disabled = RcFileGeneratorTest.disabledKeys(rcFile);

    assertThat(disabled).contains("line-length");
    assertThat(disabled).doesNotContain("syntax-error");
  }

  @Test
  void testDisabledByDefaultCheckActiveInProfileIsExplicitlyEnabled() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules = List.of(new ActiveRule("SwMethodDoc", Map.of()));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);

    assertThat(rcFile).contains("enabled=sw-method-doc");
  }

  @Test
  void testDisabledByDefaultCheckAbsentFromProfileIsNotExplicitlyDisabled() {
    // ForbiddenInheritanceCheck is @DisabledByDefault, and is not part of the active rules below:
    // it is already disabled without any config, so it should not clutter the disabled list.
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules = List.of(new ActiveRule("SyntaxError", Map.of()));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);
    final Set<String> disabled = RcFileGeneratorTest.disabledKeys(rcFile);

    assertThat(disabled).doesNotContain("forbidden-inheritance");
  }

  @Test
  void testParameterIsEmittedWithDashedKey() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules =
        List.of(new ActiveRule("LineLength", Map.of("max line length", "180")));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);

    assertThat(rcFile).contains("line-length.max-line-length=180");
  }

  @Test
  void testParameterEqualToDefaultIsOmitted() {
    // LineLengthCheck's "max line length" defaults to 120.
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules =
        List.of(new ActiveRule("LineLength", Map.of("max line length", "120")));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);

    assertThat(rcFile).doesNotContain("line-length.max-line-length");
  }

  @Test
  void testNullParameterValueIsIgnored() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final List<ActiveRule> activeRules =
        List.of(new ActiveRule("Formatting", Map.of("indent strategy", "null")));

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);

    assertThat(rcFile).doesNotContain("indent-strategy");
  }
}
