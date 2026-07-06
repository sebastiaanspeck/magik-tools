package nl.ramsolutions.sw.checks.magik.fixers;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.MagikCodeActionSupplier;
import nl.ramsolutions.sw.checks.magik.VariableNamingCheck;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;

/**
 * Fixer for {@link VariableNamingCheck} scope-prefix issues.
 *
 * <p>Only active when {@link VariableNamingCheck#forbidScopePrefixes} is enabled for the file being
 * fixed: renames a scope-prefixed (p_/l_/i_/c_) variable or parameter, and all of its usages, to
 * the same name without the prefix.
 */
public class VariableNamingScopePrefixFixer extends MagikCodeActionSupplier {

  private static final String CONFIG_KEY =
      CheckHolder.toKebabCase(VariableNamingCheck.CHECK_KEY)
          + "."
          + VariableNamingCheck.PROPERTY_KEY_FORBID_SCOPE_PREFIXES.replace(" ", "-");

  @Override
  public List<CodeAction> provideMagikCodeActions(final MagikFile magikFile, final Range range) {
    final MagikToolsProperties properties = magikFile.getProperties();
    if (!properties.getPropertyBoolean(CONFIG_KEY, false)) {
      return List.of();
    }

    final GlobalScope globalScope = magikFile.getGlobalScope();
    final List<CodeAction> codeActions = new ArrayList<>();
    for (final Scope scope : globalScope.getSelfAndDescendantScopes()) {
      for (final ScopeEntry scopeEntry : scope.getScopeEntriesInScope()) {
        if (!scopeEntry.isType(
            ScopeEntry.Type.LOCAL, ScopeEntry.Type.DEFINITION, ScopeEntry.Type.PARAMETER)) {
          continue;
        }

        final String identifier = scopeEntry.getIdentifier();
        if (!VariableNamingCheck.hasScopePrefix(identifier)) {
          continue;
        }

        final AstNode definitionNode = scopeEntry.getDefinitionNode();
        final Range definitionRange = new Range(definitionNode);
        if (!range.overlapsWith(definitionRange)) {
          continue;
        }

        final String newName = VariableNamingCheck.stripScopePrefix(identifier);
        final List<TextEdit> textEdits =
            Stream.concat(Stream.of(definitionNode), scopeEntry.getUsages().stream())
                .map(Range::new)
                .map(nodeRange -> new TextEdit(nodeRange, newName))
                .toList();
        final String title = "Remove scope prefix from \"%s\"".formatted(identifier);
        codeActions.add(new CodeAction(title, textEdits));
      }
    }

    return codeActions;
  }
}
