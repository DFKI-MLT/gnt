package de.dfki.mlt.gnt.tagger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.mlt.gnt.archive.Archivator;
import de.dfki.mlt.gnt.config.ConfigKeys;
import de.dfki.mlt.gnt.config.CorpusConfig;
import de.dfki.mlt.gnt.config.GlobalConfig;
import de.dfki.mlt.gnt.config.ModelConfig;
import de.dfki.mlt.gnt.corpus.ConllEvaluator;
import de.dfki.mlt.gnt.data.Alphabet;
import de.dfki.mlt.gnt.data.Data;
import de.dfki.mlt.gnt.data.OffSets;
import de.dfki.mlt.gnt.data.Sentence;
import de.dfki.mlt.gnt.data.Window;
import de.dfki.mlt.gnt.tokenizer.GntSimpleTokenizer;
import de.dfki.mlt.gnt.trainer.ProblemInstance;

/**
 *
 *
 * @author Günter Neumann, DFKI
 */
public class GNTagger {

  private Data data;
  private Alphabet alphabet;
  private OffSets offSets;
  private int windowSize = 2;
  private Model model;
  private Archivator archivator;
  private ModelConfig modelConfig;


  public GNTagger(String modelArchiveName)
      throws IOException, ConfigurationException {

    this.archivator = new Archivator(modelArchiveName);
    System.out.println("Extract archive ...");
    System.out.println("Set dataProps ...");
    try (InputStream in =
        this.archivator.getInputStream(GlobalConfig.MODEL_CONFIG_FILE.toString())) {
      this.modelConfig = ModelConfig.create(in);
    }
    this.alphabet = new Alphabet(this.modelConfig);

    this.data = new Data();

    initGNTagger(
        this.modelConfig.getInt(ConfigKeys.WINDOW_SIZE), this.modelConfig.getInt(ConfigKeys.DIM));
  }


  private void initGNTagger(int windowSizeParam, int dim)
      throws UnsupportedEncodingException, IOException {

    long time1 = System.currentTimeMillis();

    System.out.println("Set window size: " + windowSizeParam);
    this.windowSize = windowSizeParam;
    System.out.println("Set window count: ");
    Window.setWindowCnt(0);

    System.out.println("Load feature files with dim: " + dim);
    this.alphabet.loadFeaturesFromFiles(this.archivator, dim);

    this.data.readLabelSet(this.archivator);

    System.out.println("Cleaning non-used variables in Alphabet and in Data:");
    this.alphabet.clean();

    System.out.println("Initialize offsets:");
    this.offSets = new OffSets(this.alphabet, this.data, this.windowSize);
    System.out.println("\t" + this.offSets.toString());

    long time2 = System.currentTimeMillis();
    System.out.println("System time (msec): " + (time2 - time1) + "\n");

    time1 = System.currentTimeMillis();

    System.out.println("Load model file from archive: " + this.modelConfig.getModelName() + ".txt");

    //this.setModel(Model.load(new File(this.getModelInfo().getModelFile())));
    try (
        InputStream in = this.archivator.getInputStream(this.modelConfig.getModelName() + ".txt")) {
      this.model = Linear.loadModel(new InputStreamReader(in, "UTF-8"));
    }
    System.out.println(".... DONE!");

    time2 = System.currentTimeMillis();
    System.out.println("System time (msec): " + (time2 - time1));
    System.out.println(this.model.toString() + "\n");
  }


