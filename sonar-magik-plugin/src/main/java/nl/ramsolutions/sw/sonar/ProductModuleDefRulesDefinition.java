package nl.ramsolutions.sw.sonar;

import java.util.List;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import org.sonar.api.SonarRuntime;

/**
 * ProductDef and ModuleDef rules definition.
 *
 * <p>Combined into one repository, because they share the same file suffix (and therefore the same
 * SonarQube language).
 */
public class ProductModuleDefRulesDefinition extends AbstractMagikRulesDefinition {

  public static final String REPOSITORY_KEY = "product_module_def";

  public ProductModuleDefRulesDefinition(final SonarRuntime runtime) {
    super(runtime);
  }

  @Override
  protected String repositoryKey() {
    return ProductModuleDefRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected String languageKey() {
    return ProductModuleDefLanguage.KEY;
  }

  @Override
  protected List<ChecklistGroup> checklistGroups() {
    return List.of(
        new ChecklistGroup(
            ProductDefCheckList.PROFILE_DIR,
            toClassList(ProductDefCheckList.INSTANCE.getBaseChecks())),
        new ChecklistGroup(
            ModuleDefCheckList.PROFILE_DIR,
            toClassList(ModuleDefCheckList.INSTANCE.getBaseChecks())));
  }
}
