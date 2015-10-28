package test;

import java.io.IOException;

import trainer.GNTrainer;
import data.Alphabet;
import data.ModelInfo;

public class TrainPosTagger {

	public static void main(String[] args) throws IOException{
		ModelInfo modelInfo = new ModelInfo("FLORS");
		modelInfo.setTaggerName("POS");
		
		int windowSize = 2;
		int numberOfSentences = -1;
		int dim = 0;
		double subSamplingThreshold = 0.000000001;
		Alphabet.withWordFeats=false;
		Alphabet.withShapeFeats=true;
		Alphabet.withSuffixFeats=true;
		Alphabet.withClusterFeats=true;
		System.out.println(Alphabet.toActiveFeatureString());
		
		modelInfo.createModelFileName(windowSize, dim, numberOfSentences);
		System.out.println(modelInfo.toString());
		
		GNTrainer gnTrainer = new GNTrainer(modelInfo, windowSize, subSamplingThreshold);
		String trainingFileName = "resources/data/english/ptb3-training";
		String clusterIdSourceFileName = "/Users/gune00/data/Marmot/Word/en_marlin_cluster_1000";

		gnTrainer.gntTrainingWithDimensionFromConllFile(trainingFileName, clusterIdSourceFileName, dim, numberOfSentences);

	}

}
