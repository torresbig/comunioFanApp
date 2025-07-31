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


