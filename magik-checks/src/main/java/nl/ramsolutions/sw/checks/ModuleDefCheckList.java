package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefMissingDescriptionCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefNameDoesNotMatchDirectoryNameCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefSyntaxErrorCheck;

/** module.def {@link Check} list. */
public final class ModuleDefCheckList extends CheckList<ModuleDefCheck, CheckFixer> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "module_def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/moduledef/rules";

  private static final ModuleDefCheckList INSTANCE = new ModuleDefCheckList();

  private ModuleDefCheckList() {}

  /**
   * Get the list of {@link ModuleDefCheck}s.
   *
   * @return List of {@link ModuleDefCheck}s.
   */
  public static List<Class<? extends ModuleDefCheck>> getChecks() {
    return INSTANCE.doGetChecks();
  }

  /**
   * Get the list of {@link ModuleDefCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public static List<Class<? extends Check>> getBaseChecks() {
    return INSTANCE.doGetBaseChecks();
  }

  /**
   * Get the {@link ModuleDefCheck}s which have a {@link CheckFixer}.
   *
   * @return Map of {@link ModuleDefCheck}s and their {@link CheckFixer}s.
   */
  public static Map<Class<? extends ModuleDefCheck>, List<Class<? extends CheckFixer>>>
      getFixers() {
    return INSTANCE.doGetFixers();
  }

  @Override
  protected List<Class<? extends ModuleDefCheck>> doGetChecks() {
    return List.of(
        ModuleDefMissingDescriptionCheck.class,
        ModuleDefNameDoesNotMatchDirectoryNameCheck.class,
        ModuleDefSyntaxErrorCheck.class);
  }

  @Override
  protected Map<Class<? extends ModuleDefCheck>, List<Class<? extends CheckFixer>>> doGetFixers() {
    return Map.of();
  }
}
