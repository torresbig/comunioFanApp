package comunio.nas.objects.orga;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.dataScraper.comunio.LineupParser;
import comunio.nas.objects.NewsManager;
import comunio.nas.objects.user.User;
import comunio.nas.util.StatusManager;

public class UpdaterContextData {
	public JSONObject matchdayInfoList;
	public JSONArray clubDB;
	public JSONObject injuryDB;
	public JSONObject playerDBObject;
	public JSONObject pointsDB;
	public JSONArray marketValueDB;
	public NewsManager newsManager;
	public Map<String, User> userMap;
	public LineupParser lineupParser;
	public JSONObject notInligaDBObj;
	public JSONArray transfermarktListe;
	public Map<String, String> playerToUserMap;
	public StatusManager statusManager;

}