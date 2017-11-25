package repositories;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

@SuppressWarnings("serial")
public class PredictionReader implements java.io.Serializable{

	private List<TimeComboPrediction> timeComboPrediction;
	
	private String timeComboPredictionFilepath;
	
	public PredictionReader() {
		setTimeComboPredictionFilepath(AppProperties.getSaveDir() + AppProperties.getPredictionsFilename());
		try {
			updatePredictions();
		} catch (IllegalStateException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateTimeComboPrediction() throws IllegalStateException, FileNotFoundException {
		setTimeComboPrediction(new CsvToBeanBuilder<TimeComboPrediction>(new FileReader(getTimeComboPredictionFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(TimeComboPrediction.class)
				.build()
				.parse());
	}
	
	public void updatePredictions() throws IllegalStateException, FileNotFoundException {
		setTimeComboPrediction(new CsvToBeanBuilder<TimeComboPrediction>(new FileReader(getTimeComboPredictionFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(TimeComboPrediction.class)
				.build()
				.parse());
	}

	public List<TimeComboPrediction> getTimeComboPrediction() {
		return timeComboPrediction;
	}

	public void setTimeComboPrediction(List<TimeComboPrediction> timeComboPrediction) {
		this.timeComboPrediction = timeComboPrediction;
	}

	public String getTimeComboPredictionFilepath() {
		return timeComboPredictionFilepath;
	}

	public void setTimeComboPredictionFilepath(String timeComboPredictionFilepath) {
		this.timeComboPredictionFilepath = timeComboPredictionFilepath;
	}
	
}
