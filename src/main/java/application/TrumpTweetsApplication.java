package application;

import java.io.FileNotFoundException;
import java.io.IOException;
import streaming.TweetStreamer;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties();
		
		TweetStreamer streamer = new TweetStreamer();
		streamer.stream();
	}

}
