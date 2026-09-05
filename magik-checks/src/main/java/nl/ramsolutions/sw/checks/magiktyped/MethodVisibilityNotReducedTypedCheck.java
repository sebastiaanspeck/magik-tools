package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import org.sonar.check.Rule;

/** Check if a method reduces the visibility of an inherited public method to private. */
@Rule(key = MethodVisibilityNotReducedTypedCheck.CHECK_KEY)
public class MethodVisibilityNotReducedTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodVisibilityNotReduced";

  private static final String MESSAGE = "Method %s is private, but is public on %s.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(MethodDefinition.class::isInstance)
        .map(MethodDefinition.class::cast)
        .filter(MethodDefinition::isActualMethodDefinition)
        .filter(methodDef -> methodDef.getModifiers().contains(MethodDefinition.Modifier.PRIVATE))
        .forEach(methodDef -> this.checkMethodDefinition(resolver, methodDef));
  }

  private void checkMethodDefinition(
      final TypeStringResolver resolver, final MethodDefinition methodDef) {
    final TypeString typeStr = methodDef.getTypeName();
    final String methodName = methodDef.getMethodName();

    // Walk the full ancestor chain: Magik allows multiple/deep inheritance, so a public
    // method further up the hierarchy (not just the direct parent) must also not be hidden.
    for (final TypeString ancestorTypeStr : resolver.getAllAncestors(typeStr)) {
      final Collection<MethodDefinition> ancestorMethodDefs =
          resolver.getRespondingMethodDefinitions(ancestorTypeStr, methodName);
      final boolean ancestorHasPublicMethod =
          ancestorMethodDefs.stream()
              .anyMatch(
                  ancestorMethodDef ->
                      !ancestorMethodDef
                          .getModifiers()
                          .contains(MethodDefinition.Modifier.PRIVATE));
      if (ancestorHasPublicMethod) {
        final String methodDefName = methodDef.getName();
        final String ancestorName = ancestorTypeStr.getFullString();
        final String message = MESSAGE.formatted(methodDefName, ancestorName);
        final AstNode definitionNode = methodDef.getNode();
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
        final AstNode issueNode = helper.getMethodNameNode();
        this.addIssue(issueNode, message);

        // Only report once per method definition, even if reachable via multiple ancestors.
        return;
      }
    }
  }
}
