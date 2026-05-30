package comunio.nas.util.transferData;

import comunio.nas.dataScraper.comunio.Login;
import comunio.nas.dataVariable.UserLoginData;
import comunio.nas.enu.NewsArt;
import comunio.nas.objects.community.Community;
import comunio.nas.objects.orga.ComunioDate;
import comunio.nas.objects.user.User;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NewsExtractor PRO
 *
 * - Lädt eine lokale News.json (Datei) oder eine News.json von GitHub (RAW-URL)
 * - Extrahiert alle News-Einträge, insbesondere TRANSFER-News - Filtert nach: -
 * NewsArt (Enum) - playerId - Person (Seller oder Buyer) - Zeigt Treffer in
 * einer Tabelle - Exportiert die Tabelle als kompakte CSV-ähnliche Textdatei -
 * Lädt zusätzlich die Transfers direkt von Comunio (API, mit Bearer-Token) -
 * Vergleicht lokale Transfers mit Comunio-Transfers - Zeigt
 * fehlende/abweichende Transfers in einem Popup
 *
 * Java 18 kompatibel, bewusst ausführlich dokumentiert.
 */
public class NewsExtractor extends JFrame {

	// -----------------------------
	// Datenquellen / JSON
	// -----------------------------

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Lokale JSON-Datei (News.json) */
	private File jsonFile;

	/** Wurzelobjekt der geladenen JSON (News.json im alten Format mit "newsDB") */
	private JSONObject jsonRoot;

	// -----------------------------
	// GUI-Elemente
	// -----------------------------

	/** Auswahl der News-Art (Enum aus deinem Projekt) */
	private JComboBox<NewsArt> cbArt;

	/** Auswahl einer Person (Name, egal ob Seller oder Buyer) */
	private JComboBox<String> cbPerson;

	/** Filterfeld für playerId (String-Vergleich) */
	private JTextField tfPlayerId;

	/** Textfeld für GitHub RAW-URL (News.json) */
	private JTextField tfGithubUrl;

	/** Textfeld für das Datum, ab dem Comunio-News geladen werden sollen */
	private JTextField tfDatum;

	/** Tabelle zur Anzeige der gefilterten News/Transfers */
	private JTable table;

	/** TableModel für die Tabelle */
	private DefaultTableModel tableModel;

	// -----------------------------
	// In-Memory-Datenstrukturen
	// -----------------------------

	/**
	 * Alle News-Einträge aus der lokalen JSON (altes Format mit "newsDB"). Wird für
	 * die Filterung in der Tabelle verwendet.
	 */
	private final List<JSONObject> allEntries = new ArrayList<>();

	/**
	 * Alle lokalen Transfers (aus der alten News.json extrahiert), in ein
	 * einheitliches TransferEntry-Format gemappt. Wird für den Abgleich mit den
	 * Comunio-Transfers verwendet.
	 */
	private final List<TransferEntry> localTransfers = new ArrayList<>();

	public static Community community = new Community();
	public static User user = new User();
	public static UserLoginData uld;

	// -----------------------------
	// Einstiegspunkt
	// -----------------------------

	public static void main(String[] args) {
		uld = new UserLoginData(args);

		SwingUtilities.invokeLater(NewsExtractor::new);
	}

	// -----------------------------
	// Konstruktor / GUI-Aufbau
	// -----------------------------

