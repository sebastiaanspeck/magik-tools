package nl.ramsolutions.sw.magik.lint;

import java.util.Arrays;
import java.util.Set;

/** Shared test helpers for {@link QualityProfileImporterTest} and {@link RcFileGeneratorTest}. */
final class RcFileTestSupport {

  private RcFileTestSupport() {
    // Utility class.
  }

  /**
   * Extract the kebab-case keys listed on the {@code disabled=} line of a generated rcfile.
   *
   * @param rcFile Generated rcfile contents.
   * @return The disabled keys, or an empty set if there is no {@code disabled=} line.
   */
  static Set<String> disabledKeys(final String rcFile) {
    return rcFile
        .lines()
        .filter(line -> line.startsWith("disabled="))
        .findFirst()
        .map(line -> line.substring("disabled=".length()))
        .map(csv -> Set.copyOf(Arrays.asList(csv.split(","))))
        .orElse(Set.of());
  }
}
