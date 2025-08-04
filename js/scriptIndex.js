
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
        function toggleNewsList() {
            const list = document.getElementById('news-list');
            const icon = document.getElementById('news-toggle-icon');
            if (list.style.display === 'none') {
                list.style.display = '';
                icon.style.transform = 'rotate(0deg)';
            } else {
                list.style.display = 'none';
                icon.style.transform = 'rotate(-90deg)';
            }
        }

        function renderNews(newsList) {
            try {
                // Nach Datum absteigend sortieren (neueste zuerst!)
                newsList.sort((a, b) => {
                    const da = a.date.split('.').reverse().join('-');
                    const db = b.date.split('.').reverse().join('-');
                    return new Date(db) - new Date(da);
                });
                let html = '';
                for (const day of newsList) {
                    // Filter alle OWNERCHANGE NEWS aus day.news heraus
                    const newsForDisplay = day.news.filter(n => n.art !== 'OWNERCHANGE' && n.art !== 'UNBESTIMMT');

                    // Debug-Log für "OWNERCHANGE" separat anzeigen
                    day.news.forEach(n => {
                        if (n.art === 'OWNERCHANGE' || n.art === 'UNBESTIMMT') {
                            addDebug(`Ownerchange-Text: ${n.text}`);
                        }
                    });

                    const grouped = {};
                    for (const news of newsForDisplay) {
                        if (!grouped[news.art]) grouped[news.art] = [];
                        grouped[news.art].push(news);
                    }

                    html += `<div class="news-day"><div class="news-date">${day.date}</div>`;

                    for (const art of Object.keys(grouped).sort()) {
                        html += `<div class="news-art">${art}</div><ul class="news-list-ul">`;
                        for (const news of grouped[art]) {
                            let text = '';
                            try {
                                if (art === 'TRANSFER') {
                                    const obj = JSON.parse(news.text);
                                    const pid = obj.playerId || news.playerId || null; // fallback falls obj.playerId fehlt
                                    text = `${linkPlayer(pid, obj.playerName)} von <b style="color:#00f;">${obj.seller}</b> zu <b style="color:#00f;">${obj.buyer}</b> für <b>${obj.price.toLocaleString('de-DE')} €</b> (Marktwert: ${obj.playerValue.toLocaleString('de-DE')} €)`;
                                }
                                else if (art === 'SPIELERSTATUS') {
                                    const regex = /Statuswechsel:\s(.+?)\s\(\d+\)\sist\s(wieder|jetzt)\s([A-Z_]+)(?:\s\((.+)\))?/i;
                                    const match = regex.exec(news.text);
                                    if (match) {
                                        const playerName = match[1];
                                        const status = match[3];
                                        const statusDetail = match[4] || '';
                                        let statusDisplay = `<b>${status.replace(/_/g, ' ')}</b>`;
                                        if (statusDetail) statusDisplay += ` (${statusDetail})`;

                                        text = `Statusänderung: ${linkPlayer(news.playerId, playerName)} ist ${match[2]} ${statusDisplay}`;
                                    } else {
                                        text = news.text;
                                    }


                                } else if (art === 'VEREINSWECHSEL') {
                                    try {
                                        const obj = JSON.parse(news.text); // falls JSON
                                        const pid = obj.playerId || news.playerId || null;
                                        text = `${linkPlayer(pid, obj.playerName)} wechselt von <b>${obj.oldClub}</b> zu <b>${obj.newClub}</b>`;
                                    } catch (e) {
                                        // kein JSON, Versuch Name aus Text per Regex
                                        const regex = /^Vereinswechsel:\s(.+?)\s\(Verein \d+ → \d+\)$/;
                                        const match = regex.exec(news.text);
                                        if (match) {
                                            const playerName = match[1];
                                            text = `Vereinswechsel: ${linkPlayer(news.playerId, playerName)}`
                                        } else {
                                            text = news.text;
                                        }
                                    }
                                }
                                else if (art === 'NEW_PLAYER') {
                                    const regex = /^Neuer Spieler:\s(.+?)\s\(ID: \d+\)$/;
                                    const match = regex.exec(news.text);
                                    if (match) {
                                        const playerName = match[1];
                                        text = `Neuer Spieler: ${linkPlayer(news.playerId, playerName)} (ID: ${news.text.match(/\(ID: (\d+)\)/)[1]})`;
                                    } else {
                                        text = news.text;
                                    }
                                }
                                else {
                                    text = news.text;
                                }

                            } catch (e) {
                                console.error("Fehler beim Verarbeiten des News-Textes:", news.text, e);
                                text = news.text;
                            }
                            html += `<li class=\"news-list-li\">${text}</li>`;
                        }
                        html += `</ul>`;
                    }
                    html += `</div>`;
                }
                document.getElementById('news-list').innerHTML = html || '<div style=\"padding:16px; color:#888;\">Keine News vorhanden.</div>';
            } catch {
                document.getElementById('news-list').innerHTML = '<div style=\"padding:16px; color:#e53935;\">Fehler beim Laden der News.</div>';
            }
            document.getElementById('news-list').style.display = '';
        }

        // Hilfsfunktion innerhalb von renderNews oder global verfügbar
        function linkPlayer(playerId, playerName) {
            if (!playerId) {
                // Keine playerId vorhanden, kein Link, nur farbig hervorgehoben
                return `<span style="color:#80f; font-weight:bold;">${playerName}</span>`;
            }
            const url = getPlayerUrl(playerId);
            return `<a href="${url}" style="color:#80f; font-weight:bold;">${playerName}</a>`;
        }



       

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

            // Floating debug panel logic
            const debugBtn = document.getElementById('floatingDebugBtn');
            const debugPanel = document.getElementById('floatingDebugPanel');
            const debugClose = document.getElementById('floatingDebugPanelClose');
            debugBtn.addEventListener('click', () => {
                debugPanel.classList.toggle('open');
            });
            debugClose.addEventListener('click', () => {
                debugPanel.classList.remove('open');
            });

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

        function getPlayerUrl(playerId) {
            const currentUrl = window.location.href;
            if (currentUrl.includes('htmlpreview.github.io')) {
                return WEBSITE_URLS.playerUrl+`${playerId}`;
            } else if (currentUrl.includes('github.com') || currentUrl.includes('githubusercontent.com')) {
                return WEBSITE_URLS.playerUrl+`${playerId}`;
            } else {
                return `player.html?id=${playerId}`;
            }
        }

       

        function renderTable(players) {
            const tableBody = document.querySelector('#playerTable tbody');
            tableBody.innerHTML = '';
            addDebug(`Rendere Tabelle mit ${players.length} Spielern`);
            players.forEach(player => {
                const row = document.createElement('tr');
                row.setAttribute('data-player-id', player.id);
                row.addEventListener('click', (e) => {
                    if (e.target.tagName !== 'TH' && !e.target.closest('th')) {
                        addDebug(`Spieler angeklickt: ${player.name} (ID: ${player.id})`);
                        const playerUrl = getPlayerUrl(player.id);
                        addDebug(`Navigation zu: ${playerUrl}`);
                        window.location.href = playerUrl;
                    }
                });
                const clubId = player.data?.verein || "0";
                const clubName = clubsMap.get(clubId) || 'UNBEKANNT';
                const logoFile = getLogoFileName(clubId);
                const logoHtml = `<img src="logos/${logoFile}" class="club-logo" alt="${clubName}" title="${clubName}">`;
                const playerName = player.name || "Unbekannt";
                const playerId = player.id || "";
                const playerNameHtml = `<span title="${playerName} (ID: ${playerId})">${playerName}</span>`;
                const position = player.position || "Unbekannt";
                const hauptposition = player.data?.spielerDaten?.hauptposition || "N/A";
                const nebenpositionen = player.data?.spielerDaten?.nebenpositionen || [];
                const nebenpositionenTooltip = nebenpositionen.length > 0 ? "Hauptposition: " + hauptposition + " | Nebenposition: " + nebenpositionen.join(", ") : "";
                const positionHtml = `<span title="${nebenpositionenTooltip}">${player.position || "Unbekannt"}</span>`;
                const status = player.data?.status?.status || 'AKTIV';
                let statusClass = "";
                if (status.includes("AKTIV")) statusClass = "status-aktiv";
                else if (status.includes("VERLETZT")) statusClass = "status-verletzt";
                else if (status.includes("AUFBAU")) statusClass = "status-reha";
                else if (status.includes("ROTE_KARTE")) statusClass = "status-gesperrt";
                else if (status.includes("GELBROTE_KARTE")) statusClass = "status-gesperrt";
                else if (status.includes("FUENFTE_GELBE_KARTE")) statusClass = "status-gesperrt";
                else if (status.includes("NICHT_IN_LIGA")) statusClass = "status-nichtliga";
                else if (status.includes("NICHT_IM_KADER")) statusClass = "status-nichtliga";
                let marketValue = 'Unbekannt';
                let marketValueSort = 0;
                if (player.data?.wert) {
                    marketValueSort = player.data.wert;
                    marketValue = marketValueSort < 1000000
                        ? `${(marketValueSort / 1000).toFixed(0)} Tsd. €`
                        : `${(marketValueSort / 1000000).toFixed(2)} Mio. €`;
                }
                const owner = ownersMap.get(player.id) || 'Computer';
                const points = player.data?.punkte || 0;
                const lastUpdate = player.data?.lastUpdate || 'Unbekannt';
                row.innerHTML = `
                    <td data-sort="${playerName}">${playerNameHtml}</td>
                    <td data-sort="${clubName}">${logoHtml}</td>
                    <td data-sort="${position}">${positionHtml}</td>
                    <td data-sort="${status}" class="${statusClass}">${status}</td>
                    <td data-sort="${marketValueSort}">${marketValue}</td>
                    <td data-sort="${points}">${points}</td>
                    <td data-sort="${owner}">${owner}</td>
                    <td data-sort="${lastUpdate}">${lastUpdate}</td>
                `;
                tableBody.appendChild(row);
            });
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

        function setupSorting() {
            const table = document.getElementById('playerTable');
            const headers = table.querySelectorAll('th[data-sort]');
            headers.forEach(header => {
                header.addEventListener('click', () => {
                    const sortType = header.getAttribute('data-sort');
                    const columnIndex = [...header.parentNode.children].indexOf(header);
                    const rows = [...table.querySelectorAll('tbody tr')];
                    let sortOrder = 1;
                    if (header.classList.contains('sorted-asc')) {
                        header.classList.remove('sorted-asc');
                        header.classList.add('sorted-desc');
                        sortOrder = -1;
                    } else if (header.classList.contains('sorted-desc')) {
                        header.classList.remove('sorted-desc');
                        sortOrder = 0;
                    } else {
                        header.classList.add('sorted-asc');
                    }
                    headers.forEach(h => {
                        if (h !== header) {
                            h.classList.remove('sorted-asc', 'sorted-desc');
                        }
                    });
                    if (sortOrder === 0) {
                        applyFilters();
                        return;
                    }
                    rows.sort((a, b) => {
                        const aCell = a.children[columnIndex];
                        const bCell = b.children[columnIndex];
                        let aValue = aCell.getAttribute('data-sort') || aCell.textContent;
                        let bValue = bCell.getAttribute('data-sort') || bCell.textContent;
                        if (sortType === 'number' || sortType === 'value') {
                            aValue = parseFloat(aValue) || 0;
                            bValue = parseFloat(bValue) || 0;
                        }
                        return (aValue < bValue ? -1 : 1) * sortOrder;
                    });
                    const tbody = table.querySelector('tbody');
                    rows.forEach(row => tbody.appendChild(row));
                    addDebug(`Tabelle sortiert nach Spalte ${columnIndex + 1} (${sortOrder === 1 ? 'aufsteigend' : 'absteigend'})`);
                });
            });
        }
