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
                // Prüfe ob es bereits einen Eintrag für dieses Datum und diese Art gibt
                if (!grouped[news.art]) {
                    grouped[news.art] = [];
                }
                // Füge nur hinzu wenn nicht bereits vorhanden
                const isDuplicate = grouped[news.art].some(existingNews => 
                    existingNews.text === news.text && 
                    existingNews.date === news.date
                );
                if (!isDuplicate) {
                    grouped[news.art].push(news);
                }
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
                        else if (art === 'USERPOINTS') {
                            const obj = JSON.parse(news.text);
                            text = `<div class="player-entry">
                                <b style="color:#00f;">${obj.userName}</b> - 
                                <span class="points">${obj.gamedayPoints} Pkt.</span>
                                (Gesamt: ${obj.totalPoints} Pkt.)
                            </div>`;
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

                                if (news.text.includes(`AKTIV`)) {
                                    text = `🟢 ${linkPlayer(news.playerId, playerName)} ist ${match[2]} ${statusDisplay}`;
                                } else if (news.text.includes(`NICHT_IN_LIGA`)) {
                                    text = `❌ ${linkPlayer(news.playerId, playerName)} ist ${match[2]} ${statusDisplay}`;
                                } else {
                                    text = `🔴 ${linkPlayer(news.playerId, playerName)} ist ${match[2]} ${statusDisplay}`;
                                }
                            } else {
                                text = news.text;
                            }


                        } else if (art === 'VEREINSWECHSEL') {
                            try {
                                const obj = JSON.parse(news.text); // falls JSON
                                const pid = obj.playerId || news.playerId || null;
                                text = `${linkPlayer(pid, obj.playerName)} wechselt von <b>${clubsMap.get(obj.oldClub)}</b> zu <b>${clubsMap.get(obj.newClub)}</b>`;
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
                        } else if (art === 'POSITIONSWECHSEL') {
                            try {
                                const obj = JSON.parse(news.text); // falls JSON
                                const pid = obj.playerId || news.playerId || null;
                                text = `${linkPlayer(pid, obj.playerName)} wechselt von <b>${obj.oldPos}</b> zu <b>${obj.newPos}</b>`;
                            } catch (e) {

                                text = news.text;

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
                        }else if (art === 'ELFDESTAGES') {
                            try {
                                const players = JSON.parse(news.text);
                                
                                // Sortiere nach Position
                                const positionOrder = {
                                    'keeper': 1,
                                    'defender': 2,
                                    'midfielder': 3,
                                    'striker': 4
                                };
                                
                                players.sort((a, b) => positionOrder[a.position] - positionOrder[b.position]);

                                // Eindeutige ID für diesen Container
                                const containerId = `elf-${day.date}-${Math.random().toString(36).substr(2, 9)}`;
                                
                                text = `<div class="top11-container" id="${containerId}" style="text-align: left; padding: 10px;">
                                    <h3 style="margin-bottom: 10px;">🏆 Elf des Tages</h3>`;
                                
                                let currentPosition = '';
                                players.forEach((player, index) => {
                                    if (currentPosition !== player.position) {
                                        if (currentPosition !== '') text += '</div>';
                                        currentPosition = player.position;
                                        const positionIcons = {
                                            'keeper': '🧤',
                                            'defender': '🛡️',
                                            'midfielder': '⚡',
                                            'striker': '⚽'
                                        };
                                        const positionNames = {
                                            'keeper': 'Torwart',
                                            'defender': 'Abwehr',
                                            'midfielder': 'Mittelfeld',
                                            'striker': 'Sturm'
                                        };
                                        text += `<div class="position-group" style="margin-bottom: 5px;">
                                            <h4 style="margin: 5px 0;">${positionIcons[player.position]} ${positionNames[player.position]}</h4>`;
                                    }
                                    
                                    text += `<div class="player-entry">
                                        ${linkPlayer(player.playerId, player.playerName)} - 
                                        <span class="points">${player.punkte} Pkt.</span> 
                                        (<span class="owner-name" data-owner-id="${player.owner}">${player.owner || 'Computer'}</span>)
                                    </div>`;
                                });
                                text += '</div></div>';

                                // Verzögere die Namen-Aktualisierung bis nach dem Rendern
                                requestAnimationFrame(() => {
                                    const ownerPromises = players.map(player => getUserString(player.owner));
                                    Promise.all(ownerPromises).then(ownerNames => {
                                        const container = document.getElementById(containerId);
                                        if (container) {
                                            const ownerSpans = container.querySelectorAll('.owner-name');
                                            ownerSpans.forEach((span, index) => {
                                                span.textContent = ownerNames[index] || 'Computer';
                                            });
                                        }
                                    });
                                });
                            } catch (e) {
                                console.error("Fehler beim Verarbeiten der Elf des Tages:", e);
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