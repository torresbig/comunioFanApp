/**
 * Sammlung von Hilfsfunktionen
 */

/**
 * Debug-Ausgabefunktion, hier über die Konsole.
 * Du kannst sie anpassen, um Debug-Informationen z.B. in die Seite zu schreiben.
 * @param {string} message Text für Debug-Zwecke
 */
function addDebug(message) {
  const debugDiv = document.getElementById('floatingDebugContent');
  debugDiv.innerHTML += `<div>${new Date().toLocaleTimeString()}: ${message}</div>`;
  debugDiv.scrollTop = debugDiv.scrollHeight;
}

/**
 * Funktion zum Umschalten der Sichtbarkeit der News-Liste.
 * Wird z.B. vom Icon-Click-Event benutzt.
 */
function toggleNewsList() {
  const list = document.getElementById('news-list');
  const icon = document.getElementById('news-toggle-icon');
  if (!list || !icon) {
    addDebug('toggleNewsList: news-list oder news-toggle-icon nicht gefunden!');
    return;
  }
  if (list.style.display === 'none') {
    list.style.display = '';
    icon.style.transform = 'rotate(0deg)';
  } else {
    list.style.display = 'none';
    icon.style.transform = 'rotate(-90deg)';
  }
}

function getLokalOderGitURL(websiteUrl, lokalUrl) {
  const currentUrl = window.location.href;
  if (currentUrl.includes('https://${GITHUB_USER}.github.io/')) {
    return websiteUrl;
  } else {
    return lokalUrl;
  }
}

function getPlayerUrl(playerId) {
  const currentUrl = window.location.href;
  if (currentUrl.includes('htmlpreview.github.io')) {
    return WEBSITE_URLS.playerUrl + `${playerId}`;
  } else if (currentUrl.includes('github.com') || currentUrl.includes('githubusercontent.com')) {
    return WEBSITE_URLS.playerUrl + `${playerId}`;
  } else {
    return `player.html?id=${playerId}`;
  }
}


function getLogoFileName(clubId) {
  if (!clubId) return "unbestimmt.png";
  const mapping = {
    "3": "gladbach",
    "21": "freiburg",
    "18": "mainz",
    "92": "leipzig",
    "1": "bayern",
    "6": "werderBremen",
    "12": "wolfsburg",
    "5": "dortmund",
    "8": "leverkusen",
    "13": "koeln",
    "14": "stuttgart",
    "62": "hoffenheim",
    "68": "augsburg",
    "9": "frankfurt",
    "109": "unionBerlin",
    "110": "heidenheim",
    "25": "stpauli",
    "4": "hamburg",
  };
  return (mapping[clubId] || "unbestimmt") + ".png";
}


function getStatusIndicator(status) {
  switch (status) {
    case 'AKTIV': return '👍';
    case 'VERLETZT': return '🚨';
    case 'REHA': return '🔄';
    case 'AUFBAUTRAINING': return '🏋️💪';
    case 'NICHT_IN_LIGA': return '❌';
    case 'FUENFTE_GELBE_KARTE': return '🟨';
    case 'GELBROTE_KARTE': return '🟨🟥';
    case 'ROTE_KARTE': return '🟥';
    case 'NICHT_IM_KADER': return '🚫';
    default: return '❓';
  }
}

function getStatusDisplayName(status) {
  const statusMap = {
    'AKTIV': 'Aktiv',
    'VERLETZT': 'Verletzt',
    'REHA': 'Reha',
    'AUFBAUTRAINING': 'Aufbautraining',
    'NICHT_IM_KADER': 'Nicht im Kader',
    'ROTE_KARTE': 'Rote Karte',
    'GELBROTE_KARTE': 'Gelbrote Karte',
    'FUENFTE_GELBE_KARTE': '5. gelbe Karte',
    'NICHT_IN_LIGA': 'Nicht in Liga'
  };
  return statusMap[status] || status || 'Unbekannt';
}



async function fetchJSON(url) {
    addDebug(`Lade Datei: ${url}`);
    const cacheBusterUrl = url + '?t=' + new Date().getTime();
    try {
        const response = await fetch(cacheBusterUrl);
        if (!response.ok) {
            addDebug(`Fehler beim Laden: HTTP ${response.status}`);
            throw new Error(`HTTP ${response.status} für ${url}`);
        }
        const text = await response.text();
        if (!text.trim()) {
            addDebug("Warnung: Datei ist leer!");
            throw new Error("Leere Datei: " + url);
        }
        addDebug("Datei geladen, versuche JSON zu parsen...");
        const data = JSON.parse(text);
        addDebug(`Erfolgreich geparst: ${data.length || Object.keys(data).length} Einträge`);
        return data;
    } catch (error) {
        addDebug("Fehler beim Laden/JSON-Parsen: " + error.message);
        throw error;
    }
}