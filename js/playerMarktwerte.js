async function loadPlayerMarketValues(playerId) {
    addDebug(`Lade Marktwerte für Spieler ${playerId}`, 'info');
    try {
        const resp = await fetch(`https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/main/data/MarktwerteJson.txt`);
        const data = await resp.json();
        const entry = data.find(e => String(e.id) === String(playerId));
        if (!entry || !entry.data || !entry.data.normal) {
            addDebug(`Keine Marktwerte für Spieler ${playerId} gefunden`, 'warn');
            return [];
        }
        addDebug(`Marktwerte geladen: ${entry.data.normal.length} Einträge`, 'success');
        return entry.data.normal;
    } catch (error) {
        addDebug(`Fehler beim Laden der Marktwerte: ${error.message}`, 'error');
        return [];
    }
}

// Hilfsfunktion: Erstellt alle Tage als Strings ("dd.mm.yyyy") zwischen zwei Daten
function getDayStringsOfMonth(year, month) {
    const result = [];
    // Monat: 1-basiert
    const date = new Date(year, month - 1, 1);
    while (date.getMonth() === month - 1) {
        // Zwei Ziffern für Tag/Monat
        const day = String(date.getDate()).padStart(2, '0');
        const mon = String(date.getMonth() + 1).padStart(2, '0');
        const yearStr = String(date.getFullYear());
        result.push(`${day}.${mon}.${yearStr}`);
        date.setDate(date.getDate() + 1);
    }
    return result;
}

/**
 * Kombinierte Marktwert-Historie eines Spielers
 * – lückenlose Anzeige vom ersten gespeicherten Tag bis HEUTE
 * – lineare Interpolation übergrenzen hinweg
 * – interpolierte Werte rot, HEUTE/Folgetage schwarz falls Wert = letzter Originalwert
 * – nur der aktuellste Monat ist anfänglich aufgeklappt
 *
 * @param {Object} player  Objekt mit mindestens player.id
 */
