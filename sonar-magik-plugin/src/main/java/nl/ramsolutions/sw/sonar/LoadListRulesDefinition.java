package nl.ramsolutions.sw.sonar;

import java.util.List;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import org.sonar.api.SonarRuntime;

/** LoadList rules definition. */
public class LoadListRulesDefinition extends AbstractMagikRulesDefinition {

  public static final String REPOSITORY_KEY = "load_list";

  public LoadListRulesDefinition(final SonarRuntime runtime) {
    super(runtime);
  }

  @Override
  protected String repositoryKey() {
    return LoadListRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String languageKey() {
    return LoadListLanguage.KEY;
  }

  @Override
  protected List<ChecklistGroup> checklistGroups() {
    final List<Class<?>> checkClasses = toClassList(LoadListCheckList.INSTANCE.getBaseChecks());
    return List.of(new ChecklistGroup(LoadListCheckList.PROFILE_DIR, checkClasses));
  }
}
