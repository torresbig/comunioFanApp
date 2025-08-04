document.addEventListener('DOMContentLoaded', function () {
  const hamburgerButton = document.getElementById('hamburgerButton');
  const hamburgerDropdown = document.getElementById('hamburgerDropdown');
  const menuTarnItem = document.getElementById('menuTarnItem');

  hamburgerButton.addEventListener('click', function (e) {
    e.stopPropagation();
    this.classList.toggle('active');
    hamburgerDropdown.classList.toggle('show');
  });  // ← schließt addEventListener(click)

  // Menü-Links dynamisch zuweisen
  const linkIndex = document.getElementById('linkIndex');
  const linkTransfermarkt = document.getElementById('linkTransfermarkt');
  const linkUseruebersicht = document.getElementById('linkUseruebersicht');

  if (linkIndex) {
    linkIndex.href = getLokalOderGitURL(WEBSITE_URLS.indexUrl, "index.html");
  }  

  if (linkTransfermarkt) {
    linkTransfermarkt.href = getLokalOderGitURL(WEBSITE_URLS.transfermarktUrl, "transfermarkt.html");
  } 

  if (linkUseruebersicht) {
    linkUseruebersicht.href = getLokalOderGitURL(WEBSITE_URLS.useruebersichtUrl, "useruebersicht.html");
  }  

  // Geheimer Link-Mechanismus für 5 Klicks
let tarnClick = 0, tarnTimeout = null;    
  // Geheimer Link-Mechanismus für 5 Klicks
if (menuTarnItem) {
  let tarnClick = 0, tarnTimeout = null;
  menuTarnItem.addEventListener('click', function (e) {
    tarnClick++;
    if (tarnTimeout) clearTimeout(tarnTimeout);
    tarnTimeout = setTimeout(() => { tarnClick = 0; }, 3000);
    if (tarnClick === 5) {
      window.location.href = getLokalOderGitURL(WEBSITE_URLS.kontostaendeUrl, "kontostaende.html");
    }
  });
}


  function getLokalOderGitURL(websiteUrl, lokalUrl) {
    const currentUrl = window.location.href;
    if (currentUrl.includes('htmlpreview.github.io')) {
      return websiteUrl;
    } else {
      return lokalUrl;
    }
  } 

  document.addEventListener('click', function (e) {
    if (!e.target.closest('.hamburger-menu')) {
      hamburgerButton.classList.remove('active');
      hamburgerDropdown.classList.remove('show');
    }
  }); 

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      hamburgerButton.classList.remove('active');
      hamburgerDropdown.classList.remove('show');
    }
  });  // ← schließt addEventListener(keydown)

});  // ← schließt DOMContentLoaded
