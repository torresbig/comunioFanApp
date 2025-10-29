function initTabs() {
    addDebug('Initialisiere Tabs', 'info');
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(tab => tab.classList.remove('active'));
            button.classList.add('active');
            const tabId = button.getAttribute('data-tab');
            document.getElementById(tabId).classList.add('active');
            addDebug(`Tab gewechselt zu: ${tabId}`, 'info');
        });
    });
    tabButtons.forEach(btn => btn.classList.remove('active'));
    tabContents.forEach(tab => tab.classList.remove('active'));
    const defaultTabBtn = document.querySelector('.tab-button[data-tab="market"]')
        || document.querySelector('.tab-button.active')
        || document.querySelector('.tab-button');

    if (defaultTabBtn) {
        defaultTabBtn.classList.add('active');
        const defaultTabId = defaultTabBtn.getAttribute('data-tab');
        const defaultContent = document.getElementById(defaultTabId);
        if (defaultContent) defaultContent.classList.add('active');
    }

    addDebug('Tabs initialisiert', 'success');
}

  function getPlayerUrlWithParams(playerId) {
       let playerUrl = getPlayerUrl(playerId);
        if (urlParams.withMenue === false) {
          playerUrl += '&withMenue=false';
        }
        return playerUrl;
    }

function displayRivals(player, allPlayers) {
    addDebug('Erstelle Rivalen-Tabelle', 'info');
    const container = document.getElementById('rivalsList');
    const header = document.getElementById('rivalsHeader');
    if (!container) return;
    const rivals = allPlayers.filter(p =>
        p.data?.position === player.data?.position &&
        p.data?.verein === player.data?.verein
    );

    rivals.sort((a, b) => {
        const aRankingObj = getLigainsiderRankingObj(a);
        const bRankingObj = getLigainsiderRankingObj(b);
        const aRank = (aRankingObj && aRankingObj.rang !== undefined && aRankingObj.rang !== null && aRankingObj.rang !== 0 && aRankingObj.rang !== '') ? aRankingObj.rang : Number.MAX_SAFE_INTEGER;
        const bRank = (bRankingObj && bRankingObj.rang !== undefined && bRankingObj.rang !== null && bRankingObj.rang !== 0 && bRankingObj.rang !== '') ? bRankingObj.rang : Number.MAX_SAFE_INTEGER;
        return aRank - bRank;
    });

    addDebug(`${rivals.length} Rivalen gefunden`, 'success');
    header.textContent = `${rivals.length} Direkte Konkurrenten im Team`;

    if (rivals.length > 0) {
        let html = `
        <table class="table-container rivals-table">
            <thead>
                <tr>
                    <th>Spieler</th>
                    <th>Status</th>
                    <th class="ligainsider-ranking">Ligainsider Ranking</th>
                    <th>Marktwert</th>
                    <th>Realwert</th>
                    <th>Punkte</th>
                    <th>Besitzer</th>
                </tr>
            </thead>
            <tbody>
        `;
        rivals.forEach(rival => {
            const statusData = rival.data.status || {};
            const statusTooltip = `${getStatusDisplayName(statusData.status)}${statusData.grund ? ' - ' + statusData.grund : ''}${statusData.seit ? ' seit ' + statusData.seit : ''}`;
            const ownerName = globalOwnersMap.get(rival.id) || "Computer";
            const playerUrl = getPlayerUrlWithParams(rival.id);
            const rankingObj = getLigainsiderRankingObj(rival);
            const ligRank = (rankingObj && rankingObj.rang !== undefined && rankingObj.rang !== null && rankingObj.rang !== 0 && rankingObj.rang !== '') ? rankingObj.rang : '-';
            const status = statusData.status || 'unbekannt';
            // Marktwert als reine Zahl für Sortierung
            const marktwertNum = typeof rival.data?.wert === 'number' ? rival.data.wert : 0;

            html += `
            <tr data-player-id="${rival.id}">
                <td class="player-cell" data-sort="${rival.name}">
                    <div class="player-name-cell">
                        <a href="${playerUrl}" class="player-link" title="Zum Spieler">${rival.name}</a>
                    </div>
                    <div class="player-id-cell">(${rival.id})</div>
                </td>
                <td data-sort="${status || ''}"> 
<div 
  style="display:flex;flex-direction:column;align-items:center" 
  title="${statusTooltip || status}"
>
  <div>${getStatusIndicator(status)}</div>
  <small style="font-size:0.8em;color:#666">${status}</small>
</div>                </td>
                <td class="ligainsider-ranking" data-sort="${ligRank === '-' ? Number.MAX_SAFE_INTEGER : ligRank}">${ligRank}</td>
                <td data-sort="${marktwertNum}">${formatCurrencyFull(rival.data?.wert || 0)}</td>
                <td data-sort="${rival.data?.realWert || 0}">${formatCurrencyFull(rival.data?.realWert || 0)}</td>
                <td data-sort="${rival.data?.punkte || 0}">${rival.data?.punkte || 0}</td>
                <td data-sort="${ownerName}">${ownerName}</td>
            </tr>
            `;
        });
        html += '</tbody></table>';
        container.innerHTML = html;
        // Sortiere standardmäßig nach Marktwert (Spalte 3, Index 3), absteigend
        makeTableSortable('.rivals-table', 3, 'desc');
    } else {
        container.innerHTML = '<p>Keine direkten Konkurrenten gefunden</p>';
    }
}