	public NewsExtractor() {
		setTitle("News JSON Extractor PRO");
		setSize(1000, 650);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		add(buildTopPanel(), BorderLayout.NORTH);
		add(buildTablePanel(), BorderLayout.CENTER);
		add(buildButtonPanel(), BorderLayout.SOUTH);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Oberes Panel: - GitHub-URL - Buttons: "Von GitHub laden", "Datei auswählen" -
	 * Filter: Art, playerId, Person - Datum für Comunio-News-Abgleich
	 */
	private JPanel buildTopPanel() {
		JPanel p = new JPanel(new GridLayout(4, 4, 5, 5));

		// Buttons für Datenquelle
		JButton btnChoose = new JButton("Datei auswählen");
		btnChoose.addActionListener(this::chooseFile);

		JButton btnLoadGithub = new JButton("Von GitHub laden");
		btnLoadGithub.addActionListener(this::loadFromGithub);

		// Filter-Controls
		cbArt = new JComboBox<>(NewsArt.values());
		cbPerson = new JComboBox<>();

		tfPlayerId = new JTextField("");
		tfGithubUrl = new JTextField("https://raw.githubusercontent.com/torresbig/comunioFanApp/refs/heads/main/data/News.json");

		tfDatum = new JTextField("2025-05-28"); // Datum für Comunio-Abgleich (yyyy-MM-dd)
		tfDatum.setToolTipText("Format: yyyy-MM-dd, z.B. 2025-05-28");

		// Zeile 1: GitHub-URL + Buttons
		p.add(new JLabel("GitHub RAW URL:"));
		p.add(tfGithubUrl);
		p.add(btnLoadGithub);
		p.add(btnChoose);

		// Zeile 2: Art + playerId
		p.add(new JLabel("Art:"));
		p.add(cbArt);
		p.add(new JLabel("playerId:"));
		p.add(tfPlayerId);

		// Zeile 3: Person (Seller/Buyer)
		p.add(new JLabel("Person (Seller oder Buyer):"));
		p.add(cbPerson);
		p.add(new JLabel()); // Platzhalter
		p.add(new JLabel()); // Platzhalter

		// Zeile 4: Datum für Comunio-News-Abgleich
		p.add(new JLabel("News ab Datum (yyyy-MM-dd):"));
		p.add(tfDatum);
		p.add(new JLabel()); // Platzhalter
		p.add(new JLabel()); // Platzhalter

		return p;
	}

	/**
	 * Mittleres Panel: Tabelle zur Anzeige der Treffer.
	 */
	private JScrollPane buildTablePanel() {
		tableModel = new DefaultTableModel(new String[] { "Datum", "Id", "Art", "Spieler", "Preis", "Seller", "Buyer" }, 0);

		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);

		return new JScrollPane(table);
	}

	/**
	 * Unteres Panel: - Filtern - Filter löschen - News abgleichen (lokal vs.
	 * Comunio)
	 */
	private JPanel buildButtonPanel() {
		JPanel p = new JPanel();

		JButton btnFilter = new JButton("Filtern");
		btnFilter.addActionListener(this::filterData);

		JButton btnClear = new JButton("Filter löschen");
		btnClear.addActionListener(e -> clearFilters());

		JButton btnCompare = new JButton("News abgleichen");
		btnCompare.addActionListener(this::onNewsAbgleichenClicked);

		p.add(btnFilter);
		p.add(btnClear);
		p.add(btnCompare);

		return p;
	}

	// -----------------------------
	// Datei / GitHub laden
	// -----------------------------

