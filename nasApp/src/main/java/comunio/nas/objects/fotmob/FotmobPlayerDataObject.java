package comunio.nas.objects.fotmob;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FotmobPlayerDataObject {

	private Integer id; // fotmob ID, nicht Comunio!
	private String name;
	private String comunioId;
	private String clubId; // fotmob clubId
	private String comunioClubId;
	private Boolean isCaptain;
	private String status;
	private InjuryInformation injuryInformation;
	private List<PositionDetail> positions;
	private List<PlayerInformation> playerInformation;
	private Instant lastUpdate;

	public FotmobPlayerDataObject() {
	}

	// --- Helper for Null-Safe JSON Extraction ---
	static String optString(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optString(key) : null;
	}

	static Integer optInteger(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optInt(key) : null;
	}

	static Double optDouble(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optDouble(key) : null;
	}

	static Boolean optBoolean(JSONObject json, String key) {
		return json.has(key) && !json.isNull(key) ? json.optBoolean(key) : null;
	}

	// --- From JSON ---
	public static FotmobPlayerDataObject fromJSON(JSONObject json) {
		if (json == null)
			return null;

		FotmobPlayerDataObject player = new FotmobPlayerDataObject();
		player.setId(optInteger(json, "id"));
		player.setComunioId(optString(json, "comunioId"));
		player.setClubId(optString(json, "clubId"));
		player.setComunioClubId(optString(json, "comunioClubId"));
		player.setName(optString(json, "name"));
		player.setIsCaptain(optBoolean(json, "isCaptain"));
		player.setStatus(optString(json, "status"));

		if (json.has("injuryInformation") && !json.isNull("injuryInformation")) {
			player.setInjuryInformation(InjuryInformation.fromJSON(json.getJSONObject("injuryInformation")));
		}

		if (json.has("positionDescription") && !json.isNull("positionDescription")) {
			JSONObject positionD = json.getJSONObject("positionDescription");
			if (positionD.has("positions") && !positionD.isNull("positions")) {
				JSONArray arr = positionD.getJSONArray("positions");
				List<PositionDetail> list = new ArrayList<>();
				for (int i = 0; i < arr.length(); i++) {
					if (!arr.isNull(i))
						list.add(PositionDetail.fromJSON(arr.getJSONObject(i)));
				}
				player.setPositions(list);
			}
		}

		if (json.has("positions") && !json.isNull("poistions")) {
			JSONArray arr = json.getJSONArray("positions");
			List<PositionDetail> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i)) {
					list.add(PositionDetail.fromJSON(arr.getJSONObject(i)));
				}
			}
			player.setPositions(list);
		}

		if (json.has("playerInformation") && !json.isNull("playerInformation")) {
			JSONArray arr = json.getJSONArray("playerInformation");
			List<PlayerInformation> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				if (!arr.isNull(i)) {
					list.add(PlayerInformation.fromJSON(arr.getJSONObject(i)));
				}
			}
			player.setPlayerInformation(list);
		}

		if (json.has("lastUpdate") && !json.isNull("lastUpdate")) {
			player.setLastUpdate(Instant.parse(json.getString("lastUpdate")));
		}

		return player;
	}

	// --- To JSON ---
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (id != null)
			json.put("id", id);
		if (getComunioId() != null)
			json.put("comunioId", getComunioId());
		if (getComunioClubId() != null)
			json.put("comunioClubId", getComunioClubId());
		if (getClubId() != null)
			json.put("clubId", getClubId());
		if (name != null)
			json.put("name", name);
		if (isCaptain != null)
			json.put("isCaptain", isCaptain);
		if (status != null)
			json.put("status", status);
		if (injuryInformation != null)
			json.put("injuryInformation", injuryInformation.toJSON());

		if (positions != null) {
			JSONArray arr = new JSONArray();
			for (PositionDetail pd : positions) {
				if (pd != null)
					arr.put(pd.toJSON());
			}
			json.put("positions", arr);
		}
		if (playerInformation != null) {
			JSONArray arr = new JSONArray();
			for (PlayerInformation info : playerInformation) {
				if (info != null)
					arr.put(info.toJSON());
			}
			json.put("playerInformation", arr);
		}
		if (lastUpdate != null) {
			json.put("lastUpdate", lastUpdate.toString());
		}

		return json;
	}

	// --- Sinnvolle Utility-Methoden ---

	public boolean isInjured() {
		return injuryInformation != null && injuryInformation.getName() != null;
	}

	public Optional<PositionDetail> getMainPositionDetail() {
		if (positions == null ) {
			return null;
		}
		return positions.stream().filter(p -> Boolean.TRUE.equals(p.getIsMainPosition())).findFirst();
	}

	public Optional<PlayerInformation> getInfoByTitle(String title) {
		if (playerInformation == null)
			return Optional.empty();
		return playerInformation.stream().filter(info -> info.getTitle() != null && info.getTitle().equalsIgnoreCase(title)).findFirst();
	}

	// --- Getters & Setters ---
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsCaptain() {
		return isCaptain;
	}

	public void setIsCaptain(Boolean captain) {
		isCaptain = captain;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public InjuryInformation getInjuryInformation() {
		return injuryInformation;
	}

	public void setInjuryInformation(InjuryInformation injuryInformation) {
		this.injuryInformation = injuryInformation;
	}

	public List<PlayerInformation> getPlayerInformation() {
		return playerInformation;
	}

	public void setPlayerInformation(List<PlayerInformation> playerInformation) {
		this.playerInformation = playerInformation;
	}

	public List<PositionDetail> getPositions() {
		return positions;
	}

	public void setPositions(List<PositionDetail> positions) {
		this.positions = positions;
	}

	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Instant lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getComunioId() {
		return comunioId;
	}

	public void setComunioId(String comunioId) {
		this.comunioId = comunioId;
	}

	public String getComunioClubId() {
		return comunioClubId;
	}

	public void setComunioClubId(String comunioClubId) {
		this.comunioClubId = comunioClubId;
	}

	public String getClubId() {
		return clubId;
	}

	public void setClubId(String clubId) {
		this.clubId = clubId;
	}
	
	/**
	 * 
	 * full abfrage des players über die playerurl
	 * 
	 * 
https://www.fotmob.com/api/data/playerData?id=1179182
{
	"id": 1179182,
	"name": "Adrian Gantenbein",
	"isCaptain": false,
	"injuryInformation": {
		"name": "Ankle injury",
		"expectedReturn": {
			"expectedReturnKey": "expected_return_date_late",
			"expectedReturnDateParam": "September 2026",
			"expectedReturnFallback": "Late September 2026"
		},
		"lastUpdated": {
			"utcTime": "2026-07-25T00:00:00.000Z",
			"timezone": null
		}
	},
	"positionDescription": {
		"positions": [
			{
				"strPos": {
					"label": "Right Back",
					"key": "rightback"
				},
				"strPosShort": {
					"label": "RB",
					"key": "rightback_short"
				},
				"occurences": 2,
				"position": "32",
				"isMainPosition": false,
				"pitchPositionData": {
					"right": 0.83,
					"top": 0.74,
					"ratio": 0.23921568627450981
				}
			},
			{
				"strPos": {
					"label": "Left Back",
					"key": "leftback"
				},
				"strPosShort": {
					"label": "LB",
					"key": "leftback_short"
				},
				"occurences": 24,
				"position": "38",
				"isMainPosition": true,
				"pitchPositionData": {
					"right": 0.17,
					"top": 0.74
				}
			},
			{
				"strPos": {
					"label": "Left Wing-Back",
					"key": "left_wing_back"
				},
				"strPosShort": {
					"label": "LWB",
					"key": "left_wing_back_short"
				},
				"occurences": 14,
				"position": "68",
				"isMainPosition": false,
				"pitchPositionData": {
					"right": 0.17,
					"top": 0.58,
					"ratio": 0.47450980392156866
				}
			},
			{
				"strPos": {
					"label": "Left Midfielder",
					"key": "leftmidfielder"
				},
				"strPosShort": {
					"label": "LM",
					"key": "leftmidfielder_short"
				},
				"occurences": 6,
				"position": "78",
				"isMainPosition": false,
				"pitchPositionData": {
					"right": 0.17,
					"top": 0.42,
					"ratio": 0.3176470588235294
				}
			},
			{
				"strPos": {
					"label": "Attacking Midfielder",
					"key": "centerattackingmidfielder"
				},
				"strPosShort": {
					"label": "AM",
					"key": "centerattackingmidfielder_short"
				},
				"occurences": 3,
				"position": "85",
				"isMainPosition": false,
				"pitchPositionData": {
					"right": 0.5,
					"top": 0.26,
					"ratio": 0.25882352941176473
				}
			},
			{
				"strPos": {
					"label": "Left Winger",
					"key": "leftwinger"
				},
				"strPosShort": {
					"label": "LW",
					"key": "leftwinger_short"
				},
				"occurences": 2,
				"position": "87",
				"isMainPosition": false,
				"pitchPositionData": {
					"right": 0.17,
					"top": 0.26,
					"ratio": 0.23921568627450981
				}
			}
		],
		"primaryPosition": {
			"label": "Left Back",
			"key": "leftback"
		},
		"nonPrimaryPositions": [
			{
				"label": "Right Back",
				"key": "rightback"
			},
			{
				"label": "Left Wing-Back",
				"key": "left_wing_back"
			},
			{
				"label": "Left Midfielder",
				"key": "leftmidfielder"
			},
			{
				"label": "Attacking Midfielder",
				"key": "centerattackingmidfielder"
			},
			{
				"label": "Left Winger",
				"key": "leftwinger"
			}
		]
	},
	"playerInformation": [
		{
			"value": {
				"numberValue": 183,
				"key": null,
				"fallback": "183 cm"
			},
			"title": "Height",
			"translationKey": "height_sentencecase"
		},
		{
			"value": {
				"numberValue": 17,
				"options": {},
				"key": null,
				"fallback": 17
			},
			"title": "Shirt",
			"translationKey": "shirt"
		},
		{
			"value": {
				"numberValue": 25,
				"options": {
					"style": "unit",
					"unit": "year",
					"unitDisplay": "long"
				},
				"key": null,
				"fallback": "25"
			},
			"title": "Age",
			"translationKey": "age_sentencecase"
		},
		{
			"value": {
				"key": "right",
				"fallback": "Right"
			},
			"title": "Preferred foot",
			"translationKey": "preferred_foot"
		},
		{
			"value": {
				"key": null,
				"fallback": "Switzerland"
			},
			"title": "Country",
			"translationKey": "country_sentencecase",
			"icon": {
				"type": "country",
				"id": "SUI"
			},
			"countryCode": "SUI"
		},
		{
			"value": {
				"numberValue": 999268,
				"key": null,
				"fallback": "€999.3k"
			},
			"title": "Market value",
			"translationKey": "transfer_value"
		},
		{
			"value": {
				"dateValue": "2028-06-30",
				"key": null,
				"fallback": {
					"utcTime": "2028-06-30T00:00:00.000Z",
					"timezone": "UTC"
				}
			},
			"title": "Contract end",
			"translationKey": "contract_end"
		}
	],
	"status": "active"
}
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * ##########################################################################
	 * PLAYERDATA über den Club. schnellere api schonende abfrage!!!
	 * //https://www.fotmob.com/api/data/teams?id=10189

{
	"details": {
		"id": 10189,
		"name": "Schalke 04",
		"shortName": "Schalke 04"
	},
	"squad": {
		"squad": [	
			{
				"title": "keepers",
				"members": [
					{
						"id": 1738299,
						"name": "Faaris Yusufu",
						"shirtNumber": null,
						"ccode": "GER",
						"cname": "Germany",
						"role": {
							"key": "keeper_long",
							"fallback": "Keeper"
						},
						"positionId": 0,
						"injury": null,
						"rating": null,
						"goals": 0,
						"penalties": 0,
						"assists": 0,
						"rcards": 0,
						"ycards": 0,
						"excludeFromRanking": true,
						"positionIds": null,
						"positionIdsDesc": null,
						"height": 183,
						"age": 21,
						"dateOfBirth": "2005-02-11",
						"transferValue": 112879
					},...
				]
			},
			{
				"title": "defenders",
				"members": [
					{
						"id": 560976,
						"name": "Janik Bachmann",
						"shirtNumber": 14,
						"ccode": "GER",
						"cname": "Germany",
						"role": {
							"key": "midfielder_long",
							"fallback": "Midfielder"
						},
						"positionId": 2,
						"injury": null,
						"rating": null,
						"goals": 0,
						"penalties": 0,
						"assists": 0,
						"rcards": 0,
						"ycards": 0,
						"excludeFromRanking": true,
						"positionIds": "84",
						"positionIdsDesc": "CAM",
						"height": 189,
						"age": 30,
						"dateOfBirth": "1996-05-06",
						"transferValue": 282610
					},
					{
						"id": 1179182,
						"name": "Adrian Gantenbein",
						"shirtNumber": 17,
						"ccode": "SUI",
						"cname": "Switzerland",
						"role": {
							"key": "midfielder_long",
							"fallback": "Midfielder"
						},
						"positionId": 2,
						"injured": true,
						"injury": {
							"id": "30",
							"expectedReturn": "Mid September 2026"
						},
						"rating": null,
						"goals": 0,
						"penalties": 0,
						"assists": 0,
						"rcards": 0,
						"ycards": 0,
						"excludeFromRanking": true,
						"positionIds": "62",
						"positionIdsDesc": "RWB",
						"height": 183,
						"age": 25,
						"dateOfBirth": "2001-04-18",
						"transferValue": 999268
					},
					{
						"id": 469763,
						"name": "Kenan Karaman",
						"shirtNumber": 19,
						"ccode": "TUR",
						"cname": "Turkiye",
						"role": {
							"key": "midfielder_long",
							"fallback": "Midfielder"
						},
						"positionId": 2,
						"injury": null,
						"rating": null,
						"goals": 0,
						"penalties": 0,
						"assists": 0,
						"rcards": 0,
						"ycards": 0,
						"excludeFromRanking": true,
						"positionIds": "86,107",
						"positionIdsDesc": "CAM,LW",
						"height": 189,
						"age": 32,
						"dateOfBirth": "1994-03-05",
						"transferValue": 1354819
					},....
	 */

	
	



}