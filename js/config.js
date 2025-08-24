/**
 * Konfigurationsdatei mit Nutzer- und Repo-Daten sowie URL-Definitionen
 */

const GITHUB_USER = "torresbig";
const GITHUB_REPO = "comunioFanApp";

const DATA_URLS = {
  clubs: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/VereinsdatenbankJson.txt`,
  players: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/SpielerdatenbankNeutralJson.txt`,
  users: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/UserdatenbankJson_884691.txt`,
  playerToUser: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/PlayerToUserMap_884691.txt`,
  marktwerte: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/MarktwerteJson.txt`,
  news: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/News.json`,
  logos: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/logos/`
  
};

// Für den Produktivbetrieb auf GitHub Pages die URLs so definieren:
const WEBSITE_URLS = {
  playerUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/player.html?id=`,
  indexUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/index.html`,
  kontostaendeUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/kontostaende.html`,
  transfermarktUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/transfermarkt.html`,
  useruebersichtUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/useruebersicht.html`,
  transfersUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/transfers.html`,
  newsUrl: `https://${GITHUB_USER}.github.io/${GITHUB_REPO}/news.html`
};

// Optional: Du kannst eine Umgebungs-Variable oder eine einfache Prüfung ergänzen,
// um je nach Umgebung (lokal/htmlpreview vs. GitHub-Pages) die URLs automatisch zu wechseln.

// Beispiel:
const isHtmlPreview = window.location.href.includes('htmlpreview.github.io');

const ACTIVE_WEBSITE_URLS = isHtmlPreview
  ? {
      playerUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/player.html?id=`,
      indexUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/index.html`,
      kontostaendeUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/kontostaende.html`,
      transfermarktUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/transfermarkt.html`,
      useruebersichtUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/useruebersicht.html`,
      transfersUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/transfers.html`,
      newsUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/news.html`
    }
  : WEBSITE_URLS;