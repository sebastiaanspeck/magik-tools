package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link MethodVisibilityNotReducedTypedCheck}. */
class MethodVisibilityNotReducedTypedCheckTest {

  private static final TypeString TYPE_GRANDPARENT = TypeString.ofIdentifier("grandparent", "sw");
  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");

  private void addExemplar(final IDefinitionKeeper definitionKeeper, final TypeString typeStr) {
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeStr, null));
  }

  private void addInheritance(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final TypeString parentTypeStr) {
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, typeStr, parentTypeStr));
  }

  private void addMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final String methodName,
      final Set<MethodDefinition.Modifier> modifiers) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            typeStr,
            methodName,
            modifiers,
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testPrivateOverridesPublicParent() {
    final String code =
        """
        _private _method child.name()
          >> :child
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, TYPE_PARENT);
    this.addMethod(
        definitionKeeper, TYPE_PARENT, "name()", EnumSet.noneOf(MethodDefinition.Modifier.class));
    this.addExemplar(definitionKeeper, TYPE_CHILD);
    this.addInheritance(definitionKeeper, TYPE_CHILD, TYPE_PARENT);

    final MagikTypedCheck check = new MethodVisibilityNotReducedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testPrivateOverridesPrivateParent() {
    final String code =
        """
        _private _method child.name()
          >> :child
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, TYPE_PARENT);
    this.addMethod(
        definitionKeeper, TYPE_PARENT, "name()", Set.of(MethodDefinition.Modifier.PRIVATE));
    this.addExemplar(definitionKeeper, TYPE_CHILD);
    this.addInheritance(definitionKeeper, TYPE_CHILD, TYPE_PARENT);

    final MagikTypedCheck check = new MethodVisibilityNotReducedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testPublicOverridesPublicParent() {
    final String code =
        """
        _method child.name()
          >> :child
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, TYPE_PARENT);
    this.addMethod(
        definitionKeeper, TYPE_PARENT, "name()", EnumSet.noneOf(MethodDefinition.Modifier.class));
    this.addExemplar(definitionKeeper, TYPE_CHILD);
    this.addInheritance(definitionKeeper, TYPE_CHILD, TYPE_PARENT);

    final MagikTypedCheck check = new MethodVisibilityNotReducedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testPrivateOverridesPublicGrandparentMultiLevel() {
    final String code =
        """
        _private _method child.name()
          >> :child
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, TYPE_GRANDPARENT);
    this.addMethod(
        definitionKeeper,
        TYPE_GRANDPARENT,
        "name()",
        EnumSet.noneOf(MethodDefinition.Modifier.class));
    this.addExemplar(definitionKeeper, TYPE_PARENT);
    this.addInheritance(definitionKeeper, TYPE_PARENT, TYPE_GRANDPARENT);
    this.addExemplar(definitionKeeper, TYPE_CHILD);
    this.addInheritance(definitionKeeper, TYPE_CHILD, TYPE_PARENT);

    final MagikTypedCheck check = new MethodVisibilityNotReducedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoMatchingParentMethod() {
    final String code =
        """
        _private _method child.name()
          >> :child
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, TYPE_PARENT);
    this.addExemplar(definitionKeeper, TYPE_CHILD);
    this.addInheritance(definitionKeeper, TYPE_CHILD, TYPE_PARENT);

    final MagikTypedCheck check = new MethodVisibilityNotReducedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
