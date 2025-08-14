 function getLigainsiderRankingObj(player) {
            if (
                !player ||
                !player.data ||
                typeof player.data.attribute !== 'object' ||
                player.data.attribute === null
            ) {
                return null;
            }

            // Attributobjekt
            const attrs = player.data.attribute;

            // Da attrs ein Objekt mit Keys wie ComAnalytics, ligainsiderRanking, etc. ist,
            // genügt direkter Zugriff auf attrs.ligainsiderRanking, wenn vorhanden.
            if (
                attrs.ligainsiderRanking &&
                typeof attrs.ligainsiderRanking === 'object' &&
                attrs.ligainsiderRanking !== null
            ) {
                return attrs.ligainsiderRanking;
            }

            // Falls für irgendeinen Grund ligainsiderRanking verschachtelt ist:
            for (const key in attrs) {
                const attr = attrs[key];
                if (
                    attr &&
                    typeof attr === 'object' &&
                    !Array.isArray(attr) &&
                    attr.ligainsiderRanking &&
                    typeof attr.ligainsiderRanking === 'object' &&
                    attr.ligainsiderRanking !== null
                ) {
                    return attr.ligainsiderRanking;
                }
            }

            return null;
        }



        function displayLigainsiderPanel(player) {
            const panel = document.getElementById('ligainsiderPanel');
            const content = document.getElementById('ligainsiderPanelContent');
            if (!panel || !content) {
                addDebug('Element fehlt: ligainsiderPanel oder ligainsiderPanelContent', 'error');
                return;
            }
            // Direkter Zugriff auf das Objekt!
            const rankingObj = player.data?.attribute?.ligainsiderRanking;
            if (!rankingObj) {
                panel.style.display = 'none';
                addDebug('LigainsiderRanking nicht vorhanden, Panel ausgeblendet.', 'info');
                return;
            }
            content.innerHTML = `
        <div class="info-row">
            <span class="info-label">Rang:</span>
            <span class="info-value">${rankingObj.rang ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Punkte:</span>
            <span class="info-value">${rankingObj.punkte ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Einsätze bewertet:</span>
            <span class="info-value">${rankingObj.einsaetzeBewertet ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Durchschnittsnote:</span>
            <span class="info-value">${rankingObj.durchschnittsnote ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Durchschnittsminuten:</span>
            <span class="info-value">${rankingObj.durchschnittsminuten ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Durchschnittspunkte:</span>
            <span class="info-value">${rankingObj.durchschnittspunkte ?? '-'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">letztes Update:</span>
            <span class="info-value">${rankingObj.lastUpdate?.toString() ?? '-'}</span>
        </div>
    `;
            panel.style.display = '';
            addDebug('LigainsiderPanel angezeigt.', 'info');
        }




        function updateLigainsiderRanking(player) {
            const playerInfoHeader = document.getElementById('playerInfoHeader');
            if (playerInfoHeader) {
                playerInfoHeader.innerHTML = '📋 Spieler-Informationen';
            }
        }