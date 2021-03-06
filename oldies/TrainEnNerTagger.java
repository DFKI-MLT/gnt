package test;

import java.io.IOException;

import trainer.GNTrainer;
import trainer.TrainerInMem;
import data.Alphabet;
import data.ModelInfo;
import features.WordSuffixFeatureFactory;

public class TrainEnNerTagger {

	public static void main(String[] args) throws IOException{
		ModelInfo modelInfo = new ModelInfo("MDP");
		modelInfo.setTaggerName("NER");
		
		ModelInfo.saveModelInputFile = false;

		int windowSize = 2;
		int numberOfSentences = -1;
		int dim = 50;
		double subSamplingThreshold = 0.000000001;
		Alphabet.withWordFeats=false;
		Alphabet.withShapeFeats=true;
		Alphabet.withSuffixFeats=true;
		Alphabet.withClusterFeats=true;
		System.out.println(Alphabet.toActiveFeatureString());
		
		TrainerInMem.debug=false;
		
		WordSuffixFeatureFactory.ngram = false;

		modelInfo.createModelFileName(windowSize, dim, numberOfSentences);
		System.out.println(modelInfo.toString());
		
		GNTrainer gnTrainer = new GNTrainer(modelInfo, windowSize, subSamplingThreshold);
		String trainingFileName = "resources/data/ner/en/eng-train";
		String clusterIdSourceFileName = "/Users/gune00/data/Marmot/Word/en_marlin_cluster_500";

		gnTrainer.gntTrainingWithDimensionFromConllFile(trainingFileName, clusterIdSourceFileName, dim, numberOfSentences);
	}
}
