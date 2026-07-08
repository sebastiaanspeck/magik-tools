package nl.ramsolutions.sw.sonar;

import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.sonar.language.MagikLanguage;
import org.sonar.api.SonarRuntime;

/** Magik rules definition. */
public class MagikRulesDefinition extends AbstractMagikRulesDefinition {

  public MagikRulesDefinition(final SonarRuntime runtime) {
    super(runtime);
  }

  @Override
  protected String repositoryKey() {
    return MagikCheckList.REPOSITORY_KEY;
  }

  @Override
  protected String languageKey() {
    return MagikLanguage.KEY;
  }

  @Override
  protected List<ChecklistGroup> checklistGroups() {
    final List<Class<?>> checkClasses = toClassList(MagikCheckList.INSTANCE.getBaseChecks());
    return List.of(new ChecklistGroup(MagikCheckList.PROFILE_DIR, checkClasses));
  }
}
