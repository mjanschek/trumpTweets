package application;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import repositories.AppProperties;
import streaming.TweetStreamer;
import twitter4j.TwitterException;

/**
 * This class:
 * * starts a Spark Stream
 * * writes data into up to three csv files
 */
public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException{
		
		
		/*
		 * Set logging level if log4j not configured (override by adding log4j.properties to classpath)
		 */
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			Logger.getRootLogger().setLevel(Level.ERROR);
		}
		
		/*
		 * Read properties file and save as static object
		 */
		new AppProperties();
		
		TweetStreamer streamer = new TweetStreamer();
		
		/**
		 * In tests, spark sometimes lost connection to unknown reasons. 
		 * This "retry" expression shall cover that to a certain point
		 */
		int retry = 0;
		while(true) {
			try{
				//start stream application
				streamer.stream();
			}catch(TwitterException e){
				retry++;
				if(retry==5) {
					break;
				}
				continue;
			}
		}

	}

}
