package streaming;

import repositories.AppProperties;
import repositories.Combo;
import repositories.TimeComboPrediction;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple5;
import tools.CSVUtils;
import tools.PredictionReader;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
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
import twitter4j.TwitterException;
import twitter4j.UserMentionEntity;


/**
 * This class:
 * 
 * 01) opens a twitter stream (using TwitterUtils) with a provided filter and a 1 second intervall 	!twitter application credentials editable via file twitter4j.properties
 * 02) filters this stream for certain tweets (see filterTweets())
 * 03) adds flags for 5 hashtags to the tweets
 * 04) writes those filtered tweets with some metrics into a file									!toggleable by editing appliction.properties.writeTweetsSavefile
 * 05) calculates metrics on the filtered tweets
 * 06) summarises these values for a timewindow of 60 seconds
 * 07) writes the summaries for every flag combination into a file 									!toggleable by editing appliction.properties.writeComboSavefile
 * 08) matches the summaries w.r.t. time and flag combination to a prediction						!toggleable by editing appliction.properties.usePredictions
 * 09) compares the prediction to the actual measured metrics
 * 10) writes this evaluation for every flag combination into a file 								!toggleable by editing appliction.properties.evalPredictions
 * 
 */
@SuppressWarnings("serial")
public class TweetStreamer implements java.io.Serializable {
	
	private PredictionReader predictions;
	
	// constructor, get predictions object
	public TweetStreamer(PredictionReader predictions) {
		this.predictions = predictions;
	}

	public void stream() throws TwitterException {
		SparkConf sparkConf = new SparkConf().setAppName("TweetStreamer");

		// check Spark configuration for master URL, set it to local if not configured
		if (!sparkConf.contains("spark.master")) {
			sparkConf.setMaster("local[8]");
		}

		// set stream intervall to 1 second
		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(1000));
		
