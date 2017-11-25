package streaming;

import repositories.AppProperties;
import repositories.Combo;
import repositories.PredictionReader;
import scala.Tuple2;
import scala.Tuple5;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
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
		this.predictions = predictions;
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

		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(1000));
		jssc.sparkContext().setLogLevel("ERROR");
		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);
		
		JavaPairDStream<Status,Combo> tweetsComboMatch = 
				filterTweets(stream).mapToPair(new PairFunction<Status,Status,Combo>(){
					@Override
					public Tuple2<Status,Combo> call(Status status) throws Exception {

						List <String> hashTagList = new ArrayList<String>();
						for(HashtagEntity hashTag:status.getHashtagEntities()) {
							//normalize hashtags, ignore casing
							hashTagList.add(hashTag.getText().toLowerCase());	
						}
						boolean hasTrumpHashtag = hashTagList.contains("trump");
						boolean hasNewsHashtag = hashTagList.contains("news");
						boolean hasFakeNewsHashtag = hashTagList.contains("fakenews");
						boolean hasDemocratsHashtag = hashTagList.contains("democrats");
						boolean hasWdcHashtag = hashTagList.contains("washingtondc");
						
						return new Tuple2<>(status, 
								new Combo(hasTrumpHashtag,hasNewsHashtag,hasFakeNewsHashtag,hasDemocratsHashtag,hasWdcHashtag));
					}
				});

		tweetsComboMatch.foreachRDD(writeTweets(AppProperties.getSaveDir() + AppProperties.getTweetsSavefile()));
		
		JavaPairDStream<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>> tweetsComboCounts =
				tweetsComboMatch.mapToPair(new PairFunction<Tuple2<Status,Combo>,Combo,Tuple5<Integer,Integer,Integer,Integer,Integer>>(){

					@Override
					public Tuple2<Combo, Tuple5<Integer,Integer,Integer,Integer,Integer>> call(Tuple2<Status, Combo> input) throws Exception {
						// TODO Auto-generated method stub
						
						Status status = input._1;
						String text = status.getText();
						
						Integer count = 1;
						Integer textLength = text.length();
						Integer totalHashtagCount = status.getHashtagEntities().length;
						
						String sub = "trump";						
						String temp = text.toLowerCase().replace(sub, "");
						Integer trumpCount = (text.length() - temp.length()) / sub.length();

						Integer sensitiveCount = 0;
						if(status.isPossiblySensitive()) {
							sensitiveCount = 1;
						}
						
						return new Tuple2<>(input._2,new Tuple5<>(count, textLength, totalHashtagCount, trumpCount, sensitiveCount));
					}
				})
								.reduceByKeyAndWindow(new Function2<Tuple5<Integer,Integer,Integer,Integer,Integer>,
															Tuple5<Integer,Integer,Integer,Integer,Integer>,
															Tuple5<Integer,Integer,Integer,Integer,Integer>>() {
	        @Override
	        public Tuple5<Integer,Integer,Integer,Integer,Integer> call(Tuple5<Integer,Integer,Integer,Integer,Integer> a,
	        															Tuple5<Integer,Integer,Integer,Integer,Integer> b) {
	        	
	        	Integer count = a._1() + b._1();
	        	Integer textLength = a._2() + b._2();
	        	Integer totalHashtagCount = a._3() + b._3();
	        	Integer trumpCount = a._4() + b._4();
	        	Integer sensitiveCount = a._5() + b._5();
	        	
	        	return new Tuple5<>(count, textLength, totalHashtagCount, trumpCount, sensitiveCount);
	        }
	      }, new Duration(60*1000));
		
		tweetsComboCounts.foreachRDD(writeComboCounts(AppProperties.getSaveDir() + AppProperties.getComboSavefile()));

		jssc.start();
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
 	public VoidFunction<JavaPairRDD<Combo, Tuple5<Integer,Integer,Integer,Integer,Integer>>> writeComboCounts(String filePath){
		return new VoidFunction<JavaPairRDD<Combo, Tuple5<Integer,Integer,Integer,Integer,Integer>>>(){

			@Override
			public void call(JavaPairRDD<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>> allComboCounts)
					throws Exception {
				// TODO Auto-generated method stub
				List<Tuple2<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>>> comboCounts = allComboCounts.collect();
				
				boolean firstLine = false;
				File f = new File(filePath);
				if(!f.exists() && !f.isDirectory()) { 
					f.createNewFile();
					firstLine = true;
				}

				FileWriter writer = new FileWriter(f,true);
				if(firstLine) {
					String header = "\"timestamp\";"
									+ new Combo().getCsvHeader() + ";"
									+ "\"count\";"
									+ "\"meanTextLength\";"
									+ "\"totalHashtagCount\";"
									+ "\"totalTrumpCount\";"
									+ "\"totalSensitiveCount\""
									+ "\n";
					writer.write(header);
				}
				
				for(Tuple2<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>> comboCount:comboCounts) {
					try {
						Date timestamp = new java.util.Date();
			        	
						Combo combo = comboCount._1;
						
						Double count = (double) comboCount._2._1();
			        	Double meanTextLength = (double) comboCount._2._2()/count;
			        	Integer totalHashtagCount = comboCount._2._3();;
			        	Integer totalTrumpCount = comboCount._2._4();;
			        	Integer totalSensitiveCount = comboCount._2._5();;
			        	
			        	CSVUtils.writeLine(writer,
								Arrays.asList(timestamp.toString(),
										Boolean.toString(combo.isTrumpTweet()),
										Boolean.toString(combo.isNewsTweet()),
										Boolean.toString(combo.isFakeNewsTweet()),
										Boolean.toString(combo.isDemocratsTweet()),
										Boolean.toString(combo.isWashingtonDCTweet()),
										Double.toString(count),
										Double.toString(meanTextLength),
										Integer.toString(totalHashtagCount),
										Integer.toString(totalTrumpCount),
										Integer.toString(totalSensitiveCount)
										),
								';', '"');
			        	
//			        	System.out.println("Timestamp: " + timestamp.toString() + "\n"
//			        					 + combo.toString() + "\n"
//		        					 	 + "Count: " + count + "\n"
//			        					 + "Mean textlength: " + meanTextLength + "\n"
//		        					 	 + "Total hashtag count: " + totalHashtagCount + "\n"
//			        					 + "Total Trump count: " + totalTrumpCount + "\n"
//		        					 	 + "Total Sensitive count: " + totalSensitiveCount + "\n");
					} catch (Exception e){
						continue;
					}
					
				}
				writer.flush();
				writer.close();
			}
		};
    }
 	
	
	public VoidFunction<JavaPairRDD<Status, Combo>> writeTweets(String filePath){
		return new VoidFunction<JavaPairRDD<Status, Combo>>() {
		@Override
		public void call(JavaPairRDD<Status, Combo> allTweetsCombos) throws Exception {
			List<Tuple2<Status, Combo>> tweetCombos = allTweetsCombos.collect();

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
						+ new Combo().getCsvHeader()
						+ "\n";
				writer.write(header);
			}

			for(Tuple2<Status, Combo> tweetCombo:tweetCombos) {
				try {
					Status status = tweetCombo._1;
					Combo combo = tweetCombo._2;
					
					List <String> hashTagList = new ArrayList<String>();
					for(HashtagEntity hashTag:status.getHashtagEntities()) {
						//normalize hashtags, ignore casing
						hashTagList.add(hashTag.getText().toLowerCase());
					}

					List <String> userMentionsList = new ArrayList<String>();
					for(UserMentionEntity userMention:status.getUserMentionEntities()) {
						userMentionsList.add(Long.toString(userMention.getId()));
					}


					String timestamp 		= status.getCreatedAt().toString();
					String userId 			= Long.toString(status.getUser().getId());
					String userName 		= status.getUser().getName();
					String followers 		= Integer.toString(status.getUser().getFollowersCount());
					String tweetId 			= Long.toString(status.getId());
					String hashTagString 	= String.join(",", hashTagList);
					String userIdsMentioned = String.join(",", userMentionsList);
					String favorites 		= Integer.toString(status.getFavoriteCount());
					String retweets 		= Integer.toString(status.getRetweetCount());
					String place 			= "";
					if(status.getPlace() != null) {
						place 				= status.getPlace().getFullName();						
					}
					String text 			= status.getText().toString();
					String textLength 		= Integer.toString(text.length());

					String isTrumpTweet 		= Boolean.toString(combo.isTrumpTweet());
					String isNewsTweet 			= Boolean.toString(combo.isNewsTweet());
					String isFakeNewsTweet 		= Boolean.toString(combo.isFakeNewsTweet());
					String isDemocratsTweet 	= Boolean.toString(combo.isDemocratsTweet());
					String isWashingtonDCTweet 	= Boolean.toString(combo.isWashingtonDCTweet());

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
									isWashingtonDCTweet
									),
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
	
	

	public JavaDStream<Status> filterTweets(JavaReceiverInputDStream<Status> stream){
		return stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return tweet.getLang().equals("en");
			}
		})
					 .map(new Function<Status, Status>() {
			@Override
			public Status call(Status tweet) throws Exception {
				if(tweet.isRetweet()) {
					tweet = tweet.getRetweetedStatus();
				}
				return tweet;
			}
		})
					 .filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return !tweet.isTruncated();
			}
		});
		
	}
	
	
	public PredictionReader getPredictions() {
		return predictions;
	}

	public void setPredictions(PredictionReader predictions) {
		this.predictions = predictions;
	}
}