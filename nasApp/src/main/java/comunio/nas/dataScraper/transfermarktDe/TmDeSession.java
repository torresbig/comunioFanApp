package comunio.nas.dataScraper.transfermarktDe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import comunio.nas.objects.helper.LogManager;
import comunio.nas.util.HttpHeaderUtil;

/**
 * Zentrale Session-Verwaltung für alle Anfragen an transfermarkt.de.
 * <p>
 * <b>Warum gibt es diese Klasse?</b>
 * <p>
 * Transfermarkt.de schützt sich mit dem Bot-Schutz-System <b>DataDome</b>.
 * Wenn eine Anfrage "verdächtig" wirkt, liefert der Server nicht einfach nur
 * einen Fehler, sondern eine <b>HTTP 405 ("Method Not Allowed")</b> – obwohl
 * die URL und die HTTP-Methode (GET) eigentlich völlig korrekt sind.
 * <p>
 * Verdächtig sind für DataDome unter anderem:
 * <ul>
 *   <li><b>Keine Cookies/Session:</b> Jeder Aufruf erzeugt bisher eine neue,
 *       cookie-lose Verbindung. Der Server kann den Client dann nicht als
 *       "menschlichen" Besucher wiedererkennen.</li>
 *   <li><b>Wechselnde Header:</b> {@code HttpHeaderUtil.getRandomHeaders()}
 *       liefert bei jedem Aufruf einen anderen User-Agent, ein anderes Accept
 *       usw. Ein echter Browser bleibt innerhalb einer Sitzung konsistent.</li>
 *   <li><b>Besondere TLS-/HTTP-Fingerabdrücke:</b> Java/Jsoup sieht für den
 *       Server anders aus als ein echter Browser. Das lässt sich über reine
 *       Header nur teilweise kaschieren (siehe "Einschränkung" unten).</li>
 * </ul>
 * <p>
 * <b>Was macht diese Klasse?</b>
 * <ul>
 *   <li>Sie verwaltet einen zentralen <b>Cookie-Jar</b> (eine Map), der über
 *       alle Anfragen hinweg erhalten bleibt. Cookies, die der Server setzt
 *       (z. B. das DataDome-Cookie {@code datadome}), werden automatisch
 *       gespeichert und bei allen weiteren Anfragen mitgesendet.</li>
 *   <li>Beim ersten Zugriff lädt sie einmalig die Transfermarkt-Startseite
 *       ("Warm-up"), damit der Server überhaupt sein Cookie setzt.</li>
 *   <li>Sie verwendet KONSISTENTE Browser-Header (siehe
 *       {@link HttpHeaderUtil#getTransfermarktHeaders()}).</li>
 *   <li>Trifft sie auf eine Bot-Sperre (403/405/406/429), wird der Cookie-Jar
 *       verworfen, neu aufgebaut und die Anfrage einmal wiederholt.</li>
 *   <li>Sie zählt aufeinanderfolgende Bot-Sperren ({@link #isBotBlocked()}),
 *       damit die aufrufenden Klassen ihre Schleifen abbrechen können
 *       (kein endloses Durchlaufen wie bisher).</li>
 * </ul>
 * <p>
 * <b>Einschränkung – und der Plan B:</b> Diese Lösung (Cookies + konsistente
 * Header + Warm-up) behebt einen Teil der Ursachen. Sollte Transfermarkt
 * trotzdem weiter 405 liefern, liegt das am TLS-/HTTP-Fingerabdruck des
 * Java-HTTP-Stacks, den man über Header nicht ändern kann. Dann wäre der
 * nächste Schritt ein browserähnlicher HTTP-Client (z. B. Playwright oder
 * Jsoup mit eigenem {@code HttpClient}).
 */
public final class TmDeSession {

	private static final Logger LOGGER = LogManager.getLogger(TmDeSession.class);

	/** Startseite, die einmalig beim Session-Aufbau geladen wird. */
	private static final String STARTSEITE = "https://www.transfermarkt.de/";

	/**
	 * So viele aufeinanderfolgende Bot-Sperren sind "tolerierbar", danach wird
	 * {@link #isBotBlocked()} {@code true} und die Aufrufer können abbrechen.
	 */
	private static final int MAX_CONSECUTIVE_BOT_BLOCKS = 3;

	/** Wie lange wir vor einem Session-Reset pausieren (ms). */
	private static final long RESET_PAUSE_MS = 5000L;

	/** Timeout pro Anfrage (ms). */
	private static final int TIMEOUT_MS = 30000;

	/**
	 * Der zentrale Cookie-Jar: speichert alle Cookies, die der Server gesetzt
	 * hat (z. B. {@code datadome}). Wird bei jeder Anfrage mitgesendet.
	 */
	private static Map<String, String> cookieJar = new HashMap<>();

