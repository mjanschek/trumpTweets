package streaming;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;

import application.AppProperties;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;


/**
 * Displays the most positive hash tags by joining the streaming Twitter data with a static RDD of
 * the AFINN word list (http://neuro.imm.dtu.dk/wiki/AFINN)
 */
@SuppressWarnings("serial")
public class TweetStreamer implements java.io.Serializable {
	
public void stream() {

    //StreamingExamples.setStreamingLogLevels();
    // Set logging level if log4j not configured (override by adding log4j.properties to classpath)
    if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
      Logger.getRootLogger().setLevel(Level.WARN);
    }
    
    SparkConf sparkConf = new SparkConf().setAppName("TweetStreamer");

    // check Spark configuration for master URL, set it to local if not configured
    if (!sparkConf.contains("spark.master")) {
      sparkConf.setMaster("local[8]");
    }
    
    String filters[] = AppProperties.getFilters();

    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(1000));
    jssc.sparkContext().setLogLevel("ERROR");
    JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);
    
    JavaDStream<Status> statuses = stream.filter(new Function<Status, Boolean>() {
		@Override
		public Boolean call(Status tweet) throws Exception {
			return tweet.getLang().equals("en");
		}
      });
    

    
    statuses.foreachRDD(new VoidFunction<JavaRDD<Status>>() {
		@Override
		public void call(JavaRDD<Status> allTweets) throws Exception {
			// TODO Auto-generated method stub
			List<Status> tweets = allTweets.collect();
			
			FileWriter writer = new FileWriter(AppProperties.getCsvFilepath(), true);
			
			for(Status tweet:tweets) {
				try {
					HashtagEntity[] hashTagEnts = tweet.getHashtagEntities();
					String[] hashTags = new String[hashTagEnts.length];
					for(int i = 0; i<hashTagEnts.length; i++) {
						hashTags[i] = hashTagEnts[i].getText();
					}
					
					String timestamp = tweet.getCreatedAt().toString();
					String userId = Long.toString(tweet.getUser().getId());
					String tweetId = Long.toString(tweet.getId());
					String followers = Integer.toString(tweet.getUser().getFollowersCount());
					String hashTagString = String.join(",", hashTags);
					String favorites = Integer.toString(tweet.getFavoriteCount());
					String retweets = Integer.toString(tweet.getRetweetCount());
					String text = tweet.getText().toString();
					
					CSVUtils.writeLine(writer, 
							Arrays.asList(timestamp,
									userId,
									tweetId,
									followers,
									hashTagString,
									favorites,
									retweets,
									text),
							';', '"');
					
				}catch (Exception e){
					continue;
				}
			}
			writer.flush();
			writer.close();
		}
    });

    jssc.start();
    try {
      jssc.awaitTermination();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
  }
}