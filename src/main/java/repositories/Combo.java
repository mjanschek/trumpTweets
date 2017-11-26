package repositories;

/*
 * object for hashtag combinations
 * this object is used to provide (hopefully) easier readability of spark code
 * 
 * essentially, this is a boolean[5] array with some functionality
 */
@SuppressWarnings("serial")
public class Combo implements java.io.Serializable {
	
    private boolean isTrumpTweet;
    
    private boolean isNewsTweet;
    
    private boolean isFakeNewsTweet;
    
    private boolean isDemocratsTweet;
    
    private boolean isPoliticsTweet;

	public Combo() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Combo(boolean isTrumpTweet, boolean isNewsTweet, boolean isFakeNewsTweet, boolean isDemocratsTweet,
			boolean isPoliticsTweet) {
		super();
		this.isTrumpTweet = isTrumpTweet;
		this.isNewsTweet = isNewsTweet;
		this.isFakeNewsTweet = isFakeNewsTweet;
		this.isDemocratsTweet = isDemocratsTweet;
		this.isPoliticsTweet = isPoliticsTweet;
	}

	/*
	 * hashCode() and equals() had to be customized for spark joining, merging, etc.
	 */
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
				+ isFakeNewsTweet + ", isDemocratsTweet=" + isDemocratsTweet + ", isPoliticsTweet="
				+ isPoliticsTweet + "]";
	}
	
	/*
	 * Getters and Setters...
	 */	
	public String getCsvHeader() {
		return "\"isTrumpTweet\";\"isNewsTweet\";\"isFakeNewsTweet\";\"isDemocratsTweet\";\"isPoliticsTweet\"";
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
    
}
