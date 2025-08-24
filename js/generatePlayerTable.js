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

    // Standard-Sortierung nach Marktwert (absteigend)
    const marketValueHeader = headers[4]; // Index 4 für Marktwert
    if (marketValueHeader) {
        marketValueHeader.classList.add('sorted-desc');
        const rows = [...table.querySelectorAll('tbody tr')];
        rows.sort((a, b) => {
            const aValue = parseFloat(a.children[4].getAttribute('data-sort')) || 0;
            const bValue = parseFloat(b.children[4].getAttribute('data-sort')) || 0;
            return bValue - aValue; // Absteigend sortieren
        });
        const tbody = table.querySelector('tbody');
        rows.forEach(row => tbody.appendChild(row));
    }
}