	/** Zähler für aufeinanderfolgende Bot-Sperren (403/405/406/429). */
	private static int consecutiveBotBlocks = 0;

	/** Privater Konstruktor: Nur statische Nutzung. */
	private TmDeSession() {
		// keine Instanzen erzeugbar
	}

	// =========================================================================
	// Öffentliche API für die Transfermarkt-Scrapper
	// =========================================================================

	/**
	 * Lädt die gewünschte Transfermarkt-URL über die zentrale Session (mit
	 * Cookies, konsistenten Headern und eingebautem Retry bei Bot-Sperren).
	 * <p>
	 * Ablauf im Detail:
	 * <ol>
	 *   <li>Falls der Cookie-Jar noch leer ist, wird die Startseite warm
	 *       geladen, damit der Server seine Cookies setzt.</li>
	 *   <li>Die URL wird als GET abgerufen (mit allen gespeicherten Cookies).</li>
	 *   <li>Ist die Antwort eine Bot-Sperre (403/405/406/429), wird der
	 *       Cookie-Jar zurückgesetzt (neue Cookies anfordern) und nach einer
	 *       Pause ein zweiter Versuch unternommen.</li>
	 *   <li>Schlägt auch der zweite Versuch mit einer Sperre fehl, wird der
	 *       Bot-Block-Zähler erhöht und die {@link HttpStatusException}
	 *       weitergeworfen – der Aufrufer kann über
	 *       {@link #isBotBlocked()} entscheiden, ob er die Schleife abbricht.</li>
	 * </ol>
	 *
	 * @param url Die abzurufende Transfermarkt-URL
	 * @return Das geparste {@link Document} der Seite
	 * @throws HttpStatusException wenn die Seite einen HTTP-Fehler liefert
	 *                             (auch nach Session-Reset)
	 * @throws IOException         bei Netzwerkfehlern
	 * @throws InterruptedException wenn der Thread während eines Wartens
	 *                              unterbrochen wurde
	 */
	public static synchronized Document getDocument(String url) throws HttpStatusException, IOException, InterruptedException {
		ensureSessionWarmup();

		try {
			Document doc = request(url);
			// Erfolgreiche Anfrage -> Zähler der Bot-Sperren zurücksetzen
			consecutiveBotBlocks = 0;
			return doc;
		} catch (HttpStatusException e) {
			// Nur bei "Bot-Statuscodes" greift das Reset/Retry-Konzept,
			// bei allen anderen HTTP-Fehlern (404, 500, ...) sofort durchreichen.
			if (!isBotStatusCode(e.getStatusCode())) {
				throw e;
			}

			consecutiveBotBlocks++;
			LOGGER.log(Level.WARNING,
					"Bot-Schutz-Antwort (HTTP " + e.getStatusCode() + ") bei URL " + url
					+ " – Bot-Block #" + consecutiveBotBlocks);

			if (isBotBlocked()) {
				// Zu viele Fehlschläge hintereinander: Aufrufer entscheidet über Abbruch.
				LOGGER.log(Level.SEVERE,
						"Transfermarkt blockiert weiterhin (HTTP-Status " + e.getStatusCode()
						+ "). Es werden keine weiteren Versuche unternommen. Zu prüfen: "
						+ "Session/Warm-up erfolgreich? IP evtl. gesperrt?");
				throw e;
			}

			// Kurze Pause, dann Cookie-Jar komplett neu aufbauen (neue Cookies) und
			// einen zweiten Versuch starten.
			Thread.sleep(RESET_PAUSE_MS);
			resetSession();
			LOGGER.log(Level.INFO, "Session nach Bot-Sperre neu aufgebaut. Erneuter Versuch für: " + url);

			Document doc = request(url);
			consecutiveBotBlocks = 0;
			return doc;
		}
	}

	/**
	 * Wirft den aktuellen Cookie-Jar samt Cookies weg und baut eine neue
	 * Session auf (inklusive neuem Warm-up der Startseite).
	 * <p>
	 * Sinn: Nach einer Bot-Sperre bekommt man evtl. mit denselben Cookies
	 * nie wieder Zugriff – eine frische Session (ohne "vergiftete" Cookies)
	 * startet quasi neu.
	 */
	public static synchronized void resetSession() {
		cookieJar = new HashMap<>();
		ensureSessionWarmup();
	}

	/**
	 * @return {@code true}, wenn die letzten Anfragen mehrfach hintereinander
	 *         mit einem Bot-Status (403/405/406/429) beantwortet wurden.
	 *         Aufrufer (z. B. {@code updateAllPlayerWithLink}) können in diesem
	 *         Fall sinnvoll ihre Schleife abbrechen, anstatt endlos weiterzulaufen.
	 */
	public static synchronized boolean isBotBlocked() {
		return consecutiveBotBlocks >= MAX_CONSECUTIVE_BOT_BLOCKS;
	}

