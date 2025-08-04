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
  news: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/News.json`
  
};

const WEBSITE_URLS = {
  playerUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/player.html?id=`,
  indexUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/index.html`,
  kontostaendeUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/kontostaende.html`,
  transfermarktUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/transfermarkt.html`,
  useruebersichtUrl: `https://htmlpreview.github.io/?https://github.com/${GITHUB_USER}/${GITHUB_REPO}/main/useruebersicht.html`
};
