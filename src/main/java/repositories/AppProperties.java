package repositories;

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
    
    private static String predictionsFilename;

    private static String tweetsSavefile;

    private static String comboSavefile;

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
		
		setTweetsSavefile(properties.getProperty("tweetsSavefile"));
		setComboSavefile(properties.getProperty("comboSavefile"));

		setPredictionsFilename(properties.getProperty("predictionsFilename"));
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

	public static String getPredictionsFilename() {
		return predictionsFilename;
	}

	public static void setPredictionsFilename(String predictionsFilename) {
		AppProperties.predictionsFilename = predictionsFilename;
	}

	public static String getTweetsSavefile() {
		return tweetsSavefile;
	}

	public static void setTweetsSavefile(String tweetsSavefile) {
		AppProperties.tweetsSavefile = tweetsSavefile;
	}

	public static String getComboSavefile() {
		return comboSavefile;
	}

	public static void setComboSavefile(String comboSavefile) {
		AppProperties.comboSavefile = comboSavefile;
	}
	
}
