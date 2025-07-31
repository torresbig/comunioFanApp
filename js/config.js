/**
 * Konfigurationsdatei mit Nutzer- und Repo-Daten sowie URL-Definitionen
 */

const GITHUB_USER = "torresbig";
const GITHUB_REPO = "comunioFanApp";

const DATA_URLS = {
  clubs: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/VereinsdatenbankJson.txt`,
  players: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/SpielerdatenbankNeutralJson.txt`,
  users: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/UserdatenbankJson_884691.txt`,
  playerToUser: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/PlayerToUserMap_884691.txt`,
  news: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/News.json`
};


