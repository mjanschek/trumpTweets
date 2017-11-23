package application;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

import repositories.ComboPrediction;
import repositories.HashTagPrediction;

public class PredictionReader {
	
	private List<ComboPrediction> comboPredictions;
	
	private List<HashTagPrediction> hashTagPredictions;	
	
	private String comboPredictionsFilepath;
	
	private String hashTagPredictionsFilepath;
	
	public PredictionReader() {
		setComboPredictionsFilepath(AppProperties.getSaveDir() + AppProperties.getComboPredictionsFilename());
		setHashTagPredictionsFilepath(AppProperties.getSaveDir() + AppProperties.getHashTagPredictionsFilename());
		try {
			updatePredictions();
		} catch (IllegalStateException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateComboPredictions() throws IllegalStateException, FileNotFoundException {
		setComboPredictions(new CsvToBeanBuilder<ComboPrediction>(new FileReader(getComboPredictionsFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(ComboPrediction.class)
				.build()
				.parse());
	}
	
	public void updateHashTagPredictions() throws IllegalStateException, FileNotFoundException {
		setHashTagPredictions(new CsvToBeanBuilder<HashTagPrediction>(new FileReader(getHashTagPredictionsFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(HashTagPrediction.class)
				.build()
				.parse());
	}
	
	public void updatePredictions() throws IllegalStateException, FileNotFoundException {
		updateComboPredictions();
		updateHashTagPredictions();
	}

	public List<ComboPrediction> getComboPredictions() {
		return comboPredictions;
	}

	public void setComboPredictions(List<ComboPrediction> comboPredictions) {
		this.comboPredictions = comboPredictions;
	}

	public List<HashTagPrediction> getHashTagPredictions() {
		return hashTagPredictions;
	}

	public void setHashTagPredictions(List<HashTagPrediction> hashTagPredictions) {
		this.hashTagPredictions = hashTagPredictions;
	}

	public String getComboPredictionsFilepath() {
		return comboPredictionsFilepath;
	}

	public void setComboPredictionsFilepath(String comboPredictionsFilepath) {
		this.comboPredictionsFilepath = comboPredictionsFilepath;
	}

	public String getHashTagPredictionsFilepath() {
		return hashTagPredictionsFilepath;
	}

	public void setHashTagPredictionsFilepath(String hashTagPredictionsFilepath) {
		this.hashTagPredictionsFilepath = hashTagPredictionsFilepath;
	}
	
}
