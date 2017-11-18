package application;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import streaming.TweetStreamer;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties();
		
		TweetStreamer streamer = new TweetStreamer();
		streamer.stream();
	}

}
