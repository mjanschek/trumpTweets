package application;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AppProperties {
	
    private static List<String> hashTagList;
    private static String filters[];
    private static String csvFilepath;

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
		
		hashTagList = new ArrayList<String>();
		
		for(String hashtag:hashTags) {
			hashTagList.add(hashtag);
		}
		setHashTagList(hashTagList);
		
		setFilters(properties.getProperty("filters").split(","));
		setCsvFilepath(properties.getProperty("csvFilepath"));
	}
	
	public static List<String> getHashTagList() {
		return hashTagList;
	}
	
	public static void setHashTagList(List<String> hashTagList) {
		AppProperties.hashTagList = hashTagList;
	}

	public static String[] getFilters() {
		return filters;
	}

	public static void setFilters(String[] filters) {
		AppProperties.filters = filters;
	}

	public static String getCsvFilepath() {
		return csvFilepath;
	}

	public static void setCsvFilepath(String csvFilepath) {
		AppProperties.csvFilepath = csvFilepath;
	}
	
	
}
