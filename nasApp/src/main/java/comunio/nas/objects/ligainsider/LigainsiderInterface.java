package comunio.nas.objects.ligainsider;

import org.json.JSONObject;


public interface LigainsiderInterface {

	// 1. Getter-Methoden, damit das Interface auf die Daten zugreifen kann
	String getPlayerName();

	String getLigainsiderPlayerId();

	String getPlayerLink();

	JSONObject toJSON();
	

	default JSONObject toPlayerDataJSON() {
		JSONObject result = new JSONObject();
		result.put("playerName", getPlayerName());
		result.put("playerId", getLigainsiderPlayerId());
		result.put("playerLink", getPlayerLink());
		return result;
	}

	default String extractPlayerNameFromUrl() {
		String link = getPlayerLink();
		if (link == null || !link.contains("_")) {
			return "N/A";
		}

		// Trailing Slash entfernen
		String cleanLink = link.endsWith("/") ? link.substring(0, link.length() - 1) : link;

		int lastSlash = cleanLink.lastIndexOf("/");
		int lastUnderscore = cleanLink.lastIndexOf("_");

		if (lastUnderscore > lastSlash && lastSlash != -1) {
			String nameSlug = cleanLink.substring(lastSlash + 1, lastUnderscore);
			String rawName = nameSlug.replace("-", " ");

			String[] words = rawName.split(" ");
			StringBuilder sb = new StringBuilder();
			for (String word : words) {
				if (!word.isEmpty()) {
					sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
				}
			}
			return sb.toString().trim();
		}

		return "N/A";
	}
}