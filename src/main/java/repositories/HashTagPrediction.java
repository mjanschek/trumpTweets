package repositories;

import com.opencsv.bean.CsvBindByName;

public class HashTagPrediction {

    @CsvBindByName
    private String hashtag;
    
    @CsvBindByName
    private double probability;
    
    @CsvBindByName
    private double probability_trump;
    
    @CsvBindByName
    private double probability_news;
    
    @CsvBindByName
    private double probability_fakenews;
    
    @CsvBindByName
    private double probability_democrats;
    
    @CsvBindByName
    private double probability_washingtondc;

	public HashTagPrediction() {
		super();
		// TODO Auto-generated constructor stub
	}

	public HashTagPrediction(String hashtag, double probability, double probability_trump, double probability_news,
			double probability_fakenews, double probability_democrats, double probability_washingtondc) {
		super();
		this.hashtag = hashtag;
		this.probability = probability;
		this.probability_trump = probability_trump;
		this.probability_news = probability_news;
		this.probability_fakenews = probability_fakenews;
		this.probability_democrats = probability_democrats;
		this.probability_washingtondc = probability_washingtondc;
	}

	@Override
	public String toString() {
		return "HashTagPrediction [hashtag=" + hashtag + ", probability=" + probability + ", probability_trump="
				+ probability_trump + ", probability_news=" + probability_news + ", probability_fakenews="
				+ probability_fakenews + ", probability_democrats=" + probability_democrats
				+ ", probability_washingtondc=" + probability_washingtondc + "]";
	}

	public String getHashtag() {
		return hashtag;
	}

	public void setHashtag(String hashtag) {
		this.hashtag = hashtag;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public double getProbability_trump() {
		return probability_trump;
	}

	public void setProbability_trump(double probability_trump) {
		this.probability_trump = probability_trump;
	}

	public double getProbability_news() {
		return probability_news;
	}

	public void setProbability_news(double probability_news) {
		this.probability_news = probability_news;
	}

	public double getProbability_fakenews() {
		return probability_fakenews;
	}

	public void setProbability_fakenews(double probability_fakenews) {
		this.probability_fakenews = probability_fakenews;
	}

	public double getProbability_democrats() {
		return probability_democrats;
	}

	public void setProbability_democrats(double probability_democrats) {
		this.probability_democrats = probability_democrats;
	}

	public double getProbability_washingtondc() {
		return probability_washingtondc;
	}

	public void setProbability_washingtondc(double probability_washingtondc) {
		this.probability_washingtondc = probability_washingtondc;
	}
	
}
