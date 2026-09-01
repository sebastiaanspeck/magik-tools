package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.checks.MagikCheckList;
import org.junit.jupiter.api.Test;

class RcFileGeneratorTest {

  @Test
  void testChecksNotInAnyProfileAreDisabled() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("SyntaxError", Map.of())));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);
    final Set<String> disabled = RcFileTestSupport.disabledKeys(result.contents());

    assertThat(disabled).contains("line-length");
    assertThat(disabled).doesNotContain("syntax-error");
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void testDisabledByDefaultCheckActiveInProfileIsExplicitlyEnabled() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("SwMethodDoc", Map.of())));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);

    assertThat(result.contents()).contains("enabled=sw-method-doc");
  }

  @Test
  void testDisabledByDefaultCheckAbsentFromProfileIsNotExplicitlyDisabled() {
    // ForbiddenInheritanceCheck is @DisabledByDefault, and is not part of the active rules below:
    // it is already disabled without any config, so it should not clutter the disabled list.
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("SyntaxError", Map.of())));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);
    final Set<String> disabled = RcFileTestSupport.disabledKeys(result.contents());

    assertThat(disabled).doesNotContain("forbidden-inheritance");
  }

  @Test
  void testParameterIsEmittedWithDashedKey() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("LineLength", Map.of("max line length", "180"))));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);

    assertThat(result.contents()).contains("line-length.max-line-length=180");
  }

  @Test
  void testParameterEqualToDefaultIsOmitted() {
    // LineLengthCheck's "max line length" defaults to 120.
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("LineLength", Map.of("max line length", "120"))));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);

    assertThat(result.contents()).doesNotContain("line-length.max-line-length");
  }

  @Test
  void testNullParameterValueIsIgnored() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("Formatting", Map.of("indent strategy", "null"))));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);

    assertThat(result.contents()).doesNotContain("indent-strategy");
  }

  @Test
  void testLanguageWithoutSuppliedProfileIsNotDisabledAndWarns() {
    // Only a "magik" profile is supplied; "load_list" is omitted entirely, e.g. because the user
    // forgot to export it. LoadListSyntaxErrorCheck and LoadListEntryExistsCheck are both
    // enabled-by-default, so they must NOT silently end up in the disabled list, and the user
    // must be warned that the rcfile may be incomplete.
    final List<Class<? extends Check>> allChecks =
        Stream.of(
                MagikCheckList.INSTANCE.getBaseChecks().stream(),
                LoadListCheckList.INSTANCE.getBaseChecks().stream())
            .flatMap(stream -> stream)
            .collect(Collectors.toUnmodifiableList());
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of("magik", List.of(new ActiveRule("SyntaxError", Map.of())));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);
    final Set<String> disabled = RcFileTestSupport.disabledKeys(result.contents());

    assertThat(disabled).doesNotContain("load-list-syntax-error", "load-list-entry-exists");
    assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("load_list"));
  }

  @Test
  void testZeroProfilesSuppliedDisablesNothingAndWarnsForEveryLanguage() {
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage = Map.of();

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);
    final Set<String> disabled = RcFileTestSupport.disabledKeys(result.contents());

    assertThat(disabled).isEmpty();
    assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("magik"));
  }

  @Test
  void testUnknownParameterKeyIsSkippedAndWarns() {
    // LineLengthCheck only has "max line length" and "tab width" @RuleProperty parameters:
    // "old removed param" does not match any of them, e.g. because the profile was exported from
    // a different magik-lint version with a since-renamed or removed parameter.
    final List<Class<? extends Check>> allChecks = MagikCheckList.INSTANCE.getBaseChecks();
    final Map<String, List<ActiveRule>> activeRulesByLanguage =
        Map.of(
            "magik", List.of(new ActiveRule("LineLength", Map.of("old removed param", "120"))));

    final RcFileGenerator.GenerationResult result =
        RcFileGenerator.generate(allChecks, activeRulesByLanguage);

    assertThat(result.contents()).doesNotContain("old-removed-param");
    assertThat(result.warnings())
        .anySatisfy(
            warning -> {
              assertThat(warning).contains("old removed param");
              assertThat(warning).contains("line-length");
            });
  }
}
