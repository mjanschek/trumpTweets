package streaming;

import application.AppProperties;
import application.PredictionReader;

import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.UserMentionEntity;


/**
 * Displays the most positive hash tags by joining the streaming Twitter data with a static RDD of
 * the AFINN word list (http://neuro.imm.dtu.dk/wiki/AFINN)
 */
@SuppressWarnings("serial")
public class TweetStreamer implements java.io.Serializable {
	
	private PredictionReader predictions;
	
	public TweetStreamer(PredictionReader predictions) {
		this.setPredictions(predictions);
	}

	public void stream() {

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

		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(200));
		jssc.sparkContext().setLogLevel("ERROR");
		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);

		/**
		 * START OF FILTERING BLOCK
		 * filter for english tweets exclude retweets and truncated retweets
		 * also, filter for wanted hashtags
		 */
		JavaDStream<Status> englishStatuses = stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return tweet.getLang().equals("en");
			}
		});

		JavaDStream<Status> statusesNoRetweets = englishStatuses.map(new Function<Status, Status>() {
			@Override
			public Status call(Status tweet) throws Exception {
				if(tweet.isRetweet()) {
					tweet = tweet.getRetweetedStatus();
				}
				return tweet;
			}
		});

		JavaDStream<Status> statusesNoTruncation = statusesNoRetweets.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return !tweet.isTruncated();
			}
		});

		JavaDStream<Status> wantedHashtags = statusesNoTruncation.filter(filterForHashtags(AppProperties.getHashTagListNormalized()));

		/**
		 * END OF FILTERING BLOCK
		 */

		statusesNoTruncation.foreachRDD(writeTweets(AppProperties.getSaveDir() + "allTweets.csv"));
		wantedHashtags.foreachRDD(writeTweets(AppProperties.getSaveDir() + "filteredTweets.csv"));

		jssc.start();
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Function<Status, Boolean> filterForHashtags(List <String> hashTagFilterList) { return new Function<Status, Boolean>() {
		@Override
		public Boolean call(Status tweet) throws Exception {
			List <String> hashTagList = new ArrayList<String>();
			for(HashtagEntity hashTag:tweet.getHashtagEntities()) {
				//normalize hashtags, ignore casing
				hashTagList.add(hashTag.getText().toLowerCase());
			}
			return !Collections.disjoint(hashTagList, hashTagFilterList);
		}
	};
	}

	public VoidFunction<JavaRDD<Status>> writeTweets(String filePath){return new VoidFunction<JavaRDD<Status>>() {
		@Override
		public void call(JavaRDD<Status> allTweets) throws Exception {
			// TODO Auto-generated method stub
			List<Status> tweets = allTweets.collect();

			boolean firstLine = false;
			File f = new File(filePath);
			if(!f.exists() && !f.isDirectory()) { 
				f.createNewFile();
				firstLine = true;
			}

			FileWriter writer = new FileWriter(f,true);
			if(firstLine) {
				String header = "\"timestamp\";"
						+ "\"userId\";"
						+ "\"userName\";"
						+ "\"followers\";"
						+ "\"tweetId\";"
						+ "\"hashTagString\";"
						+ "\"userIdsMentioned\";"
						+ "\"favorites\";"
						+ "\"retweets\";"
						+ "\"place\";"
						+ "\"text\";"
						+ "\"textLength\";"
						+ "\"isTrumpTweet\";"
						+ "\"isNewsTweet\";"
						+ "\"isFakeNewsTweet\";"
						+ "\"isDemocratsTweet\";"
						+ "\"isWashingtonDCTweet\""
						+ "\n";
				writer.write(header);
			}

			for(Status tweet:tweets) {
				try {
					List <String> hashTagList = new ArrayList<String>();
					for(HashtagEntity hashTag:tweet.getHashtagEntities()) {
						//normalize hashtags, ignore casing
						hashTagList.add(hashTag.getText().toLowerCase());
					}

					List <String> userMentionsList = new ArrayList<String>();
					for(UserMentionEntity userMention:tweet.getUserMentionEntities()) {
						userMentionsList.add(Long.toString(userMention.getId()));
					}


					String timestamp 		= tweet.getCreatedAt().toString();
					String userId 			= Long.toString(tweet.getUser().getId());
					String userName 		= tweet.getUser().getName();
					String followers 		= Integer.toString(tweet.getUser().getFollowersCount());
					String tweetId 			= Long.toString(tweet.getId());
					String hashTagString 	= String.join(",", hashTagList);
					String userIdsMentioned = String.join(",", userMentionsList);
					String favorites 		= Integer.toString(tweet.getFavoriteCount());
					String retweets 		= Integer.toString(tweet.getRetweetCount());
					String place 			= "";
					if(tweet.getPlace() != null) {
						place 				= tweet.getPlace().getFullName();						
					}
					String text 			= tweet.getText().toString();
					String textLength 		= Integer.toString(text.length());


					String isTrumpTweet 		= Boolean.toString(hashTagList.contains("trump"));
					String isNewsTweet 			= Boolean.toString(hashTagList.contains("news"));
					String isFakeNewsTweet 		= Boolean.toString(hashTagList.contains("fakenews"));
					String isDemocratsTweet 	= Boolean.toString(hashTagList.contains("democrats"));
					String isWashingtonDCTweet 	= Boolean.toString(hashTagList.contains("washingtondc"));

					CSVUtils.writeLine(writer,
							Arrays.asList(timestamp,
									userId,
									userName,
									followers,
									tweetId,
									hashTagString,
									userIdsMentioned,
									favorites,
									retweets,
									place,
									text,
									textLength,
									isTrumpTweet,
									isNewsTweet,
									isFakeNewsTweet,
									isDemocratsTweet,
									isWashingtonDCTweet),
							';', '"');

				}catch (Exception e){
					continue;
				}
			}
			writer.flush();
			writer.close();
		}
	};
	}

	public PredictionReader getPredictions() {
		return predictions;
	}

	public void setPredictions(PredictionReader predictions) {
		this.predictions = predictions;
	}
}