package nl.ramsolutions.sw.magik.lint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Generates {@code magik-lint.properties} contents from the active rules of one or more SonarQube
 * quality profiles.
 *
 * <p>Magik-lint has no notion of "language": all checks (Magik, product.def, module.def,
 * load_list.txt/patch_list.txt) share a single {@code enabled}/{@code disabled} namespace, keyed by
 * the kebab-cased SonarQube rule key. SonarQube, on the other hand, requires one quality profile
 * per language. This class reconciles the two, emitting only actual overrides of the check's own
 * code-side defaults: a check absent from its language's profile, but not {@link
 * DisabledByDefault}, is added to {@code disabled}; a check present in its language's profile, but
 * {@link DisabledByDefault}, is added to {@code enabled}; parameter values that differ from the
 * check's own {@code @RuleProperty} default are emitted as {@code <check>.<parameter>=<value>}
 * lines. Everything else already matches the code-side default and is omitted, to keep the
 * generated file limited to actual overrides.
 *
 * <p>Crucially, a check is only ever force-disabled if a quality profile for its language was
 * actually supplied: SonarQube gives no way to tell "the user disabled this check" apart from "the
 * user forgot to export this language's profile", so when a language's profile is missing
 * entirely, its checks are left at their code-side default instead, and a warning is emitted.
 */
final class RcFileGenerator {

  /** Result of {@link #generate}. */
  record GenerationResult(String contents, List<String> warnings) {}

  private static final Map<String, String> LANGUAGE_BY_PACKAGE =
      Map.of(
          "nl.ramsolutions.sw.checks.magik", "magik",
          "nl.ramsolutions.sw.checks.magiktyped", "magik",
          "nl.ramsolutions.sw.checks.productdef", "product_module_def",
          "nl.ramsolutions.sw.checks.moduledef", "product_module_def",
          "nl.ramsolutions.sw.checks.loadlist", "load_list");

  private RcFileGenerator() {
    // Utility class.
  }

  /**
   * Generate {@code magik-lint.properties} contents.
   *
   * @param allChecks All known check classes, across all Magik-family check lists.
   * @param activeRulesByLanguage Active rules, keyed by SonarQube language. A language present as
   *     a key (even with an empty list) means a profile for that language was supplied; a
   *     language with no key at all means no profile was supplied for it.
   * @return The generated properties file contents, plus any warnings about incomplete input.
   */
  static GenerationResult generate(
      final List<Class<? extends Check>> allChecks,
      final Map<String, List<ActiveRule>> activeRulesByLanguage) {
    final List<String> warnings = new ArrayList<>();

    final Set<String> languagesInChecks = new LinkedHashSet<>();
    for (final Class<? extends Check> checkClass : allChecks) {
      languagesInChecks.add(RcFileGenerator.languageOf(checkClass));
    }
    languagesInChecks.stream()
        .filter(language -> !activeRulesByLanguage.containsKey(language))
        .sorted()
        .forEach(
            language ->
                warnings.add(
                    "No quality profile was supplied for language '"
                        + language
                        + "'; checks for this language were left at their default "
                        + "enabled/disabled state, so the generated rcfile may be incomplete."));

    final Map<String, Map<String, ActiveRule>> activeByLanguageAndKebabKey = new LinkedHashMap<>();
    activeRulesByLanguage.forEach(
        (language, activeRules) -> {
          final Map<String, ActiveRule> byKebabKey = new LinkedHashMap<>();
          for (final ActiveRule activeRule : activeRules) {
            final String kebabKey = CheckHolder.toKebabCase(activeRule.key());
            byKebabKey.put(kebabKey, activeRule);
          }
          activeByLanguageAndKebabKey.put(language, byKebabKey);
        });

    final List<String> enabled = new ArrayList<>();
    final List<String> disabled = new ArrayList<>();
    final List<String> parameterLines = new ArrayList<>();
    for (final Class<? extends Check> checkClass : allChecks) {
      final Rule rule = checkClass.getAnnotation(Rule.class);
      final String kebabKey = CheckHolder.toKebabCase(rule.key());
      final String language = RcFileGenerator.languageOf(checkClass);
      final boolean profileSuppliedForLanguage = activeRulesByLanguage.containsKey(language);
      final Map<String, ActiveRule> activeForLanguage =
          activeByLanguageAndKebabKey.getOrDefault(language, Map.of());
      final ActiveRule activeRule = activeForLanguage.get(kebabKey);
      final boolean activeInProfile = activeRule != null;
      final boolean disabledByDefault = checkClass.getAnnotation(DisabledByDefault.class) != null;

      if (profileSuppliedForLanguage && activeInProfile == disabledByDefault) {
        // Differs from the code-side default: needs an explicit override.
        if (activeInProfile) {
          enabled.add(kebabKey);
        } else {
          disabled.add(kebabKey);
        }
      }

      if (!activeInProfile) {
        continue;
      }

      activeRule.parameters().entrySet().stream()
          .filter(entry -> !"null".equalsIgnoreCase(entry.getValue()))
          .forEach(
              entry -> {
                final String defaultValue =
                    RcFileGenerator.findDefaultValue(checkClass, entry.getKey());
                if (defaultValue == null) {
                  warnings.add(
                      "Unknown parameter '"
                          + entry.getKey()
                          + "' for check '"
                          + kebabKey
                          + "': no matching @RuleProperty was found, skipping.");
                  return;
                }

                if (entry.getValue().equals(defaultValue)) {
                  return;
                }

                final String parameterKey = entry.getKey().replace(" ", "-");
                parameterLines.add(kebabKey + "." + parameterKey + "=" + entry.getValue());
              });
    }

    final StringBuilder output = new StringBuilder();
    if (!enabled.isEmpty()) {
      output.append("enabled=").append(String.join(",", enabled)).append('\n');
    }
    if (!disabled.isEmpty()) {
      output.append("disabled=").append(String.join(",", disabled)).append('\n');
    }
    parameterLines.forEach(line -> output.append(line).append('\n'));

    return new GenerationResult(output.toString(), warnings);
  }

  /**
   * Determine the SonarQube language a check belongs to, based on its package.
   *
   * @param checkClass Check class.
   * @return SonarQube language key, e.g. {@code magik}.
   */
  private static String languageOf(final Class<? extends Check> checkClass) {
    final String packageName = checkClass.getPackageName();
    final String language = RcFileGenerator.LANGUAGE_BY_PACKAGE.get(packageName);
    if (language == null) {
      throw new IllegalStateException(
          "Cannot determine SonarQube language for check package: " + packageName);
    }

    return language;
  }

  /**
   * Find the {@code @RuleProperty} default value for the given parameter key, on the given check
   * class.
   *
   * @param checkClass Check class.
   * @param parameterKey SonarQube parameter key, e.g. {@code max line length}.
   * @return The default value, or {@code null} if no matching {@code @RuleProperty} was found.
   */
  private static String findDefaultValue(
      final Class<? extends Check> checkClass, final String parameterKey) {
    return Arrays.stream(checkClass.getFields())
        .map(field -> field.getAnnotation(RuleProperty.class))
        .filter(Objects::nonNull)
        .filter(ruleProperty -> ruleProperty.key().equals(parameterKey))
        .map(RuleProperty::defaultValue)
        .findFirst()
        .orElse(null);
  }
}
