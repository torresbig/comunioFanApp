// ----- FILTERMENÜ & LOGIK -----

let activeFilters = {};

document.addEventListener('DOMContentLoaded', () => {
    // ... bestehendes Eventlistener ...
    setupFilterMenu();
});

function setupFilterMenu() {
    const toggleBtn = document.getElementById('toggleFilterMenuBtn');
    const filterForm = document.getElementById('filterForm');
    const resetBtn = document.getElementById('resetFilterBtn');

    toggleBtn.addEventListener('click', () => {
        if (filterForm.style.display === 'none' || filterForm.style.display === '') {
            filterForm.style.display = 'block';
            toggleBtn.textContent = "🔎 Filter ausblenden";
        } else {
            filterForm.style.display = 'none';
            toggleBtn.textContent = "🔎 Filter anzeigen";
        }
    });

    // Checkbox Events
    filterForm.querySelectorAll('input[type="checkbox"]').forEach(box => {
        box.addEventListener('change', () => {
            applyAllFilters();
        });
    });

    resetBtn.addEventListener('click', () => {
        filterForm.reset();
        applyAllFilters();
    });
}

// Kombiniere alle Filter und rendere die gefilterte Tabelle
function applyAllFilters() {
    const isComputerOwner = document.getElementById('filterComputerOwner').checked;
    const isPrice160000 = document.getElementById('filterPrice160000').checked;
    const isNotStatusAktiv = document.getElementById('filterNotStatusAktiv').checked;
    const isPriceBelowValue = document.getElementById('filterPriceBelowValue').checked;

    let filteredData = originalData.filter(item => {
        let show = true;
        // Nur Computer-Angebote
        if (isComputerOwner) {
            show = show && ((ownersMap.get(item.playerID) || "Computer") === "Computer");
        }
        // Preis = 160000
        if (isPrice160000) {
            show = show && (item.preis === 160000);
        }
        // Status ungleich aktiv
        if (isNotStatusAktiv) {
            show = show && (item.status.toLowerCase() !== "aktiv");
        }
        // Preis niedriger als Wert
        if (isPriceBelowValue) {
            show = show && (item.preis < item.wert);
        }
        return show;
    });

    renderTable(sortedData(filteredData));
}

// Passe sortedData, so dass optional ein custom Array übernommen wird
function sortedData(arr = null) {
    const data = (arr || originalData).slice();
    return data.sort((a, b) => {
        switch (sortColumnIndex) {
            case 0: return cmpStr(a.verein, b.verein);
            case 1: return cmpStr(a.playerName, b.playerName);
            case 2: return cmpStr(a.status, b.status);
            case 3: return cmpStr(a.position, b.position);
            case 4: return cmpNum(a.wert, b.wert);
            case 5: return cmpNum(a.preis, b.preis);
            case 6: return cmpStr(ownersMap.get(a.playerID), ownersMap.get(b.playerID));
            case 7:
                return sortDirection * (getTimeLeftForSort(a.setOnMarket) - getTimeLeftForSort(b.setOnMarket));
            default: return 0;
        }
    });
}

// --- Damit die Filter auch initial nach dem Laden angewandt werden, passe loadTransferMarktData an:
async function loadTransferMarktData() {
    try {
        showLoading();
        addDebug("Lade Transfermarkt-Liste...");
        const response = await fetch(DATA_URLS.transfermarkt);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        addDebug(`${data.length} Einträge geladen`);
        ownersMap = window.globalOwnersMap || new Map();
        originalData = data;
        applyAllFilters(); // GANZ WICHTIG für initiales Anwenden und Sortieren!
        hideLoading();
        showContent();
        initSortEvents();
    } catch (error) {
        hideLoading();
        throw error;
    }
}
