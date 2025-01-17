package nl.ramsolutions.sw.magik.analysis.definitions;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;

/** Definition keeper. */
public interface IDefinitionKeeper {

  void add(ProductDefinition definition);

  void add(ModuleDefinition definition);

  void add(MagikFileDefinition definition);

  void add(PackageDefinition definition);

  void add(ExemplarDefinition definition);

  void add(MethodDefinition definition);

  void add(GlobalDefinition definition);

  void add(BinaryOperatorDefinition definition);

  void add(ConditionDefinition definition);

  void add(ProcedureDefinition definition);

  void add(IDefinition definition);

  void remove(ProductDefinition definition);

  void remove(ModuleDefinition definition);

  void remove(MagikFileDefinition definition);

  void remove(PackageDefinition definition);

  void remove(ExemplarDefinition definition);

  void remove(MethodDefinition definition);

  void remove(GlobalDefinition definition);

  void remove(BinaryOperatorDefinition definition);

  void remove(ConditionDefinition definition);

  void remove(ProcedureDefinition definition);

  void remove(IDefinition definition);

  Collection<ProductDefinition> getProductDefinitions(String name);

  Collection<ProductDefinition> getProductDefinitions();

  Collection<ModuleDefinition> getModuleDefinitions(String name);

  Collection<ModuleDefinition> getModuleDefinitions();

  Collection<MagikFileDefinition> getMagikFileDefinitions(URI uri);

  Collection<MagikFileDefinition> getMagikFileDefinitions();

  Collection<PackageDefinition> getPackageDefinitions(String name);

  Collection<PackageDefinition> getPackageDefinitions();

  Collection<ExemplarDefinition> getExemplarDefinitions(TypeString typeName);

  Collection<ExemplarDefinition> getExemplarDefinitions();

  Collection<MethodDefinition> getMethodDefinitions(TypeString typeName);

  Collection<MethodDefinition> getMethodDefinitions();

  Collection<GlobalDefinition> getGlobalDefinitions(TypeString typeName);

  Collection<GlobalDefinition> getGlobalDefinitions();

  Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions(
      String operator, TypeString lhs, TypeString rhs);

  Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions();

  Collection<ConditionDefinition> getConditionDefinitions(String name);

  Collection<ConditionDefinition> getConditionDefinitions();

  Collection<ProcedureDefinition> getProcedureDefinitions(TypeString typeName);

  Collection<ProcedureDefinition> getProcedureDefinitions();

  /**
   * Get all {@link IDefinitions} from path or lower.
   *
   * <p>Used when a directory is deleted or renamed, since we only get the delete of the directory
   * itself, not the individual files within the directory or sub-directories.
   *
   * @param path Path to search from.
   * @return Definitions.
   */
  Collection<IDefinition> getDefinitionsByPath(Path path);

  void clear();
}
