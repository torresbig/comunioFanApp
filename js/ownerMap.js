// globalOwnersMap als globale Variable
window.globalOwnersMap = new Map();

async function loadOwnersData() {
    addDebug('Lade Besitzerdaten...', 'info');
    try {
        const [usersData, playerToUserMap] = await Promise.all([
            fetchJSON(DATA_URLS.users),
            fetchJSON(DATA_URLS.playerToUser)
        ]);

        const userMap = new Map();
        usersData.forEach(user => {
            userMap.set(user.user.id, `${user.user.firstName} ${user.user.lastName || ''}`);
        });

        playerToUserMap.forEach(item => {
            const playerId = Object.keys(item)[0];
            const ownerId = item[playerId];
            window.globalOwnersMap.set(playerId, userMap.get(ownerId) || `Unbekannt (${ownerId})`);
        });

        addDebug(`Besitzerdaten geladen: ${window.globalOwnersMap.size} Zuordnungen`, 'success');
        console.log('globalOwnersMap:', window.globalOwnersMap); // Konsole prüfen

    } catch (error) {
        addDebug(`Fehler beim Laden der Besitzerdaten: ${error.message}`, 'error');
    }
}


// Globale Helferfunktion zum Owner-Lookup
function getOwnerString(playerId) {
    return window.globalOwnersMap.get(playerId) || "Computer";
}
