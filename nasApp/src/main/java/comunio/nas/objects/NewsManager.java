package comunio.nas.objects;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.enu.NewsArt;
import comunio.nas.objects.helper.LogManager;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.util.DateUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verwalter für alle News-Einträge, gruppiert nach Datum. Unterstützt das
 * Hinzufügen und Zusammenführen von News.
 */
public class NewsManager {
	private static final Logger LOGGER = LogManager.getLogger(NewsManager.class);
	private final Map<ComunioDate, List<News>> newsByDate = new TreeMap<>((d1, d2) -> d2.toString().compareTo(d1.toString()));
	private static String lastUpdateDate = null;

	/**
	 * Fügt eine News hinzu.
	 * 
	 * @param news News-Objekt
	 */
	public void addNews(News news, boolean debug) {
		if (news.getArt().isUNBESTIMMT()) {
			LOGGER.log(Level.WARNING, "NEWS UNBESTIMMT!: " + news.toString());
			return;
		}
		getNewsByDate().computeIfAbsent(news.getDate(), k -> new ArrayList<>()).add(news);
		
		lastUpdateDate = DateUtils.getNowToStringWithHours();
		if (debug) {
			LOGGER.log(Level.INFO, "NEWS hinzugefügt: " + news.toString());
		}
	}

	/**
	 * Führt bestehende News aus GitHub mit neuen News zusammen. Doppelte News (über
	 * equals) werden NICHT erneut hinzugefügt.
	 * 
	 * @param existing JSONArray der bestehenden News (z.B. von GitHub)
	 */
	public void mergeWithExistingNews(JSONArray existing) {
		for (int i = 0; i < existing.length(); i++) {
			JSONObject dateEntry = existing.getJSONObject(i);
			JSONArray newsArray = dateEntry.getJSONArray("news");
			for (int j = 0; j < newsArray.length(); j++) {
				News news = News.fromJson(newsArray.getJSONObject(j));
				if (!contains(news)) {
					addNews(news, false);
				}
			}
		}
	}

	/**
	 * Prüft, ob eine News bereits enthalten ist.
	 */
	public boolean contains(News news) {
		List<News> list = getNewsByDate().get(news.getDate());
		return list != null && list.contains(news);
	}
	
	

	/**
	 * Exportiert alle News als JSONArray (gruppiert nach Datum).
	 */
	public JSONArray dbToJson() {
		JSONArray result = new JSONArray();
		getNewsByDate().forEach((date, newsList) -> {
			JSONObject dateEntry = new JSONObject();
			JSONArray newsArray = new JSONArray();
			newsList.forEach(news -> newsArray.put(news.toJson()));
			dateEntry.put("date", date.toString());
			dateEntry.put("news", newsArray);
			result.put(dateEntry);
		});
		return result;
	}
	
	public JSONObject objectToJson() {
		JSONObject result = new JSONObject();
		JSONArray db = dbToJson();
		result.put("lastUpdate", lastUpdateDate);
		result.put("newsDB", db);
		return result;
	}


	/**
	 * Erstellt einen NewsManager aus einem JSONArray.
	 */
	public static NewsManager fromJsonDb(JSONArray jsonArray) {
		NewsManager manager = new NewsManager();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject dateEntry = jsonArray.getJSONObject(i);
			JSONArray newsArray = dateEntry.getJSONArray("news");
			for (int j = 0; j < newsArray.length(); j++) {
				manager.addNews(News.fromJson(newsArray.getJSONObject(j)), false);
			}
		}
		return manager;
	}
	
	public static NewsManager fromJsonObject(JSONObject jsonObject) {
		NewsManager manager = new NewsManager();
		if(jsonObject.has("lastUpdate")) {
			lastUpdateDate = jsonObject.getString("lastUpdate");
		}
		if(jsonObject.has("newsDB")) {
			return fromJsonDb(jsonObject.getJSONArray("newsDB"));
		}
		return manager;
	}

	public Map<ComunioDate, List<News>> getNewsByDate() {
		return newsByDate;
	}

	public Map<ComunioDate, List<News>> getTransferNewsByDate() {
		Map<ComunioDate, List<News>> result = new HashMap<ComunioDate, List<News>>();
		for (Entry<ComunioDate, List<News>> entry : newsByDate.entrySet()) {
			ComunioDate date = entry.getKey();
			List<News> newsList = entry.getValue();
			List<News> resultList = new ArrayList<News>();
			for (News n : newsList) {
				if (n.getArt().isTRANSFER()) {
					resultList.add(n);
				}
			}
			result.put(date, resultList);
		}
		return result;
	}

	public Set<String>  getNewsIdsForArt(NewsArt art) {
		Set<String> resultSet = new HashSet<String>();
		for (Entry<ComunioDate, List<News>> entry : newsByDate.entrySet()) {
			entry.getKey();
			List<News> newsList = entry.getValue();
			for (News n : newsList) {
				if (n.getArt().equals(art)) {
					String id = n.getId();
					if(!id.equals("0")) {
						resultSet.add(n.getId());
					}
					
				}
			}
		}
		return resultSet;
	}
	
	/**
	 * Entfernt alle News aus dem Manager.
	 */
	public void clear() {
		newsByDate.clear();
		lastUpdateDate = null;
	}
	
}
