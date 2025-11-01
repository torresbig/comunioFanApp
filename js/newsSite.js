/**
 * newsSite.js - News-Seite mit verbessertem Filter-Handling
 * Angepasst für Dropdown-basierte Filterung statt Chips
 */

async function fetchJSON(url) {
    const cacheBusterUrl = url + '?t=' + new Date().getTime();
    try {
        const response = await fetch(cacheBusterUrl);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const text = await response.text();
        if (!text.trim()) throw new Error('Leere Datei');
        return JSON.parse(text);
    } catch (error) {
        addDebug(`[fetchJSON] FEHLER: ${error.message}`);
        throw error;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    addDebug('[DOMContentLoaded] Start');
    
    const newsListDiv = document.getElementById('news-list');
    const userFilter = document.getElementById('userFilter');
    const newsTypeFilter = document.getElementById('newsTypeFilter');
    const resetFilters = document.getElementById('resetFilters');

    if (!newsListDiv || !userFilter || !newsTypeFilter || !resetFilters) {
        addDebug('[DOMContentLoaded] DOM-Elemente FEHLEN!');
        addDebug('  - news-list: ' + !!newsListDiv);
        addDebug('  - userFilter: ' + !!userFilter);
        addDebug('  - newsTypeFilter: ' + !!newsTypeFilter);
        addDebug('  - resetFilters: ' + !!resetFilters);
        return;
    }

    let allNewsData = [];
    let userMap = new Map();
    let ownersMap = new Map();
    let allUsers = [];
    let allTypes = [];

    try {
        addDebug('[Load] Lade News...');
        const newsData = await fetchJSON(DATA_URLS.news);
        
        if (newsData.newsDB && Array.isArray(newsData.newsDB)) {
            allNewsData = newsData.newsDB;
            addDebug('[Load] newsDB geladen, Länge: ' + allNewsData.length);
        } else {
            throw new Error('Ungültige News-Struktur');
        }

        addDebug('[Load] Lade User...');
        const userData = await fetchJSON(DATA_URLS.users);
        addDebug('[Load] User geladen: ' + userData.length);

        addDebug('[Load] Lade PlayerToUser Map...');
        const playerToUserMap = await fetchJSON(DATA_URLS.playerToUser);
        addDebug('[Load] PlayerToUser geladen: ' + playerToUserMap.length);

        // Erstelle UserMap
        userData.forEach(userEntry => {
            if (userEntry.user && userEntry.user.id) {
                userMap.set(userEntry.user.id, userEntry.user.name);
            }
        });

        // Erstelle OwnersMap
        playerToUserMap.forEach(item => {
            const playerId = Object.keys(item)[0];
            const ownerId = item[playerId];
            const ownerName = userMap.get(ownerId) || 'Unbekannt';
            ownersMap.set(playerId, ownerName);
        });

        // Extrahiere User
        allUsers = userData.map(userEntry => ({
            id: userEntry.user.id,
            name: userEntry.user.name
        })).sort((a, b) => a.name.localeCompare(b.name));

        // Extrahiere News-Typen
        const typesSet = new Set();
        allNewsData.forEach(day => {
            if (day.news && Array.isArray(day.news)) {
                day.news.forEach(n => {
                    if (n.art && n.art !== 'OWNERCHANGE') {
                        typesSet.add(n.art);
                    }
                });
            }
        });
        allTypes = Array.from(typesSet).sort();

        // Zähle Gesamt-News
        const totalNews = allNewsData.reduce((sum, day) => sum + (day.news ? day.news.length : 0), 0);
        addDebug('[Load] Gesamt News: ' + totalNews + ' in ' + allNewsData.length + ' Tagen');

        // ===== BENUTZER-DROPDOWN BEFÜLLEN =====
        userFilter.innerHTML = '<option value="">Alle Benutzer</option>';
        allUsers.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = user.name;
            userFilter.appendChild(option);
        });
        addDebug('[Load] Benutzer-Dropdown befüllt mit ' + allUsers.length + ' Usern');

        // ===== NEWSTYP-DROPDOWN BEFÜLLEN =====
        newsTypeFilter.innerHTML = '<option value="">Alle Newstypen</option>';
        allTypes.forEach(type => {
            const option = document.createElement('option');
            option.value = type;
            option.textContent = type;
            newsTypeFilter.appendChild(option);
        });
        addDebug('[Load] Newstyp-Dropdown befüllt mit ' + allTypes.length + ' Typen');

        addDebug('[Load] Initialisierung fertig');

        // ===== EVENT-LISTENER =====
        userFilter.addEventListener('change', applyFilters);
        newsTypeFilter.addEventListener('change', applyFilters);
        
        resetFilters.addEventListener('click', () => {
            addDebug('[Reset-Filter] Klick');
            userFilter.value = '';
            newsTypeFilter.value = '';
            applyFilters();
        });

        // Initiales Rendering
        applyFilters();

    } catch (error) {
        addDebug('[DOMContentLoaded] FEHLER: ' + error.message);
        newsListDiv.innerHTML = '<div style="padding:20px;color:#e74c3c;background:#fef2f2;border-radius:8px;border-left:4px solid #e74c3c;"><strong>⚠️ Fehler:</strong><br>' + error.message + '</div>';
    }

    function applyFilters() {
        addDebug('[applyFilters] AUFGERUFEN');
        
        const selectedUserId = userFilter.value;
        const selectedUserName = selectedUserId ? userMap.get(selectedUserId) : '';
        const selectedType = newsTypeFilter.value;

        addDebug('[applyFilters] selectedUserId: ' + (selectedUserId || 'Alle'));
        addDebug('[applyFilters] selectedType: ' + (selectedType || 'Alle'));

        let filteredNews = [];

        if (!selectedUserId && !selectedType) {
            addDebug('[applyFilters] Modus: ALLE News');
            filteredNews = allNewsData.map(day => ({
                date: day.date,
                news: day.news ? day.news.filter(n => n.art !== 'OWNERCHANGE' && n.art !== 'UNBESTIMMT') : []
            })).filter(day => day.news.length > 0);
        } else {
            addDebug('[applyFilters] Modus: Gefilterte News');
            filteredNews = allNewsData.map(day => {
                const filtered = day.news ? day.news.filter(news => {
                    if (news.art === 'OWNERCHANGE' || news.art === 'UNBESTIMMT') return false;
                    
                    if (selectedUserId) {
                        const match = news.text.includes(selectedUserName) || 
                                     news.text.includes(selectedUserId) ||
                                     (news.playerId && ownersMap.get(news.playerId) === selectedUserName);
                        if (!match) return false;
                    }
                    
                    if (selectedType && news.art !== selectedType) return false;
                    
                    return true;
                }) : [];
                
                return { date: day.date, news: filtered };
            }).filter(day => day.news.length > 0);
        }

        const totalNews = filteredNews.reduce((sum, day) => sum + day.news.length, 0);
        addDebug('[applyFilters] Ergebnis: ' + totalNews + ' News in ' + filteredNews.length + ' Tagen');

        if (typeof renderNews === 'function') {
            addDebug('[applyFilters] Rufe renderNews auf');
            renderNews(filteredNews);
        } else {
            addDebug('[applyFilters] FEHLER: renderNews nicht gefunden!');
        }
    }
});
