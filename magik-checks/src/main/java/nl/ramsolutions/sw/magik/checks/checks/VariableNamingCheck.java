package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for valid variable names. */
@Rule(key = VariableNamingCheck.CHECK_KEY)
public class VariableNamingCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "VariableNaming";

  private static final String MESSAGE = "Give the variable \"%s\" a proper descriptive name.";
  private static final int DEFAULT_MIN_LENGTH = 3;
  private static final int DEFAULT_MAX_LENGTH = 32;
  private static final String DEFAULT_WHITELIST = "x,y,z";

  /** Minimum number of characters for a variable name. */
  @RuleProperty(
      key = "min length",
      defaultValue = "" + DEFAULT_MIN_LENGTH,
      description = "Minimum number of characters for a variable name",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int minLength = DEFAULT_MIN_LENGTH;

  /** Maximum number of characters for a variable name. */
  @RuleProperty(
      key = "max length",
      defaultValue = "" + DEFAULT_MAX_LENGTH,
      description = "Maximum number of characters for a variable name",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maxLength = DEFAULT_MAX_LENGTH;

  /** Whitelist of variable names to allow/ignore, separated by ','. */
  @RuleProperty(
      key = "whitelist",
      defaultValue = "" + DEFAULT_WHITELIST,
      description = "Whitelist of variable names to allow/ignore, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String whitelist = DEFAULT_WHITELIST;

  @Override
  protected void walkPostMagik(final AstNode node) {
    final MagikFile magikFile = this.getMagikFile();
    final GlobalScope globalScope = magikFile.getGlobalScope();
    for (final Scope scope : globalScope.getSelfAndDescendantScopes()) {
      for (final ScopeEntry scopeEntry : scope.getScopeEntriesInScope()) {
        if (scopeEntry.isType(ScopeEntry.Type.LOCAL)
            || scopeEntry.isType(ScopeEntry.Type.DEFINITION)
            || scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
          final String identifier = scopeEntry.getIdentifier();

          if (!this.isValidName(identifier)) {
            final String message = String.format(MESSAGE, identifier);
            final AstNode identifierNode = scopeEntry.getDefinitionNode();
            this.addIssue(identifierNode, message);
          }
        }
      }
    }
  }

  private String stripPrefix(final String identifier) {
    final String lowered = identifier.toLowerCase();
    if (lowered.startsWith("p_")
        || lowered.startsWith("l_")
        || lowered.startsWith("i_")
        || lowered.startsWith("c_")) {
      return identifier.substring(2);
    }
    return identifier;
  }

  private boolean isValidName(final String identifier) {
    final String strippedIdentifier = this.stripPrefix(identifier);
    final List<String> whitelistItems = this.getWhitelistItems();
    return whitelistItems.contains(strippedIdentifier)
        || (strippedIdentifier.length() >= this.minLength
            && strippedIdentifier.length() <= this.maxLength);
  }

  private List<String> getWhitelistItems() {
    return List.of(this.whitelist.split(","));
  }
}
