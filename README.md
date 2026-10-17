# ComunioFanApp

**ComunioFanApp** ist ein Java-basiertes Projekt zur Sammlung, Aktualisierung und Aufbereitung von Daten rund um Comunio und den Fußball. Die Anwendung führt Daten aus verschiedenen Quellen zusammen und verwaltet sie in JSON-Dateien, die unter anderem in einem GitHub-Repository gespeichert werden.

> **Projektstatus:** Das Repository enthält eine umfangreiche Java-Anwendung mit mehreren Datenimportern und Aktualisierungsprozessen. Die konkreten Laufzeitvoraussetzungen und Zugangsdaten müssen vor dem ersten Start lokal eingerichtet werden.

**ComunioWebsite**
In meinem anderen repository /comunioWebsite werden die Daten, die hier unter data liegen dargestellt und aufbereitet.

**wichtig**
- privates Projekt, um weiter und besser programmieren zu lernen
- es klappt alles nur perfekt, für comunities die neue Spieler und 20 Mio startgeld am Anfang der Saison bekommen. Sonst passen die Berechnungen nicht.
- die nasApp läuft bei mir 3 mal am Tag um z. B. Transfers neu zu holen. 


## Inhaltsverzeichnis

- [Funktionen](#funktionen)
- [Datenquellen](#datenquellen)
- [Technologien](#technologien)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Einrichtung](#einrichtung)
- [Starten](#starten)
- [Konfiguration und Sicherheit](#konfiguration-und-sicherheit)
- [Datenablage](#datenablage)
- [Fehlerbehebung](#fehlerbehebung)
- [Hinweise zu Datenquellen und Nutzungsrechten](#hinweise-zu-datenquellen-und-nutzungsrechten)

## Funktionen

Die im Repository vorhandenen Klassen decken unter anderem folgende Bereiche ab:

- **Comunio-Daten aktualisieren:** Spieler-, Vereins- und Benutzerdaten sowie Transferinformationen abrufen und verarbeiten.
- **Spieltag verwalten:** Spieltagsinformationen und zugehörige Statistiken erfassen.
- **Marktwerte und Transfers:** Marktwertdaten verwalten und Transfermarktinformationen aufbereiten.
- **Spieler- und Vereinszuordnung:** Daten unterschiedlicher Anbieter einander zuordnen, unter anderem über Mapping-Dateien für ESPN.
- **Verletzungen und Sperren:** entsprechende Informationen aus Fußball-Datenquellen auslesen.
- **News und Auswertungen:** Nachrichten analysieren sowie Top-/Flop- und weitere Statistikdaten verarbeiten.
- **JSON-Datenpflege:** vorhandene Daten laden, bereinigen, aktualisieren und speichern.
- **GitHub-Integration:** unterstützte Datendateien über die GitHub-API laden beziehungsweise aktualisieren.

Die tatsächliche Ausführung einzelner Funktionen hängt von der jeweiligen Konfiguration und der Verfügbarkeit der externen Quellen ab.

## Datenquellen

Im Code sind Integrationen beziehungsweise Parser für mehrere Quellen enthalten:

- [Comunio](https://www.comunio.de/)
- [Comstats](https://stats.comunio.de/)
- [LigaInsider](https://www.ligainsider.de/)
- [ESPN](https://www.espn.com/)
- [FotMob](https://www.fotmob.com/)
- [Transfermarkt](https://www.transfermarkt.de/)
- fussballdaten.de

Nicht jede Quelle liefert zwangsläufig alle Datentypen. Aufbau und Verfügbarkeit externer Webseiten und Schnittstellen können sich ändern.

## Technologien

- **Java** als Programmiersprache
- **Maven** zur Verwaltung des Builds und der Abhängigkeiten
- **JSON** zur Speicherung und Verarbeitung vieler Datensätze
- **HTTP/API-Aufrufe** für den Zugriff auf externe Dienste
- **GitHub REST API** für den Datenaustausch mit dem Repository
- **Jsoup** für Teile der Webdatenverarbeitung

Die genauen Abhängigkeiten und Compiler-Einstellungen sind in [`nasApp/pom.xml`](nasApp/pom.xml) definiert.

## Projektstruktur

```text
.
├── data/
│   ├── EspnClubMapping.json
│   ├── LastUpdatesList.json
│   ├── MarketValueArchive.json
│   ├── MatchdayDataList.json
│   ├── TransfermarktListe.json
│   └── ...
└── nasApp/
    ├── pom.xml
    ├── src/main/java/comunio/nas/
    │   ├── ComunioDataUpdater.java
    │   ├── dataScraper/
    │   │   ├── comunio/
    │   │   ├── espn/
    │   │   ├── fotmob/
    │   │   ├── ligainsider/
    │   │   └── ...
    │   ├── dataVariable/
    │   ├── enu/
    │   ├── git/
    │   ├── objects/
    │   └── util/
    └── .github/
```

### Wichtige Bereiche

| Pfad | Zweck |
|---|---|
| `nasApp/src/main/java/comunio/nas/ComunioDataUpdater.java` | Einstiegspunkt und Orchestrierung des Aktualisierungslaufs |
| `nasApp/src/main/java/comunio/nas/dataScraper/` | Importer und Parser für verschiedene Datenquellen |
| `nasApp/src/main/java/comunio/nas/objects/` | Datenobjekte und Container |
| `nasApp/src/main/java/comunio/nas/git/` | GitHub-Datenzugriff und Upload-Funktionen |
| `nasApp/src/main/java/comunio/nas/dataVariable/` | Konfiguration, URLs und Aktualisierungsinformationen |
| `data/` | JSON- und Textdateien mit gespeicherten Daten |

## Voraussetzungen

- Ein installiertes **JDK**, das mit den Einstellungen in `nasApp/pom.xml` kompatibel ist.
- **Apache Maven** oder eine IDE mit Maven-Unterstützung, beispielsweise Eclipse oder IntelliJ IDEA.
- Ein Comunio-Konto, falls die auszuführenden Funktionen eine Anmeldung benötigen.
- Ein GitHub-Token mit den für das Ziel-Repository erforderlichen Berechtigungen, wenn Daten hochgeladen werden sollen.
- Netzwerkzugriff auf die verwendeten externen Dienste.

## Einrichtung

1. Repository klonen:

   ```bash
   git clone https://github.com/torresbig/comunioFanApp.git
   cd comunioFanApp
   ```

2. Das Maven-Projekt im Verzeichnis `nasApp` öffnen oder in einer IDE als Maven-Projekt importieren.

3. Abhängigkeiten auflösen und das Projekt bauen:

   ```bash
   cd nasApp
   mvn clean package
   ```

4. Zugangsdaten sicher bereitstellen. Die Anwendung erwartet beim Start mindestens fünf Argumente. Die genaue Reihenfolge ist im Abschnitt [Starten](#starten) dokumentiert.

> **Hinweis:** Der Build-Befehl ist der übliche Maven-Ablauf. Ob der Build ohne weitere lokale Anpassungen erfolgreich ist, hängt unter anderem von der aktuellen `pom.xml` und den verfügbaren Abhängigkeiten ab.

## Starten

Der Einstiegspunkt ist:

```text
comunio.nas.ComunioDataUpdater
```

Der Konstruktor der Konfiguration erwartet die folgenden Kommandozeilenargumente in genau dieser Reihenfolge:

| Position | Argument | Beschreibung |
|---:|---|---|
| 1 | `username` | Comunio-Benutzername |
| 2 | `passwort` | Comunio-Passwort |
| 3 | `gitName` | GitHub-Benutzer beziehungsweise Repository-Eigentümer |
| 4 | `gitProjectName` | Name des GitHub-Repositories |
| 5 | `gitToken` | GitHub-Zugriffstoken |
| 6 (optional) | `DEBUG` | Aktiviert den Debug-Modus, wenn der Wert `DEBUG` lautet |

Beispiel für die Argumentreihenfolge – **nur Platzhalter, keine echten Zugangsdaten**:

```text
<username> <passwort> <gitName> <gitProjectName> <gitToken> DEBUG
```

In einer IDE können die Argumente in der Run-Konfiguration unter **Program arguments / Programmargumente** eingetragen werden. Für einen produktiven Betrieb sollten Passwörter und Tokens nicht dauerhaft im Klartext in einer geteilten Run-Konfiguration oder in Versionskontrolle gespeichert werden.

## Konfiguration und Sicherheit

Die Anwendung verwendet unter anderem Login-Daten für Comunio und einen GitHub-Token. Beachte deshalb:

- **Keine echten Passwörter oder Tokens committen.**
- Zugangsdaten nicht in Screenshots, Logs, Issues oder öffentlichen Beispielen veröffentlichen.
- Bei versehentlich veröffentlichten Zugangsdaten den betroffenen Token beziehungsweise das Passwort umgehend widerrufen oder ändern.
- Für den GitHub-Token nur die minimal erforderlichen Berechtigungen vergeben.
- Vor dem ersten Lauf prüfen, welche Dateien der Updater überschreibt oder auf GitHub hochlädt.
- Bei automatisierten Läufen auf eine sichere Ablage der Zugangsdaten und passende Dateiberechtigungen achten.

Die vorhandene Konfiguration über Kommandozeilenargumente ist funktional, kann aber Zugangsdaten in Prozesslisten oder IDE-Konfigurationen sichtbar machen. Für einen dauerhaften Betrieb empfiehlt sich eine sichere Secret-Verwaltung oder eine lokale, nicht versionierte Konfiguration.

## Datenablage

Im Verzeichnis `data/` liegen verschiedene JSON- und Textdateien, beispielsweise:

- `EspnClubMapping.json` – Zuordnung von ESPN-Vereinen zu Comunio-Vereinen
- `LastUpdatesList.json` – Zeitpunkte beziehungsweise Kennzeichen letzter Aktualisierungen
- `MatchdayDataList.json` – gespeicherte Spieltagsinformationen
- `TransfermarktListe.json` – gespeicherte Transfermarkt-/Marktdaten
- weitere Dateien mit Spieler-, Vereins-, Benutzer- und Zuordnungsdaten

Ein Teil der Daten ist communitybezogen. Prüfe deshalb vor einer öffentlichen Veröffentlichung, ob enthaltene Benutzerkennungen, Aufstellungen oder andere personenbezogene beziehungsweise zugangssensitive Informationen entfernt werden sollten.

## Fehlerbehebung

**Maven-Build schlägt fehl**
- Prüfe die installierte Java-Version mit `java -version` und `mvn -version`.
- Vergleiche die lokale Java-Version mit den Compiler-Einstellungen in `nasApp/pom.xml`.
- Führe den Build aus dem Verzeichnis `nasApp` aus.

**Anmeldung oder Datenabruf schlägt fehl**
- Prüfe die übergebenen Argumente und die Gültigkeit der Zugangsdaten.
- Stelle sicher, dass die externen Dienste erreichbar sind.
- Beachte, dass externe APIs und Webseiten geändert werden können.

**GitHub-Upload schlägt fehl**
- Prüfe Repository-Eigentümer und Repository-Namen.
- Kontrolliere, ob der Token gültig ist und die erforderlichen Rechte besitzt.
- Vergewissere dich, dass der Zielpfad im Repository existiert beziehungsweise über die verwendete API angelegt oder aktualisiert werden kann.

**Daten wirken veraltet oder unvollständig**
- Prüfe die Aktualisierungsinformationen in `data/LastUpdatesList.json`.
- Prüfe die Logs und die Verfügbarkeit der jeweils verwendeten Quelle.
- Beachte, dass einzelne Importer unabhängig voneinander fehlschlagen können.

## Hinweise zu Datenquellen und Nutzungsrechten

Dieses Projekt ist ein unabhängiges Community-/Privatprojekt. Es ist nicht als offizielles Produkt von Comunio, ESPN, LigaInsider, FotMob oder Transfermarkt ausgewiesen.

Bitte beachte die Nutzungsbedingungen, Urheberrechte, API-Vorgaben und gegebenenfalls geltende Einschränkungen der jeweiligen Anbieter. Die Nutzung und Weiterveröffentlichung abgerufener Daten liegt in der Verantwortung des Betreibers.

## Mitwirken

Verbesserungen, Fehlerberichte und Vorschläge können über GitHub-Issues oder Pull Requests eingebracht werden. Beschreibe bei Fehlerberichten möglichst:

- was du ausgeführt hast,
- welches Ergebnis du erwartet hast,
- was tatsächlich passiert ist,
- relevante Fehlermeldungen (ohne Passwörter oder Tokens),
- Java- und Maven-Version.

## Lizenz

Eine Lizenz ist in den hier ausgewerteten Projektdateien nicht eindeutig ersichtlich. Bis eine Lizenzdatei ergänzt wurde, sollte nicht davon ausgegangen werden, dass der Code frei kopiert, verändert oder weiterveröffentlicht werden darf.
