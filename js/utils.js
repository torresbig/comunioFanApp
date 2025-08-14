/**
 * Sammlung von Hilfsfunktionen
 */

/**
 * Debug-Ausgabefunktion, hier über die Konsole.
 * Du kannst sie anpassen, um Debug-Informationen z.B. in die Seite zu schreiben.
 * @param {string} message Text für Debug-Zwecke
 */
 function addDebug(message) {
            const debugDiv = document.getElementById('floatingDebugContent');
            debugDiv.innerHTML += `<div>${new Date().toLocaleTimeString()}: ${message}</div>`;
            debugDiv.scrollTop = debugDiv.scrollHeight;
        }

/**
 * Funktion zum Umschalten der Sichtbarkeit der News-Liste.
 * Wird z.B. vom Icon-Click-Event benutzt.
 */
function toggleNewsList() {
  const list = document.getElementById('news-list');
  const icon = document.getElementById('news-toggle-icon');
  if (!list || !icon) {
    addDebug('toggleNewsList: news-list oder news-toggle-icon nicht gefunden!');
    return;
  }
  if (list.style.display === 'none') {
    list.style.display = '';
    icon.style.transform = 'rotate(0deg)';
  } else {
    list.style.display = 'none';
    icon.style.transform = 'rotate(-90deg)';
  }
}

  function getLokalOderGitURL(websiteUrl, lokalUrl) {
    const currentUrl = window.location.href;
    if (currentUrl.includes('https://${GITHUB_USER}.github.io/')) {
      return websiteUrl;
    } else {
      return lokalUrl;
    }
  } 
