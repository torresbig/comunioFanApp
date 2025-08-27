/**
 * Haupt-Event-Listener, der beim Laden der Seite initial alle benötigten Daten lädt.
 * Zuerst Besitzerdaten (globalOwnersMap), danach Transfermarkt-Daten.
 */
document.addEventListener('DOMContentLoaded', async () => {
    try {
        addDebug("Seite geladen, lade Besitzerdaten...");
        await loadOwnersData();  // Besitzer laden, füllt globalOwnersMap


        addDebug("Besitzerdaten geladen, lade Transfermarkt-Daten...");
        await loadTransferMarktData();  // Spieler-Daten laden und rendern
    } catch (error) {
        showError("Fehler beim Laden: " + error.message);
        addDebug("Fehler beim Laden: " + error.message);
    }
});

/**
 * Lädt die Transfermarktliste von der Remote-URL als JSON
 * Zeigt Ladeanzeige, lädt die Daten, rendert dann die Tabelle.
 */
async function loadTransferMarktData() {
    try {
        showLoading();
        addDebug("Lade Transfermarkt-Liste...");

        const response = await fetch('https://raw.githubusercontent.com/' + GITHUB_USER + '/' + GITHUB_REPO + '/main/data/TransfermarktListe.json');
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();
        addDebug(`${data.length} Einträge geladen`);

        renderTable(data);

        hideLoading();
        showContent();
    } catch (error) {
        hideLoading();
        throw error;
    }
}

let ownersMap = new Map();

/**
 * Erstellt die HTML-Tabelle mit Spielerinformationen.
 * Dabei wird als Besitzer jeweils der Eintrag aus globalOwnersMap gewählt,
 * ist kein Eintrag vorhanden, wird "Computer" angezeigt.
 * 
 * @param {Array} data Array von Spielern (Transfermarkt-Liste)
 */
function renderTable(data) {
    const tbody = document.querySelector('#transferTable tbody');
    tbody.innerHTML = '';

    ownersMap = window.globalOwnersMap || new Map();

    data.forEach(item => {
        const row = document.createElement('tr');

        // Spieler Name + ID
        const playerCell = document.createElement('td');
        const playerInfo = document.createElement('div');
        playerInfo.className = 'player-info';

        const nameLink = document.createElement('a');
        nameLink.href = getPlayerUrl(item.playerID);
        nameLink.className = 'player-name';
        nameLink.textContent = item.playerName;

        const playerId = document.createElement('div');
        playerId.className = 'player-id';
        playerId.textContent = `ID: ${item.playerID}`;

        playerInfo.appendChild(nameLink);
        playerInfo.appendChild(playerId);
        playerCell.appendChild(playerInfo);
        playerCell.classList.add('player');

        // Vereinslogo
        const clubCell = document.createElement('td');
        clubCell.className = 'club-logo';
        const clubLogo = document.createElement('img');
        clubLogo.src = DATA_URLS.logos + getLogoFileName(item.verein);
        clubLogo.alt = `Club ${item.verein}`;
        clubCell.appendChild(clubLogo);
        clubCell.classList.add('club');

        // Status
        const statusCell = document.createElement('td');
        statusCell.className = 'status-logo';

        const statusWrapper = document.createElement('div');
        statusWrapper.style.display = 'flex';
        statusWrapper.style.flexDirection = 'column';
        statusWrapper.style.alignItems = 'center';

        const statusIcon = document.createElement('div');
        statusIcon.textContent = getStatusIndicator(item.status);

        const statusText = document.createElement('small');
        statusText.textContent = item.status;

        statusWrapper.appendChild(statusIcon);
        statusWrapper.appendChild(statusText);
        statusCell.appendChild(statusWrapper);

        // Position
        const positionCell = document.createElement('td');
        positionCell.textContent = item.position;
         positionCell.classList.add('position');

        // Marktwert, grün/fett falls höher als Preis
        const valueCell = document.createElement('td');
        if (item.wert > item.preis) {
            const valueSpan = document.createElement('span');
            valueSpan.style.fontWeight = 'bold';
            valueSpan.style.color = 'green';
            valueSpan.textContent = formatCurrency(item.wert);
            valueCell.appendChild(valueSpan);
        } else {
            valueCell.textContent = formatCurrency(item.wert);
        }
        valueCell.classList.add('value');


        // Preis
        const priceCell = document.createElement('td');
        priceCell.textContent = formatCurrency(item.preis);
        priceCell.classList.add('price');


        // Restzeit
        const timeCell = document.createElement('td');
        const remainingTime = calculateRemainingTime(item.remainingDate);
        timeCell.textContent = remainingTime;
        timeCell.classList.add('time');


        if (remainingTime === 'Abgelaufen') {
            row.classList.add('expired');
            timeCell.className = 'expired'; // Bestehende rote Klasse
        } else {
            // Parse Stunden aus dem String "Xh Ym"
            const match = remainingTime.match(/(\d+)h/);
            const hoursLeft = match ? parseInt(match[1]) : 72;

            if (hoursLeft > 24) {
                timeCell.classList.add('time-high');     // Grün ab >24h
            } else if (hoursLeft > 5) {
                timeCell.classList.add('time-medium');  // Gelb zw. 5h und 24h
            } else {
                timeCell.classList.add('time-low');     // Rot ab <5h
            }
        }


        // Besitzer-Name aus globalOwnersMap oder "Computer" wenn nicht vorhanden
        const ownerCell = document.createElement('td');
        ownerCell.textContent = ownersMap.get(item.playerID) || "Computer";
        ownerCell.classList.add('owner');

        // Alle Zellen an die Zeile hängen
        row.appendChild(clubCell);
        row.appendChild(playerCell);
        row.appendChild(statusCell);
        row.appendChild(positionCell);
        row.appendChild(valueCell);
        row.appendChild(priceCell);
        row.appendChild(ownerCell);
        row.appendChild(timeCell);

        tbody.appendChild(row);
    });
}

