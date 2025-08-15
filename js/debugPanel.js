// js/debugPanel.js

// Fügt eine Debug-Nachricht hinzu
function addDebug(msg) {
  const debugContent = document.getElementById('floatingDebugContent');
  if (!debugContent) return;
  const entry = document.createElement('div');
  entry.textContent = `${new Date().toLocaleTimeString()}: ${msg}`;
  debugContent.appendChild(entry);
  debugContent.scrollTop = debugContent.scrollHeight;
}

// Debug-Button & Panel initialisieren
function initDebugPanel() {
  const debugBtn = document.getElementById('floatingDebugBtn');
  const debugPanel = document.getElementById('floatingDebugPanel');
  const debugClose = document.getElementById('floatingDebugPanelClose');
  if (!debugBtn || !debugPanel || !debugClose) return;

  debugBtn.addEventListener('click', () => {
    debugPanel.classList.toggle('open');
    addDebug("Debug-Panel geöffnet/geschlossen");
  });

  debugClose.addEventListener('click', () => {
    debugPanel.classList.remove('open');
    addDebug("Debug-Panel geschlossen");
  });

  addDebug("Debug-Panel initialisiert.");
}

document.addEventListener('DOMContentLoaded', () => {
  initDebugPanel();
  addDebug("Seite geladen.");
});



