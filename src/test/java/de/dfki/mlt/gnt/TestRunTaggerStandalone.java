package de.dfki.mlt.gnt;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;

import de.dfki.mlt.gnt.tagger.GNTagger;

/**
 *
 *
 * @author Günter Neumann, DFKI
 */
public final class TestRunTaggerStandalone {

  private TestRunTaggerStandalone() {

    // private constructor to enforce noninstantiability
  }


  public static void main(String[] args) throws IOException, ConfigurationException {

    GNTagger tagger =
        new GNTagger("resources/models/model_DENERKONV_2_0iw-1sent_FTTTT_MCSVM_CS.zip");

    System.out.println("Tag text: ");
    tagger.tagString(
        "Die Bundesanwaltschaft in Karlsruhe hat die Ermittlungen gegen einen terrorverdächtigen "
            + "Bundeswehroffizier übernommen. "
            + "Es bestehe der Anfangsverdacht der Vorbereitung einer schweren staatsgefährdenden "
            + "Gewalttat, sagte ein Sprecher."
            + "Oberleutnant Franco A. war Anfang Februar in Österreich aufgefallen, weil er auf "
            + "dem Flughafen Wien eine Pistole in einer Toilette versteckt hatte. "
            + "Ermittlungen ergaben, dass er sich in Deutschland unter falschem Namen als "
            + "syrischer Flüchtling ausgab. Daraufhin wurde er vergangenen Mittwoch festgenommen. "
            + "Die Staatsanwaltschaft in Frankfurt geht davon aus, dass mit der Waffe eine schwere "
            + "staatsgefährdende Straftat geplant war. Franco A. und ein 24-jähriger mutmaßlicher "
            + "Komplize sitzen in Untersuchungshaft. Wir haben Unstimmigkeiten festgestellt\", "
            + "sagte der Generalinspekteur der Bundeswehr, General Volker Wieker. e aus den "
            + "vergangenen Monaten zu sprechen.");
    /*
    runner.tagFileRunner(
        "/Users/gune00/data/InformationExtraction/GoogleRelations/all.txt", "UTF-8", "utf-8");
    */
  }
}
