package streaming;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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


/**
 * Displays the most positive hash tags by joining the streaming Twitter data with a static RDD of
 * the AFINN word list (http://neuro.imm.dtu.dk/wiki/AFINN)
 */
@SuppressWarnings("serial")
public class TweetStreamer implements java.io.Serializable {

//    private static List<String> hashTagList;

	public TweetStreamer() {
//		readPropertyFile("src/main/resources/application.properties");
	}
	
//private void readPropertyFile(String filePath) {
//	Properties properties = new Properties();
//	try {
//		properties.load(new FileInputStream(filePath));
//	} catch (IOException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//	String hashTags[] = properties.getProperty("hashtags").split(",");
//	
//	hashTagList = new ArrayList<String>();
//	
//	for(String hashtag:hashTags) {
//		hashTagList.add(hashtag);
//	}
//	setHashTagList(hashTagList);
//}
	
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
    
    String filters[] = AppProperties.getHashTagList().stream().toArray(String[]::new);

    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(2000));
    jssc.sparkContext().setLogLevel("ERROR");
    JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);
    
    JavaDStream<Status> statuses = stream.filter(new Function<Status, Boolean>() {
		@Override
		public Boolean call(Status tweet) throws Exception {
			List <String> hashTagList = AppProperties.getHashTagList();
//			
//			System.out.println(filters);
			HashtagEntity[] hashTagEntities = tweet.getHashtagEntities();
//			
			
			for (HashtagEntity hashTag:hashTagEntities) {				
				if(hashTagList.contains(hashTag.getText())) {
					return tweet.getLang().equals("en");
					}
				}
			return false;			
		}
      });
    
    statuses.foreachRDD(new VoidFunction<JavaRDD<Status>>() {
		@Override
		public void call(JavaRDD<Status> allTweets) throws Exception {
			// TODO Auto-generated method stub
			List<Status> tweets = allTweets.collect();
			for(Status tweet:tweets) {
				try {
					String hashTags = "";
					for(HashtagEntity hashTag:tweet.getHashtagEntities()) {
						hashTags = hashTags + " " + hashTag.getText();
					}
					System.out.println(String.format("\nTIME: %s",tweet.getCreatedAt().toString()));
					System.out.println(String.format("USER: %s", tweet.getUser().getName()));
					System.out.println(String.format("HASHTAGS: %s", hashTags));
					System.out.printf(tweet.getText().toString() + "\n");
				}catch (Exception e){
					continue;
				}
			}
		}
    });

    jssc.start();
    try {
      jssc.awaitTermination();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


//public static List<String> getHashTagList() {
//	return hashTagList;
//}
//
//public void setHashTagList(List<String> hashTagList) {
//	this.hashTagList = hashTagList;
//}

}