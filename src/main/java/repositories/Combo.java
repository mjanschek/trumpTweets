package repositories;

@SuppressWarnings("serial")
public class Combo implements java.io.Serializable {
	
    private boolean isTrumpTweet;
    
    private boolean isNewsTweet;
    
    private boolean isFakeNewsTweet;
    
    private boolean isDemocratsTweet;
    
    private boolean isWashingtonDCTweet;

	public Combo() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Combo(boolean isTrumpTweet, boolean isNewsTweet, boolean isFakeNewsTweet, boolean isDemocratsTweet,
			boolean isWashingtonDCTweet) {
		super();
		this.isTrumpTweet = isTrumpTweet;
		this.isNewsTweet = isNewsTweet;
		this.isFakeNewsTweet = isFakeNewsTweet;
		this.isDemocratsTweet = isDemocratsTweet;
		this.isWashingtonDCTweet = isWashingtonDCTweet;
	}

    @Override
    public int hashCode() {
       return 371 * toString().hashCode();
    }

    @Override 
    public boolean equals(Object other) {
       if (this == other) return true;
       if (other == null || this.getClass() != other.getClass()) return false;

       Combo combo = (Combo) other;
       return this.toString().equals(combo.toString());
    }

	@Override
	public String toString() {
		return "Combo [isTrumpTweet=" + isTrumpTweet + ", isNewsTweet=" + isNewsTweet + ", isFakeNewsTweet="
				+ isFakeNewsTweet + ", isDemocratsTweet=" + isDemocratsTweet + ", isWashingtonDCTweet="
				+ isWashingtonDCTweet + "]";
	}
	
	public String getCsvHeader() {
		return "\"isTrumpTweet\";\"isNewsTweet\";\"isFakeNewsTweet\";\"isDemocratsTweet\";\"isWashingtonDCTweet\"";
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
    
}
