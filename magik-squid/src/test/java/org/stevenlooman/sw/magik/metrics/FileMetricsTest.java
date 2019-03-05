package org.stevenlooman.sw.magik.metrics;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.TestVisitorContext;

import static org.fest.assertions.Assertions.assertThat;

public class FileMetricsTest {

  @Test
  public void testStatements1() {
    String code = "print(1)";
    assertThat(metrics(code).numberOfStatements()).isEqualTo(1);
  }

  @Test
  public void testMethodDefinition1() {
    String code = "_method a.b _endmethod";
    assertThat(metrics(code).numberOfMethods()).isEqualTo(1);
  }

  @Test
  public void testProcedureDefinition1() {
    String code = "_proc() _endproc";
    assertThat(metrics(code).numberOfProcedures()).isEqualTo(1);
  }

  @Test
  public void testExemplarDefinition1() {
    String code = "def_slotted_exemplar(:test, {})";
    assertThat(metrics(code).numberOfExemplars()).isEqualTo(1);
  }

  @Test
  public void testExemplarDefinition2() {
    String code = "def_indexed_exemplar(:test, {})";
    assertThat(metrics(code).numberOfExemplars()).isEqualTo(1);
  }

  @Test
  public void testFileComplexity1() {
    String code = "_if a = b _then _endif";
    assertThat(metrics(code).fileComplexity()).isEqualTo(2);
  }

  @Test
  public void testExecutableLines1() {
    String code = "print(1)";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(1));
  }

  @Test
  public void testExecutableLines2() {
    String code =
        "_pragma(classify_level=basic, topic={test})\n" +
        "_method a.b\n" +
        "\t## method header\n" +
        "\t# comment\n" +
        "\tprint(1)\n" +
        "\t_return _self\n" +
        "_endmethod\n";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(5, 6));
  }

  @Test
  public void testExecutableLines3() {
    String code =
        "_pragma(classify_level=basic, topic={test})\n" +
        "_method a.b(_optional c)\n" +
        "\t# comment\n" +
        "\t## method header\n" +
        "\t_return _self.call(:symbol, c)\n" +
        "_endmethod\n";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(5));
  }

  @Test
  public void testExecutableLines4() {
    String code =
        "_if a\n" +
        "_then\n" +
        "\tprint(1)\n" +
        "_elif b\n" +
        "_then\n" +
        "\tprint(2)\n" +
        "_else\n" +
        "\tprint(3)\n" +
        "_endif\n";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(1,3,4,6,8));
  }

  @Test
  public void testExecutableLines5() {
    String code =
        "_method a.b\n" +
        "\t_local a << _proc@test_proc()\n" +
        "\t\tprint(1)\n" +
        "\t_endproc\n" +
        "_endmethod\n";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(2,3));
  }

  @Test
  public void testExecutableLines6() {
    String code =
        "_pragma(classify_level=restricted, topic={dummy})\n" +
        "_method dummy_engine.start(props) \n" +
        "\t.slot_1 << rope.new()\n" +
        "\t.slot_2 << equality_property_list.new()\n" +
        "\t(var1, l_code) << _self.call_me(props[:key_1])\n" +
        "\t.slot_3    << var1\n" +
        "\t.slot_4    << p_props[:key_2]\n" +
        "\t.slot_5    << write_string(p_props[:key_3])\n" +
        "\t_self.method_1()\n" +
        "\t_self.method_2()\n" +
        "\t_self.method_3()\n" +
        "\t_self.method_4(p_props[:key_4])\n" +
        "_endmethod\n" +
        "$";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(3,4,5,6,7,8,9,10,11,12));
  }

  @Test
  public void testExecutableLines7() {
    String code =
        "_pragma(classify_level=restricted)\n" +
        "_iter _method a.b()\n" +
        "\t# comment\n" +
        "\t_for i _over v.fast_elements()\n" +
        "\t_loop\n" +
        "\t\t_loopbody(f(i))\n" +
        "\t_endloop\n" +
        "_endmethod\n" +
        "$";
    assertThat(metrics(code).executableLines()).isEqualTo(Sets.newHashSet(4,6));
  }

  private FileMetrics metrics(String code) {
    MagikVisitorContext context = TestVisitorContext.create(code);
    return new FileMetrics(context, true);
  }

}
