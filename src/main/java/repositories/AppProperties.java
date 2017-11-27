package repositories;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*
 * Repository for static properties
 */
public class AppProperties {
	
    private static List<String> hashTagList;
    
    private static List<String> hashTagListNormalized;
    
    private static boolean usePredictions;
    
    private static boolean writeTweetsSavefile;
    
    private static boolean writeComboSavefile;
    
    private static String filters[];
    
    private static String workDir;
    
    private static String predictionsFilename;
    
    private static String predictionsEvalFilename;

    private static String tweetsSavefile;

    private static String comboSavefile;

	/*
	 * Read properties file on creation
	 */
	public AppProperties() {
		readPropertyFile();
	}
	
	/*
	 * Read properties file and fill static AppProperties instance
	 */
	private void readPropertyFile() {
		
		// read properties
		Properties properties = new Properties();
		try {
			InputStream file = this.getClass().getResourceAsStream("/application.properties");
			properties.load(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// string arrays
		String hashTags[] = properties.getProperty("hashtags").split(",");
		
		List<String> hashTagList = new ArrayList<String>();
		List<String> hashTagListNormalized = new ArrayList<String>();
		
		for(String hashtag:hashTags) {
			hashTagList.add(hashtag);
			hashTagListNormalized.add(hashtag.toLowerCase());
		}
		
		// fill static values
		setHashTagList(hashTagList);
		setHashTagListNormalized(hashTagListNormalized);
		
		setFilters(properties.getProperty("filters").split(","));
		String workDir = properties.getProperty("workDir");
		setWorkDir(workDir);
		
		setWriteTweetsSavefile(Boolean.parseBoolean(properties.getProperty("writeTweetsSavefile")));
		setTweetsSavefile(properties.getProperty("tweetsSavefile"));
		
		setWriteComboSavefile(Boolean.parseBoolean(properties.getProperty("writeComboSavefile")));
		setComboSavefile(properties.getProperty("comboSavefile"));

		setUsePredictions(Boolean.parseBoolean(properties.getProperty("usePredictions")));
		setPredictionsFilename(properties.getProperty("predictionsFilename"));
		setPredictionsEvalFilename(properties.getProperty("predictionsEvalFilename"));
	}

	/*
	 * Getters and Setters...
	 */	
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

	public static boolean isUsePredictions() {
		return usePredictions;
	}

	public static void setUsePredictions(boolean usePredictions) {
		AppProperties.usePredictions = usePredictions;
	}

	public static boolean isWriteTweetsSavefile() {
		return writeTweetsSavefile;
	}

	public static void setWriteTweetsSavefile(boolean writeTweetsSavefile) {
		AppProperties.writeTweetsSavefile = writeTweetsSavefile;
	}

	public static boolean isWriteComboSavefile() {
		return writeComboSavefile;
	}

	public static void setWriteComboSavefile(boolean writeComboSavefile) {
		AppProperties.writeComboSavefile = writeComboSavefile;
	}

	public static String[] getFilters() {
		return filters;
	}

	public static void setFilters(String[] filters) {
		AppProperties.filters = filters;
	}

	public static String getWorkDir() {
		return workDir;
	}

	public static void setWorkDir(String workDir) {
		AppProperties.workDir = workDir;
	}

	public static String getPredictionsFilename() {
		return predictionsFilename;
	}

	public static void setPredictionsFilename(String predictionsFilename) {
		AppProperties.predictionsFilename = predictionsFilename;
	}

	public static String getPredictionsEvalFilename() {
		return predictionsEvalFilename;
	}

	public static void setPredictionsEvalFilename(String predictionsEvalFilename) {
		AppProperties.predictionsEvalFilename = predictionsEvalFilename;
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