/**
 * Formatiert einen numerischen Wert als EUR-Währung ohne Nachkommastellen.
 * @param {number} value Numerischer Wert
 * @returns {string} formatierter String
 */
function formatCurrency(value) {
    return new Intl.NumberFormat('de-DE', {
        style: 'currency',
        currency: 'EUR',
        maximumFractionDigits: 0
    }).format(value);
}

/**
 * Berechnet verbleibende Zeit bis zum angegebenen Datum.
 * Gibt "Abgelaufen" zurück, wenn das Datum in der Vergangenheit liegt,
 * ansonsten verbleibende Stunden und Minuten als String im Format "Xh Ym".
 * @param {string} dateString ISO-Datumsstring
 * @returns {string} Verbleibende Zeit oder "Abgelaufen"
 */
function calculateRemainingTime(dateString) {
    // Korrigiere Zeitzonen-Format von +0200 zu +02:00 für Date Parsing
    const normalizedDateString = dateString.replace(/([+-]\d{2})(\d{2})$/, '$1:$2');
    const endDate = new Date(normalizedDateString);
    const now = new Date();

    if (endDate <= now) return 'Abgelaufen';

    const diff = endDate - now;
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    return `${hours}h ${minutes}m`;
}


/**
 * Zeigt einen Lade-Spinner und versteckt den Inhaltsbereich.
 */
function showLoading() {
    document.getElementById('loading').style.display = 'block';
    document.getElementById('content').style.display = 'none';
    document.getElementById('error').style.display = 'none';
}

/**
 * Versteckt den Lade-Spinner.
 */
function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}

/**
 * Zeigt den Hauptinhalt.
 */
function showContent() {
    document.getElementById('content').style.display = 'block';
}

/**
 * Zeigt eine Fehlermeldung auf der Seite an.
 * @param {string} message Fehlermeldungstext
 */
function showError(message) {
    document.getElementById('error').style.display = 'block';
    document.getElementById('errorMessage').textContent = message;
}
