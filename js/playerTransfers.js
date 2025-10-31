document.addEventListener("DOMContentLoaded", () => {
    const urlParams = new URLSearchParams(window.location.search);
    const playerId = urlParams.get("id");

    if (!playerId) return;

    fetch(DATA_URLS.news)
        .then(res => res.json())
        .then(newsData => {
            const transfers = [];

            newsData.newsDB.forEach(day => {
                if (!Array.isArray(day.news)) return;
                day.news.forEach(entry => {
                    if (entry.art === "TRANSFER" && entry.playerId === playerId) {
                        try {
                            const tData = JSON.parse(entry.text);
                            transfers.push({
                                date: entry.date,
                                seller: tData.seller,
                                buyer: tData.buyer,
                                price: tData.price,
                                value: tData.playerValue
                            });
                        } catch (err) {
                            console.error("Fehler beim Parsen eines Transfers:", err);
                        }
                    }
                });
            });

            renderTransfersTable(transfers);
        })
        .catch(err => console.error("Fehler beim Laden der News:", err));

    function renderTransfersTable(transfers) {
        const tbody = document.querySelector("#transfersTable tbody");
        tbody.innerHTML = "";

        if (transfers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5">Keine Transferdaten verfügbar</td></tr>`;
            return;
        }

        // Neueste oben
        transfers.sort((a,b) => new Date(b.date.split('.').reverse().join('-')) - new Date(a.date.split('.').reverse().join('-')));

        transfers.forEach(tr => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${tr.date}</td>
                <td>${tr.seller}</td>
                <td>${tr.buyer}</td>
                <td>${tr.price.toLocaleString("de-DE")}</td>
                <td>${tr.value.toLocaleString("de-DE")}</td>
            `;
            tbody.appendChild(row);
        });
    }
});
