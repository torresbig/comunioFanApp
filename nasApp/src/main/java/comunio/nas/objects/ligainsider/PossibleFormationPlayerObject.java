package comunio.nas.objects.ligainsider;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import comunio.nas.enu.PlayerMatchStatus;

public class PossibleFormationPlayerObject implements LigainsiderInterface {

	private String playerName;
	private String playerId;
	private String clubId;
	private String comunioPlayerId;
	private String playerLink;
	private String playerNote;
	Map<Integer, PlayerMatchStatus> last5MatchStatus;

	public PossibleFormationPlayerObject(String playerName, String playerId, String clubId, String playerLink, String playerNote, Map<Integer, PlayerMatchStatus> last5MatchStatus) {
		this.setPlayerName(playerName);
		this.setPlayerId(playerId);
		this.setClubId(clubId);
		this.setPlayerLink(playerLink);
		this.setPlayerNote(playerNote);
		this.comunioPlayerId = ""; 
		this.last5MatchStatus = last5MatchStatus;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("playerName", playerName);
		result.put("playerId", playerId);
		result.put("clubId", clubId);
		result.put("playerLink", playerLink);
		result.put("playerNote", playerNote);
		result.put("comunioPlayerId", comunioPlayerId);
		if (last5MatchStatus != null && !last5MatchStatus.isEmpty()) {
			JSONArray resArray = new JSONArray();
			for (Map.Entry<Integer, PlayerMatchStatus> entry : last5MatchStatus.entrySet()) {
				JSONObject resultLast5JSON = new JSONObject();
				resultLast5JSON.put(String.valueOf(entry.getKey()), entry.getValue());
				resArray.put(resultLast5JSON);
			}
			result.put("last5MatchStatus", resArray);
		}
		return result;
	}
	
	public JSONObject toPlayerDataJSON() {
		JSONObject result = new JSONObject();
		result.put("playerName", playerName);
		result.put("playerId", playerId);
		result.put("playerLink", playerLink);
		return result;
	}


	public static PossibleFormationPlayerObject fromJSON(JSONObject json) {
		String playerName = json.getString("playerName");
		String playerId = json.getString("playerId");
		String clubId = json.getString("clubId");
		String playerLink = json.getString("playerLink");
		String playerNote = json.getString("playerNote");
		String comunioPlayerId = json.optString("comunioPlayerId","");
		
		Map<Integer, PlayerMatchStatus> last5MatchStatus = new HashMap<>();
		if (json.has("last5MatchStatus")) {
			JSONArray last5Array = json.getJSONArray("last5MatchStatus");
			for (int i = 0; i < last5Array.length(); i++) {
				JSONObject matchObj = last5Array.getJSONObject(i);
				for (String key : matchObj.keySet()) {
					PlayerMatchStatus pms = PlayerMatchStatus.valueOf(matchObj.getString(key));
					last5MatchStatus.put(Integer.parseInt(key), pms);
				}
			}
		}
		PossibleFormationPlayerObject result = new PossibleFormationPlayerObject(playerName, playerId, clubId, playerLink, playerNote, last5MatchStatus);
		if(!comunioPlayerId.isBlank()) {
			result.setComunioPlayerId(comunioPlayerId);
		}
		return result;
	}

	@Override
	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
	@Override
	public String getLigainsiderPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getClubId() {
		return clubId;
	}

	public void setClubId(String clubId) {
		this.clubId = clubId;
	}
	@Override
	public String getPlayerLink() {
		return playerLink;
	}

	public void setPlayerLink(String playerLink) {
		this.playerLink = playerLink;
	}
	public String getPlayerNote() {
		return playerNote;
	}

	public void setPlayerNote(String playerNote) {
		this.playerNote = playerNote;
	}
	public Map<Integer, PlayerMatchStatus> getLast5MatchStatus() {
		return last5MatchStatus;
	}

	public void setLast5MatchStatus(Map<Integer, PlayerMatchStatus> last5MatchStatus) {
		this.last5MatchStatus = last5MatchStatus;
	}

	public void addLastMatchStatus(int pos, PlayerMatchStatus matchStatus) {
		if (this.last5MatchStatus == null) {
			this.last5MatchStatus = new HashMap<>();
		}
		this.last5MatchStatus.put(pos, matchStatus);
	}
	
	public String getComunioPlayerId() {
		return comunioPlayerId;
	}

	public void setComunioPlayerId(String comunioPlayerId) {
		this.comunioPlayerId = comunioPlayerId;
	}




	
	
}
