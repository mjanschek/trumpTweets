package repositories;

import java.util.Date;

import com.opencsv.bean.CsvBindByName;

@SuppressWarnings("serial")
public class TimeComboPrediction implements java.io.Serializable {
	
	@CsvBindByName
	private Date time;

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
    private double count;
    
    @CsvBindByName
    private double meanTextLength;
    
    @CsvBindByName
    private double totalHashtagCount;
    
    @CsvBindByName
    private double totalTrumpCount;
    
    @CsvBindByName
    private double totalSensitiveCount;

	public TimeComboPrediction() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TimeComboPrediction(Date time, boolean isTrumpTweet, boolean isNewsTweet, boolean isFakeNewsTweet,
			boolean isDemocratsTweet, boolean isWashingtonDCTweet, double count, double meanTextLength,
			double totalHashtagCount, double totalTrumpCount, double totalSensitiveCount) {
		super();
		this.time = time;
		this.isTrumpTweet = isTrumpTweet;
		this.isNewsTweet = isNewsTweet;
		this.isFakeNewsTweet = isFakeNewsTweet;
		this.isDemocratsTweet = isDemocratsTweet;
		this.isWashingtonDCTweet = isWashingtonDCTweet;
		this.count = count;
		this.meanTextLength = meanTextLength;
		this.totalHashtagCount = totalHashtagCount;
		this.totalTrumpCount = totalTrumpCount;
		this.totalSensitiveCount = totalSensitiveCount;
	}

	@Override
	public String toString() {
		return "TimeComboPrediction [time=" + time + ", isTrumpTweet=" + isTrumpTweet + ", isNewsTweet=" + isNewsTweet
				+ ", isFakeNewsTweet=" + isFakeNewsTweet + ", isDemocratsTweet=" + isDemocratsTweet
				+ ", isWashingtonDCTweet=" + isWashingtonDCTweet + ", count=" + count + ", meanTextLength="
				+ meanTextLength + ", totalHashtagCount=" + totalHashtagCount + ", totalTrumpCount=" + totalTrumpCount
				+ ", totalSensitiveCount=" + totalSensitiveCount + "]";
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
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

	public boolean isWashingtonDCTweet() {
		return isWashingtonDCTweet;
	}

	public void setWashingtonDCTweet(boolean isWashingtonDCTweet) {
		this.isWashingtonDCTweet = isWashingtonDCTweet;
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

	public double getTotalHashtagCount() {
		return totalHashtagCount;
	}

	public void setTotalHashtagCount(double totalHashtagCount) {
		this.totalHashtagCount = totalHashtagCount;
	}

	public double getTotalTrumpCount() {
		return totalTrumpCount;
	}

	public void setTotalTrumpCount(double totalTrumpCount) {
		this.totalTrumpCount = totalTrumpCount;
	}

	public double getTotalSensitiveCount() {
		return totalSensitiveCount;
	}

	public void setTotalSensitiveCount(double totalSensitiveCount) {
		this.totalSensitiveCount = totalSensitiveCount;
	}

}
