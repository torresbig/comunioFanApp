







let allPlayers = [];
let clubsMap = new Map();
let ownersMap = new Map();
let filteredPlayers = [];
let lastUpdateTime = null;



document.addEventListener('DOMContentLoaded', async () => {
    try {
        addDebug("Seite geladen, starte Datenladevorgang...");
        await loadData();
        setupSorting();
    } catch (error) {
        showError("Laden fehlgeschlagen: " + error.message);
        addDebug("Fehler beim Laden: " + error.message);
    }
    document.getElementById('refreshButton').addEventListener('click', async () => {
        try {
            addDebug("Manuelles Neuladen gestartet...");
            await loadData();
        } catch (error) {
            showError("Neuladen fehlgeschlagen: " + error.message);
            addDebug("Fehler beim Neuladen: " + error.message);
        }
    });
    document.getElementById('hideNonLeague').addEventListener('change', applyFilters);

   

    // Toggle-Menü-Logik
    function setupToggle(labelId, contentId) {
        const label = document.getElementById(labelId);
        const content = document.getElementById(contentId);
        let open = false;
        function update() {
            let maxHeight = '300px';
            if (labelId === 'newsLabel') maxHeight = '600px';
            if (open) {
                content.style.maxHeight = maxHeight;
                label.classList.add('open');
            } else {
                content.style.maxHeight = '0';
                label.classList.remove('open');
            }
        }
        label.addEventListener('click', () => {
            open = !open;
            update();
        });
        update();
    }
    setupToggle('newsLabel', 'newsContentWrapper');
    setupToggle('filterLabel', 'filterContentWrapper');
});

