package comunio.nas.objects;

import comunio.nas.objects.orga.ComunioDate;

public class TransferNewsEntry {
	private ComunioDate  date;
	private String playerId;
	private String buyerId;
	private News news;
	

	public TransferNewsEntry(ComunioDate  date, String playerId, String buyerId, News news) {
		this.date = date;
		this.playerId = playerId;
		this.buyerId = buyerId;
		this.news = news;
	}

	/**
	 * @return the date
	 */
	public ComunioDate  getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(ComunioDate  date) {
		this.date = date;
	}

	/**
	 * @return the playerId
	 */
	public String getPlayerId() {
		return playerId;
	}

	/**
	 * @param playerId the playerId to set
	 */
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	/**
	 * @return the buyerId
	 */
	public String getBuyerId() {
		return buyerId;
	}

	/**
	 * @param buyerId the buyerId to set
	 */
	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}

	/**
	 * @return the news
	 */
	public News getNews() {
		return news;
	}

	/**
	 * @param news the news to set
	 */
	public void setNews(News news) {
		this.news = news;
	}
	
	
}
