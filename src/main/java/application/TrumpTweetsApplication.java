package application;

import java.io.FileNotFoundException;
import java.io.IOException;
import streaming.TweetStreamer;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties();
		
		PredictionReader predictions = new PredictionReader();	
		
		TweetStreamer streamer = new TweetStreamer(predictions);
		streamer.stream();
	}

}
