package tools;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

import repositories.AppProperties;
import repositories.TimeComboPrediction;

/*
 * class for reading the predictions csv file
 */
@SuppressWarnings("serial")
public class PredictionReader implements java.io.Serializable{

	private List<TimeComboPrediction> timeComboPredictionList;
	
	private HashMap<Integer, TimeComboPrediction> timeComboPredictionHashMap;
	
	private String timeComboPredictionFilepath;
	
	/*
	 * read csv file on creation, use filepath given by properties
	 */
	public PredictionReader() {
		setTimeComboPredictionFilepath(AppProperties.getSaveDir() + AppProperties.getPredictionsFilename());
		try {
			updateTimeComboPredictionList();
		} catch (IllegalStateException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	 * read csv file into a List<TimeComboPrediction> object
	 * after this, build the HashMap
	 */
	public void updateTimeComboPredictionList() throws IllegalStateException, FileNotFoundException {
		setTimeComboPredictionList(new CsvToBeanBuilder<TimeComboPrediction>(new FileReader(getTimeComboPredictionFilepath()))
				.withSeparator(';')
				.withQuoteChar('"')
				.withType(TimeComboPrediction.class)
				.build()
				.parse());
		buildHashMap();
	}
	
	/*
	 * build the HashMap, use combination of flag-combo and time string as key
	 */
	public void buildHashMap() {
		timeComboPredictionHashMap = new HashMap<Integer, TimeComboPrediction>();
		for (TimeComboPrediction tcp : timeComboPredictionList) {
			timeComboPredictionHashMap.put((tcp.getTime().toString() + tcp.getCombo().toFastString()).hashCode(), tcp);
		}
	}

	/*
	 * Getters and Setters...
	 */
	public List<TimeComboPrediction> getTimeComboPredictionList() {
		return timeComboPredictionList;
	}

	public void setTimeComboPredictionList(List<TimeComboPrediction> timeComboPredictionList) {
		this.timeComboPredictionList = timeComboPredictionList;
	}

	public HashMap<Integer, TimeComboPrediction> getTimeComboPredictionHashMap() {
		return timeComboPredictionHashMap;
	}

	public void setTimeComboPredictionHashMap(HashMap<Integer, TimeComboPrediction> timeComboPredictionHashMap) {
		this.timeComboPredictionHashMap = timeComboPredictionHashMap;
	}

	public String getTimeComboPredictionFilepath() {
		return timeComboPredictionFilepath;
	}

	public void setTimeComboPredictionFilepath(String timeComboPredictionFilepath) {
		this.timeComboPredictionFilepath = timeComboPredictionFilepath;
	}
	
}
