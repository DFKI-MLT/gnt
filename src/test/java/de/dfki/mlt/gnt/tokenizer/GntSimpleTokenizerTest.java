package de.dfki.mlt.gnt.tokenizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

/**
 * Test class for {@link GntSimpleTokenizer}.
 *
 * @author Jörg Steffen, DFKI
 */
public class GntSimpleTokenizerTest {

  @Test
  public void test1() {

    String text =
        "Der Abriss wird schätzungsweise etwa 40 Jahre dauern, sagt Dr. Günter Neumann, der "
            + "3. Reiter danach! Alleh hopp noch e mal.";

    List<String> tokens = GntSimpleTokenizer.tokenize(text);
    assertThat(tokens).hasSize(27);
  }


  @Test
  public void test2() {

    String text =
        "Current immunosuppression protocols to prevent lung transplant rejection reduce "
            + "pro-inflammatory and T-helper type 1 (Th1) cytokines. However, Th1 T-cell "
            + "pro-inflammatory cytokine production is important in host defense against bacterial "
            + "infection in the lungs. Excessive immunosuppression of Th1 T-cell pro-inflammatory "
            + "cytokines leaves patients susceptible to infection.";

    List<String> tokens = GntSimpleTokenizer.tokenize(text);
    assertThat(tokens).hasSize(51);
  }


  @Test
  public void test3() {

    String text = "CELLULAR COMMUNICATIONS INC. sold 1,550,000 common shares at $21.75 each "
        + "yesterday, according to lead underwriter L.F. Rothschild & Inc. . "
        + "Der 3.        Mann geht nahc hause 3. Und was macht er denn daheim? Weiss mnicht, "
        + "weisst du es ? Wieso nicht?    Weil ";

    List<String> tokens = GntSimpleTokenizer.tokenize(text);
    assertThat(tokens).hasSize(58);
  }


  @Test
  public void test4() {

    String text = "From paper Optimizing Dependency Parsing Throughput "
        + "-  MDParser transforms String features to integer values since the used liblinear "
        + "classifier operates on numerical values. "
        + "This step requires a total of 27 transformations for every word, since MDParser "
        + "computes 27 different features templates. "
        + "Syntactic Parser, in contrast, transforms elementary features to integer values before "
        + "the features are combined, "
        + "requiring only three transformations (part-of-speech, word form and dependency) for "
        + "every word in a sentence."
        + "-  Syntactic Parser represents its numerical features n as 64-bit integers "
        + "(Java long type) which consist of (i) "
        + "an 8 bit value indicating the feature type, and (ii) one or more of the following "
        + "elementary features: "
        + "word forms (20 bit), part-of-speech tag (16 bit) or dependency label (16 bit). "
        + "The feature type is always stored in the last eight bits, while the position of "
        + "elementary features depends on the feature type. "
        + "-  We, therefore, replace the mapping M: F -> N of parser feature strings fi in F "
        + "to integer values n in N with three mappings that translate word forms wi in W "
        + "(M_wordform: W -> {0, … ,1048575}), part-of-speech tags posi \\in P "
        + "(M_pos: P -> {0, …, 65535}) and dependency labels di \\in D "
        + "(M_dep: D -> {0, …, 65535}) to integer values. "
        + "The numerical feature value n is then derived by performing bit operations on these "
        + "elementary features."
        + "-  The test for cycles and projectiveness traverse the dependency tree "
        + "-  assuming that projectiveness is a symmetric property, then only a word pair has "
        + "to be tested once rather than twice "
        + "(for potential dependency n_i -> n_j and n_j -> n_i) ."
        + "-  We also perform a final optimization step on the trained model which eliminates "
        + "features with a weight of zero.";

    List<String> tokens = GntSimpleTokenizer.tokenize(text);
    assertThat(tokens).hasSize(335);
  }


  @Test
  public void test5() {

    String text = ":Text with separator at the beginning.";

    List<String> tokens = GntSimpleTokenizer.tokenize(text);
    assertThat(tokens).hasSize(8);
  }
}

