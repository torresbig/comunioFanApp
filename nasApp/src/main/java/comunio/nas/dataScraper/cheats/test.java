package comunio.nas.dataScraper.cheats;

//Java 17+, jsoup 1.17+
//Zweck: Freitags 19:30 CEST alle erwarteten Aufstellungen aggregieren und P(Start), P(Sub), P(DNP) schätzen.
//Nutzung nur privat, bitte Nutzungsbedingungen der Seiten respektieren.

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.*;
import java.util.regex.*;

public class test {

 public static class PlayerProb {
     public String player;
     public String team;
     public boolean predictedStartBL;   // bundesliga.com
     public boolean predictedStartBuli; // Bulinews
     public boolean predictedStartRW;   // RotoWire
     public boolean doubtful;
     public boolean out;
     public boolean keeper;
     public double pStart, pSub, pDnp;
 }

 // Quellen
 private static final String BL_MATCHDAY_PROBABLE = "https://www.bundesliga.com/en/bundesliga/news/matchday-probable-teams-confirmed-2024-25-bayern-dortmund-leverkusen-fantasy-20707"; // Beispiel-Link für aktuellen Spieltag [web:17]
 private static final String BULINEWS_PREDICTED    = "https://bulinews.com/predicted-lineups"; // Matchday-Hub mit erwarteten XIs [web:11]
 private static final String ROTOWIRE_LINEUPS      = "https://www.rotowire.com/soccer/lineups.php?league=BUND"; // Live/Predicted Lineups [web:1]

 public static void main(String[] args) throws Exception {
     Map<String, PlayerProb> index = new LinkedHashMap<>();

     // 1) bundesliga.com probable teams
     fetchBundesligaProbable(index);

     // 2) Bulinews predicted lineups
     fetchBulinewsPredicted(index);

     // 3) RotoWire predicted lineups + injuries
     fetchRotoWire(index);

     // 4) Scoring Wahrscheinlichkeiten
     scoreProbabilities(index);

     // 5) Ausgabe (CSV)
     System.out.println("player,team,pStart,pSub,pDNP,predBL,predBuli,predRW,doubtful,out");
     for (PlayerProb p : index.values()) {
         System.out.printf(Locale.US,
             "%s,%s,%.3f,%.3f,%.3f,%b,%b,%b,%b,%b%n",
             sanitize(p.player), sanitize(p.team),
             p.pStart, p.pSub, p.pDnp,
             p.predictedStartBL, p.predictedStartBuli, p.predictedStartRW, p.doubtful, p.out
         );
     }
 }

 private static void fetchBundesligaProbable(Map<String, PlayerProb> idx) throws Exception {
     Document doc = Jsoup.connect(BL_MATCHDAY_PROBABLE)
             .userAgent("Mozilla/5.0").get(); // [web:17]

     // Heuristik: Abschnitte je Team enthalten Startelf‑Auflistungen mit "Out"/"Doubtful".
     // Selektoren können variieren; folgende Strategie:
     // - Teamblöcke sind oft als headings + lists vorhanden.
     Elements sections = doc.select("h2, h3, strong, b, p, li");

     String currentTeam = null;
     boolean inLineup = false;

     for (Element el : sections) {
         String t = el.text().trim();

         // Teamname erkennen (einfache Heuristik)
         if (t.matches("(?i).*(Bayern|Dortmund|Leverkusen|Leipzig|Stuttgart|Bremen|Mainz|Augsburg|Gladbach|Frankfurt|Freiburg|Hoffenheim|Heidenheim|Union|Köln|Hamburg|Wolfsburg|St\\. Pauli).*")) {
             currentTeam = t.replaceAll(":.*", "").trim();
             inLineup = true;
             continue;
         }
         if (currentTeam == null) continue;

         // Out / Doubtful
         if (t.matches("(?i).*Out:.*") || t.matches("(?i).*Doubtful:.*")) {
             markStatusFromLine(idx, currentTeam, t);
             continue;
         }

         // Startelf-Kandidaten (durch Bindestriche/Kommas getrennt)
         if (inLineup && (t.contains("–") || t.contains("-") || t.contains(" - "))) {
             // Beispielzeile: "Neuer – Laimer, Upamecano, Tah, Guerreiro – Kimmich, Pavlović – Olise, Gnabry, Díaz – Kane"
             List<String> names = splitNamesLine(t);
             for (String name : names) {
                 addPredicted(idx, name, currentTeam, "BL");
             }
         }
     }
 }

