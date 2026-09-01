package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for valid variable names. */
@Rule(key = VariableNamingCheck.CHECK_KEY)
public class VariableNamingCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "VariableNaming";

  private static final String MESSAGE = "Give the variable \"%s\" a proper descriptive name.";
  private static final String MESSAGE_SCOPE_PREFIX =
      "Do not prefix the variable \"%s\" with p_, l_, i_ or c_.";
  private static final int DEFAULT_MIN_LENGTH = 3;
  private static final int DEFAULT_MAX_LENGTH = 32;
  private static final String DEFAULT_WHITELIST = "x,y,z,id";
  private static final boolean DEFAULT_FORBID_SCOPE_PREFIXES = false;
  private static final String SCOPE_PREFIXES = "p_,l_,i_,c_";
  private static final List<String> SCOPE_PREFIX_ITEMS = List.of(SCOPE_PREFIXES.split(","));

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROPERTY_KEY_FORBID_SCOPE_PREFIXES = "forbid scope prefixes";

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

  /** Forbid variable/parameter names prefixed with p_, l_, i_ or c_. */
  @RuleProperty(
      key = PROPERTY_KEY_FORBID_SCOPE_PREFIXES,
      defaultValue = "" + DEFAULT_FORBID_SCOPE_PREFIXES,
      description = "Forbid variable/parameter names prefixed with p_, l_, i_ or c_",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean forbidScopePrefixes = DEFAULT_FORBID_SCOPE_PREFIXES;

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
          final AstNode identifierNode = scopeEntry.getDefinitionNode();

          // Report at most one issue per identifier: an invalid (post-strip) name is the more
          // fundamental problem (removing the scope prefix alone would not fix it), so prefer
          // that message over the scope-prefix message.
          if (!this.isValidName(identifier)) {
            final String message = MESSAGE.formatted(identifier);
            this.addIssue(identifierNode, message);
          } else if (this.forbidScopePrefixes && VariableNamingCheck.hasScopePrefix(identifier)) {
            final String message = MESSAGE_SCOPE_PREFIX.formatted(identifier);
            this.addIssue(identifierNode, message);
          }
        }
      }
    }
  }

  /**
   * Check whether an identifier starts with one of the scope prefixes (p_, l_, i_ or c_).
   *
   * @param identifier Identifier to check.
   * @return {@code true} if the identifier is scope-prefixed.
   */
  public static boolean hasScopePrefix(final String identifier) {
    final String lowered = identifier.toLowerCase();
    return VariableNamingCheck.getScopePrefixItems().stream().anyMatch(lowered::startsWith);
  }

  /**
   * Strip a scope prefix (p_, l_, i_ or c_) from an identifier, if present.
   *
   * @param identifier Identifier to strip.
   * @return Identifier without its scope prefix, or the original identifier if it had none.
   */
  public static String stripScopePrefix(final String identifier) {
    if (VariableNamingCheck.hasScopePrefix(identifier)) {
      return identifier.substring(2);
    }

    return identifier;
  }

  private static List<String> getScopePrefixItems() {
    return SCOPE_PREFIX_ITEMS;
  }

  private boolean isValidName(final String identifier) {
    final String strippedIdentifier = VariableNamingCheck.stripScopePrefix(identifier);
    final List<String> whitelistItems = this.getWhitelistItems();
    return whitelistItems.contains(strippedIdentifier)
        || (strippedIdentifier.length() >= this.minLength
            && strippedIdentifier.length() <= this.maxLength);
  }

  private List<String> getWhitelistItems() {
    return List.of(this.whitelist.split(","));
  }
}
