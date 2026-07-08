package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import org.junit.jupiter.api.Test;

class QualityProfileImporterTest {

  private Path resourcePath(final String name) throws URISyntaxException {
    final var url = this.getClass().getClassLoader().getResource("quality_profiles/" + name);
    return Path.of(url.toURI());
  }

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
  void testParseSingleProfile() throws Exception {
    final Path xmlPath = this.resourcePath("magik_profile.xml");

    final List<ActiveRule> activeRules = QualityProfileImporter.parse(xmlPath);

    assertThat(activeRules).hasSize(2);
    assertThat(activeRules)
        .anySatisfy(
            rule -> {
              assertThat(rule.key()).isEqualTo("LineLength");
              assertThat(rule.parameters()).containsEntry("max line length", "120");
              assertThat(rule.parameters()).containsEntry("tab width", "8");
            });
    assertThat(activeRules).anySatisfy(rule -> assertThat(rule.key()).isEqualTo("SyntaxError"));
  }

  @Test
  void testMultipleProfilesAreMergedIntoOneRcFile() throws Exception {
    final Path magikXmlPath = this.resourcePath("magik_profile.xml");
    final Path productModuleDefXmlPath = this.resourcePath("product_module_def_profile.xml");

    final List<ActiveRule> activeRules = new ArrayList<>();
    activeRules.addAll(QualityProfileImporter.parse(magikXmlPath));
    activeRules.addAll(QualityProfileImporter.parse(productModuleDefXmlPath));

    final List<Class<? extends Check>> allChecks =
        Stream.of(
                MagikCheckList.INSTANCE.getBaseChecks().stream(),
                ProductDefCheckList.INSTANCE.getBaseChecks().stream(),
                ModuleDefCheckList.INSTANCE.getBaseChecks().stream())
            .flatMap(stream -> stream)
            .collect(Collectors.toUnmodifiableList());

    final String rcFile = RcFileGenerator.generate(allChecks, activeRules);
    final Set<String> disabled = QualityProfileImporterTest.disabledKeys(rcFile);

    // Active in one of the two profiles: must not be disabled.
    assertThat(disabled)
        .doesNotContain(
            "line-length", "syntax-error", "product-def-syntax-error", "module-def-syntax-error");
    // Not active in either profile: must be disabled.
    assertThat(disabled)
        .contains("product-def-missing-description", "module-def-missing-description");
  }
}