 private static void fetchBulinewsPredicted(Map<String, PlayerProb> idx) throws Exception {
     Document doc = Jsoup.connect(BULINEWS_PREDICTED)
             .userAgent("Mozilla/5.0").get(); // [web:11]

     // Bulinews enthält Links zu „Predicted lineups“ je Matchday oder Gesamtlisten.
     // Grobe Heuristik: Sammle Spielerlisten aus Artikeln/blocks mit XI-Aufzählungen.
     Elements items = doc.select("article, .article, .content, .post, li, p");

     String currentTeam = null;
     for (Element el : items) {
         String text = el.text().trim();
         if (text.matches("(?i).*(Bayern|Dortmund|Leverkusen|Leipzig|Stuttgart|Bremen|Mainz|Augsburg|Gladbach|Frankfurt|Freiburg|Hoffenheim|Heidenheim|Union|Köln|Hamburg|Wolfsburg|St\\. Pauli).*")) {
             currentTeam = text.replaceAll(":.*", "").trim();
         }
         if (currentTeam != null && (text.contains("XI") || text.contains("lineup") || text.contains("line-up") || text.contains("Predicted"))) {
             // Spieler nach Kommas/Bindestrichen herausziehen
             List<String> names = splitNamesLine(text);
             for (String name : names) {
                 addPredicted(idx, name, currentTeam, "BULI");
             }
         }
     }
 }

 private static void fetchRotoWire(Map<String, PlayerProb> idx) throws Exception {
     Document doc = Jsoup.connect(ROTOWIRE_LINEUPS)
             .userAgent("Mozilla/5.0").get(); // [web:1]

     // Struktur: Mehrere Spielkarten mit "Predicted Lineup" und "Injuries"
     Elements gameBlocks = doc.select("div.lineup__matchup, div.LineupMatchup, div.Pagecontainer");
     if (gameBlocks.isEmpty()) {
         // Fallback: Suche generisch nach „Predicted Lineup“ und darunter Spielernamen
         gameBlocks = doc.select("*:matchesOwn((?i)Predicted Lineup)");
     }

     // Verletzungsstatus-Blöcke
     Set<String> outNames = new HashSet<>();
     Set<String> doubtfulNames = new HashSet<>();

     Elements injuryBlocks = doc.select("*:matchesOwn((?i)Injuries|OUT|Questionable|QUES|GTD)");
     for (Element b : injuryBlocks) {
         String text = b.parent() != null ? b.parent().text() : b.text();
         extractInjuryFlags(text, outNames, doubtfulNames);
     }

     // Predicted XI-Blöcke
     Elements predictedLabels = doc.select("*:matchesOwn((?i)Predicted Lineup)");
     for (Element label : predictedLabels) {
         Element container = label.parent();
         if (container == null) continue;
         String teamName = findTeamNameUpwards(container);
         List<String> players = findPlayersBelow(container);
         for (String p : players) {
             addPredicted(idx, p, teamName, "RW");
         }
     }

     // Flags setzen
     for (String n : outNames) setStatus(idx, n, true, false);
     for (String n : doubtfulNames) setStatus(idx, n, false, true);
 }

 // ===== Utilities =====