	/**
	 * Setzt den Block-Zähler wieder auf 0 – z. B. nach einer erfolgreichen
	 * Verarbeitungsphase, damit eine neue "Bot-Chance" starten kann.
	 */
	public static synchronized void resetBotBlockCounter() {
		consecutiveBotBlocks = 0;
	}

	/**
	 * Liefert den aktuellen Wert des aufeinanderfolgenden Sperr-Zählers
	 * (nur für Diagnose/Log-Zwecke).
	 */
	public static synchronized int getConsecutiveBotBlocks() {
		return consecutiveBotBlocks;
	}

	// =========================================================================
	// Interne Hilfsmethoden
	// =========================================================================

	/**
	 * Lädt beim allerersten Zugriff die Startseite, damit der Server seine
	 * Cookies setzt.
	 * <p>
	 * <b>Warum die Startseite?</b> DataDome vergibt in der Regel erst nach
	 * einem normalen Seitenabruf das Cookie {@code datadome}. Ohne dieses
	 * Cookie gilt jede Anfrage als "unbekannt" und wird leichter geblockt.
	 */
	private static void ensureSessionWarmup() {
		if (cookieJar.isEmpty()) {
			warmUp();
		}
	}

	/**
	 * Lädt die Startseite über eine frische Verbindung und speichert die vom
	 * Server gesetzten Cookies im Cookie-Jar.
	 * <p>
	 * Hinweis: Damit wir auch bei einer evtl. Sperre der Startseite noch den
	 * Status auslesen können, setzen wir {@code ignoreHttpErrors(true)} –
	 * sonst würde {@code execute()} bei 403/405 bereits eine Exception werfen,
	 * bevor wir den Status ablesen können.
	 */
	private static void warmUp() {
		try {
			Connection.Response response = Jsoup.connect(STARTSEITE)
					.headers(HttpHeaderUtil.getTransfermarktHeaders())
					.ignoreContentType(true)
					.ignoreHttpErrors(true)
					.timeout(TIMEOUT_MS)
					.execute();

			// Set-Cookie-Header der Antwort in den Cookie-Jar übernehmen
			Map<String, String> cookies = response.cookies();
			if (cookies != null && !cookies.isEmpty()) {
				cookieJar.putAll(cookies);
			}
			LOGGER.info("Transfermarkt-Session aufgebaut (Status " + response.statusCode() + "). "
					+ "Cookie(s) vom Server: " + (cookieJar.isEmpty() ? "keine" : cookieJar.keySet()));

			if (response.statusCode() == 405 || response.statusCode() == 403) {
				LOGGER.warning("Warm-up bereits geblockt (HTTP " + response.statusCode() + "). "
						+ "Startseite verweigert Cookies – Session funktioniert vermutlich nicht.");
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Warm-up (Startseite) fehlgeschlagen: " + e.getMessage(), e);
		}
	}

	/**
	 * Führt den eigentlichen GET-Request aus und gibt das geparste Dokument
	 * zurück. Alle im Cookie-Jar gespeicherten Cookies werden mitgesendet;
	 * neue Set-Cookie-Header der Antwort werden in den Cookie-Jar übernommen.
	 * <p>
	 * Hier bewusst OHNE {@code ignoreHttpErrors}: Wir möchten die
	 * {@link HttpStatusException} ausgelöst bekommen, um Bot-Sperren (405 etc.)
	 * erkennen und behandeln zu können.
	 *
	 * @param url Die abzurufende URL
	 * @return geparstes Document
	 * @throws IOException         bei Netzwerkfehlern
	 * @throws HttpStatusException bei HTTP-Fehlern (z. B. 405)
	 */
	private static Document request(String url) throws IOException, HttpStatusException {
		Connection.Response response = Jsoup.connect(url)
				.headers(HttpHeaderUtil.getTransfermarktHeaders())
				.cookies(cookieJar)
				.ignoreContentType(true)
				.timeout(TIMEOUT_MS)
				.execute();

		// Neue Cookies aus der Antwort übernehmen (z. B. erneuertes datadome-Cookie)
		Map<String, String> newCookies = response.cookies();
		if (newCookies != null && !newCookies.isEmpty()) {
			cookieJar.putAll(newCookies);
		}

		return response.parse();
	}

	/**
	 * Entscheidet, ob ein HTTP-Status zu den Bot-Schutz-Blocks gehört.
	 * Transfermarkt/DataDome liefert typischerweise 403, 405 oder 406, wenn es
	 * einen Client blockt. Zusätzlich wird 429 (Too Many Requests) gewertet.
	 *
	 * @param statusCode HTTP-Status
	 * @return true, wenn es sich um eine Bot-Klassifizierung handelt
	 */
	private static boolean isBotStatusCode(int statusCode) {
		return statusCode == 403 || statusCode == 405 || statusCode == 406 || statusCode == 429;
	}
}
