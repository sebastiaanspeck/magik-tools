package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.Check;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Base for Magik-family rules definitions.
 *
 * <p>Every subclass registers the rules for exactly one SonarQube language/repository, since
 * SonarQube ties a {@link NewRepository} to a single language. A repository can still be built up
 * from multiple {@link ChecklistGroup}s, which is needed when several check lists share the same
 * file suffix (and therefore the same SonarQube language), e.g. product.def and module.def.
 */
public abstract class AbstractMagikRulesDefinition implements RulesDefinition {

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  protected AbstractMagikRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  /**
   * SonarQube repository key.
   *
   * @return Repository key.
   */
  protected abstract String repositoryKey();

  /**
   * SonarQube language key this repository belongs to.
   *
   * @return Language key.
   */
  protected abstract String languageKey();

  /**
   * Groups of checks to load into the repository.
   *
   * @return Checklist groups.
   */
  protected abstract List<ChecklistGroup> checklistGroups();

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(this.repositoryKey(), this.languageKey())
            .setName(AbstractMagikRulesDefinition.REPOSITORY_NAME);

    for (final ChecklistGroup group : this.checklistGroups()) {
      final RuleMetadataLoader loader = new RuleMetadataLoader(group.profileDir(), this.runtime);
      loader.addRulesByAnnotatedClass(repository, group.checkClasses());
    }

    repository.done();
  }

  /**
   * Cast a list of {@link Check} subclasses to a plain {@link Class} list, as required by {@link
   * RuleMetadataLoader#addRulesByAnnotatedClass}.
   *
   * @param checks Checks, as returned by {@code CheckList#getBaseChecks()}.
   * @return Plain class list.
   */
  protected static List<Class<?>> toClassList(final List<Class<? extends Check>> checks) {
    return checks.stream().map(clazz -> (Class<?>) clazz).collect(Collectors.toUnmodifiableList());
  }

  /** A resource directory holding rule metadata, and the check classes found there. */
  protected record ChecklistGroup(String profileDir, List<Class<?>> checkClasses) {}
}