 private static void addPredicted(Map<String, PlayerProb> idx, String rawName, String team, String source) {
     String name = normalizeName(rawName);
     if (name.isEmpty() || team == null) return;
     String key = teamKey(team) + "|" + name;
     PlayerProb p = idx.computeIfAbsent(key, k -> {
         PlayerProb np = new PlayerProb();
         np.player = name;
         np.team = teamKey(team);
         np.keeper = isKeeper(name);
         return np;
     });
     switch (source) {
         case "BL":   p.predictedStartBL = true; break;
         case "BULI": p.predictedStartBuli = true; break;
         case "RW":   p.predictedStartRW = true; break;
     }
 }

 private static void markStatusFromLine(Map<String, PlayerProb> idx, String team, String line) {
     // Parse "Out: A, B, C" / "Doubtful: X, Y"
     String t = line.replace("\u00a0", " ");
     Matcher mOut = Pattern.compile("(?i)Out:\\s*(.*)").matcher(t);
     if (mOut.find()) {
         for (String n : mOut.group(1).split(",")) {
             String name = normalizeName(n);
             if (!name.isEmpty()) {
                 setStatus1(idx, teamKey(team) + "|" + name, true, false);
             }
         }
     }
     Matcher mD = Pattern.compile("(?i)Doubtful:\\s*(.*)").matcher(t);
     if (mD.find()) {
         for (String n : mD.group(1).split(",")) {
             String name = normalizeName(n);
             if (!name.isEmpty()) {
                 setStatus1(idx, teamKey(team) + "|" + name, false, true);
             }
         }
     }
 }

 private static void setStatus1(Map<String, PlayerProb> idx, String compositeKey, boolean out, boolean doubtful) {
     PlayerProb p = idx.get(compositeKey);
     if (p == null) {
         // Wenn noch nicht erfasst, minimal anlegen (Team fehlt evtl.)
         p = new PlayerProb();
         String[] parts = compositeKey.split("\\|", 2);
         p.team = parts.length > 1 ? parts[0] : "";
         p.player = parts.length > 1 ? parts[1] : compositeKey;
         idx.put(compositeKey, p);
     }
     p.out |= out;
     p.doubtful |= doubtful;
 }

 private static void setStatus(Map<String, PlayerProb> idx, String playerNameOnly, boolean out, boolean doubtful) {
     // ohne Team: fuzzy setze Flag für alle Spieler mit diesem Namen
     for (PlayerProb p : idx.values()) {
         if (p.player.equalsIgnoreCase(playerNameOnly)) {
             p.out |= out;
             p.doubtful |= doubtful;
         }
     }
 }

 private static void extractInjuryFlags(String text, Set<String> outNames, Set<String> doubtfulNames) {
     String t = text.replace("\u00a0", " ");
     // Primitive Extraktion: „OUT“ und „QUES/GTD“ Zeilen enthalten Namen.
     if (t.matches("(?i).*OUT.*")) {
         for (String n : t.split(",")) {
             if (n.toUpperCase().contains("OUT")) continue;
             String name = normalizeName(n);
             if (name.length() > 2) outNames.add(name);
         }
     }
     if (t.matches("(?i).*(QUES|Questionable|GTD).*")) {
         for (String n : t.split(",")) {
             if (n.toUpperCase().contains("QUES") || n.toUpperCase().contains("GTD") || n.toUpperCase().contains("QUESTIONABLE")) continue;
             String name = normalizeName(n);
             if (name.length() > 2) doubtfulNames.add(name);
         }
     }
 }

 private static String findTeamNameUpwards(Element el) {
     // Suche in Elternknoten nach ersichtlichem Teamnamen
     for (Element e = el; e != null; e = e.parent()) {
         String t = e.text();
         if (t.matches("(?i).*(Bayern|Dortmund|Leverkusen|Leipzig|Stuttgart|Bremen|Mainz|Augsburg|Gladbach|Frankfurt|Freiburg|Hoffenheim|Heidenheim|Union|Köln|Hamburg|Wolfsburg|St\\. Pauli).*")) {
             return t.replaceAll(":.*", "").trim();
         }
     }
     return "";
 }

