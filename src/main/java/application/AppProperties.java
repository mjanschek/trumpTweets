package application;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AppProperties {
	
    private static List<String> hashTagList;
    
    private static List<String> hashTagListNormalized;
    
    private static String filters[];
    
    private static String saveDir;
    
    private static String hashTagPredictionsFilename;
    
    private static String comboPredictionsFilename;

	public AppProperties() {
		readPropertyFile();
	}
	
	private void readPropertyFile() {
		
		Properties properties = new Properties();
		try {
			InputStream file = this.getClass().getResourceAsStream("/application.properties");
			properties.load(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String hashTags[] = properties.getProperty("hashtags").split(",");
		
		List<String> hashTagList = new ArrayList<String>();
		List<String> hashTagListNormalized = new ArrayList<String>();
		
		for(String hashtag:hashTags) {
			hashTagList.add(hashtag);
			hashTagListNormalized.add(hashtag.toLowerCase());
		}
		
		setHashTagList(hashTagList);
		setHashTagListNormalized(hashTagListNormalized);
		
		setFilters(properties.getProperty("filters").split(","));
		setSaveDir(properties.getProperty("saveDir"));

		setHashTagPredictionsFilename(properties.getProperty("hashTagPredictionsFilename"));
		setComboPredictionsFilename(properties.getProperty("comboPredictionsFilename"));
	}
	
	public static List<String> getHashTagList() {
		return hashTagList;
	}
	
	public static void setHashTagList(List<String> hashTagList) {
		AppProperties.hashTagList = hashTagList;
	}

	public static List<String> getHashTagListNormalized() {
		return hashTagListNormalized;
	}

	public static void setHashTagListNormalized(List<String> hashTagListNormalized) {
		AppProperties.hashTagListNormalized = hashTagListNormalized;
	}
	
	public static String[] getFilters() {
		return filters;
	}

	public static void setFilters(String[] filters) {
		AppProperties.filters = filters;
	}

	public static String getSaveDir() {
		return saveDir;
	}

	public static void setSaveDir(String saveDir) {
		AppProperties.saveDir = saveDir;
	}

	public static String getHashTagPredictionsFilename() {
		return hashTagPredictionsFilename;
	}

	public static void setHashTagPredictionsFilename(String hashTagPredictionsFilename) {
		AppProperties.hashTagPredictionsFilename = hashTagPredictionsFilename;
	}

	public static String getComboPredictionsFilename() {
		return comboPredictionsFilename;
	}

	public static void setComboPredictionsFilename(String comboPredictionsFilename) {
		AppProperties.comboPredictionsFilename = comboPredictionsFilename;
	}

	
}
