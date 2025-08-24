        
        let globalOwnersMap = new Map();
        
        async function loadOwnersData() {
            addDebug('Lade Besitzerdaten...', 'info');
            try {
                const [usersData, playerToUserMap] = await Promise.all([
                    fetchJSON(DATA_URLS.users  ),
                    fetchJSON(DATA_URLS.playerToUser )
                ]);

                const userMap = new Map();
                usersData.forEach(user => {
                    userMap.set(user.user.id, `${user.user.firstName} ${user.user.lastName || ''}`);
                });

                playerToUserMap.forEach(item => {
                    const playerId = Object.keys(item)[0];
                    const ownerId = item[playerId];
                    globalOwnersMap.set(playerId, userMap.get(ownerId) || `Unbekannt (${ownerId})`);
                });
                addDebug(`Besitzerdaten geladen: ${globalOwnersMap.size} Zuordnungen`, 'success');
            } catch (error) {
                addDebug(`Fehler beim Laden der Besitzerdaten: ${error.message}`, 'error');
            }
        }