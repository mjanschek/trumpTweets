package streaming;

import repositories.AppProperties;

import tools.CSVUtils;

import scala.Tuple11;
import scala.Tuple2;
import scala.Tuple5;
import scala.Tuple6;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
 * 08) creates a batch with prediction data															!toggleable by editing appliction.properties.usePredictions
 * 08) matches the summaries w.r.t. time and flag combination to a prediction						!""
 * 09) compares the prediction to the actual measured metrics										!""
 * 10) writes this evaluation for every flag combination into a file 								!""
 * 
 * 
 */
@SuppressWarnings("serial")
public class TweetStreamer implements java.io.Serializable {
	
	// constructor
	public TweetStreamer() {
	}

	public void stream() throws TwitterException {
		SparkConf sparkConf = new SparkConf().setAppName("TweetStreamer");

		// check Spark configuration for master URL, set it to local if not configured
		if (!sparkConf.contains("spark.master")) {
			sparkConf.setMaster("local[8]");
		}

		// set stream intervall to 1 second
		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(1000));
		jssc.sparkContext().setLogLevel("ERROR");

		/*
		 * ################################# STREAMING STARTS HERE #################################
		 */
		// start actual stream, use filter given by AppProperties.getFilters()
		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, AppProperties.getFilters());
		
		// filter tweets (details in filterTweets()) and build <Status,Combo> pair
		JavaPairDStream<Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> tweetsComboMatch = filterTweets(stream)
														 .mapToPair(statusAndCombo());

		// if activated, write all filtered tweets to a file
		if(AppProperties.isWriteTweetsSavefile()) {
			tweetsComboMatch.foreachRDD(writeTweets(AppProperties.getWorkDir() + AppProperties.getTweetsSavefile()));
		}
		
		// calculate metrics and reduce on Combo for a 60 seconds timewindow
		JavaPairDStream<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double, Double, Double, Double, Double>> comboMeanCounts = 
				tweetsComboMatch.mapToPair(comboAndMetrics())
								.reduceByKeyAndWindow(sumMetrics(), new Duration(60*1000))
								.mapToPair(meanValues());
		
		// if activated, write metric summaries to a file
		if(AppProperties.isWriteComboSavefile()) {
			comboMeanCounts.foreachRDD(writeComboCounts(AppProperties.getWorkDir() + AppProperties.getComboSavefile()));
		}
		
		// if activated, access predictions
		if(AppProperties.isUsePredictions()) {
			
			// access predictions as spark batch and cache it
			// (this data will be used for joining every task)
			JavaPairRDD<Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>,
						Tuple5<Double, Double, Double, Double, Double>> predBatch = 
						jssc.sparkContext().textFile(AppProperties.getWorkDir() + AppProperties.getPredictionsFilename())
							.filter(filterHeader())
							.map(parseTuple11())
							.mapToPair(splitIntoKeyValuePair())
							.cache();
	
			// join the stream with the batch and write evaluation results into a file
			comboMeanCounts.mapToPair(addTime())
						   .transformToPair(joinWithPredictionBatch(predBatch))
						   .foreachRDD(writePredictionEval(AppProperties.getWorkDir() + AppProperties.getPredictionsEvalFilename()));
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

	
	/*
	 * take a tweet and build a Combo object for it, map it to this tweet
	 */
	public PairFunction<Status,Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> statusAndCombo(){
		return new PairFunction<Status,Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>(){
			
		@Override
		public Tuple2<Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> call(Status status) throws Exception {
	
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
			
			return new Tuple2<>(status, new Tuple5<>(hasTrumpHashtag,
													 hasNewsHashtag,
													 hasFakeNewsHashtag,
													 hasDemocratsHashtag,
													 hasPoliticsHashtag));
			}
		};
	}

	/*
	 * calculate the metrics for a single tweet 
	 */
	public PairFunction<Tuple2<Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>,
						Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,
						Tuple5<Integer,Integer,Integer,Integer,Integer>> comboAndMetrics(){
		return new PairFunction<Tuple2<Status,Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>,
								Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,
								Tuple5<Integer,Integer,Integer,Integer,Integer>>(){
			
			@Override
			public Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, 
						  Tuple5<Integer,Integer,Integer,Integer,Integer>> call(
								  Tuple2<Status,
								  		 Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> input) throws Exception {

				
				
				Integer count 				= 1;
				Integer textLength 			= input._1.getText().length();
				Integer totalHashtagCount 	= input._1.getHashtagEntities().length;
				
				/*
				 * calculate trumpCount by replacing 'trump' in this tweets text with ''
				 * then, compare the string length
				 */
				String sub 			= "trump";						
				String temp 		= input._1.getText().toLowerCase().replace(sub, "");
				Integer trumpCount 	= (input._1.getText().length() - temp.length()) / sub.length();
	
				Integer sensitiveCount = 0;
				if(input._1.isPossiblySensitive()) {
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
	 * take all the date of this timewindow and build the mean values
	 */
	public PairFunction<Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,Tuple5<Integer,Integer,Integer,Integer,Integer>>,
						Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,
						Tuple5<Double,Double,Double,Double,Double>>meanValues(){
		return new PairFunction<Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,Tuple5<Integer,Integer,Integer,Integer,Integer>>,
							    Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>,
							    Tuple5<Double,Double,Double,Double,Double>>(){

			@Override
			public Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double, Double, Double, Double, Double>> call(
				   Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Integer, Integer, Integer, Integer, Integer>> t)
					throws Exception {
				// TODO Auto-generated method stub
				
				Double count 				= (double) t._2()._1();
	        	Double meanTextLength 		= (double) t._2()._2()/count;
	        	Double meanHashtagCount 	= (double) t._2()._3()/count;;
	        	Double meanTrumpCount 		= (double) t._2()._4()/count;;
	        	Double meanSensitiveCount 	= (double) t._2()._5()/count;
				
				
				return new Tuple2<>(t._1,new Tuple5<>(count,
													  meanTextLength,
													  meanHashtagCount,
													  meanTrumpCount,
													  meanSensitiveCount));
			}
		};
	}
		
	/*
	 * Filter out the heaser of the predictions csv file
	 */
	public Function<String,Boolean> filterHeader(){
		return new Function<String,Boolean>(){

			@Override
			public Boolean call(String v1) throws Exception {
				// first column "time" is enough to find match
				return !v1.contains("time");
			}
		};
	}
	
	/*
	 * Take the read data and fill a Tuple11 with it's columns
	 * 
	 * Note: I tried a very long time to run the app with custom data objects (for Prediciton data, for example)
	 *       but Java Garbage Collection and Deserialization slowed it so much down that i decided to use only prebuilt objects (Tuples, mostly)
	 *       
	 *       I am aware that this is very hard to read. However, the processing time improved from ~30sec/Task to 0.1sec/Task
	 */
	public Function<String,Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>> parseTuple11(){
		return new Function<String,Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>>(){

			@Override
			public Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double> call(
					String string) throws Exception {
				
				String[] stringArray = string.trim().replaceAll("\"", "").split(";");
				// TODO Auto-generated method stub
				return new Tuple11<>(stringArray[0],
									 Boolean.parseBoolean(stringArray[1]),
									 Boolean.parseBoolean(stringArray[2]),
									 Boolean.parseBoolean(stringArray[3]),
									 Boolean.parseBoolean(stringArray[4]),
									 Boolean.parseBoolean(stringArray[5]),
									 Double.parseDouble(stringArray[6]),
									 Double.parseDouble(stringArray[7]),
									 Double.parseDouble(stringArray[8]),
									 Double.parseDouble(stringArray[9]),
									 Double.parseDouble(stringArray[10]));
			}
		};
	}
	
	/*
	 * Split up the parsed prediction Tuple11 into a key and a value, this is needed for joining with the stream
	 */
	public PairFunction<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
						Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>, 
						Tuple5<Double, Double, Double, Double, Double>> splitIntoKeyValuePair(){
		return new PairFunction<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
								Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>, 
								Tuple5<Double, Double, Double, Double, Double>>(){

			@Override
			public Tuple2<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
						  Tuple5<Double, Double, Double, Double, Double>> call(
								  Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double> tcp) throws Exception {
				// TODO Auto-generated method stub
				return new Tuple2<>(new Tuple6<>(tcp._1(), tcp._2(),tcp._3(),tcp._4(),tcp._5(),tcp._6()),
									new Tuple5<>(tcp._7(),tcp._8(),tcp._9(),tcp._10(),tcp._11()));
			}
		};
	}
	
	/*
	 * Add a timestamp to the stream for correct matching with the batch
	 */
	public PairFunction<Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double, Double, Double, Double, Double>>,
						Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
						Tuple5<Double, Double, Double, Double, Double>> addTime() {
		return new PairFunction<Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double, Double, Double, Double, Double>>,
								Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
								Tuple5<Double, Double, Double, Double, Double>>(){
			@Override
			public Tuple2<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
				   Tuple5<Double, Double, Double, Double, Double>> call(
						   Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, 
						   		  Tuple5<Double, Double, Double, Double, Double>> t) throws Exception {
				
				Calendar cal 			= Calendar.getInstance();
				SimpleDateFormat sdf 	= new SimpleDateFormat("HH:mm:ss");
				String now 				= sdf.format(cal.getTime());

				return new Tuple2<>(new Tuple6<>(now,
												 t._1._1(),
												 t._1._2(),
												 t._1._3(),
												 t._1._4(),
												 t._1._5()),
												 t._2);
			}
		};
	}
	
	/*
	 * Join the stream with a prediction batch, use the timestamp and hashtag combination as key.
	 * Then, reconstruct the prediction Tuple11 (for a little tidiness) and pair it with the just measured values
	 */
	public Function<JavaPairRDD<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
								Tuple5<Double, Double, Double, Double, Double>>,
					JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
								Tuple5<Double, Double, Double, Double, Double>>> joinWithPredictionBatch(JavaPairRDD<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double, Double, Double, Double, Double>> batch) {
		return new Function<JavaPairRDD<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
										Tuple5<Double, Double, Double, Double, Double>>,
							JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
										Tuple5<Double, Double, Double, Double, Double>>>(){

			@Override
			public JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>, Tuple5<Double, Double, Double, Double, Double>> call(
				   JavaPairRDD<Tuple6<String,Boolean, Boolean, Boolean, Boolean, Boolean>, 
							   Tuple5<Double, Double, Double, Double, Double>> streamRDD) throws Exception {
				
				// TODO Auto-generated method stub
				JavaPairRDD<Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>, 
							Tuple2<Tuple5<Double, Double, Double, Double, Double>, Tuple5<Double, Double, Double, Double, Double>>> joinedData = streamRDD.join(batch);
				return joinedData.mapToPair(new PairFunction<Tuple2<Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>, 
																	Tuple2<Tuple5<Double, Double, Double, Double, Double>, Tuple5<Double, Double, Double, Double, Double>>>,
																	Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
																	Tuple5<Double, Double, Double, Double, Double>>(){
					@Override
					public Tuple2<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>, Tuple5<Double, Double, Double, Double, Double>> call(
							Tuple2<Tuple6<String, Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple2<Tuple5<Double, Double, Double, Double, Double>, Tuple5<Double, Double, Double, Double, Double>>> t)
									throws Exception {
						// TODO Auto-generated method stub
						return new Tuple2<>(new Tuple11<>(t._1._1(),t._1._2(),t._1._3(),t._1._4(),t._1._5(),t._1._6(),t._2._2._1(),t._2._2._2(),t._2._2._3(),t._2._2._4(),t._2._2._5()), t._2._1) ;
						}
				});
			}
		};
	}
	
	/*
	 * Write the tweets as data foundation for an explorational analysis 
	 * 
	 * (not used in standard application anymore, but it still works)
	 */
	public VoidFunction<JavaPairRDD<Status, Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>> writeTweets(String filePath){
		return new VoidFunction<JavaPairRDD<Status, Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>>() {
		@Override
		public void call(JavaPairRDD<Status, Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> allTweetsCombos) throws Exception {
			List<Tuple2<Status, Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>>> tweetCombos = allTweetsCombos.collect();
	
			// if file doesnt exist, create it
			boolean firstLine = false;
			File f = new File(filePath);
			if(!f.exists() && !f.isDirectory()) { 
				f.createNewFile();
				firstLine = true;
			}
	
			// and write a csv header
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
						+ getComboCsvHeader()
						+ "\n";
				writer.write(header);
			}
			
			// for every Status object, get some metrics and write them to the file
			for(Tuple2<Status, Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>> tweetCombo:tweetCombos) {
				try {
					Status status = tweetCombo._1;
					
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
	
					String isTrumpTweet 		= Boolean.toString(tweetCombo._2._1());
					String isNewsTweet 			= Boolean.toString(tweetCombo._2._2());
					String isFakeNewsTweet 		= Boolean.toString(tweetCombo._2._3());
					String isDemocratsTweet 	= Boolean.toString(tweetCombo._2._4());
					String isPoliticsTweet 		= Boolean.toString(tweetCombo._2._5());
	
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
	 * Write the summaries for hashtag combinations into a file
	 * (this information is used to create predictions later)
	 */
	public VoidFunction<JavaPairRDD<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double,Double,Double,Double,Double>>> writeComboCounts(String filePath){
		return new VoidFunction<JavaPairRDD<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double,Double,Double,Double,Double>>>(){

			@Override
			public void call(JavaPairRDD<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double,Double,Double,Double,Double>> allComboCounts)
					throws Exception {
				List<Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double,Double,Double,Double,Double>>> comboCounts = allComboCounts.collect();
				
				// if file doesnt exist, create it
				boolean firstLine = false;
				File f = new File(filePath);
				if(!f.exists() && !f.isDirectory()) { 
					f.createNewFile();
					firstLine = true;
				}

				// and write a csv header
				FileWriter writer = new FileWriter(f,true);
				if(firstLine) {
					String header = "\"timestamp\";"
									+ "\"time\";"
									+ getComboCsvHeader() + ";"
									+ "\"count\";"
									+ "\"meanTextLength\";"
									+ "\"meanHashtagCount\";"
									+ "\"meanTrumpCount\";"
									+ "\"meanSensitiveCount\""
									+ "\n";
					writer.write(header);
				}
				
				for(Tuple2<Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean>, Tuple5<Double,Double,Double,Double,Double>> comboCount:comboCounts) {
					try {
						Date timestamp = new java.util.Date();
						DateFormat df = new SimpleDateFormat("HH:mm:ss");
						String time = df.format(timestamp);
						
						String isTrumpTweet 		= Boolean.toString(comboCount._1._1());
						String isNewsTweet 			= Boolean.toString(comboCount._1._2());
						String isFakeNewsTweet 		= Boolean.toString(comboCount._1._3());
						String isDemocratsTweet 	= Boolean.toString(comboCount._1._4());
						String isPoliticsTweet 		= Boolean.toString(comboCount._1._5());
						
						// get mean metrics in this minute
						Double count 				= (double) comboCount._2._1();
			        	Double meanTextLength 		= (double) comboCount._2._2();
			        	Double meanHashtagCount 	= (double) comboCount._2._3();
			        	Double meanTrumpCount 		= (double) comboCount._2._4();
			        	Double meanSensitiveCount 	= (double) comboCount._2._5();
			        	
			        	CSVUtils.writeLine(writer,
								Arrays.asList(timestamp.toString(),
											  time,
											  isTrumpTweet,
											  isNewsTweet,
											  isFakeNewsTweet,
											  isDemocratsTweet,
											  isPoliticsTweet,
											  Double.toString(count),
											  Double.toString(meanTextLength),
											  Double.toString(meanHashtagCount),
											  Double.toString(meanTrumpCount),
											  Double.toString(meanSensitiveCount)),
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
 	 	
	/*
	 * Write the evaluation for predictions into a file
	 * (this information is used to visualize the evaluation in python)
	 */
	public VoidFunction<JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>, 
									Tuple5<Double,Double,Double,Double,Double>>> writePredictionEval(String filePath){
		return new VoidFunction<JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>, 
											Tuple5<Double,Double,Double,Double,Double>>>(){
	
			@Override
			public void call(JavaPairRDD<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
										 Tuple5<Double,Double,Double,Double,Double>> allComboPredictions)
					throws Exception {
				List<Tuple2<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
							Tuple5<Double,Double,Double,Double,Double>>> comboPredictions = allComboPredictions.collect();
				
				// if file doesnt exist, create it
				boolean firstLine = false;
				File f = new File(filePath);
				if(!f.exists() && !f.isDirectory()) { 
					f.createNewFile();
					firstLine = true;
				}
	
				// and write a csv header
				FileWriter writer = new FileWriter(f,true);
				if(firstLine) {
					String header = "\"timestamp\";"
									+ "\"time\";"
									+ getComboCsvHeader() + ";"
									+ "\"errorCount\";"
									+ "\"errorMeanTextLength\";"
									+ "\"errorMeanHashtagCount\";"
									+ "\"errorMeanTrumpCount\";"
									+ "\"errorMeanSensitiveCount\""
									+ "\n";
					writer.write(header);
				}
				
				for(Tuple2<Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double>,
						   Tuple5<Double,Double,Double,Double,Double>> comboPrediction:comboPredictions) {
					try {
						Tuple11<String, Boolean, Boolean, Boolean, Boolean, Boolean, Double, Double, Double, Double, Double> tcp = comboPrediction._1;
						
						Double errorCount;
						Double errorMeanTextLength;
						Double errorMeanHashtagCount;
						Double errorMeanTrumpCount;
						Double errorMeanSensitiveCount;
						
						Double count 				= comboPrediction._2()._1();
			        	Double meanTextLength 		= comboPrediction._2()._2();
			        	Double meanHashtagCount 	= comboPrediction._2()._3();;
			        	Double meanTrumpCount 		= comboPrediction._2()._4();;
			        	Double meanSensitiveCount 	= comboPrediction._2()._5();
	
						Date timestamp = new java.util.Date();
							
						Double predCount 				= tcp._7();
						Double predMeanTextLength 		= tcp._8();
						Double predMeanHashtagCount 	= tcp._9();
						Double predMeanTrumpCount 		= tcp._10();
						Double predMeanSensitiveCount 	= tcp._11();
						
						// calculate errors
						errorCount 					= count - predCount;
						errorMeanTextLength 		= predMeanTextLength - meanTextLength;
						errorMeanHashtagCount 		= predMeanHashtagCount - meanHashtagCount;
						errorMeanTrumpCount 		= predMeanTrumpCount - meanTrumpCount;
						errorMeanSensitiveCount 	= predMeanSensitiveCount - meanSensitiveCount;
						
						List<String> metrics = Arrays.asList(timestamp.toString(),
															 tcp._1(),
															 Boolean.toString(tcp._2()),
															 Boolean.toString(tcp._3()),
															 Boolean.toString(tcp._4()),
															 Boolean.toString(tcp._5()),
															 Boolean.toString(tcp._6()),
															 Double.toString(errorCount),
															 Double.toString(errorMeanTextLength),
															 Double.toString(errorMeanHashtagCount),
															 Double.toString(errorMeanTrumpCount),
															 Double.toString(errorMeanSensitiveCount)
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
	
	public String getComboCsvHeader() {
		return "\"isTrumpTweet\";\"isNewsTweet\";\"isFakeNewsTweet\";\"isDemocratsTweet\";\"isPoliticsTweet\"";		
	}
	
}