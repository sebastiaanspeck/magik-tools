package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for check lists. Provides common functionality for managing check and fixer
 * classes.
 *
 * @param <C> The specific check type (e.g., MagikCheck, ModuleDefCheck, ProductDefCheck)
 * @param <F> The specific fixer type (e.g., CheckFixer, MagikTypedCheckFixer)
 */
public abstract class CheckList<C extends Check, F> {

  /**
   * Get the list of checks.
   *
   * @return List of check classes.
   */
  protected abstract List<Class<? extends C>> doGetChecks();

  /**
   * Get the checks which have fixers.
   *
   * @return Map of check classes and their fixer classes.
   */
  protected abstract Map<Class<? extends C>, List<Class<? extends F>>> doGetFixers();

  /**
   * Get the list of checks, casted to base {@link Check} type.
   *
   * @return List of {@link Check} classes.
   */
  protected List<Class<? extends Check>> doGetBaseChecks() {
    return this.doGetChecks().stream()
        .map(clazz -> (Class<? extends Check>) clazz)
        .collect(Collectors.toList());
  }
}
