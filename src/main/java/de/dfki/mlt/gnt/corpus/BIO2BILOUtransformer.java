package de.dfki.mlt.gnt.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * {@code
 * Maps a taged file in BIO format to a tagged file in BILOU format.
 *
 * B - 'beginning'
 * I - 'inside'
 * L - 'last'
 * O - 'outside'
 * U - 'unit'
 *
 * each line of input file is of form:
 * dekonvens format:
 * 8  Ecce  B-OTH  O
 *
 * conll 2003 format:
 *    4  Stadtverordnetenversammlung  NN  I-NC  I-ORG
 *    1  Nadim  NNP  I-NP  I-PER
 *
 *
 * Rules for mapping:
 *
 * If we have a single NE-token word then tag it as
 * word\ U-NeType
 *
 * else
 *
 * Tag the last I-tagged token as L-tagged token.
 *
 * Everything else remains as it is.
 *
 * NOTE:
 * Here I can later also add additional Tags for specific O-tags etc.
 * }
 * </pre>
 *
 * @author Günter Neumann, DFKI
 */
public class BIO2BILOUtransformer {

  private List<String> bioPhrase = new ArrayList<String>();

  private int labelPos = 0;
  private String sepChar = "\t";


  public BIO2BILOUtransformer(boolean conll2003) {

    if (conll2003) {
      this.labelPos = 4;
    } else {
      this.labelPos = 2;
    }
  }


