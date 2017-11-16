package application;

import java.io.FileNotFoundException;
import java.io.IOException;

import streaming.TweetStreamer;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties("src/main/resources/application.properties");
		
		TweetStreamer streamer = new TweetStreamer();
		streamer.stream();
	}

}
