package repositories;


import java.sql.Time;

import com.opencsv.bean.CsvBindByName;

@SuppressWarnings("serial")
public class TimeComboPrediction implements java.io.Serializable {
	
	private Combo combo;
	
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
		// TODO Auto-generated constructor stub+
		combo = new Combo();
	}

	public TimeComboPrediction(Combo combo, Time time, boolean isTrumpTweet, boolean isNewsTweet,
			boolean isFakeNewsTweet, boolean isDemocratsTweet, boolean isWashingtonDCTweet, double count,
			double meanTextLength, double totalHashtagCount, double totalTrumpCount, double totalSensitiveCount) {
		super();
		this.combo = combo;
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
		return "TimeComboPrediction [combo=" + combo + ", time=" + time + ", isTrumpTweet=" + isTrumpTweet
				+ ", isNewsTweet=" + isNewsTweet + ", isFakeNewsTweet=" + isFakeNewsTweet + ", isDemocratsTweet="
				+ isDemocratsTweet + ", isWashingtonDCTweet=" + isWashingtonDCTweet + ", count=" + count
				+ ", meanTextLength=" + meanTextLength + ", totalHashtagCount=" + totalHashtagCount
				+ ", totalTrumpCount=" + totalTrumpCount + ", totalSensitiveCount=" + totalSensitiveCount + "]";
	}

	public Combo getCombo() {
		return combo;
	}

	public void setCombo(Combo combo) {
		this.combo = combo;
	}

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
		combo.setTrumpTweet(isTrumpTweet);
	}

	public boolean isNewsTweet() {
		return isNewsTweet;
	}

	public void setNewsTweet(boolean isNewsTweet) {
		this.isNewsTweet = isNewsTweet;
		combo.setNewsTweet(isNewsTweet);
	}

	public boolean isFakeNewsTweet() {
		return isFakeNewsTweet;
	}

	public void setFakeNewsTweet(boolean isFakeNewsTweet) {
		this.isFakeNewsTweet = isFakeNewsTweet;
		combo.setFakeNewsTweet(isFakeNewsTweet);
	}

	public boolean isDemocratsTweet() {
		return isDemocratsTweet;
	}

	public void setDemocratsTweet(boolean isDemocratsTweet) {
		this.isDemocratsTweet = isDemocratsTweet;
		combo.setDemocratsTweet(isDemocratsTweet);
	}

	public boolean isWashingtonDCTweet() {
		return isWashingtonDCTweet;
	}

	public void setWashingtonDCTweet(boolean isWashingtonDCTweet) {
		this.isWashingtonDCTweet = isWashingtonDCTweet;
		combo.setWashingtonDCTweet(isWashingtonDCTweet);
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