		/*
		 * Set logging level if log4j not configured (override by adding log4j.properties to classpath)
		 */
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			Logger.getRootLogger().setLevel(Level.ERROR);
		}
		jssc.sparkContext().setLogLevel("ERROR");

		/*
		 * ################################# STREAMING STARTS HERE #################################
		 */
		
		// start actual stream, use filter given by AppProperties.getFilters()
		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, AppProperties.getFilters());
		
		// filter tweets (details in filterTweets()) and build <Status,Combo> pair
		JavaPairDStream<Status,Combo> tweetsComboMatch = filterTweets(stream)
														 .mapToPair(statusAndCombo());

		// if activated, write all filtered tweets to a file
		if(AppProperties.isWriteTweetsSavefile()) {
			tweetsComboMatch.foreachRDD(writeTweets(AppProperties.getSaveDir() + AppProperties.getTweetsSavefile()));
		}
		
		// calculate metrics and reduce on Combo for a 60 seconds timewindow
		JavaPairDStream<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>> tweetsComboCounts = 
				tweetsComboMatch.mapToPair(comboAndMetrics())
								.reduceByKeyAndWindow(sumMetrics(), new Duration(60*1000));
		
		// if activated, write metric summaries to a file
		if(AppProperties.isWriteComboSavefile()) {
			tweetsComboCounts.foreachRDD(writeComboCounts(AppProperties.getSaveDir() + AppProperties.getComboSavefile()));
		}
		
		// if activated, access predictions
		if(AppProperties.isUsePredictions()) {
			JavaDStream<Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>> comboPredictionMatches = 
					tweetsComboCounts.map(mapComboToPrediction());
			
			// if activated, evaluate predictions and write to file
			if(AppProperties.isEvalPredictions()) {
				comboPredictionMatches.foreachRDD(writePredictionEval(AppProperties.getSaveDir() + AppProperties.getPredictionsEvalFilename()));
			}
		}
		
		
		// technically, streaming starts here >_>
		jssc.start();
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * generate a valid hashkey, get the corresponding prediction and map it to the right combo and it's metrics
	 */
	public Function<Tuple2<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>>, 
					Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>> mapComboToPrediction() {
		return new Function<Tuple2<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>>,
							Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>>(){

			@Override
			public Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>> call(
					Tuple2<Combo, Tuple5<Integer, Integer, Integer, Integer, Integer>> tuple)
					throws Exception {
				
				Combo combo 			= tuple._1;

				// get current time to build hashkey for prediction HashMap
		        Calendar cal 			= Calendar.getInstance(); 
		        SimpleDateFormat sdf 	= new SimpleDateFormat("HH:mm:ss");
				String now 				= sdf.format(cal.getTime());
				String hashKey 			= now + combo.toString();
				
				TimeComboPrediction tcp = predictions.getTimeComboPredictionHashMap().get(hashKey);
				
				return new Tuple3<>(combo,
									tcp,
									tuple._2);
			}
		};
	}
	
	/*
	 * calculate the metrics for a single tweet 
	 */
	public PairFunction<Tuple2<Status,Combo>,Combo,Tuple5<Integer,Integer,Integer,Integer,Integer>> comboAndMetrics(){
		return new PairFunction<Tuple2<Status,Combo>,Combo,Tuple5<Integer,Integer,Integer,Integer,Integer>>(){
			
			@Override
			public Tuple2<Combo, Tuple5<Integer,Integer,Integer,Integer,Integer>> call(Tuple2<Status, Combo> input) throws Exception {
				
				Status status 				= input._1;
				String text 				= status.getText();
				
				
				Integer count 				= 1;
				Integer textLength 			= text.length();
				Integer totalHashtagCount 	= status.getHashtagEntities().length;
				
				/*
				 * calculate trumpCount by replacing 'trump' in this tweets text with ''
				 * then, compare the string length
				 */
				String sub 			= "trump";						
				String temp 		= text.toLowerCase().replace(sub, "");
				Integer trumpCount 	= (text.length() - temp.length()) / sub.length();
	
				Integer sensitiveCount = 0;
				if(status.isPossiblySensitive()) {
					sensitiveCount = 1;
				}
				
				return new Tuple2<>(input._2,new Tuple5<>(count,
														  textLength, 
														  totalHashtagCount, 
														  trumpCount, 
														  sensitiveCount));
			}
		};
	}
	
	/*
	 * take two Tuples of metrics and combine them by summing each metric
	 */
	public Function2<Tuple5<Integer,Integer,Integer,Integer,Integer>,
					 Tuple5<Integer,Integer,Integer,Integer,Integer>,
					 Tuple5<Integer,Integer,Integer,Integer,Integer>> sumMetrics() {
		return new Function2<Tuple5<Integer,Integer,Integer,Integer,Integer>,
							 Tuple5<Integer,Integer,Integer,Integer,Integer>,
							 Tuple5<Integer,Integer,Integer,Integer,Integer>>() {
			@Override
			public Tuple5<Integer,Integer,Integer,Integer,Integer> call(Tuple5<Integer,Integer,Integer,Integer,Integer> a,
																		Tuple5<Integer,Integer,Integer,Integer,Integer> b) {
				Integer count 				= a._1() + b._1();
				Integer textLength 			= a._2() + b._2();
				Integer totalHashtagCount 	= a._3() + b._3();
				Integer trumpCount 			= a._4() + b._4();
				Integer sensitiveCount 		= a._5() + b._5();
				
				return new Tuple5<>(count,
									textLength,
									totalHashtagCount,
									trumpCount,
									sensitiveCount);
			}
		};
	}
	
	
	/*
	 * take a tweet and build a Combo object for it, map it to this tweet
	 */
	public PairFunction<Status,Status,Combo> statusAndCombo(){
		return new PairFunction<Status,Status,Combo>(){
			
		@Override
		public Tuple2<Status,Combo> call(Status status) throws Exception {

			// build a List of hashtags
			List <String> hashTagList = new ArrayList<String>();
			for(HashtagEntity hashTag:status.getHashtagEntities()) {
				//normalize hashtags, ignore casing
				hashTagList.add(hashTag.getText().toLowerCase());
			}
			
			// set the flag on true if this hashtag is part of this tweet, all combinations are valid
			boolean hasTrumpHashtag 	= hashTagList.contains("trump");
			boolean hasNewsHashtag 		= hashTagList.contains("news");
			boolean hasFakeNewsHashtag 	= hashTagList.contains("fakenews");
			boolean hasDemocratsHashtag = hashTagList.contains("democrats");
			boolean hasPoliticsHashtag 	= hashTagList.contains("politics");
			
			return new Tuple2<>(status, new Combo(hasTrumpHashtag,
												  hasNewsHashtag,
												  hasFakeNewsHashtag,
												  hasDemocratsHashtag,
												  hasPoliticsHashtag));
			}
		};
	}
	
	
	
	public VoidFunction<JavaRDD<Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>>> writePredictionEval(String filePath){
		return new VoidFunction<JavaRDD<Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>>>(){

			@Override
			public void call(JavaRDD<Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>> allComboPredictions)
					throws Exception {
				// TODO Auto-generated method stub
				List<Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>>> comboPredictions = allComboPredictions.collect();
				
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
									+ "\"time\";"
									+ "\"count\";"
									+ "\"meanTextLength\";"
									+ "\"totalHashtagCount\";"
									+ "\"totalTrumpCount\";"
									+ "\"totalSensitiveCount\";"
									+ "\"hasPrediction\";"
									+ "\"errorCount\";"
									+ "\"errorCountRel\";"
									+ "\"errorMeanTextLength\";"
									+ "\"errorMeanTextLengthRel\";"
									+ "\"errorTotalHashtagCount\";"
									+ "\"errorTotalHashtagCountRel\";"
									+ "\"errorTotalTrumpCount\";"
									+ "\"errorTotalTrumpCountRel\";"
									+ "\"errorTotalSensitiveCount\";"
									+ "\"errorTotalSensitiveCountRel\";"
									+ "\"meanErrorRel\""
									+ "\n";
					writer.write(header);
				}
				
		        Calendar cal = Calendar.getInstance(); 
		        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				
				for(Tuple3<Combo, TimeComboPrediction, Tuple5<Integer, Integer, Integer, Integer, Integer>> comboPrediction:comboPredictions) {
					try {
						Boolean hasPrediction;
						
						Double errorCount;
						Double errorMeanTextLength;
						Double errorTotalHashtagCount;
						Double errorTotalTrumpCount;
						Double errorTotalSensitiveCount;
						Double errorCountRel;
						Double errorMeanTextLengthRel;
						Double errorTotalHashtagCountRel;
						Double errorTotalTrumpCountRel;
						Double errorTotalSensitiveCountRel;
						Double meanErrorRel;
						
						Combo combo = comboPrediction._1();
						
						Integer count = comboPrediction._3()._1();
			        	Double  meanTextLength = (double) comboPrediction._3()._2()/count;
			        	Integer totalHashtagCount = comboPrediction._3()._3();;
			        	Integer totalTrumpCount = comboPrediction._3()._4();;
			        	Integer totalSensitiveCount = comboPrediction._3()._5();

						Date timestamp = new java.util.Date();
						String timeStr;

						TimeComboPrediction tpc = comboPrediction._2();
						if(tpc==null) {
							hasPrediction = false;
							timeStr = sdf.format(cal.getTime());
							
							combo = comboPrediction._1();
							
							
							errorCount = (double) count;
							errorMeanTextLength = (double) meanTextLength;
							errorTotalHashtagCount = (double) totalHashtagCount;
							errorTotalTrumpCount = (double) totalTrumpCount;
							errorTotalSensitiveCount = (double) totalSensitiveCount;
							
							errorCountRel = 1.0;
							errorMeanTextLengthRel = 1.0;
							errorTotalHashtagCountRel = 1.0;
							errorTotalTrumpCountRel = 1.0;
							errorTotalSensitiveCountRel = 1.0;
							
							meanErrorRel = 1.0;
						} else {
							hasPrediction = true;
							
							combo = tpc.getCombo();
							
							timeStr = tpc.getTime()
										 .toString();
							
							Double predCount = tpc.getCount();
							Double predMeanTextLength = tpc.getMeanTextLength();
							Double predTotalHashtagCount = tpc.getTotalHashtagCount();
							Double predTotalTrumpCount = tpc.getTotalTrumpCount();
							Double predTotalSensitiveCount = tpc.getTotalSensitiveCount();
							
							errorCount = (double) count - predCount;
							errorMeanTextLength = (double) meanTextLength - predMeanTextLength;
							errorTotalHashtagCount = (double) totalHashtagCount - predTotalHashtagCount;
							errorTotalTrumpCount = (double) totalTrumpCount - predTotalTrumpCount;
							errorTotalSensitiveCount = (double) totalSensitiveCount - predTotalSensitiveCount;
							
							errorCountRel = Math.abs(errorCount/count);
							errorMeanTextLengthRel = Math.abs(errorMeanTextLength/meanTextLength);
							errorTotalHashtagCountRel = Math.abs(errorTotalHashtagCount/totalHashtagCount);
							errorTotalTrumpCountRel = Math.abs(errorTotalTrumpCount/totalTrumpCount);
							errorTotalSensitiveCountRel = Math.abs(errorTotalSensitiveCount/totalSensitiveCount);
							
							meanErrorRel = (errorCountRel+errorMeanTextLengthRel+errorTotalHashtagCountRel+errorTotalTrumpCountRel+errorTotalSensitiveCountRel)/5;
						}
						
						List<String> metrics = Arrays.asList(timestamp.toString(),
															 Boolean.toString(combo.isTrumpTweet()),
															 Boolean.toString(combo.isNewsTweet()),
															 Boolean.toString(combo.isFakeNewsTweet()),
															 Boolean.toString(combo.isDemocratsTweet()),
															 Boolean.toString(combo.isPoliticsTweet()),
															 timeStr,
															 Double.toString(count),
															 Double.toString(meanTextLength),
															 Integer.toString(totalHashtagCount),
															 Integer.toString(totalTrumpCount),
															 Integer.toString(totalSensitiveCount),
															 Boolean.toString(hasPrediction),
															 Double.toString(errorCount),
															 Double.toString(errorCountRel),
															 Double.toString(errorMeanTextLength),
															 Double.toString(errorMeanTextLengthRel),
															 Double.toString(errorTotalHashtagCount),
															 Double.toString(errorTotalHashtagCountRel),
															 Double.toString(errorTotalTrumpCount),
															 Double.toString(errorTotalTrumpCountRel),
															 Double.toString(errorTotalSensitiveCount),
															 Double.toString(errorTotalSensitiveCountRel),
															 Double.toString(meanErrorRel)
															 );
						
			        	CSVUtils.writeLine(writer, metrics, ';', '"');
					} catch (Exception e){
						continue;
					}
					
				}
				writer.flush();
				writer.close();
			}
		};
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
										Boolean.toString(combo.isPoliticsTweet()),
										Double.toString(count),
										Double.toString(meanTextLength),
										Integer.toString(totalHashtagCount),
										Integer.toString(totalTrumpCount),
										Integer.toString(totalSensitiveCount)
										),
								';', '"');
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
					String isPoliticsTweet 		= Boolean.toString(combo.isPoliticsTweet());

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
									isPoliticsTweet
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
	
	
	/*
	 * filter out tweets which:
	 * * are not written in english
	 * * are retweets
	 * * are truncated (new Twitter API truncates tweets)
	 */
	public JavaDStream<Status> filterTweets(JavaReceiverInputDStream<Status> stream){
		return stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return tweet.getLang().equals("en");
			}
		})
					 .filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status tweet) throws Exception {
				return !tweet.isRetweet();
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