function makeTableSortable(tableSelector, defaultSortCol = 0, defaultSortDir = 'desc') {
    const table = document.querySelector(tableSelector);
    if (!table) {
        addDebug(`Tabelle mit Selektor '${tableSelector}' nicht gefunden`, 'error');
        return;
    }
    const ths = table.querySelectorAll('th');
    let sortCol = defaultSortCol;
    let sortDir = defaultSortDir;

    function sortTable(colIndex, dir) {
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        rows.sort((a, b) => {
            const aCell = a.children[colIndex];
            const bCell = b.children[colIndex];
            let aValue = aCell.getAttribute('data-sort') || aCell.textContent;
            let bValue = bCell.getAttribute('data-sort') || bCell.textContent;
            addDebug(`Vergleiche Werte: aValue='${aValue}', bValue='${bValue}'`, 'info');
            // Ligainsider Ranking (colIndex 2) als Integer sortieren
            if (colIndex === 2) {
                aValue = parseInt(aValue.replace(/[^\d-]/g, '')) || Number.MAX_SAFE_INTEGER;
                bValue = parseInt(bValue.replace(/[^\d-]/g, '')) || Number.MAX_SAFE_INTEGER;
                return dir === 'asc' ? aValue - bValue : bValue - aValue;
            } else if ([3, 4, 5].includes(colIndex)) {
                aValue = parseFloat(aValue.replace(/[^\d.-]/g, '')) || 0;
                bValue = parseFloat(bValue.replace(/[^\d.-]/g, '')) || 0;
                return dir === 'asc' ? aValue - bValue : bValue - aValue;
            } else {
                aValue = aValue.toString().toLowerCase();
                bValue = bValue.toString().toLowerCase();
                if (aValue < bValue) return dir === 'asc' ? -1 : 1;
                if (aValue > bValue) return dir === 'asc' ? 1 : -1;
                return 0;
            }
        });
        rows.forEach(row => tbody.appendChild(row));
        addDebug(`Tabelle sortiert: Spalte=${colIndex}, Richtung=${dir}`, 'success');
    }

    ths.forEach((th, i) => {
        th.style.cursor = 'pointer';
        let arrowSpan = th.querySelector('.sort-arrow');
        if (!arrowSpan) {
            arrowSpan = document.createElement('span');
            arrowSpan.className = 'sort-arrow';
            th.appendChild(arrowSpan);
        }
        th.addEventListener('click', () => {
            ths.forEach(h => h.querySelector('.sort-arrow').textContent = '');
            if (sortCol === i) {
                sortDir = sortDir === 'asc' ? 'desc' : 'asc';
            } else {
                sortCol = i;
                sortDir = 'asc';
            }
            arrowSpan.textContent = sortDir === 'asc' ? '▲' : '▼';
            sortTable(sortCol, sortDir);
        });
    });

    sortTable(sortCol, sortDir);
    if (ths[sortCol]) {
        ths[sortCol].querySelector('.sort-arrow').textContent = sortDir === 'asc' ? '▲' : '▼';
    }
}

function displayPointsHistory(player) {
    addDebug('Erstelle Punkte-Anzeige', 'info');
    const container = document.getElementById('pointsHistory');
    if (!container) return;
    const pointsHistory = player.data?.historicalPoints || [];
    const spieltagspunkte = player.data?.spieltagspunkte || [];
    const currentPoints = player.data?.punkte || 0;
    const currentSeason = new Date().getFullYear();

    let html = `
        <h3>📈 Punkteverlauf - Aktuelle Saison (${currentSeason})</h3>
        <table class="points-table">
            <thead>
                <tr>
                    <th>Spieltag</th><th>Punkte</th>
                    <th>Spieltag</th><th>Punkte</th>
                    <th>Spieltag</th><th>Punkte</th>
                    <th>Spieltag</th><th>Punkte</th>
                </tr>
            </thead>
            <tbody>
    `;

    for (let i = 1; i <= 34; i += 4) {
        html += '<tr>';
        for (let j = 0; j < 4; j++) {
            const spieltag = i + j;
            if (spieltag > 34) {
                html += '<td></td><td></td>';
            } else {
                const punktEntry = spieltagspunkte.find(p => p.key === spieltag);
                const punkte = punktEntry ? punktEntry.value : '-';
                html += `<td class="matchday-cell">${spieltag}</td><td class="points-cell">${punkte}</td>`;
            }
        }
        html += '</tr>';
    }

    html += `
            </tbody>
        </table>
    `;

    if (pointsHistory.length > 0) {
        html += '<h3>📅 Historische Saisons</h3>';
        html += '<table class="points-table">';
        html += '<thead><tr><th>Saison</th><th>Punkte</th></tr></thead><tbody>';

        pointsHistory.forEach(season => {
            // season ist z. B. { "2011": 2 }
            const [year, points] = Object.entries(season)[0];
            html += `<tr><td>${year}</td><td>${points}</td></tr>`;
        });

        html += '</tbody></table>';
    }


    container.innerHTML = html;
    addDebug('Punkte-Anzeige erstellt', 'success');
}

// NEU (ohne Fehler, für jede Seite nutzbar):
const defaultTabBtn = document.querySelector('.tab-button[data-tab="market"]')
    || document.querySelector('.tab-button.active')
    || document.querySelector('.tab-button');
const defaultTabId = defaultTabBtn && defaultTabBtn.getAttribute('data-tab');
if (defaultTabBtn && defaultTabId) {
    defaultTabBtn.classList.add('active');
    const defaultContent = document.getElementById(defaultTabId);
    if (defaultContent) defaultContent.classList.add('active');
}