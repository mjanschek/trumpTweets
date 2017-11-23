package application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

import streaming.TweetStreamer;
import repositories.ComboPrediction;
import repositories.HashTagPrediction;

public class TrumpTweetsApplication {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		new AppProperties();
		
		TweetStreamer streamer = new TweetStreamer();
		streamer.stream();
		
//		List<ComboPrediction> comboPredictions = new CsvToBeanBuilder<ComboPrediction>(new FileReader("/home/garg/tweets/pred_hashTagCombMetrics.csv"))
//				.withSeparator(';').withQuoteChar('"').withType(ComboPrediction.class).build().parse();
//		
//		for(ComboPrediction comboPrediction:comboPredictions) {
//			System.out.println(comboPrediction.toString());
//		}
//		
//		List<HashTagPrediction> hashTagPredictions = new CsvToBeanBuilder<HashTagPrediction>(new FileReader("/home/garg/tweets/pred_hashTags.csv"))
//				.withSeparator(';').withQuoteChar('"').withType(HashTagPrediction.class).build().parse();
//		
//		for(HashTagPrediction hashTagPrediction:hashTagPredictions) {
//			System.out.println(hashTagPrediction.toString());
//		}
		
	}

}