	/**
	 * Öffnet einen JFileChooser und lädt eine lokale JSON-Datei.
	 */
	private void chooseFile(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			jsonFile = chooser.getSelectedFile();
			loadJsonFromFile();
		}
	}

	/**
	 * Liest die JSON aus der ausgewählten Datei und extrahiert die Einträge.
	 */
	private void loadJsonFromFile() {
		try {
			String content = Files.readString(jsonFile.toPath());
			jsonRoot = new JSONObject(content);
			extractEntries();
			JOptionPane.showMessageDialog(this, "Datei geladen.");
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Fehler beim Laden der Datei: " + ex.getMessage());
		}
	}

	/**
	 * Lädt die JSON von einer GitHub-RAW-URL und extrahiert die Einträge.
	 */
	private void loadFromGithub(ActionEvent e) {
		try {
			URL url = new URL(tfGithubUrl.getText());
			String content = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
			jsonRoot = new JSONObject(content);
			extractEntries();
			JOptionPane.showMessageDialog(this, "GitHub-Datei geladen.");
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Fehler beim Laden von GitHub: " + ex.getMessage());
		}
	}

	/**
	 * Liest alle News-Einträge aus der lokalen JSON-Struktur (altes Format mit
	 * "newsDB") und befüllt:
	 *
	 * - allEntries: alle News-Objekte (für Filter/Tabelle) - localTransfers: nur
	 * TRANSFER-News, gemappt auf TransferEntry - cbPerson: alle beteiligten
	 * Personen (Seller/Buyer) als Auswahl
	 *
	 * WICHTIG: - Die ID liegt in der lokalen JSON als String vor ("id":
	 * "958487222"). Wir parsen sie explizit mit Long.parseLong(...), damit newsId
	 * exakt dem Typ der Remote-API (long) entspricht.
	 */
	private void extractEntries() {

		// Vorherige Daten zurücksetzen
		allEntries.clear();
		localTransfers.clear();
		cbPerson.removeAllItems();

		Set<String> persons = new HashSet<>();

		if (jsonRoot == null || !jsonRoot.has("newsDB")) {
			JOptionPane.showMessageDialog(this, "JSON enthält kein 'newsDB'-Array.");
			return;
		}

		JSONArray newsDB = jsonRoot.getJSONArray("newsDB");

		for (int i = 0; i < newsDB.length(); i++) {

			JSONArray news = newsDB.getJSONObject(i).getJSONArray("news");

			for (int j = 0; j < news.length(); j++) {

				JSONObject entry = news.getJSONObject(j);

				// Für generische Filter/Tabelle merken wir uns alle Einträge
				allEntries.add(entry);

				// Nur TRANSFER-News sind für den Transfer-Abgleich relevant
				if (!"TRANSFER".equals(entry.optString("art"))) {
					continue;
				}

				JSONObject t = new JSONObject(entry.getString("text"));

				TransferEntry te = new TransferEntry();

				// *** ID: lokal als String, explizit nach long parsen ***
				String idStr = entry.optString("id", "0");
				try {
					te.newsId = Long.parseLong(idStr);
				} catch (NumberFormatException nfe) {
					// Fallback: 0, damit man solche Fälle im Vergleich leicht erkennt
					te.newsId = 0L;
				}

				te.date = entry.optString("date");
				te.playerName = t.optString("playerName");
				te.price = t.optInt("price");
				te.seller = t.optString("seller");
				te.buyer = t.optString("buyer");

				localTransfers.add(te);
				System.out.println("[LOCAL] ID=" + te.newsId + " | Spieler=" + te.playerName + " | Preis=" + te.price + " | Seller=" + te.seller + " | Buyer=" + te.buyer);

				if (!te.seller.isBlank()) {
					persons.add(te.seller);
				}
				if (!te.buyer.isBlank()) {
					persons.add(te.buyer);
				}
			}
		}

		persons.forEach(cbPerson::addItem);
	}

	// -----------------------------
	// Filter / Tabelle
	// -----------------------------

	/**
	 * Wendet die Filter (Art, playerId, Person) auf allEntries an und füllt die
	 * Tabelle mit den passenden TRANSFER-Einträgen.
	 */
	private void filterData(ActionEvent e) {
		tableModel.setRowCount(0);

		NewsArt selectedArt = (NewsArt) cbArt.getSelectedItem();
		String playerId = tfPlayerId.getText().trim();
		String person = (String) cbPerson.getSelectedItem();

		if (selectedArt == null) {
			JOptionPane.showMessageDialog(this, "Bitte eine Art auswählen.");
			return;
		}

		for (JSONObject entry : allEntries) {

			// Art prüfen
			if (!entry.optString("art").equals(selectedArt.name())) {
				continue;
			}

			// playerId prüfen (optional)
			if (!playerId.isBlank() && !entry.optString("playerId").equals(playerId)) {
				continue;
			}

			// Nur TRANSFER-Einträge werden in der Tabelle angezeigt
			if (selectedArt == NewsArt.TRANSFER) {
				JSONObject t = new JSONObject(entry.getString("text"));

				// Person (Seller/Buyer) prüfen (optional)
				if (person != null && !person.isBlank()) {
					boolean isSeller = t.optString("seller").equals(person);
					boolean isBuyer = t.optString("buyer").equals(person);

					if (!isSeller && !isBuyer) {
						continue; // Person ist an diesem Transfer nicht beteiligt
					}
				}

				tableModel.addRow(new Object[] { entry.optString("date"), entry.optString("id"), entry.optString("art"), t.optString("playerName"), t.optString("price"), t.optString("seller"), t.optString("buyer") });
			}
		}
	}

	/**
	 * Setzt die Filter zurück und leert die Tabelle.
	 */
	private void clearFilters() {
		tfPlayerId.setText("");
		cbPerson.setSelectedIndex(-1);
		tableModel.setRowCount(0);
	}

	/**
	 * Lädt die Transfers direkt von Comunio über die News-API.
	 *
	 * - Verwendet Login.getToken() für den Bearer-Token - Lädt seitenweise
	 * (start/limit) - Bricht ab, wenn ein Datum erreicht wird, das vor fromDate
	 * liegt - Extrahiert nur Einträge vom Typ "TRANSACTION_TRANSFER" - Mapped
	 * FROM_COMPUTER und TO_COMPUTER in TransferEntry-Objekte
	 *
	 * WICHTIG: - Hier wird mapTransfer(entry, dateKey, t) aufgerufen, damit die
	 * News-ID aus dem News-Objekt (entry) korrekt in TransferEntry.newsId landet.
	 */
	public List<TransferEntry> loadComunioTransfers(ComunioDate fromDate) {

		List<TransferEntry> result = new ArrayList<>();

		Login.login(uld.getUsername(), uld.getPasswortAlsString(), community, user);

		int page = 0;
		int limit = 20;

		while (true) {

			String url = "https://www.comunio.de/api/communities/" + community.getId() + "/users/" + user.getId() + "/news" + "?group=true&originaltypes=true&start=" + page + "&limit=" + limit;

			JSONObject newsRoot;
			try {
				Connection connection = Jsoup.connect(url).header("Accept", "application/json, text/plain, */*").header("Authorization", "Bearer " + Login.getToken()).ignoreContentType(true);

				Document doc = connection.get();
				newsRoot = new JSONObject(doc.body().text());

			} catch (Exception e) {
				System.out.println("Fehler beim Laden der Comunio-News: " + e.getMessage());
				break;
			}

			if (!newsRoot.has("newsList")) {
				break;
			}

			JSONObject groups = newsRoot.getJSONObject("newsList").optJSONObject("groups");
			if (groups == null || groups.isEmpty()) {
				break;
			}

			for (String dateKey : groups.keySet()) {

				// dateKey ist z.B. "2026-05-10"
				ComunioDate newsDate = new ComunioDate(dateKey);

				if (newsDate.before(fromDate)) {
					continue; // nur diese Gruppe überspringen
				}

				JSONArray entries = groups.getJSONObject(dateKey).getJSONArray("entries");

				for (int i = 0; i < entries.length(); i++) {

					JSONObject entry = entries.getJSONObject(i);

					if (!entry.optString("type").equals("TRANSACTION_TRANSFER")) {
						continue;
					}

					JSONObject msg = entry.getJSONObject("message");

					// FROM_COMPUTER
					if (msg.has("FROM_COMPUTER")) {
						JSONArray arr = msg.getJSONArray("FROM_COMPUTER");
						for (int j = 0; j < arr.length(); j++) {
							JSONObject t = arr.getJSONObject(j);
							result.add(mapTransfer(entry, dateKey, msg, t));
						}
					}

					// TO_COMPUTER
					if (msg.has("TO_COMPUTER")) {
						JSONArray arr = msg.getJSONArray("TO_COMPUTER");
						for (int j = 0; j < arr.length(); j++) {
							JSONObject t = arr.getJSONObject(j);
							result.add(mapTransfer(entry, dateKey, msg, t));
						}
					}

					if (msg.has("BETWEEN_USERS")) {
						JSONArray arr = msg.getJSONArray("BETWEEN_USERS");
						for (int j = 0; j < arr.length(); j++) {
							JSONObject t = arr.getJSONObject(j);
							result.add(mapTransfer(entry, dateKey, msg, t)); 
						}
					}
				}
			}

			page += limit;
		}

		return result;
	}

	/**
	 * Mapped einen Transfer aus der neuen Comunio-API korrekt.
	 *
	 * @param entry        kompletter News-Eintrag (enthält owner, id, date)
	 * @param dateKey      Datum der Gruppe
	 * @param msg          das komplette message-Objekt (enthält price)
	 * @param t            einzelner Transfer aus FROM_COMPUTER / TO_COMPUTER
	 * @param isToComputer true = Computer kauft, false = Computer verkauft
	 */
	private TransferEntry mapTransfer(JSONObject entry, String dateKey, JSONObject msg, JSONObject t) {

		TransferEntry e = new TransferEntry();

		// News-ID (eine pro Tag/Block)
		e.newsId = entry.optLong("id");

		// Datum
		e.date = dateKey;

		// Spielername
		e.playerName = t.getJSONObject("tradable").getString("name");

		// Preis: in neuer API im message-Objekt
		int price = t.optInt("price", 0);
		if (price == 0) {
			System.out.println("WARN: Kein Preis im message-Objekt gefunden, versuche Fallback für Transfer: " + e.playerName);
		}
		e.price = price;

		e.seller = t.getJSONObject("from").getString("name");
		e.buyer = t.getJSONObject("to").getString("name");

		return e;
	}

	/**
	 * Vergleicht zwei Transferlisten (lokal vs. remote) und liefert alle Transfers,
	 * die in der Remote-Liste NICHT gefunden wurden.
	 *
	 * WICHTIG: - NICHT mehr über newsId vergleichen, weil: - alte News.json und
	 * aktuelle Comunio-API unterschiedliche IDs verwenden - derselbe Transfer kann
	 * also zwei verschiedene IDs haben - Stattdessen wird über inhaltliche Felder
	 * verglichen: - playerName - price - seller - buyer - Das Datum wird bewusst
	 * NICHT verglichen, weil: - lokal: "31.01.2026" - remote: "2026-01-31" →
	 * unterschiedliche Formate, aber derselbe Transfer
	 *
	 * @param local  Liste der lokalen Transfers (aus alter News.json)
	 * @param remote Liste der Transfers aus der Comunio-API
	 * @return Liste der Transfers, die lokal existieren, aber remote nicht gefunden
	 *         wurden
	 */
	public List<TransferEntry> findMissingTransfers(List<TransferEntry> local, List<TransferEntry> remote) {

		List<TransferEntry> missing = new ArrayList<>();

		for (TransferEntry l : local) {

			boolean found = remote.stream().anyMatch(r -> safeEquals(l.playerName, r.playerName) && l.price == r.price && safeEquals(l.seller, r.seller) && safeEquals(l.buyer, r.buyer));

			if (!found) {
				missing.add(l);
			}
		}

		return missing;
	}

	/**
	 * Wendet den aktuell gewählten Filter (Art + Person) auf eine Liste von
	 * Transfers an.
	 *
	 * - Art: Es werden nur Transfers berücksichtigt, wenn die ausgewählte Art
	 * TRANSFER ist. - Person: Wenn im ComboBox eine Person ausgewählt ist, werden
	 * nur Transfers zurückgegeben, bei denen diese Person entweder Seller oder
	 * Buyer ist.
	 *
	 * Diese Methode wird sowohl für lokale Transfers als auch für Remote-Transfers
	 * verwendet, damit der Vergleich nur auf dem gefilterten Ausschnitt erfolgt.
	 *
	 * @param list Ursprüngliche Liste von TransferEntry (lokal oder remote)
	 * @return Gefilterte Liste von TransferEntry
	 */
	private List<TransferEntry> filterByUserAndArt(List<TransferEntry> list) {

		String selectedPerson = (String) cbPerson.getSelectedItem();
		NewsArt selectedArt = (NewsArt) cbArt.getSelectedItem();

		List<TransferEntry> result = new ArrayList<>();

		// Wenn keine Art gewählt ist, geben wir eine leere Liste zurück
		if (selectedArt == null) {
			return result;
		}

		// Wir interessieren uns hier nur für TRANSFER-News
		if (selectedArt != NewsArt.TRANSFER) {
			return result;
		}

		for (TransferEntry t : list) {

			// Wenn eine Person ausgewählt ist, muss sie entweder Seller oder Buyer sein
			if (selectedPerson != null && !selectedPerson.isBlank()) {
				boolean isSeller = selectedPerson.equals(t.seller);
				boolean isBuyer = selectedPerson.equals(t.buyer);

				if (!isSeller && !isBuyer) {
					continue; // Person ist an diesem Transfer nicht beteiligt
				}
			}

			result.add(t);
		}

		return result;
	}

	/**
	 * Wird aufgerufen, wenn der Button "News abgleichen" geklickt wird.
	 *
	 * Ablauf: 1. Datum aus tfDatum einlesen (Startdatum für Comunio-News) 2.
	 * Comunio-Transfers ab diesem Datum laden (API) 3. Sowohl lokale als auch
	 * remote Transfers mit dem gleichen Filter (Art + Person) einschränken 4.
	 * Vollständigen Vergleich durchführen (fehlend / zuViel / abweichend) 5.
	 * Ergebnis in einem Popup anzeigen
	 */
	private void onNewsAbgleichenClicked(ActionEvent e) {

		String dateText = tfDatum.getText().trim();
		if (dateText.isBlank()) {
			JOptionPane.showMessageDialog(this, "Bitte ein Datum für den Abgleich eingeben (yyyy-MM-dd).");
			return;
		}

		ComunioDate fromDate;
		try {
			fromDate = new ComunioDate(dateText);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Ungültiges Datum: " + ex.getMessage());
			return;
		}

		if (localTransfers.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Es wurden noch keine lokalen Transfers geladen (News.json).");
			return;
		}

		// 3. Remote-Transfers laden
		List<TransferEntry> remote = loadComunioTransfers(fromDate);
		System.out.println("DEBUG: loadComunioTransfers -> " + remote.size() + " Einträge");

		// 4. Filter anwenden
		List<TransferEntry> filteredLocal = filterByUserAndArt(localTransfers);
		List<TransferEntry> filteredRemote = filterByUserAndArt(remote);

		System.out.println("DEBUG: filteredLocal  size = " + filteredLocal.size());
		System.out.println("DEBUG: filteredRemote size = " + filteredRemote.size());

		// 5. Vollständigen Vergleich durchführen
		ComparisonResult res = compareTransfersFull(filteredLocal, filteredRemote);

		// 6. Ergebnis anzeigen
		showComparisonResult(res);
	}

	/**
	 * Null-sichere String-Gleichheit.
	 *
	 * @param a erster String (kann null sein)
	 * @param b zweiter String (kann null sein)
	 * @return true, wenn beide null oder inhaltlich gleich sind
	 */
	private boolean safeEquals(String a, String b) {
		if (a == null)
			return b == null;
		return a.equals(b);
	}

	/**
	 * Einfache Transfer-DTO-Klasse, die sowohl lokale Transfers (alte News.json)
	 * als auch Comunio-Transfers (API) in ein einheitliches Format bringt.
	 *
	 * Vergleich erfolgt später primär über die newsId (eindeutige News-ID).
	 */
	public static class TransferEntry {

		/** Eindeutige Comunio-News-ID (lokal: Feld "id", remote: Feld "id") */
		public long newsId;

		/** Datum der News / des Transfers (Format hängt von Quelle ab) */
		public String date;

		/** Name des Spielers, der transferiert wurde */
		public String playerName;

		/** Transferpreis in Euro (Integer, wie von Comunio geliefert) */
		public int price;

		/** Name des Verkäufers (z.B. "Computer", "Thomas", "Christian") */
		public String seller;

		/** Name des Käufers (z.B. "Computer", "Thomas", "Christian") */
		public String buyer;

		@Override
		public String toString() {
			return newsId + " | " + date + " | " + playerName + " | " + price + " | " + seller + " -> " + buyer;
		}
	}

	/**
	 * Wrapper für einen Transfer mit einer Begründung/Kommentar (z.B. warum er
	 * fehlt).
	 */
	public static class AnnotatedTransfer {
		public final TransferEntry entry;
		public final String reason;

		public AnnotatedTransfer(TransferEntry entry, String reason) {
			this.entry = entry;
			this.reason = reason;
		}

		@Override
		public String toString() {
			return reason + " | " + entry.toString();
		}
	}

	/**
	 * Ergebniscontainer für den vollständigen Transfervergleich.
	 *
	 * - fehlend : Transfers, die lokal existieren, aber remote nicht gefunden
	 * wurden - zuViel : Transfers, die remote existieren, aber lokal nicht gefunden
	 * wurden - abweichend: Transfers, die in beiden Quellen existieren, deren
	 * Felder aber (z.B. Preis, Seller, Buyer, Spielername) voneinander abweichen
	 */
	public static class ComparisonResult {

		/** Lokal vorhanden, remote nicht gefunden (oder remote mit Abweichungen) */
		public final List<AnnotatedTransfer> fehlend = new ArrayList<>();

		/** Remote vorhanden, lokal nicht gefunden (oder lokal mit Abweichungen) */
		public final List<AnnotatedTransfer> zuViel = new ArrayList<>();

		/** In beiden vorhanden, aber mit unterschiedlichen Werten */
		public final List<Mismatch> abweichend = new ArrayList<>();
	}

	/**
	 * Repräsentiert einen abweichenden Transfer: - local : Version aus der lokalen
	 * JSON - remote : Version aus der Comunio-API
	 */
	public static class Mismatch {

		public final TransferEntry local;
		public final TransferEntry remote;

		public Mismatch(TransferEntry local, TransferEntry remote) {
			this.local = local;
			this.remote = remote;
		}

		@Override
		public String toString() {
			return "LOCAL : " + local.toString() + System.lineSeparator() + "REMOTE: " + remote.toString();
		}
	}

	/**
	 * Vergleicht lokale und remote Transfers vollständig.
	 *
	 * WICHTIG: - Die lokale JSON hat pro ID mehrere Transfers. - Die Comunio-API
	 * hat pro Transfer eine eigene ID. - IDs sind NICHT vergleichbar → wir
	 * ignorieren sie komplett.
	 *
	 * Matching erfolgt ausschließlich über: - playerName - price - seller - buyer
	 */
	public ComparisonResult compareTransfersFull(List<TransferEntry> local, List<TransferEntry> remote) {

		ComparisonResult result = new ComparisonResult();

		System.out.println("=== DEBUG compareTransfersFull ===");
		System.out.println("Local size  = " + local.size());
		System.out.println("Remote size = " + remote.size());

		// --- 1) Lokal → Remote: fehlend + abweichend ---
		for (TransferEntry l : local) {

			TransferEntry r = remote.stream().filter(x -> safeEquals(l.playerName, x.playerName) && l.price == x.price && safeEquals(l.seller, x.seller) && safeEquals(l.buyer, x.buyer)).findFirst().orElse(null);

			if (r == null) {
				// Versuche, anhand des Spielernamens einen Kandidaten zu finden, um einen
				// genauen Grund zu liefern
				TransferEntry rByName = remote.stream().filter(x -> safeEquals(l.playerName, x.playerName)).findFirst().orElse(null);

				if (rByName == null) {
					System.out.println("[COMPARE] FEHLT REMOTE: " + l);
					result.fehlend.add(new AnnotatedTransfer(l, "Nicht in remote vorhanden"));
				} else {
					List<String> diffs = new ArrayList<>();
					if (l.price != rByName.price)
						diffs.add("Preis: local=" + l.price + ", remote=" + rByName.price);
					if (!safeEquals(l.seller, rByName.seller))
						diffs.add("Seller: local='" + l.seller + "', remote='" + rByName.seller + "'");
					if (!safeEquals(l.buyer, rByName.buyer))
						diffs.add("Buyer: local='" + l.buyer + "', remote='" + rByName.buyer + "'");

					String reason = diffs.isEmpty() ? "Remote-Eintrag unklar abweichend" : "Remote mit Abweichungen: " + String.join("; ", diffs);
					System.out.println("[COMPARE] FEHLT (Abweichung) REMOTE: " + l + " -> " + reason);
					result.fehlend.add(new AnnotatedTransfer(l, reason));
				}

				continue;
			}

			System.out.println("[COMPARE] MATCH: " + l.playerName);

			// Felder vergleichen (theoretisch redundant, da wir oben nach exaktem Match
			// suchen)
			boolean differs = !safeEquals(l.playerName, r.playerName) || l.price != r.price || !safeEquals(l.seller, r.seller) || !safeEquals(l.buyer, r.buyer);

			if (differs) {
				result.abweichend.add(new Mismatch(l, r));
			}
		}

		// --- 2) Remote → Lokal: zu viele Transfers ---
		for (TransferEntry r : remote) {

			TransferEntry l = local.stream().filter(x -> safeEquals(r.playerName, x.playerName) && r.price == x.price && safeEquals(r.seller, x.seller) && safeEquals(r.buyer, x.buyer)).findFirst().orElse(null);

			if (l == null) {
				// Versuche, anhand des Spielernamens einen Kandidaten zu finden, um einen Grund
				// zu liefern
				TransferEntry lByName = local.stream().filter(x -> safeEquals(r.playerName, x.playerName)).findFirst().orElse(null);

				if (lByName == null) {
					System.out.println("[COMPARE] ZU VIEL REMOTE: " + r);
					result.zuViel.add(new AnnotatedTransfer(r, "Nicht in lokal vorhanden"));
				} else {
					List<String> diffs = new ArrayList<>();
					if (r.price != lByName.price)
						diffs.add("Preis: remote=" + r.price + ", local=" + lByName.price);
					if (!safeEquals(r.seller, lByName.seller))
						diffs.add("Seller: remote='" + r.seller + "', local='" + lByName.seller + "'");
					if (!safeEquals(r.buyer, lByName.buyer))
						diffs.add("Buyer: remote='" + r.buyer + "', local='" + lByName.buyer + "'");

					String reason = diffs.isEmpty() ? "Lokal-Eintrag unklar abweichend" : "Lokal mit Abweichungen: " + String.join("; ", diffs);
					System.out.println("[COMPARE] ZU VIEL (Abweichung) REMOTE: " + r + " -> " + reason);
					result.zuViel.add(new AnnotatedTransfer(r, reason));
				}
			}
		}

		return result;
	}

	/**
	 * Zeigt das Ergebnis des vollständigen Vergleichs in einem modalen Dialog an.
	 * Der Dialog enthält eine Tabelle mit den Ergebnissen und einen
	 * "Exportieren"-Button.
	 *
	 * @param res Ergebnis des Vergleichs (compareTransfersFull)
	 */
	private void showComparisonResult(ComparisonResult res) {
		// Tabelle mit Ergebnissen aufbauen
		DefaultTableModel model = new DefaultTableModel(new String[] { "Kategorie", "Datum", "Id", "Spieler", "Preis", "Seller", "Buyer", "Grund" }, 0);

		// FEHLEND
		if (!res.fehlend.isEmpty()) {
			for (AnnotatedTransfer at : res.fehlend) {
				TransferEntry t = at.entry;
				model.addRow(new Object[] { "FEHLEND", t.date, t.newsId, t.playerName, t.price, t.seller, t.buyer, at.reason });
			}
		}

		// ZU VIEL
		if (!res.zuViel.isEmpty()) {
			for (AnnotatedTransfer at : res.zuViel) {
				TransferEntry t = at.entry;
				model.addRow(new Object[] { "ZU VIEL", t.date, t.newsId, t.playerName, t.price, t.seller, t.buyer, at.reason });
			}
		}

		// ABWEICHEND
		if (!res.abweichend.isEmpty()) {
			for (Mismatch m : res.abweichend) {
				// Erzeuge eine kompakte Begründung mit Local vs Remote Differenzen
				List<String> diffs = new ArrayList<>();
				if (m.local.price != m.remote.price)
					diffs.add("Preis: local=" + m.local.price + ", remote=" + m.remote.price);
				if (!safeEquals(m.local.seller, m.remote.seller))
					diffs.add("Seller: local='" + m.local.seller + "', remote='" + m.remote.seller + "'");
				if (!safeEquals(m.local.buyer, m.remote.buyer))
					diffs.add("Buyer: local='" + m.local.buyer + "', remote='" + m.remote.buyer + "'");

				String reason = diffs.isEmpty() ? "Abweichung (Details unbekannt)" : String.join("; ", diffs);

				model.addRow(new Object[] { "ABWEICHEND", m.local.date, m.local.newsId, m.local.playerName, m.local.price, m.local.seller, m.local.buyer, reason });
			}
		}

		JTable resultTable = new JTable(model);
		resultTable.setFillsViewportHeight(true);

		JScrollPane scroll = new JScrollPane(resultTable);
		scroll.setPreferredSize(new Dimension(1000, 500));

		// Dialog mit Tabelle und Exportieren-Button erstellen
		JDialog dialog = new JDialog(this, "Transfer-Abgleich", true);
		dialog.setLayout(new BorderLayout());

		dialog.add(scroll, BorderLayout.CENTER);

		// Panel mit Export-Button
		JPanel buttonPanel = new JPanel();
		JButton btnExport = new JButton("Exportieren");
		btnExport.addActionListener(e -> exportComparisonResult(model));
		buttonPanel.add(btnExport);

		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setSize(1050, 600);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	/**
	 * Exportiert die Vergleichsergebnisse aus dem ComparisonResult-Dialog in eine
	 * Datei "comparison_export.txt".
	 *
	 * @param model das TableModel mit allen Vergleichsergebnissen
	 */
	private void exportComparisonResult(DefaultTableModel model) {
		try {
			File out = new File("comparison_export.txt");
			List<String> lines = new ArrayList<>();

			// Header
			StringBuilder headerLine = new StringBuilder();
			for (int i = 0; i < model.getColumnCount(); i++) {
				if (i > 0)
					headerLine.append(",");
				headerLine.append(model.getColumnName(i));
			}
			lines.add(headerLine.toString());

			// Datenzeilen
			for (int row = 0; row < model.getRowCount(); row++) {
				StringBuilder line = new StringBuilder();
				for (int col = 0; col < model.getColumnCount(); col++) {
					if (col > 0)
						line.append(",");
					Object val = model.getValueAt(row, col);
					String strVal = val != null ? val.toString() : "";
					// Escape Kommas und Anführungszeichen
					if (strVal.contains(",") || strVal.contains("\"")) {
						strVal = "\"" + strVal.replace("\"", "\"\"") + "\"";
					}
					line.append(strVal);
				}
				lines.add(line.toString());
			}

			Files.write(out.toPath(), lines);
			JOptionPane.showMessageDialog(this, "Export abgeschlossen: " + out.getAbsolutePath());

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Fehler beim Export: " + ex.getMessage());
		}
	}

}
