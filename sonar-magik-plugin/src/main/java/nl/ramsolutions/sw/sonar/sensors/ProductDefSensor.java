package nl.ramsolutions.sw.sonar.sensors;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.ProductModuleDefRulesDefinition;
import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import nl.ramsolutions.sw.sonar.visitors.ProductDefHighlighterVisitor;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

/** product.def squid Sensor. */
public class ProductDefSensor extends AbstractMagikFamilySensor<ProductDefFile> {

  private static final String PRODUCT_DEF = "product.def";

  public ProductDefSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    super(checkFactory, fileLinesContextFactory, noSonarFilter);
  }

  @Override
  protected String sensorName() {
    return "ProductDef Sensor";
  }

  @Override
  protected String languageKey() {
    return ProductModuleDefLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return ProductModuleDefRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected List<Class<? extends Check>> checkClasses() {
    return ProductDefCheckList.INSTANCE.getBaseChecks();
  }

  @Override
  protected FilePredicate extraPredicate(final FilePredicates predicates) {
    return predicates.hasFilename(ProductDefSensor.PRODUCT_DEF);
  }

  @Override
  protected ProductDefFile parseFile(final URI uri, final String content) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    return new ProductDefFile(uri, content, definitionKeeper, null);
  }

  @Override
  protected void saveMetrics(
      final SensorContext context,
      final InputFile inputFile,
      final ProductDefFile file,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    final FileMetrics metrics = new FileMetrics(file, true);

    saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfDefinition().size());
    saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLineCount());
    // A product.def file is always one class.
    // TODO: Do we really want this?
    saveMetric(context, inputFile, CoreMetrics.CLASSES, 1);

    final FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
    metrics
        .linesOfDefinition()
        .forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    fileLinesContext.save();

    noSonarFilter.noSonarInFile(inputFile, metrics.nosonarLines());
  }

  @Override
  protected void saveHighlighting(
      final SensorContext context, final InputFile inputFile, final ProductDefFile file) {
    final ProductDefHighlighterVisitor tokensVisitor =
        new ProductDefHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(file);
  }

  @Override
  protected void saveCpdTokens(
      final CpdTokenSaver cpdTokenSaver, final InputFile inputFile, final ProductDefFile file) {
    cpdTokenSaver.saveCpdTokens(inputFile, file);
  }
}