async function displayMarketValue(player) {

    let currentNumDays = 10; // Standard-Anzeige
    let chartInstance = null; // Zum Löschen des alten Charts vor Neuzeichnen


    /* ─── 1. DOM-Zielbox prüfen ─────────────────────────── */
    const box = document.getElementById('marketValueChart');
    if (!box) return;

    /* ─── 2. Daten laden ────────────────────────────────── */
    const raw = await loadPlayerMarketValues(player.id);
    if (!raw?.length) {
        box.innerHTML = '<p>Keine Marktwertdaten verfügbar.</p>';
        return;
    }

    /* ─── 3. Hilfsfunktionen ────────────────────────────── */
    const MON = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
        'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];

    // ➜ Datum in DD.MM.YYYY
    const fmt = d => {
        const dd = String(d.getDate()).padStart(2, '0');
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        return `${dd}.${mm}.${d.getFullYear()}`;
    };

    // ➜ String → lokales Date-Objekt (0:00 Uhr)
    const parse = s => {
        const [d, m, y] = s.split('.');
        return new Date(y, m - 1, d);          // !!! KEIN ISO-String
    };

    // ➜ Alle Tage zwischen a … z (inkl.)
    const span = (a, z) => {
        const r = [], c = new Date(a);
        c.setHours(0, 0, 0, 0);
        z = new Date(z); z.setHours(0, 0, 0, 0);
        while (c <= z) {
            r.push(fmt(c));
            c.setDate(c.getDate() + 1);
        }
        return r;
    };

    /* ─── 4. Originalwerte als Map ──────────────────────── */
    const orig = {}; raw.forEach(e => orig[e.date] = e.wert);

    const dates = raw.map(e => parse(e.date));
    const first = new Date(Math.min(...dates)); first.setHours(0, 0, 0, 0);

    const today = new Date(); today.setHours(0, 0, 0, 0);          // immer anzeigen

    const endDay = today;                                        // IMMER bis heute

    const allDays = span(first, endDay);                         // lückenlos

    /* ─── 5. Stützpunkte für lineare Interpolation ───────── */
    const points = allDays
        .map((d, i) => orig[d] != null ? { i, val: orig[d] } : null)
        .filter(Boolean);                                          // [{i,val},…]

    /* ─── 6. Tagesreihe erstellen ───────────────────────── */
    const rows = [];
    for (let i = 0; i < allDays.length; i++) {
        const d = allDays[i];

        // — Originalwert vorhanden
        if (orig[d] != null) {
            rows.push({ date: d, val: orig[d], red: false });
            continue;
        }

        // — Interpolation / Extrapolation
        const prev = [...points].reverse().find(p => p.i < i);
        const next = points.find(p => p.i > i);

        let v, red = true;
        if (prev && next) {                        // linear
            const t = (i - prev.i) / (next.i - prev.i);
            v = Math.round(prev.val + t * (next.val - prev.val));
        } else if (prev) {                         // nach letztem Original
            v = prev.val;
            red = false;
        } else if (next) {                         // vor erstem Original
            v = next.val;
            red = false;
        }

        // HEUTE schwarz, falls identisch mit letztem Originalwert
        if (d === fmt(today) && prev && v === prev.val) red = false;

        rows.push({ date: d, val: v, red });
    }

    /* ─── 7. Monate gruppieren + Durchschnitt ───────────── */
    const months = {};
    rows.forEach(r => {
        const [, m, y] = r.date.split('.');
        const key = `${y}-${m}`;
        (months[key] ??= []).push(r);
    });

    const mAvg = {};
    Object.entries(months).forEach(([k, arr]) => {
        mAvg[k] = Math.round(arr.reduce((s, r) => s + r.val, 0) / arr.length);
    });

    const keys = Object.keys(months).sort((a, b) => b.localeCompare(a)); // Neu → Alt

    // --- NACH rows[]-Erstellung, VOR dem HTML-Aufbau:
    // Holt die letzten 10 (also die NEUESTEN) Werte aus rows, egal wie es sortiert ist:
    const last10 = rows.slice(-10); // Die letzten 10 Einträge, also die neuesten!

    // Wenn du sie im Chart von ALT (links) nach NEU (rechts) willst, lässt du .reverse() einfach weg:
    const datesLast10 = last10.map(r => r.date);
    const valuesLast10 = last10.map(r => r.val);
    // Falls du rows neu -> alt willst, dann nochmal: last10.reverse()



    // --- Chart anzeigen:
    function showMarketChart(days) {
        // Holt die letzten "days" Werte (ALT => NEU)
        const lastN = rows.slice(-days);
        const dates = lastN.map(r => r.date);
        const values = lastN.map(r => r.val);

        // Vorheriges Chart löschen, damit es nicht überlagert wird
        if (chartInstance) chartInstance.destroy();

        const ctx = document.getElementById('marketValueGraph').getContext('2d');
        chartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: dates,
                datasets: [{
                    label: 'Marktwert (€)',
                    data: values,
                    borderColor: '#3498db',
                    backgroundColor: 'rgba(52,152,219,0.14)',
                    pointRadius: 3,
                    fill: true,
                    tension: 0.18
                }]
            },
            options: {
                scales: {
                    x: { title: { display: true, text: 'Datum' } },
                    y: { title: { display: true, text: 'Marktwert (€)' }, beginAtZero: false }
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }


    // --- direkt im displayMarketValue aufrufen:
    showMarketChart(currentNumDays);

    // Button-Logik:
    const btn = document.getElementById('marketGraphToggle');
    if (btn) {
        btn.onclick = function () {
            currentNumDays = (currentNumDays === 10) ? 30 : 10;
            btn.textContent = (currentNumDays === 10) ? '30 Tage anzeigen' : '10 Tage anzeigen';
            showMarketChart(currentNumDays);
        }
    }



    /* ─── 8. HTML-Aufbau ────────────────────────────────── */
    const foot = `<div class="market-footnote" style="color:#b13737;font-size:.99em;margin-bottom:.7em">
* Rot markierte Werte wurden interpoliert (linear zwischen Originalwerten).</div>`;

    const hasRed = rows.some(r => r.red);
    let html = '';

    keys.forEach((k, idx) => {
        const [y, m] = k.split('-').map(Number);
        const title = `${MON[m - 1]} ${y}`;
        const open = idx === 0 ? '' : 'collapsed';

        const monthData = months[k].slice().sort((a, b) => parse(b.date) - parse(a.date)); // Neu → Alt
        let rowHtml = '';

        // monthData: absteigend (NEU → ALT) sortiert!
        for (let i = 0; i < monthData.length; i++) {
            const r = monthData[i];
            let trend = '';

            // Standardfall: Vergleich mit Vortag im monthData (innerhalb des Monats)
            if (i + 1 < monthData.length) {
                const prevVal = monthData[i + 1].val;
                if (r.val > prevVal) trend = 'up';
                else if (r.val < prevVal) trend = 'down';
                else trend = 'eq';
            } else {
                // Fall: Monatsanfang – Vergleich mit Vortag über rows, korrekt über Monatsgrenze!
                const thisIndex = rows.findIndex(row => row.date === r.date);
                if (thisIndex !== -1 && thisIndex + 1 < rows.length) {
                    const prevVal = rows[thisIndex + 1].val;
                    if (r.val > prevVal) trend = 'up';
                    else if (r.val < prevVal) trend = 'down';
                    else trend = 'eq';
                }
            }

            rowHtml += `<div class="market-list-row">
        <span class="market-list-date">${r.date}</span>
        <span class="market-list-value${r.red ? ' interpolated' : ''}" style="${r.red ? 'color:#d47171;font-weight:600;' : ''}">
            ${formatCurrencyFull(r.val)} ${unicodeTrend(trend)}
        </span>
    </div>`;
        }



        html += `<div class="market-month-block">
        <div class="market-month-header ${open}" data-month="${k}">
            ${title}
            <span class="market-month-avg">${avgIcon()} ${formatCurrencyShort(mAvg[k])}</span>
        </div>
        <div class="market-list ${open}">${rowHtml}</div>
    </div>`;
    });

    box.innerHTML = (hasRed ? foot : '') + html;
    /* ─── 9. Auf-/Zuklappen der Monatsblöcke ────────────── */



    box.querySelectorAll('.market-month-header').forEach(h => {
        const list = h.nextElementSibling;
        h.addEventListener('click', () => {
            h.classList.toggle('collapsed');
            list.classList.toggle('collapsed');
        });
    });

    addDebug('Marktwerte-Anzeige aktualisiert', 'success');
}

function unicodeTrend(trend) {
    if (trend === "up") return '<span class="trend-mini trend-up">&#9650;</span>';
    if (trend === "down") return '<span class="trend-mini trend-down">&#9660;</span>';
    if (trend === "eq") return '<span class="trend-mini trend-eq">&#8213;</span>';
    return "";
}

function avgIcon() {
    return `<svg class="market-avg-icon" viewBox="0 0 20 20"><circle cx="10" cy="10" r="9" fill="#b3c6e7" stroke="#3498db" stroke-width="2"/><text x="10" y="14" text-anchor="middle" font-size="10" fill="#3498db" font-family="Arial" font-weight="bold">Ø</text></svg>`;
}

function formatCurrencyFull(value) {
    if (!value || value === 0) return '-';
    return value.toLocaleString('de-DE') + ' €';
}

function formatCurrencyShort(value) {
    if (!value || value === 0) return '-';
    if (value >= 1_000_000) return (value / 1_000_000).toFixed(1).replace('.', ',').replace(',0', '') + ' Mio €';
    if (value >= 1_000) return (value / 1_000).toFixed(1).replace('.', ',').replace(',0', '') + ' Tsd €';
    return value + ' €';
}