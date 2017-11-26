package application;

import java.io.FileNotFoundException;
import java.io.IOException;
import repositories.AppProperties;
import repositories.PredictionReader;
import streaming.TweetStreamer;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties();
		
		PredictionReader predictions = null;
		
		
		// this takes a while...
		if(AppProperties.isUsePredictions()) {
			System.out.println("Reading Predictions...");
			double start = System.currentTimeMillis();
			predictions = new PredictionReader();
			double end = System.currentTimeMillis();
			
			double time = (end - start)/1000/60;
			
			System.out.println("Done after " + time + " minutes.");
		}
		
		TweetStreamer streamer = new TweetStreamer(predictions);
		streamer.stream();

	}

}
