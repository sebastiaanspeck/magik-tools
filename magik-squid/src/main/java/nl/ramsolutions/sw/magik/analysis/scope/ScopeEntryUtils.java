package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Utility methods for working with {@link ScopeEntry}s, e.g., for renaming purposes. */
public final class ScopeEntryUtils {

  private ScopeEntryUtils() {}

  /**
   * Get all the {@link AstNode}s related to {@code scopeEntry}, i.e., its own declaration and
   * usages, plus -- recursively -- the declarations/usages of any {@link ScopeEntry.Type#IMPORT}
   * {@link ScopeEntry} (anywhere in {@code globalScope}) which (transitively) imports {@code
   * scopeEntry}.
   *
   * <p>This is needed since a variable which is {@code _import}-ed into a nested
   * procedure/method gets its own, separate, {@link ScopeEntry} (of type {@link
   * ScopeEntry.Type#IMPORT}) in that nested scope. Usages of the variable within that nested
   * scope are attached to that {@link ScopeEntry}, not to the original declaration's {@link
   * ScopeEntry}. To fully rename a variable, these need to be included as well. A variable can be
   * imported multiple levels deep, hence the recursion.
   *
   * @param scopeEntry ScopeEntry to get related nodes for.
   * @param globalScope GlobalScope to search for related (imported) ScopeEntries in.
   * @return All related AstNodes, i.e., the declaration and all (transitive) usages.
   */
  public static List<AstNode> getRelatedNodes(
      final ScopeEntry scopeEntry, final GlobalScope globalScope) {
    final Set<ScopeEntry> relatedEntries =
        Collections.newSetFromMap(new IdentityHashMap<>());
    ScopeEntryUtils.collectRelatedEntries(scopeEntry, globalScope, relatedEntries);

    final Set<AstNode> seenNodes = Collections.newSetFromMap(new IdentityHashMap<>());
    final List<AstNode> nodes = new ArrayList<>();
    for (final ScopeEntry relatedEntry : relatedEntries) {
      Stream.concat(Stream.of(relatedEntry.getDefinitionNode()), relatedEntry.getUsages().stream())
          .map(ScopeEntryUtils::toIdentifierNode)
          .filter(seenNodes::add)
          .forEach(nodes::add);
    }

    return nodes;
  }

  /**
   * Resolve a (possibly non-identifier) rename node to its {@link MagikGrammar#IDENTIFIER} node.
   *
   * @param node Node, possibly an IDENTIFIER, possibly a wrapping node (e.g. an ATOM).
   * @return The IDENTIFIER node.
   */
  private static AstNode toIdentifierNode(final AstNode node) {
    return node.isNot(MagikGrammar.IDENTIFIER) ? node.getFirstChild(MagikGrammar.IDENTIFIER) : node;
  }

  /**
   * Recursively collect {@code entry} and any {@link ScopeEntry.Type#IMPORT} entries which
   * (transitively) import it.
   *
   * @param entry Entry to collect related entries for.
   * @param globalScope GlobalScope to search in.
   * @param collected Set to add related entries to, used as accumulator/visited-set.
   */
  private static void collectRelatedEntries(
      final ScopeEntry entry, final GlobalScope globalScope, final Set<ScopeEntry> collected) {
    if (!collected.add(entry)) {
      // Already visited, prevent infinite recursion.
      return;
    }

    for (final Scope scope : globalScope.getSelfAndDescendantScopes()) {
      for (final ScopeEntry candidate : scope.getScopeEntriesInScope()) {
        if (candidate.getImportedEntry() == entry) {
          ScopeEntryUtils.collectRelatedEntries(candidate, globalScope, collected);
        }
      }
    }
  }
}
