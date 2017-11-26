package repositories;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

@SuppressWarnings("serial")
public class PredictionReader implements java.io.Serializable{

	private List<TimeComboPrediction> timeComboPredictionList;
	
	private HashMap<String, TimeComboPrediction> timeComboPredictionHashMap;
	
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

	public void updateTimeComboPredictionList() throws IllegalStateException, FileNotFoundException {
		setTimeComboPredictionList(new CsvToBeanBuilder<TimeComboPrediction>(new FileReader(getTimeComboPredictionFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(TimeComboPrediction.class)
				.build()
				.parse());
	}
	
	public void updatePredictions() throws IllegalStateException, FileNotFoundException {
		setTimeComboPredictionList(new CsvToBeanBuilder<TimeComboPrediction>(new FileReader(getTimeComboPredictionFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(TimeComboPrediction.class)
				.build()
				.parse());
		buildHashMap();
	}
	
	public void buildHashMap() {
		timeComboPredictionHashMap = new HashMap<String, TimeComboPrediction>();
		for (TimeComboPrediction tcp : timeComboPredictionList) {
			timeComboPredictionHashMap.put(tcp.getTime().toString() + tcp.getCombo().toString(), tcp);
		}
	}

	public List<TimeComboPrediction> getTimeComboPredictionList() {
		return timeComboPredictionList;
	}

	public void setTimeComboPredictionList(List<TimeComboPrediction> timeComboPredictionList) {
		this.timeComboPredictionList = timeComboPredictionList;
	}

	public HashMap<String, TimeComboPrediction> getTimeComboPredictionHashMap() {
		return timeComboPredictionHashMap;
	}

	public void setTimeComboPredictionHashMap(HashMap<String, TimeComboPrediction> timeComboPredictionHashMap) {
		this.timeComboPredictionHashMap = timeComboPredictionHashMap;
	}

	public String getTimeComboPredictionFilepath() {
		return timeComboPredictionFilepath;
	}

	public void setTimeComboPredictionFilepath(String timeComboPredictionFilepath) {
		this.timeComboPredictionFilepath = timeComboPredictionFilepath;
	}
	
}
