package nl.ramsolutions.sw.sonar.sensors;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.loadlist.metrics.FileMetrics;
import nl.ramsolutions.sw.sonar.LoadListRulesDefinition;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import nl.ramsolutions.sw.sonar.sensors.cpd.CpdTokenSaver;
import nl.ramsolutions.sw.sonar.visitors.LoadListHighlighterVisitor;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

/** load_list.txt/patch_list.txt squid Sensor. */
public class LoadListSensor extends AbstractMagikFamilySensor<LoadListFile> {

  public LoadListSensor(
      final CheckFactory checkFactory,
      final FileLinesContextFactory fileLinesContextFactory,
      final NoSonarFilter noSonarFilter) {
    super(checkFactory, fileLinesContextFactory, noSonarFilter);
  }

  @Override
  protected String sensorName() {
    return "LoadList Sensor";
  }

  @Override
  protected String languageKey() {
    return LoadListLanguage.KEY;
  }

  @Override
  protected String repositoryKey() {
    return LoadListRulesDefinition.REPOSITORY_KEY;
  }

  @Override
  protected List<Class<? extends Check>> checkClasses() {
    return LoadListCheckList.INSTANCE.getBaseChecks();
  }

  @Override
  protected FilePredicate extraPredicate(final FilePredicates predicates) {
    return predicates.or(
        predicates.matchesPathPattern("**/load_list.txt"),
        predicates.matchesPathPattern("**/patch_list.txt"));
  }

  @Override
  protected LoadListFile parseFile(final URI uri, final String content) {
    return new LoadListFile(uri, content);
  }

  @Override
  protected void saveMetrics(
      final SensorContext context, final InputFile inputFile, final LoadListFile file) {
    final FileMetrics metrics = new FileMetrics(file, true);

    saveMetric(context, inputFile, CoreMetrics.NCLOC, metrics.linesOfEntries().size());
    saveMetric(context, inputFile, CoreMetrics.COMMENT_LINES, metrics.commentLineCount());

    final FileLinesContext fileLinesContext = this.fileLinesContextFactory.createFor(inputFile);
    metrics
        .linesOfEntries()
        .forEach(line -> fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1));
    fileLinesContext.save();

    this.noSonarFilter.noSonarInFile(inputFile, metrics.nosonarLines());
  }

  @Override
  protected void saveHighlighting(
      final SensorContext context, final InputFile inputFile, final LoadListFile file) {
    final LoadListHighlighterVisitor tokensVisitor =
        new LoadListHighlighterVisitor(context, inputFile);
    tokensVisitor.scanFile(file);
  }

  @Override
  protected void saveCpdTokens(
      final CpdTokenSaver cpdTokenSaver, final InputFile inputFile, final LoadListFile file) {
    cpdTokenSaver.saveCpdTokens(inputFile, file);
  }
}
