document.addEventListener('DOMContentLoaded', function() {
    const hamburgerButton = document.getElementById('hamburgerButton');
    const hamburgerDropdown = document.getElementById('hamburgerDropdown');
    const menuTarnItem = document.getElementById('menuTarnItem');
    hamburgerButton.addEventListener('click', function(e) {
        e.stopPropagation();
        this.classList.toggle('active');
        hamburgerDropdown.classList.toggle('show');
    });

    // Geheimer Link Mechanismus für 5 Klicks, OHNE visuelle Änderung (auch KEINE Veränderung durch JS am style!):
    let tarnClick = 0, tarnTimeout = null;
    menuTarnItem.addEventListener('click', function(e) {
        // Kein visuelles Feedback, kein scale, keine Farbe, kein Cursor!
        // Zähler nur aufbauen
        tarnClick++;
        if(tarnTimeout){ clearTimeout(tarnTimeout);}
        tarnTimeout = setTimeout(()=>{tarnClick = 0;}, 3000);
        if (tarnClick === 5){
           window.location.href = 'https://htmlpreview.github.io/?https://github.com/torresbig/comunioFanApp/blob/main/kontostaende.html';
        }
    });

    document.addEventListener('click', function(e) {
        if (!e.target.closest('.hamburger-menu')) {
            hamburgerButton.classList.remove('active');
            hamburgerDropdown.classList.remove('show');
        }
    });
    document.addEventListener('keydown', function(e){
        if(e.key === 'Escape'){
            hamburgerButton.classList.remove('active');
            hamburgerDropdown.classList.remove('show');
        }
    });
});