  /**
   * Main mapper loop:
   * <p>
   * Idea is to firstly collect a bio-phrase, and then to map the BIO schema to the BILOU schema.
   * @param line
   * @param writer
   */
  public void mapBIOtoBILOUschema(String line, BufferedWriter writer) {

    String[] splitLine = line.split(this.sepChar);

    if (splitLine.length == 1) {
      try {
        writer.write(line + "\n");
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      String neLabel = splitLine[this.labelPos];

      if (neLabel.equals("O")) {
        // no NE label found, but we have an open bioPhrase, so close it and store it.
        if (!this.bioPhrase.isEmpty()) {
          // close, map and save open bioPhrase
          mapAndWriteBioPhrase(writer);
          // - and reset it
          this.bioPhrase = new ArrayList<String>();
        }
        // - write other line
        try {
          writer.write(line + "\n");
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else if (neLabel.startsWith("B-")) {
        // neLabel starts with B- and we have an open bioPhrase
        // covers cases like O I-PER I-PER B-PER-I-PER I-ORG
        // close open bioPhrase and make a new bioPhrase
        // close, map and save open bioPhrase
        mapAndWriteBioPhrase(writer);
        // - and reset it
        this.bioPhrase = new ArrayList<String>();
        this.bioPhrase.add(line);
      } else if (neLabel.startsWith("I-")) {
        // should start with "I-"
        if (this.bioPhraseLastElemHasSameLabel(neLabel)) {
          // open bioPhrase and current token have same type
          this.bioPhrase.add(line);
        } else {
          // close, map and save open bioPhrase
          mapAndWriteBioPhrase(writer);
          // - and reset it
          this.bioPhrase = new ArrayList<String>();
          this.bioPhrase.add(line);
        }
      }
    }
  }


  private boolean bioPhraseLastElemHasSameLabel(String neLabel) {

    return (!this.bioPhrase.isEmpty()
        && this.bioPhrase.get(this.bioPhrase.size() - 1).split(this.sepChar)[this.labelPos]
            .equals(neLabel));
  }


  private void mapAndWriteBioPhrase(BufferedWriter writer) {

    writeBioPhrase(
        mapBio2Bilou(this.bioPhrase),
        writer);
  }


  /**
   * Main mapper function for BOI to BILOU; Since I have a bioPhrase, it is easy:
   * <li> if bioPhrase only has a single token, make it an U- one
   * <li> else just change the first element to B- and the last element to L-,
   * <li> and leave the others (if any) untouched
   * @param bioPhraseParam
   * @return
   */
  private List<String> mapBio2Bilou(List<String> bioPhraseParam) {

    List<String> bilouPhrase = new ArrayList<String>();
    if (bioPhraseParam.size() == 1) {
      String bilouLine = makeNewBilouLine(bioPhraseParam.get(0), "U-");
      bilouPhrase.add(bilouLine);

    } else if (bioPhraseParam.size() > 1) {
      String firstBilouLine = makeNewBilouLine(bioPhraseParam.get(0), "B-");
      bilouPhrase.add(firstBilouLine);
      for (int i = 1; i < bioPhraseParam.size() - 1; i++) {
        bilouPhrase.add(bioPhraseParam.get(i));
      }
      String lastBilouLine = makeNewBilouLine(bioPhraseParam.get(bioPhraseParam.size() - 1), "L-");
      bilouPhrase.add(lastBilouLine);
    }
    return bilouPhrase;
  }


  // This actually changes the BOI tag to an BILOU tag, by copying the string and only change
  // the label part
  // TODO
  // Could be more efficient by using a internal data structure instead of the line.
  private String makeNewBilouLine(String bioLine, String neLabelPrefix) {

    String[] lineSplit = bioLine.split(this.sepChar);
    String bilouLine = lineSplit[0];
    String newlabel = neLabelPrefix + lineSplit[this.labelPos].split("-")[1];
    lineSplit[this.labelPos] = newlabel;
    for (int i = 1; i < lineSplit.length; i++) {
      bilouLine += this.sepChar + lineSplit[i];
    }
    return bilouLine;
  }


  private void writeBioPhrase(List<String> bioPhraseParam, BufferedWriter writer) {

    for (int i = 0; i < bioPhraseParam.size(); i++) {
      try {
        writer.write(bioPhraseParam.get(i) + "\n");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * Main driver for handling a BIO input file and makign a BILOU outfile
   * @param sourceFileName
   * @param sourceEncoding
   * @param targetFileName
   * @param targetEncoding
   * @throws IOException
   */
  public void transcode(String sourceFileName, String sourceEncoding,
      String targetFileName, String targetEncoding)
      throws IOException {

    // init reader
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(sourceFileName),
            sourceEncoding));
    // init writer
    BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(targetFileName),
            targetEncoding));

    String line = "";
    System.out.println("Read BIO from: " + sourceFileName);
    System.out.println("Write BILOU to: " + targetFileName);

    // process complete conll-ner file line-wise
    while ((line = reader.readLine()) != null) {
      this.mapBIOtoBILOUschema(line, writer);
    }

    reader.close();
    writer.close();

    System.out.println(" ... Done!");
  }


  public static void main(String[] args) {

    try {
      BIO2BILOUtransformer bilou = new BIO2BILOUtransformer(true);
      // EN data
      bilou.transcode("resources/data/ner/en/eng-train.conll",
          "utf-8",
          "resources/data/ner/bilou/eng-train.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/en/eng-testa.conll",
          "utf-8",
          "resources/data/ner/bilou/eng-testa.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/en/eng-testb.conll",
          "utf-8",
          "resources/data/ner/bilou/eng-testb.conll",
          "utf-8");

      // DE data
      bilou.transcode("resources/data/ner/de/deu-train.conll",
          "utf-8",
          "resources/data/ner/bilou/deu-train.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/de/deu-testa.conll",
          "utf-8",
          "resources/data/ner/bilou/deu-testa.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/de/deu-testb.conll",
          "utf-8",
          "resources/data/ner/bilou/deu-testb.conll",
          "utf-8");
      // DE konvens data
      bilou = new BIO2BILOUtransformer(false);
      bilou.transcode("resources/data/ner/dekonvens/deu.konvens.train.conll",
          "utf-8",
          "resources/data/ner/bilou/deu.konvens.train.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/dekonvens/deu.konvens.dev.conll",
          "utf-8",
          "resources/data/ner/bilou/deu.konvens.dev.conll",
          "utf-8");
      bilou.transcode("resources/data/ner/dekonvens/deu.konvens.test.conll",
          "utf-8",
          "resources/data/ner/bilou/deu.konvens.test.conll",
          "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
