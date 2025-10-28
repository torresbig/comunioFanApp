function setupAccordion() {
    const infoCards = document.querySelectorAll('.info-card');
    infoCards.forEach((card, index) => {
        const header = card.querySelector('h3');
        const content = card.querySelector('.info-card-content');
        if (!header || !content) {
            addDebug('setupAccordion: header oder content fehlt in info-card', 'error');
            return;
        }
        if (index === 0) {
            content.classList.remove('collapsed');
            header.classList.remove('collapsed');
        } else {
            content.classList.add('collapsed');
            header.classList.add('collapsed');
        }
        header.onclick = () => {
            infoCards.forEach(c => {
                const h = c.querySelector('h3');
                const cont = c.querySelector('.info-card-content');
                if (!h || !cont) {
                    addDebug('setupAccordion: header oder content fehlt in info-card (onclick)', 'error');
                    return;
                }
                if (c !== card) {
                    cont.classList.add('collapsed');
                    h.classList.add('collapsed');
                }
            });
            content.classList.toggle('collapsed');
            header.classList.toggle('collapsed');
        };
    });
}

document.addEventListener('DOMContentLoaded', async () => {

    addDebug('DOM geladen - starte Initialisierung', 'info');

    await loadOwnersData();
    initTabs();

    const url = window.location.href;
    addDebug(`Aktuelle URL: ${url}`, 'info');

    const idMatch = url.match(/[?&]id=([^&]*)/);
    const playerId = idMatch ? idMatch[1] : null;

    addDebug(`Extrahierte Spieler-ID: ${playerId}`, playerId ? 'success' : 'error');

    // Element-Checks für Debug
    const errorMessageEl = document.getElementById('errorMessage');
    const loadingIndicatorEl = document.getElementById('loadingIndicator');
    const playerContentEl = document.getElementById('playerContent');
    addDebug(`Element-Check: errorMessageEl=${!!errorMessageEl}, loadingIndicatorEl=${!!loadingIndicatorEl}, playerContentEl=${!!playerContentEl}`, 'info');

    if (!playerId) {
        const errorMsg = "Keine Spieler-ID in der URL gefunden!";
        if (errorMessageEl) {
            errorMessageEl.textContent = errorMsg;
            errorMessageEl.style.display = 'block';
        } else {
            addDebug('errorMessageEl fehlt!', 'error');
        }
        if (loadingIndicatorEl) {
            loadingIndicatorEl.style.display = 'none';
        } else {
            addDebug('loadingIndicatorEl fehlt!', 'error');
        }
        addDebug(errorMsg, 'error');
        return;
    }

    try {
        addDebug('Lade Spielerdatenbank...', 'info');
        const resp = await fetch(DATA_URLS.players);
        const allPlayersResponse = await resp.json();

        // Array aus playerDB extrahieren oder leeres Array, falls playerDB fehlt
        const allPlayers = allPlayersResponse.playerDB || [];

        addDebug(`Spielerdatenbank geladen: ${allPlayers.length} Spieler`, 'success');

        let player = allPlayers.find(p => String(p.id) === String(playerId));
        if (!player) player = allPlayers.find(p => parseInt(p.id) === parseInt(playerId));

        if (!player) {
            const errorMsg = `Spieler mit ID ${playerId} nicht gefunden!`;
            if (errorMessageEl) {
                errorMessageEl.textContent = errorMsg;
                errorMessageEl.style.display = 'block';
            } else {
                addDebug('errorMessageEl fehlt!', 'error');
            }
            if (loadingIndicatorEl) {
                loadingIndicatorEl.style.display = 'none';
            } else {
                addDebug('loadingIndicatorEl fehlt!', 'error');
            }
            addDebug(errorMsg, 'error');
            return;
        }

        addDebug(`Spieler gefunden: ${player.name} (ID: ${player.id})`, 'success');
        // Show Transfermarkt-Link button only if link is missing
        showTmLinkButtonIfNeeded(player);

        // Element-Checks für alle IDs, die per .style oder .textContent angesprochen werden
        function safeSet(id, prop, value) {
            const el = document.getElementById(id);
            addDebug(`safeSet: id=${id}, exists=${!!el}, prop=${prop}, value=${value}`);
            if (el) {
                if (prop === 'style' && typeof value === 'object' && value !== null) {
                    for (const key in value) {
                        el.style[key] = value[key];
                    }
                } else {
                    el[prop] = value;
                }
            } else {
                addDebug(`Element fehlt: ${id}`, 'error');
            }
        }

        safeSet('playerNameLink', 'textContent', player.name);
        safeSet('playerId', 'textContent', `ID: ${player.id}`);

        const playerNameLinkEl = document.getElementById('playerNameLink');
        if (player.data && player.data.transfermarktDoDe && player.data.transfermarktDoDe.link) {
            safeSet('playerNameLink', 'href', player.data.transfermarktDoDe.link);
        } else {
            safeSet('playerNameLink', 'href', "#");
            if (playerNameLinkEl) {
                playerNameLinkEl.style.cursor = "default";
                playerNameLinkEl.style.textDecoration = "none";
            }
        }

        const clubId = player.data?.verein || "0";

        const logoFileName = getLogoFileName(clubId);
        const logoUrl = DATA_URLS.logos + logoFileName;

        const clubLogoEl = document.getElementById('clubLogo');
        if (clubLogoEl) {
            const logoImg = document.createElement('img');
            logoImg.src = logoUrl;
            logoImg.alt = `Logo ${clubId}`;
            logoImg.onerror = () => {
                logoImg.src = DATA_URLS.logos + unbestimmt.png;
            };
            clubLogoEl.appendChild(logoImg);
        } else {
            addDebug('Element fehlt: clubLogo', 'error');
        }

        const statusData = player.data?.status || {};
        const statusIndicator = document.getElementById('statusIndicator');
        const statusInfoRow = document.querySelector('.info-row:has(#detailStatusInfo)');
        let statusTooltip = "";

        if (statusIndicator) {
            statusIndicator.textContent = getStatusIndicator(statusData.status);
            statusTooltip = `${getStatusDisplayName(statusData.status)}${statusData.grund ? ' - ' + statusData.grund : ''}${statusData.seit ? ' seit ' + statusData.seit : ''}`;
            statusIndicator.title = statusTooltip;

            // Zeige/Verstecke die Status-Info Zeile
            if (statusInfoRow) {
                statusInfoRow.style.display = statusData.status === 'AKTIV' ? 'none' : '';
                if (statusData.status !== 'AKTIV') {
                    document.getElementById('detailStatusInfo').textContent = statusTooltip;
                }
            }
        } else {
            addDebug('Element fehlt: statusIndicator', 'error');
        }

        const spielerDaten = player.data?.spielerDaten || {};
        const hatNebenpositionen = spielerDaten.nebenpositionen && spielerDaten.nebenpositionen.length > 0;

        const positionText = hatNebenpositionen ? spielerDaten.hauptposition + " (" + spielerDaten.nebenpositionen.join(", ") + ")" : spielerDaten.hauptposition || '-';

        safeSet('birthday', 'textContent', spielerDaten.geburtstag || '-');
        safeSet('age', 'textContent', calcAge(spielerDaten.geburtstag));
        safeSet('height', 'textContent', spielerDaten.groesse || '-');
        safeSet('nationality', 'textContent', spielerDaten.nationalitaet || '-');
        safeSet('position', 'textContent', positionText);
        safeSet('foot', 'textContent', spielerDaten.fuss || '-');
        safeSet('jerseyNumber', 'textContent', spielerDaten.trikotNummer || '-');

        updateLigainsiderRanking(player);

        safeSet('comunioPosition', 'textContent', player.data?.position || '-');
        safeSet('marketValue', 'textContent', formatCurrencyFull(player.data?.wert || 0));
        safeSet('realmarketValue', 'textContent', formatCurrencyFull(player.data?.realWert || 0));
        safeSet('pointsWithLastYear', 'textContent', player.data?.punkte + " (" + (player.data?.lastSeasonPoints || 0) + ")" || '-');
        safeSet('owner', 'textContent', globalOwnersMap.get(player.id) || 'Computer');
        safeSet('11desTages', 'textContent', (player.data?.elfDesSpieltages || 0) + " x");


        // für den Besitzer oben im Header
        safeSet('playerOwnerName', 'textContent', globalOwnersMap.get(player.id) || 'Computer');


        safeSet('detailStatus', 'textContent', getStatusDisplayName(statusData.status));
        if (statusData.status === 'AKTIV' || statusData.status === "aktiv") {
            statusTooltip = 'Aktiv';
        } else {
            if (statusData.grund !== undefined && statusData.grund !== '') {
                statusTooltip += ' - ' + statusData.grund;
            }

        }
        safeSet('detailStatusInfo', 'textContent', statusTooltip);
        const stats = player.data?.stats || {};
        safeSet('gamesPlayed', 'textContent', stats.playedGames || '-');
        safeSet('goals', 'textContent', stats.totalGoals || '-');
        safeSet('averageRating', 'textContent', stats.notenDurchschnitt || '-');
        safeSet('yellowCards', 'textContent', stats.gelbekarten || '-');
        safeSet('redCards', 'textContent', stats.rotekarten || '-');

        displayAttributes(player);
        displayLigainsiderPanel(player);

        safeSet('loadingIndicator', 'style', { display: 'none' });
        safeSet('playerContent', 'style', { display: 'block' });
        // Da .style ein Objekt ist, setze ich display direkt, falls das Element existiert
        const loadingIndicatorEl2 = document.getElementById('loadingIndicator');
        if (loadingIndicatorEl2) loadingIndicatorEl2.style.display = 'none';
        else addDebug('Element fehlt: loadingIndicator', 'error');
        const playerContentEl2 = document.getElementById('playerContent');
        if (playerContentEl2) playerContentEl2.style.display = 'block';
        else addDebug('Element fehlt: playerContent', 'error');

        setupAccordion();

        displayRivals(player, allPlayers);
        displayMarketValue(player);
        displayPointsHistory(player);

        addDebug('Spielerdaten vollständig geladen und angezeigt', 'success');

    } catch (error) {
        const errorMsg = `Fehler beim Laden der Spielerdaten: ${error.message}`;
        addDebug(`Fehler-Element-Check: errorMessageEl=${!!errorMessageEl}, loadingIndicatorEl=${!!loadingIndicatorEl}, playerContentEl=${!!playerContentEl}`, 'error');
        if (errorMessageEl) {
            errorMessageEl.textContent = errorMsg;
            errorMessageEl.style.display = 'block';
        } else {
            addDebug('errorMessageEl fehlt!', 'error');
        }
        if (loadingIndicatorEl) {
            loadingIndicatorEl.style.display = 'none';
        } else {
            addDebug('loadingIndicatorEl fehlt!', 'error');
        }
        if (playerContentEl) {
            playerContentEl.style.display = 'none';
        } else {
            addDebug('playerContentEl fehlt!', 'error');
        }
        addDebug(errorMsg, 'error');
    }

    // Transfermarkt-Link Button & Modal logic
    const tmBtn = document.getElementById('tm-link-btn');
    const tmModal = document.getElementById('tm-link-modal');
    const tmModalClose = document.getElementById('tm-link-modal-close');
    const tmInput = document.getElementById('tm-link-input');
    const tmSave = document.getElementById('tm-link-save');
    const tmMsg = document.getElementById('tm-link-modal-msg');
    let currentPlayerId = null;

    // Show Transfermarkt-Link button only if link is missing
    function showTmLinkButtonIfNeeded(player) {
        const tmBtn = document.getElementById('tm-link-btn');
        if (!tmBtn) return;
        const hasLink = player.data && player.data.transfermarktDoDe && player.data.transfermarktDoDe.link;
        tmBtn.style.display = hasLink ? 'none' : 'block';
    }

    tmBtn.addEventListener('click', () => {
        tmModal.classList.add('open');
        tmInput.value = '';
        tmMsg.textContent = '';
    });
    tmModalClose.addEventListener('click', () => {
        tmModal.classList.remove('open');
    });
    tmModal.addEventListener('click', (e) => {
        if (e.target === tmModal) tmModal.classList.remove('open');
    });
    tmSave.addEventListener('click', async () => {
        const link = tmInput.value.trim();
        if (!link.match(/^https?:\/\/www\.transfermarkt\.de\//)) {
            tmMsg.textContent = 'Bitte einen gültigen Transfermarkt-Link einfügen.';
            return;
        }
        // Extract Transfermarkt ID from link
        const idMatch = link.match(/spieler\/(\d+)/);
        let idText = '';
        if (idMatch && idMatch[1]) {
            idText = `\n  "id": ${idMatch[1]}`;
        }
        tmMsg.textContent = 'Speichern...';
        try {
            tmMsg.innerHTML = 'Automatisches Speichern ist nicht möglich.<br>Bitte kopiere folgenden JSON-Snippet und füge ihn manuell in die <b>SpielerdatenbankNeutralJson.txt</b> ein:<br><br>' +
                `<pre style='background:#f9f9f9;padding:8px;border-radius:6px;'>"transfermarktDoDe": {\n  "link": "${link}"${idText}\n}</pre>`;
        } catch (err) {
            tmMsg.textContent = 'Fehler beim Speichern: ' + err.message;
        }
    });

    // Set currentPlayerId for modal
    // Use already extracted playerId
    currentPlayerId = playerId;
});


async function displayAttributes(player) {
    const container = document.getElementById('attributesList');
    const card = document.getElementById('attributesCard');
    if (!container) return;
    container.innerHTML = '';
    let found = false;

    const attrs = player.data?.attribute;
    if (attrs && typeof attrs === 'object') {
        Object.keys(attrs).forEach(key => {
            if (key === 'ligainsiderRanking') return; // Separates Panel

            const value = attrs[key];
            // Prüfen, ob überhaupt Inhalte da sind
            if (
                !(Array.isArray(value) && value.length > 0) &&
                typeof value !== 'string' &&
                typeof value !== 'number' &&
                typeof value !== 'boolean'
            ) return;

            found = true;

            // Accordion Item erstellen
            const accordionItem = document.createElement('div');
            accordionItem.className = 'accordion-item';

            // Header/Button – standardmäßig "offen"
            const header = document.createElement('button');
            header.className = 'accordion-header';
            header.textContent = key;
            header.setAttribute('aria-expanded', 'true');
            header.type = 'button';

            // Content-Div – standardmäßig sichtbar
            const content = document.createElement('div');
            content.className = 'accordion-content';
            content.style.display = 'block';

            // Kategoriewerte als Tags
            if (Array.isArray(value)) {
                value.forEach(item => {
                    const span = document.createElement('span');
                    span.className = 'attribute-tag';
                    span.textContent = item;
                    content.appendChild(span);
                });
            } else {
                const span = document.createElement('span');
                span.className = 'attribute-tag';
                span.textContent = String(value);
                content.appendChild(span);
            }

            // TOGGLE-Event: Klick schließt/öffnet Panel
            header.addEventListener('click', () => {
                const isExpanded = header.getAttribute('aria-expanded') === 'true';
                header.setAttribute('aria-expanded', isExpanded ? 'false' : 'true');
                content.style.display = isExpanded ? 'none' : 'block';
            });

            accordionItem.appendChild(header);
            accordionItem.appendChild(content);
            container.appendChild(accordionItem);
        });
    }

    if (card) {
        card.style.display = found ? '' : 'none';
    } else {
        addDebug('Element fehlt: attributesCard', 'error');
    }
    addDebug(`Attribute angezeigt: ${found ? 'Ja' : 'Keine'}`, found ? 'success' : 'info');
}


function calcAge(birthday) {
    if (!birthday || birthday === 'N/A') return '-';
    try {
        const birthDate = new Date(birthday.split('.').reverse().join('-'));
        const diff = Date.now() - birthDate.getTime();
        const ageDate = new Date(diff);
        return Math.abs(ageDate.getUTCFullYear() - 1970) + ' Jahre';
    } catch {
        return '-';
    }

}
