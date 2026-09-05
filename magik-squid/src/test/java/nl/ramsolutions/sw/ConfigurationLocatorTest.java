package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test {@link ConfigurationLocator}. */
class ConfigurationLocatorTest {

  @AfterEach
  void tearDown() {
    ConfigurationLocator.resetCache();
  }

  @Test
  void testLocatesConfigurationAtGitRepoRootFromNestedDir(final @TempDir Path tempDir)
      throws IOException {
    // Repo root, with magik-lint.properties and a .git dir, but no product.def/module.def.
    Files.createDirectory(tempDir.resolve(".git"));
    final Path expectedConfig =
        Files.createFile(tempDir.resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME));

    // Nested subdirectory, with nothing of its own.
    final Path nestedDir = Files.createDirectories(tempDir.resolve("products/product_a/module_b"));

    final Path result = ConfigurationLocator.locateConfiguration(nestedDir);

    assertThat(result).isEqualTo(expectedConfig);
  }

  @Test
  void testProductDirTakesPriorityOverGitRepoRoot(final @TempDir Path tempDir) throws IOException {
    // Repo root, with its own magik-lint.properties and a .git dir.
    Files.createDirectory(tempDir.resolve(".git"));
    Files.createFile(tempDir.resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME));

    // Product dir somewhere below the repo root, with product.def and its own
    // magik-lint.properties.
    final Path productDir = Files.createDirectories(tempDir.resolve("products/product_a"));
    Files.createFile(productDir.resolve("product.def"));
    final Path expectedConfig =
        Files.createFile(productDir.resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME));

    // Nested subdirectory below the product dir.
    final Path nestedDir = Files.createDirectories(productDir.resolve("module_b"));

    final Path result = ConfigurationLocator.locateConfiguration(nestedDir);

    assertThat(result).isEqualTo(expectedConfig);
  }

  @Test
  void testReturnsNullWhenNoProductModuleOrGitRepoFound(final @TempDir Path tempDir)
      throws IOException {
    // No .git, no product.def/module.def, no magik-lint.properties anywhere.
    final Path nestedDir = Files.createDirectories(tempDir.resolve("some/nested/dir"));

    final Path result = ConfigurationLocator.locateConfiguration(nestedDir);

    assertThat(result).isNull();
  }

  @Test
  void testGitFileIsAlsoRecognizedAsRepoRoot(final @TempDir Path tempDir) throws IOException {
    // A ".git" *file* (as used for worktrees/submodules), not a directory.
    Files.createFile(tempDir.resolve(".git"));
    final Path expectedConfig =
        Files.createFile(tempDir.resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME));

    final Path nestedDir = Files.createDirectories(tempDir.resolve("products/product_a"));

    final Path result = ConfigurationLocator.locateConfiguration(nestedDir);

    assertThat(result).isEqualTo(expectedConfig);
  }
}
