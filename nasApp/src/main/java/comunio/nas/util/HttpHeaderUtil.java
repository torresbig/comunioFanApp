package comunio.nas.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HttpHeaderUtil {

	private static final List<String> USER_AGENTS = List.of("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36", "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Safari/605.1.15", "Mozilla/5.0 (X11; Linux x86_64) Gecko/20100101 Firefox/124.0", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0", "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.61 Mobile Safari/537.36");

	private static final List<String> ACCEPT_LANGUAGES = List.of("de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7", "en-US,en;q=0.9,de;q=0.8", "de,en;q=0.9,fr;q=0.8", "en-GB,en;q=0.8,de-DE;q=0.7");

	private static final List<String> ACCEPT_HEADERS = List.of("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8", "text/html,application/xml;q=0.9,*/*;q=0.8", "application/json,text/plain,*/*", "text/html;q=0.8,application/xhtml+xml;q=0.7,application/xml;q=0.6,*/*;q=0.5");

	private static final List<String> REFERERS = List.of("https://www.google.com", "https://www.bing.com", "https://duckduckgo.com/", "https://news.ycombinator.com/", "https://www.reddit.com/");

	private static final Random RANDOM = new Random();

	public static Map<String, String> getRandomHeaders() {
		Map<String, String> headers = new HashMap<>();

		headers.put("User-Agent", getRandom(USER_AGENTS));
		headers.put("Accept", getRandom(ACCEPT_HEADERS));
		headers.put("Accept-Language", getRandom(ACCEPT_LANGUAGES));
		headers.put("Referer", getRandom(REFERERS));
		headers.put("Connection", "close");

		// Optional: Etwas mehr "Noise" hinzufügen
		if (RANDOM.nextBoolean()) {
			headers.put("DNT", "1");
		}

		if (RANDOM.nextInt(4) == 0) {
			headers.put("Upgrade-Insecure-Requests", "1");
		}

		return headers;
	}

	private static <T> T getRandom(List<T> list) {
		return list.get(RANDOM.nextInt(list.size()));
	}

	public static String getRandomUserAgent() {
		return USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));
	}

	/**
	 * Liefert eine Map mit HTTP-Headern, die eine Browser-Anfrage simulieren.
	 *
	 * @return Map<String, String> mit Headern
	 */
	public static Map<String, String> getBrowserLikeHeaders() {
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json, text/plain, */*");
		headers.put("Accept-Language", "de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7");
		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
		headers.put("Referer", "https://www.transfermarkt.de/");
		headers.put("Connection", "keep-alive");
		return headers;
	}
}