async function loadData() {
    try {
        showLoading();
        addDebug("Starte Ladevorgang...");
        addDebug(`Lade URLs:
                    Clubs: ${DATA_URLS.clubs}
                    Players: ${DATA_URLS.players}
                    Users: ${DATA_URLS.users}
                    PlayerToUser: ${DATA_URLS.playerToUser}
                    News: ${DATA_URLS.news}`);
        const [clubsData, playersData, usersData, playerToUserMap, newsList] = await Promise.all([
            fetchJSON(DATA_URLS.clubs),
            fetchJSON(DATA_URLS.players),
            fetchJSON(DATA_URLS.users),
            fetchJSON(DATA_URLS.playerToUser),
            fetchJSON(DATA_URLS.news)
        ]);

        addDebug("Daten erfolgreich geladen, verarbeite...");
        addDebug(`Daten empfangen:
                    Clubs: ${clubsData.length}
                    Players: ${playersData.length}
                    Users: ${usersData.length}
                    PlayerToUser: ${playerToUserMap.length}
                    News: ${newsList.length}`);
        processData(clubsData, playersData, usersData, playerToUserMap);
        renderNews(newsList);
        lastUpdateTime = new Date();
        hideLoading();
        showStats();
        showContent();
        setupEventListeners();
        applyFilters();
        addDebug("Datenverarbeitung abgeschlossen.");
    } catch (error) {
        hideLoading();
        addDebug("Fehler beim Laden: " + error.message);
        throw error;
    }
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

function processData(clubsData, playersData, usersData, playerToUserMap) {
    addDebug("Starte Datenverarbeitung...");
    clubsMap = new Map();
    clubsData.forEach(club => {
        clubsMap.set(club.id, club.name);
    });
    addDebug(`Vereine verarbeitet: ${clubsMap.size}`);
    allPlayers = playersData.playerDB.map(player => ({
        ...player,
        position: player.data?.position || "Unbekannt"
    }));
    addDebug(`Spieler verarbeitet: ${allPlayers.length}`);
    const userMap = new Map();
    usersData.forEach(user => {
        userMap.set(user.user.id, `${user.user.firstName} ${user.user.lastName || ''}`);
    });
    addDebug(`Benutzer verarbeitet: ${userMap.size}`);
    ownersMap = new Map();
    playerToUserMap.forEach(item => {
        const playerId = Object.keys(item)[0];
        const ownerId = item[playerId];
        ownersMap.set(playerId, userMap.get(ownerId) || `Unbekannt (${ownerId})`);
    });
    allPlayers.forEach(player => {
        if (!ownersMap.has(player.id)) {
            ownersMap.set(player.id, "Computer");
        }
    });


    addDebug(`Besitzerzuordnungen: ${ownersMap.size}`);
    initClubFilter();
    initOwnerFilter();
    addDebug("Datenverarbeitung abgeschlossen.");
}

function initClubFilter() {
    const clubFilter = document.getElementById('club');
    clubFilter.innerHTML = '<option value="">Alle Vereine</option>';
    const uniqueClubs = [...new Set(clubsMap.values())].sort();
    uniqueClubs.forEach(club => {
        const option = document.createElement('option');
        option.value = club;
        option.textContent = club;
        clubFilter.appendChild(option);
    });
}

function initOwnerFilter() {
    const ownerFilter = document.getElementById('owner');
    ownerFilter.innerHTML = '<option value="">Alle Besitzer</option>';
    const ownerSet = new Set(ownersMap.values());
    ownerSet.forEach(owner => {
        if (owner !== "Kein Besitzer") {
            const option = document.createElement('option');
            option.value = owner;
            option.textContent = owner;
            ownerFilter.appendChild(option);
        }
    });
}

function showLoading() {
    document.getElementById('loading').style.display = 'block';
    document.getElementById('content').style.display = 'none';
    document.getElementById('error').style.display = 'none';
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}

function showContent() {
    document.getElementById('content').style.display = 'block';
}

function showError(message) {
    hideLoading();
    const errorDiv = document.getElementById('error');
    document.getElementById('errorMessage').textContent = message;
    errorDiv.style.display = 'block';
}

function showStats() {
    const statsDiv = document.getElementById('stats');
    const statsText = document.getElementById('statsText');
    const updateTime = lastUpdateTime ? lastUpdateTime.toLocaleTimeString() : "Noch nicht aktualisiert";
    const anzahlBesitzer = (() => {
        const ownerSet = new Set(ownersMap.values());
        ownerSet.delete("Kein Besitzer");
        return ownerSet.size - 1;
    })();
    statsText.innerHTML = `📊 ${allPlayers.length} Spieler • ${clubsMap.size - 1} Vereine • ${anzahlBesitzer} Besitzer<br>
                                   🕒 Letztes Update: ${updateTime}`;
    statsDiv.style.display = 'block';
}

function applyFilters() {
    addDebug("Filter werden angewendet...");
    const searchTerm = document.getElementById('search').value.toLowerCase();
    const clubFilter = document.getElementById('club').value;
    const positionFilter = document.getElementById('position').value;
    const statusFilter = document.getElementById('status').value;
    const ownerFilter = document.getElementById('owner').value;
    const hideNonLeague = document.getElementById('hideNonLeague').checked;
    filteredPlayers = allPlayers.filter(player => {
        if (searchTerm &&
            !player.name.toLowerCase().includes(searchTerm) &&
            !player.id.toLowerCase().includes(searchTerm)) {
            return false;
        }
        if (clubFilter) {
            const clubName = clubsMap.get(player.data?.verein) || '';
            if (clubName !== clubFilter) return false;
        }
        if (positionFilter && player.position !== positionFilter) return false;
        const statusValue = player.data?.status?.status || '';
        if (statusFilter && !statusValue.includes(statusFilter)) return false;
        const owner = ownersMap.get(player.id) || 'Kein Besitzer';
        if (ownerFilter === "Kein Besitzer" && owner !== 'Kein Besitzer') return false;
        if (ownerFilter && ownerFilter !== "Kein Besitzer" && owner !== ownerFilter) return false;
        if (hideNonLeague && statusValue.includes("NICHT_IN_LIGA")) {
            return false;
        }
        return true;
    });
    renderTable(filteredPlayers);
    addDebug("Filter angewendet.");
}

function resetFilters() {
    document.getElementById('search').value = '';
    document.getElementById('position').value = '';
    document.getElementById('status').value = '';
    document.getElementById('club').value = '';
    document.getElementById('owner').value = '';
    document.getElementById('hideNonLeague').checked = true;
    applyFilters();
    addDebug("Filter zurückgesetzt.");
}

function setupEventListeners() {
    document.getElementById('search').addEventListener('input', applyFilters);
    document.getElementById('position').addEventListener('change', applyFilters);
    document.getElementById('status').addEventListener('change', applyFilters);
    document.getElementById('club').addEventListener('change', applyFilters);
    document.getElementById('owner').addEventListener('change', applyFilters);
    document.getElementById('resetFilters').addEventListener('click', resetFilters);
}