 private static List<String> findPlayersBelow(Element el) {
     List<String> players = new ArrayList<>();
     Elements next = el.parent() != null ? el.parent().select("li, .player, .Lineup__player, .lineup__player, p") : new Elements();
     for (Element n : next) {
         String tx = n.text();
         if (looksLikePlayerList(tx)) {
             players.addAll(splitNamesLine(tx));
         }
     }
     // Fallback: direkt unter Label nach Geschwistern suchen
     Elements sib = el.siblingElements();
     for (Element s : sib) {
         String tx = s.text();
         if (looksLikePlayerList(tx)) {
             players.addAll(splitNamesLine(tx));
         }
     }
     return players;
 }

 private static boolean looksLikePlayerList(String t) {
     return t.split(",").length >= 3 || t.contains(" – ") || t.contains(" - ");
 }

 private static List<String> splitNamesLine(String t) {
     String s = t.replace("–", "-");
     // Entferne Rolle/Coach/Hinweise
     s = s.replaceAll("(?i)Coach:.*", "");
     s = s.replaceAll("(?i)Out:.*", "");
     s = s.replaceAll("(?i)Doubtful:.*", "");
     // Split an Kommas und Bindestrichen
     List<String> out = new ArrayList<>();
     for (String part : s.split("[,\\-]")) {
         String name = normalizeName(part);
         if (name.length() >= 3 && !name.matches("(?i)coach|c\\)|\\(c\\)|gtd|ques|out|injuries|predicted|lineup|line-up|xi")) {
             out.add(name);
         }
     }
     return out;
 }

 private static String normalizeName(String raw) {
     String n = raw.trim()
             .replaceAll("\\(c\\)", "")
             .replaceAll("\\(GTD\\)", "")
             .replaceAll("\\(QUES\\)", "")
             .replaceAll("\\(Q\\)", "")
             .replaceAll("\\s+", " ")
             .replaceAll("[^\\p{L} .\\-']", "");
     return n.trim();
 }

 private static String teamKey(String t) {
     // Vereinfache Teamnamen
     return t.replaceAll("\\(.*?\\)", "").trim();
 }

 private static boolean isKeeper(String name) {
     // Minimalheuristik für GK
     return false; // Optional: Map bekannter GKs pflegen
 }

 private static void scoreProbabilities(Map<String, PlayerProb> idx) {
     for (PlayerProb p : idx.values()) {
         // Basis-Scores
         int votes = (p.predictedStartBL ? 1 : 0) + (p.predictedStartBuli ? 1 : 0) + (p.predictedStartRW ? 1 : 0);

         double baseStart;
         if (p.out) {
             baseStart = 0.0;
         } else if (votes >= 2) {
             baseStart = 0.80;
         } else if (votes == 1) {
             baseStart = 0.55;
         } else {
             baseStart = 0.15; // nicht als Starter gelistet
         }

         if (p.doubtful) baseStart *= 0.6; // Zweifel senken Start‑Wkt.

         // Einwechslung: nur wenn nicht sicher out; Keeper sehr niedrig
         double baseSub;
         if (p.out) {
             baseSub = 0.0;
         } else if (votes == 0) {
             baseSub = p.keeper ? 0.03 : 0.40;
         } else {
             baseSub = p.keeper ? 0.02 : 0.20;
         }

         // Normierung und DNP
         double pStart = clamp(baseStart, 0, 0.98);
         double pSub = clamp(baseSub, 0, 0.98 - pStart);
         double pDnp = clamp(1.0 - pStart - pSub, 0, 1);

         p.pStart = pStart;
         p.pSub = pSub;
         p.pDnp = pDnp;
     }
 }

 private static double clamp(double v, double lo, double hi) {
     return Math.max(lo, Math.min(hi, v));
 }

 private static String sanitize(String s) {
     return s == null ? "" : s.replace(",", " ").replace("\n", " ").trim();
 }
}