  /**
   * Tags all files in the given directory.
   *
   * @param inputDir
   * @param inEncode
   * @param outEncode
   * @throws IOException
   */
  public void tagFolder(String inputDirName, String inEncode, String outEncode)
      throws IOException {

    Path inputDirPath = Paths.get(inputDirName);
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(inputDirPath, "*.{txt}")) {
      for (Path entry : stream) {
        long time1 = System.currentTimeMillis();

        System.out.println("Tagging file ... " + entry.toString());

        tagFile(entry, inEncode, outEncode);

        long time2 = System.currentTimeMillis();
        System.out.println("System time (msec): " + (time2 - time1));
      }
    }
  }


  /**
   * Tags each line of the given file and saves resulting tagged string in output file.
   * Output file is build from sourceFilename by adding suffix .GNT
   *
   * @param sourceFileName
   * @param inEncode
   * @param outEncode
   * @throws IOException
   */
  public void tagFile(Path sourcePath, String inEncode, String outEncode)
      throws IOException {

    Window.setWindowCnt(0);

    Path resultPath = Paths.get(sourcePath.toString() + ".GNT");
    try (BufferedReader in = Files.newBufferedReader(
        sourcePath, Charset.forName(inEncode));
        PrintWriter out = new PrintWriter(Files.newBufferedWriter(
            resultPath, Charset.forName(outEncode)))) {

      String line;
      while ((line = in.readLine()) != null) {
        if (!line.isEmpty()) {
          List<String> tokens = GntSimpleTokenizer.tokenize(line);
          Sentence sentence = tagUnlabeledTokens(tokens);
          String taggedString = taggedSentenceToString(sentence);
          for (String token : taggedString.split(" ")) {
            out.println(token);
          }
        }
      }
    }
  }


  /**
   * Tags the given string and outputs it in a line-oriented format.
   *
   * @param inputString
   * @return the tagged string
   */
  public String tagString(String inputString) {

    Window.setWindowCnt(0);

    List<String> tokens = GntSimpleTokenizer.tokenize(inputString);

    Sentence sentence = tagUnlabeledTokens(tokens);

    String taggedString = taggedSentenceToString(sentence);

    for (String token : taggedString.split(" ")) {
      System.out.println(token);
    }

    return taggedString;
  }


  /**
   * A method for tagging a single sentence given as list of tokens.
   * @param tokens
   */
  public Sentence tagUnlabeledTokens(List<String> tokens) {

    Window.setWindowCnt(0);

    // create internal sentence object
    Sentence sentence = this.data.generateSentenceObjectFromUnlabeledTokens(tokens);

    // tag sentence object
    this.tagSentenceObject(sentence);

    return sentence;
  }


  /**
   * Applies the tagger to the tokens of the given CoNLL tables.
   *
   * @param coNllTables
   *          CoNLL tables, each table representing a single sentence
   * @param tokenColumIndex
   *          column where to read the tokens from, zero-based
   * @param tagColumnIndex
   *          column where to add tags, zero-based
   * @return CoNLL tables, extended with computed tag
   */
  public List<String[][]> tagCoNllTables(
      List<String[][]> coNllTables, int tokenColumnIndex, int tagColumnIndex) {

    for (String[][] oneCoNllTable : coNllTables) {
      // collect tokens
      List<String> tokens = new ArrayList<>();
      for (int i = 0; i < oneCoNllTable.length; i++) {
        tokens.add(oneCoNllTable[i][tokenColumnIndex]);
      }
      // apply tagger
      Sentence taggedSentence = tagUnlabeledTokens(tokens);
      // write tags to CoNLL table
      for (int i = 0; i < taggedSentence.getTags().length; i++) {
        oneCoNllTable[i][tagColumnIndex] = taggedSentence.getTags()[i];
      }
    }

    return coNllTables;
  }


  private void tagSentenceObject(Sentence sentence) {

    // create window frames from sentence and store in list
    this.createWindowFramesFromSentence(sentence);

    // create feature vector instance for each window frame and tag
    this.constructProblemAndTag(false, true, sentence);

    // reset instances - need to do this here, because learner is called directly on windows
    this.data.cleanInstances();
  }


  // the same as trainer.TrainerInMem.createWindowFramesFromSentence()!
  private void createWindowFramesFromSentence(Sentence sentence) {

    // for each token t_i of current training sentence do
    // System.out.println("Sentence no: " + data.getSentenceCnt());
    int mod = 100000;
    for (int i = 0; i < sentence.getWords().length; i++) {
      // Assume that both arrays together define an ordered one-to-one correspondence
      // between token and label (POS)
      int labelIndex = this.data.getLabelSet().getIndex(sentence.getTags()[i]);

      // create local context for tagging t_i of size 2*windowSize+1 centered around t_i
      Window tokenWindow = new Window(sentence, i, this.windowSize, this.data, this.alphabet);
      // This basically has no effect during tagging
      tokenWindow.setLabelIndex(labelIndex);

      this.data.getInstances().add(tokenWindow);

      // Print how many windows are created so far, and pretty print every mod-th window
      if ((Window.getWindowCnt() % mod) == 0) {
        System.out.println("# Window instances: " + Window.getWindowCnt());
      }
    }
  }


  /**
   * Iterate through all window frames:
   * - create the feature vector: train=false means: handle unknown words; adjust=true:
   *   means adjust feature indices
   * - create a problem instance -> mainly the feature vector
   * - and call the learner with model and feature vector
   * - save the predicted label in the corresponding field of the word in the sentence.
   *
   * Mainly the same as trainer.TrainerInMem.constructProblem(train, adjust), but uses predictor
   */
  private void constructProblemAndTag(boolean train, boolean adjust, Sentence sentence) {

    int prediction = 0;

    for (int i = 0; i < this.data.getInstances().size(); i++) {
      // For each window frame of a sentence
      Window nextWindow = this.data.getInstances().get(i);
      // Fill the frame with all available features. First boolean sets
      // training mode to false which means that unknown words are handled.
      nextWindow.setOffSets(this.offSets);
      nextWindow.fillWindow(train, adjust);
      // Create the feature vector
      ProblemInstance problemInstance = new ProblemInstance();
      problemInstance.createProblemInstanceFromWindow(nextWindow);

      // Call the learner to predict the label
      prediction = (int)Linear.predict(this.model, problemInstance.getFeatureVector());
      /*
      System.out.println(
          "Word: "
              + this.data.getWordSet().getNum2label().get(this.data.getSentence().getWordArray()[i])
              + "\tPrediction: " + this.data.getLabelSet().getNum2label().get(prediction));
      */

      //  Here, I am assuming that sentence length equals # of windows
      // So store predicted label i to word i
      String tag = this.data.getLabelSet().getLabel(prediction);
      sentence.getTags()[i] = tag;

      // Free space by resetting filled window to unfilled-window
      nextWindow.clean();
    }
  }


  /**
   * A simple print out of a sentence in form of list of word/tag
   * @return
   */
  private String taggedSentenceToString(Sentence sentence) {

    StringBuilder output = new StringBuilder();
    for (int i = 0; i < sentence.getWords().length; i++) {
      String word = sentence.getWords()[i];
      String label = sentence.getTags()[i];

      label = PostProcessor.determineTwitterLabel(word, label);

      output.append(word + "/" + label + " ");
    }
    return output.toString();
  }


  /**
   * Evaluates the performance of the tagger using the given annotated corpus.
   * NOTE:
   * it computes evaluation files for devel and test file in that order
   * and sets variables in evaluator class (accuracy for pos/oov/inv.
   * but it overwrites these values so that only numbers for test will survive.
   *
   * @param corpusConfigFileName
   *          corpus configuration file name
   */
  public ConllEvaluator eval(String corpusConfigFileName)
      throws IOException, ConfigurationException {

    CorpusConfig corpusConfig = CorpusConfig.create(corpusConfigFileName);

    int wordFormIndex = corpusConfig.getInt(ConfigKeys.WORD_FORM_INDEX);
    int tagIndex = corpusConfig.getInt(ConfigKeys.TAG_INDEX);

    Data wordSetData = new Data();
    wordSetData.readWordSet(this.archivator);
    System.out.println(" words: " + wordSetData.getWordSet().size());
    ConllEvaluator evaluator = new ConllEvaluator(wordSetData.getWordSet());

    for (String fileName : corpusConfig.getList(String.class, ConfigKeys.DEV_LABELED_DATA,
        Collections.emptyList())) {
      Path evalPath = tagAndWriteFromConllDevelFile(fileName, -1, wordFormIndex, tagIndex);
      evaluator.computeAccuracy(evalPath, GlobalConfig.getBoolean(ConfigKeys.DEBUG));
    }

    for (String fileName : corpusConfig.getList(String.class, ConfigKeys.TEST_LABELED_DATA,
        Collections.emptyList())) {
      Path evalPath = tagAndWriteFromConllDevelFile(fileName, -1, wordFormIndex, tagIndex);
      evaluator.computeAccuracy(evalPath, GlobalConfig.getBoolean(ConfigKeys.DEBUG));
      }

    return evaluator;
  }

  /**
   * This version additionally creates an output file which can be used by the official UD evaluation script
   * AND can be used as input also for testing MDParser on predicted POS tags !
   *
   * @param corpusConfigFileName
   * @param sourceConnlFile
   * @param targetConllFile
   * @return
   * @throws IOException
   * @throws ConfigurationException
   */
  public ConllEvaluator evalAndWriteResultFile(String corpusConfigFileName, String sourceConnlFile, String targetConllFile)
      throws IOException, ConfigurationException {

    CorpusConfig corpusConfig = CorpusConfig.create(corpusConfigFileName);

    int wordFormIndex = corpusConfig.getInt(ConfigKeys.WORD_FORM_INDEX);
    int tagIndex = corpusConfig.getInt(ConfigKeys.TAG_INDEX);

    Data wordSetData = new Data();
    wordSetData.readWordSet(this.archivator);
    System.out.println(" words: " + wordSetData.getWordSet().size());
    ConllEvaluator evaluator = new ConllEvaluator(wordSetData.getWordSet());

    for (String fileName : corpusConfig.getList(String.class, ConfigKeys.DEV_LABELED_DATA,
        Collections.emptyList())) {
      Path evalPath = tagAndWriteFromConllDevelFile(fileName, -1, wordFormIndex, tagIndex);
      evaluator.computeAccuracy(evalPath, GlobalConfig.getBoolean(ConfigKeys.DEBUG));
    }

    for (String fileName : corpusConfig.getList(String.class, ConfigKeys.TEST_LABELED_DATA,
        Collections.emptyList())) {
      Path evalPath = tagAndWriteFromConllDevelFile(fileName, -1, wordFormIndex, tagIndex);
      evaluator.computeAccuracy(evalPath, GlobalConfig.getBoolean(ConfigKeys.DEBUG));

      // Create UD conform output file: overwrite gold tag with predicted tag in conll gold file and store it in result file
      this.createConllResultFileFromEvalFile(tagIndex, evalPath, sourceConnlFile, targetConllFile);
      }

    return evaluator;
  }

  /*
   * Given an evalFile and a conllSourceFile
   * make a new conllTargetFile
   * by copy all conll fields from source file to target file
   * but the tag index field
   * Here use the second (counting from 0) column from eval file
   * and overwrite column index by tagIndex
   *
   *
   */

  private void createConllResultFileFromEvalFile(int tagIndex, Path evalPath, String conllSourceFile, String  conllTargetFile) {

    try (BufferedReader evalReader = Files.newBufferedReader(evalPath, StandardCharsets.UTF_8);
        BufferedReader sourceFileReader = Files.newBufferedReader(Paths.get(conllSourceFile), StandardCharsets.UTF_8);
        PrintWriter targetFileWriter =
            new PrintWriter(Files.newBufferedWriter(Paths.get(conllTargetFile), StandardCharsets.UTF_8))) {

      String evalLine = "";
      String sourceLine = "";
      int evalPredictedTagIndex = 3;
      while ((evalLine = evalReader.readLine()) != null) {
        sourceLine = sourceFileReader.readLine();

        if (evalLine.isEmpty()) {
          targetFileWriter.println();
          } else {
            String[] evalTokens = evalLine.split(" ");

            String[] sourceToken = sourceLine.split("\t");

            sourceToken[tagIndex] = evalTokens[evalPredictedTagIndex];
            String newConllToken = sourceToken[0];
            for (int i = 1; i < sourceToken.length; i++) {
              newConllToken += "\t" + sourceToken[i];
            }
            targetFileWriter.write(newConllToken);
            targetFileWriter.println();
        }
      }
      evalReader.close();
      sourceFileReader.close();
      targetFileWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  // This is the current main caller for the GNTagger
  private Path tagAndWriteFromConllDevelFile(
      String sourceFileName, int sentenceCnt, int wordFormIndex, int tagIndex)
      throws IOException {

    long localTime1;
    long localTime2;

    System.out.println("\n++++\nDo testing from file: " + sourceFileName);
    // Reset some data to make sure each file has same change
    this.data.setSentenceCnt(0);
    Window.setWindowCnt(0);

    localTime1 = System.currentTimeMillis();

    Path evalPath = this.tagAndWriteSentencesFromConllReader(
        sourceFileName, sentenceCnt, wordFormIndex, tagIndex);

    localTime2 = System.currentTimeMillis();
    System.out.println("System time (msec): " + (localTime2 - localTime1));

    long tokenPerSec = (Window.getWindowCnt() * 1000) / (localTime2 - localTime1);
    System.out.println("Sentences: " + this.data.getSentenceCnt());
    System.out.println("Testing instances: " + Window.getWindowCnt());
    System.out.println(
        "Sentences/sec: " + (this.data.getSentenceCnt() * 1000) / (localTime2 - localTime1));
    System.out.println("Words/sec: " + tokenPerSec);

    return evalPath;
  }


  private Path tagAndWriteSentencesFromConllReader(
      String sourceFileName, int max, int wordFormIndex, int tagIndex)
      throws IOException {

    Path sourcePath = Paths.get(sourceFileName);
    String evalFileName = sourcePath.getFileName().toString();
    evalFileName = evalFileName.substring(0, evalFileName.lastIndexOf(".")) + ".txt";
    Path evalPath = GlobalConfig.getPath(ConfigKeys.EVAL_FOLDER).resolve(evalFileName);
    Files.createDirectories(evalPath.getParent());
    System.out.println("Create eval file: " + evalPath);

    try (BufferedReader conllReader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8);
        PrintWriter conllWriter =
            new PrintWriter(Files.newBufferedWriter(evalPath, StandardCharsets.UTF_8))) {

      String line = "";
      List<String[]> tokens = new ArrayList<String[]>();

      while ((line = conllReader.readLine()) != null) {
        if (line.isEmpty()) {
          // For found sentence, do tagging:
          // Stop if max sentences have been processed
          if ((max > 0) && (this.data.getSentenceCnt() > max)) {
            break;
          }

          // create internal sentence object and label maps
          // Use specified label from conll file for evaluation purposes later
          Sentence sentence =
              this.data.generateSentenceObjectFromConllLabeledSentence(
                  tokens, wordFormIndex, tagIndex);

          // tag sentence object
          this.tagSentenceObject(sentence);

          // Create conlleval consistent output using original conll tokens plus predicted labels
          this.writeTokensAndWithLabels(conllWriter, tokens, sentence, wordFormIndex, tagIndex);

          // reset tokens
          tokens = new ArrayList<String[]>();
        } else {
          // Collect all the words of a conll sentence

          String[] tokenizedLine = line.split("\t");
          tokens.add(tokenizedLine);
        }
      }
    }

    return evalPath;
  }


  // NOTE:
  // I have adjusted the NER conll format to be consistent with the other conll formats, i.e.,
  // LONDON NNP I-NP I-LOC -> 1  LONDON  NNP  I-NP  I-LOC
  // This is why I have 5 elements instead of 4
  private void writeTokensAndWithLabels(
      PrintWriter conllWriter, List<String[]> tokens, Sentence sentence,
      int wordFormIndex, int tagIndex) {

    for (int i = 0; i < tokens.size(); i++) {
      String[] token = tokens.get(i);
      String label = sentence.getTags()[i];

      String word = token[wordFormIndex];

      label = PostProcessor.determineTwitterLabel(word, label);


      String newConllToken = token[0] + " "
          + word + " "
          + token[tagIndex] + " "
          + label;

      conllWriter.println(newConllToken);
    }
    conllWriter.println();
  }

}
