// transferTable.js

document.addEventListener("DOMContentLoaded", async () => {
    // --- Datenquellen wie config.js
    const GITHUB_USER = "torresbig";
    const GITHUB_REPO = "comunioFanApp";
    const DATA_URLS = {
        news: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/News.json`,
        players: `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/SpielerdatenbankNeutralJson.txt`
    };

    // Elemente in HTML
    const userFilter = document.getElementById("userFilter"); // <select> für Nutzer
    const tableBody = document.querySelector("#transfersTable tbody"); // Tabelle

    // --- Spielerdatenbank laden
    const playerDbResp = await fetch(DATA_URLS.players);
    const playerDbJson = await playerDbResp.json();
    const playerDb = {};
    (playerDbJson.playerDB || []).forEach(p => {
        // ID und aktueller Wert merken
        playerDb[String(p.id)] = p.data?.wert || 0;
    });

    // --- Transfernews laden
    const newsResp = await fetch(DATA_URLS.news);
    const newsJson = await newsResp.json();

    // Alle Transfers extrahieren (flach als Array)
    const transfers = [];
    newsJson.forEach(day => {
        (day.news || []).forEach(entry => {
            if (entry.art === "TRANSFER") {
                try {
                    const tData = JSON.parse(entry.text);
                    transfers.push({
                        date: entry.date,
                        playerId: String(entry.playerId),
                        playerName: tData.playerName,
                        seller: tData.seller,
                        buyer: tData.buyer,
                        price: tData.price,
                        value: tData.playerValue
                    });
                } catch (err) {
                    // Fehler ignorieren
                }
            }
        });
    });

    // --- User-Liste sammeln
    const allUsersSet = new Set();
    transfers.forEach(t => {
        if (t.buyer) allUsersSet.add(t.buyer);
        if (t.seller) allUsersSet.add(t.seller);
    });
    const allUsers = Array.from(allUsersSet).sort();

    // --- Filter-Dropdown erstellen (Standard Computer)
    allUsers.forEach(u => {
        const opt = document.createElement("option");
        opt.value = u;
        opt.textContent = u;
        if (u === "Computer") opt.selected = true;
        userFilter.appendChild(opt);
    });

    // --- Tabelle rendern
    function renderTable(filterUser) {
        tableBody.innerHTML = "";
        const filtered = transfers.filter(t => t.buyer === filterUser || t.seller === filterUser);
        filtered.forEach(t => {
            const currentValue = playerDb[t.playerId] || t.value || 0;
            let dealHtml = "";
            let diff = 0;
            let tooltip = "";

            if (t.buyer === filterUser) { // Kauf
                diff = currentValue - t.price;
                tooltip = `Aktueller Wert minus Kaufpreis: ${formatCurrency(diff)}`;
                if (diff > 0) dealHtml = `<span style="color:#27ae60;font-weight:bold;" title="${tooltip}">&#9650;</span>`;
                else if (diff < 0) dealHtml = `<span style="color:#e74c3c;font-weight:bold;" title="${tooltip}">&#9660;</span>`;
                else dealHtml = `<span style="color:#888;font-weight:bold;" title="Keine Änderung">&#9654;</span>`;
            } else if (t.seller === filterUser) { // Verkauf
                diff = t.price - currentValue;
                tooltip = `Verkaufspreis minus aktueller Wert: ${formatCurrency(diff)}`;
                if (diff > 0) dealHtml = `<span style="color:#27ae60;font-weight:bold;" title="${tooltip}">&#9650;</span>`;
                else if (diff < 0) dealHtml = `<span style="color:#e74c3c;font-weight:bold;" title="${tooltip}">&#9660;</span>`;
                else dealHtml = `<span style="color:#888;font-weight:bold;" title="Keine Änderung">&#9654;</span>`;
            }

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${t.date}</td>
                <td><a class="player-link" href="player.html?id=${t.playerId}" target="_blank">${t.playerName}</a></td>
                <td>${t.seller}</td>
                <td>${t.buyer}</td>
                <td>${formatCurrency(t.price)}</td>
                <td>${formatCurrency(currentValue)}</td>
                <td style="text-align:center">${dealHtml}</td>
            `;
            tableBody.appendChild(tr);
        });
    }

    // Währungsformat (mit Komma und €)
    function formatCurrency(v) {
        if (v == null) return "-";
        return (parseInt(v) || 0).toLocaleString("de-DE") + " €";
    }

    // --- Initial-Tabelle für "Computer"
    renderTable("Computer");

    // --- Filter-Event
    userFilter.addEventListener("change", () => {
        renderTable(userFilter.value);
    });
});

// --- Zusatz: Beispiel für <select id="userFilter"> und Tabelle:
/// <select id="userFilter"></select>
/// <table id="transfersTable">
///   <thead>
///     <tr>
///       <th>Datum</th>
///       <th>Spieler</th>
///       <th>Verkäufer</th>
///       <th>Käufer</th>
///       <th>Preis</th>
///       <th>Akt. Wert</th>
///       <th>Deal</th>
///     </tr>
///   </thead>
///   <tbody></tbody>
/// </table>
