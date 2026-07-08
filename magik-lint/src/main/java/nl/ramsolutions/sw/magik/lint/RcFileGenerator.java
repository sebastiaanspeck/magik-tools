package nl.ramsolutions.sw.magik.lint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * code-side defaults: a check absent from every given profile, but not {@link DisabledByDefault},
 * is added to {@code disabled}; a check present in a profile, but {@link DisabledByDefault}, is
 * added to {@code enabled}; parameter values that differ from the check's own {@code @RuleProperty}
 * default are emitted as {@code <check>.<parameter>=<value>} lines. Everything else already matches
 * the code-side default and is omitted, to keep the generated file limited to actual overrides.
 */
final class RcFileGenerator {

  private RcFileGenerator() {
    // Utility class.
  }

  /**
   * Generate {@code magik-lint.properties} contents.
   *
   * @param allChecks All known check classes, across all Magik-family check lists.
   * @param activeRules Active rules, combined from one or more quality profile exports.
   * @return Contents of the generated properties file.
   */
  static String generate(
      final List<Class<? extends Check>> allChecks, final List<ActiveRule> activeRules) {
    final Map<String, ActiveRule> activeByKebabKey = new LinkedHashMap<>();
    for (final ActiveRule activeRule : activeRules) {
      final String kebabKey = CheckHolder.toKebabCase(activeRule.key());
      activeByKebabKey.put(kebabKey, activeRule);
    }

    final List<String> enabled = new ArrayList<>();
    final List<String> disabled = new ArrayList<>();
    final List<String> parameterLines = new ArrayList<>();
    for (final Class<? extends Check> checkClass : allChecks) {
      final Rule rule = checkClass.getAnnotation(Rule.class);
      final String kebabKey = CheckHolder.toKebabCase(rule.key());
      final ActiveRule activeRule = activeByKebabKey.get(kebabKey);
      final boolean activeInProfile = activeRule != null;
      final boolean disabledByDefault = checkClass.getAnnotation(DisabledByDefault.class) != null;

      if (activeInProfile == disabledByDefault) {
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
          .filter(
              entry ->
                  !entry
                      .getValue()
                      .equals(RcFileGenerator.findDefaultValue(checkClass, entry.getKey())))
          .forEach(
              entry -> {
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

    return output.toString();
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
