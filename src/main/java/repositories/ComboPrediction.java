package repositories;

import com.opencsv.bean.CsvBindByName;

@SuppressWarnings("serial")
public class ComboPrediction implements java.io.Serializable {

	@CsvBindByName
    private boolean isTrumpTweet;
    
    @CsvBindByName
    private boolean isNewsTweet;
    
    @CsvBindByName
    private boolean isFakeNewsTweet;
    
    @CsvBindByName
    private boolean isDemocratsTweet;
    
    @CsvBindByName
    private boolean isWashingtonDCTweet;
    
    @CsvBindByName
    private double meanTextLength;
    
    @CsvBindByName
    private double meanFavPerMin;
    
    @CsvBindByName
    private double meanRTPerMin;
    
    @CsvBindByName
    private double meanTweetsPerMinute;

	public ComboPrediction() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ComboPrediction(boolean isTrumpTweet, boolean isNewsTweet, boolean isFakeNewsTweet, boolean isDemocratsTweet,
			boolean isWashingtonDCTweet, double meanTextLength, double meanFavPerMin, double meanRTPerMin,
			double meanTweetsPerMinute) {
		super();
		this.isTrumpTweet = isTrumpTweet;
		this.isNewsTweet = isNewsTweet;
		this.isFakeNewsTweet = isFakeNewsTweet;
		this.isDemocratsTweet = isDemocratsTweet;
		this.isWashingtonDCTweet = isWashingtonDCTweet;
		this.meanTextLength = meanTextLength;
		this.meanFavPerMin = meanFavPerMin;
		this.meanRTPerMin = meanRTPerMin;
		this.meanTweetsPerMinute = meanTweetsPerMinute;
	}

	@Override
	public String toString() {
		return "ComboPrediction [isTrumpTweet=" + isTrumpTweet + ", isNewsTweet=" + isNewsTweet + ", isFakeNewsTweet="
				+ isFakeNewsTweet + ", isDemocratsTweet=" + isDemocratsTweet + ", isWashingtonDCTweet="
				+ isWashingtonDCTweet + ", meanTextLength=" + meanTextLength + ", meanFavPerMin=" + meanFavPerMin
				+ ", meanRTPerMin=" + meanRTPerMin + ", meanTweetsPerMinute=" + meanTweetsPerMinute + "]";
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

	public boolean isWashingtonDCTweet() {
		return isWashingtonDCTweet;
	}

	public void setWashingtonDCTweet(boolean isWashingtonDCTweet) {
		this.isWashingtonDCTweet = isWashingtonDCTweet;
	}

	public double getMeanTextLength() {
		return meanTextLength;
	}

	public void setMeanTextLength(double meanTextLength) {
		this.meanTextLength = meanTextLength;
	}

	public double getMeanFavPerMin() {
		return meanFavPerMin;
	}

	public void setMeanFavPerMin(double meanFavPerMin) {
		this.meanFavPerMin = meanFavPerMin;
	}

	public double getMeanRTPerMin() {
		return meanRTPerMin;
	}

	public void setMeanRTPerMin(double meanRTPerMin) {
		this.meanRTPerMin = meanRTPerMin;
	}

	public double getMeanTweetsPerMinute() {
		return meanTweetsPerMinute;
	}

	public void setMeanTweetsPerMinute(double meanTweetsPerMinute) {
		this.meanTweetsPerMinute = meanTweetsPerMinute;
	}

	
}
