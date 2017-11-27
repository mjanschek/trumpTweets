package repositories;


import java.sql.Time;

import com.opencsv.bean.CsvBindByName;

import scala.Tuple5;

/*
 * This repository enables comfortable csv parsing via annotations
 * 
 * The TimeComboPrediction object is a complete prediction for a hashtag combination (Combo object) and a time
 */
public class TimeComboPrediction implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@CsvBindByName
	private Time time;

	@CsvBindByName
    private boolean isTrumpTweet;
    
    @CsvBindByName
    private boolean isNewsTweet;
    
    @CsvBindByName
    private boolean isFakeNewsTweet;
    
    @CsvBindByName
    private boolean isDemocratsTweet;
    
    @CsvBindByName
    private boolean isPoliticsTweet;
    
    @CsvBindByName
    private double count;
    
    @CsvBindByName
    private double meanTextLength;
    
    @CsvBindByName
    private double meanHashtagCount;
    
    @CsvBindByName
    private double meanTrumpCount;
    
    @CsvBindByName
    private double meanSensitiveCount;

	public TimeComboPrediction() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TimeComboPrediction(Time time, boolean isTrumpTweet, boolean isNewsTweet, boolean isFakeNewsTweet,
			boolean isDemocratsTweet, boolean isPoliticsTweet, double count, double meanTextLength,
			double meanHashtagCount, double meanTrumpCount, double meanSensitiveCount) {
		super();
		this.time = time;
		this.isTrumpTweet = isTrumpTweet;
		this.isNewsTweet = isNewsTweet;
		this.isFakeNewsTweet = isFakeNewsTweet;
		this.isDemocratsTweet = isDemocratsTweet;
		this.isPoliticsTweet = isPoliticsTweet;
		this.count = count;
		this.meanTextLength = meanTextLength;
		this.meanHashtagCount = meanHashtagCount;
		this.meanTrumpCount = meanTrumpCount;
		this.meanSensitiveCount = meanSensitiveCount;
	}
	
	@Override
	public String toString() {
		return "TimeComboPrediction [time=" + time + ", isTrumpTweet=" + isTrumpTweet + ", isNewsTweet=" + isNewsTweet
				+ ", isFakeNewsTweet=" + isFakeNewsTweet + ", isDemocratsTweet=" + isDemocratsTweet
				+ ", isPoliticsTweet=" + isPoliticsTweet + ", count=" + count + ", meanTextLength=" + meanTextLength
				+ ", meanHashtagCount=" + meanHashtagCount + ", meanTrumpCount=" + meanTrumpCount
				+ ", meanSensitiveCount=" + meanSensitiveCount + "]";
	}

	@Override
    public int hashCode() {
       return 371 * toFastString().hashCode();
    }
    
	public String toFastString() {
		return time.toString() + isTrumpTweet + isNewsTweet + isFakeNewsTweet + isDemocratsTweet + isPoliticsTweet;
	}
	
	public Tuple5<Boolean, Boolean, Boolean, Boolean, Boolean> getTuple5() {
		return new Tuple5<>(isTrumpTweet,
							isNewsTweet,
							isFakeNewsTweet,
							isDemocratsTweet,
							isPoliticsTweet);
	}
	
	/*
	 * Getters and Setters
	 * 
	 * the boolean set functions also directly manipulate the Combo() object of this class 
	 */
	
	public Time getTime() {
		return time;
	}

	public void setTime(Time time) {
		this.time = time;
	}

	public boolean isTrumpTweet() {
		return isTrumpTweet;
	}

	public void setTrumpTweet(boolean isTrumpTweet) {
		this.isTrumpTweet = isTrumpTweet;
	}

	public boolean isNewsTweet() {
		return isNewsTweet;
	}

	public void setNewsTweet(boolean isNewsTweet) {
		this.isNewsTweet = isNewsTweet;
	}

	public boolean isFakeNewsTweet() {
		return isFakeNewsTweet;
	}

	public void setFakeNewsTweet(boolean isFakeNewsTweet) {
		this.isFakeNewsTweet = isFakeNewsTweet;
	}

	public boolean isDemocratsTweet() {
		return isDemocratsTweet;
	}

	public void setDemocratsTweet(boolean isDemocratsTweet) {
		this.isDemocratsTweet = isDemocratsTweet;
	}

	public boolean isPoliticsTweet() {
		return isPoliticsTweet;
	}

	public void setPoliticsTweet(boolean isPoliticsTweet) {
		this.isPoliticsTweet = isPoliticsTweet;
	}

	public double getCount() {
		return count;
	}

	public void setCount(double count) {
		this.count = count;
	}

	public double getMeanTextLength() {
		return meanTextLength;
	}

	public void setMeanTextLength(double meanTextLength) {
		this.meanTextLength = meanTextLength;
	}

	public double getMeanHashtagCount() {
		return meanHashtagCount;
	}

	public void setMeanHashtagCount(double meanHashtagCount) {
		this.meanHashtagCount = meanHashtagCount;
	}

	public double getMeanTrumpCount() {
		return meanTrumpCount;
	}

	public void setMeanTrumpCount(double meanTrumpCount) {
		this.meanTrumpCount = meanTrumpCount;
	}

	public double getMeanSensitiveCount() {
		return meanSensitiveCount;
	}

	public void setMeanSensitiveCount(double meanSensitiveCount) {
		this.meanSensitiveCount = meanSensitiveCount;
	}
	
}
