// Helpers and renderer for the user info card
function formatCurrencyEUR(value) {
  if (value === null || value === undefined || isNaN(Number(value))) return '—';
  return '€' + Number(value).toLocaleString('de-DE');
}

function buildPointsSparkline(punkteHistorie) {
  if (!punkteHistorie || typeof punkteHistorie !== 'object') return '';
  const keys = Object.keys(punkteHistorie).sort((a,b)=> Number(a)-Number(b));
  const vals = keys.map(k=> Number(punkteHistorie[k]) || 0);
  if (vals.length === 0) return '';
  const w = 140, h = 40, pad = 6;
  const min = Math.min(...vals);
  const max = Math.max(...vals);
  const range = Math.max(1, max - min);
  const step = (w - pad*2) / Math.max(1, vals.length - 1);
  const pointsArr = vals.map((v,i)=> {
    const x = Math.round(pad + i * step);
    const y = Math.round(pad + (1 - (v - min) / range) * (h - pad*2));
    return {x,y,v,round: keys[i]};
  });
  const points = pointsArr.map(p=> `${p.x},${p.y}`).join(' ');
  const areaPath = `M ${pad} ${h-pad} ` + pointsArr.map(p=> `L ${p.x} ${p.y}`).join(' ') + ` L ${w-pad} ${h-pad} Z`;
  const title = vals.join(', ');

  // build circles for each point to allow interaction
  const circles = pointsArr.map((p, idx) => `<circle data-idx="${idx}" data-value="${p.v}" data-round="${p.round}" cx="${p.x}" cy="${p.y}" r="3" fill="#1f6feb" opacity="0.95"></circle>`).join('');
  const labels = pointsArr.map((p, idx) => `<text class="sparkline-point-label" x="${p.x}" y="${p.y - 8}" text-anchor="middle">${p.v}</text>`).join('');

  const svg = `<svg class="user-sparkline" viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Punkteentwicklung: ${title}">
    <path d="${areaPath}" fill="rgba(31,111,235,0.08)" stroke="none" />
    <polyline points="${points}" fill="none" stroke="#1f6feb" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
    ${circles}
    ${labels}
    <title>Punkteentwicklung: ${title}</title>
  </svg>`;
  return svg;
}

// attach interactions (tooltip, hover) to the sparkline inside container
function attachSparklineInteractions(container) {
  const svg = container.querySelector('.user-sparkline');
  if (!svg) return;
  const wrapper = container.closest('.user-info-card') || document;
  let tooltip = container.querySelector('.user-sparkline-tooltip');
  if (!tooltip) {
    tooltip = document.createElement('div');
    tooltip.className = 'user-sparkline-tooltip';
    container.querySelector('.user-sparkline-container')?.appendChild(tooltip);
  }

  const circles = Array.from(svg.querySelectorAll('circle'));
  circles.forEach(c => {
    c.addEventListener('mouseenter', (ev) => {
      const v = c.getAttribute('data-value');
      const r = c.getAttribute('data-round');
      tooltip.textContent = `Runde ${r}: ${v}`;
      tooltip.style.display = 'block';
      c.setAttribute('r', 5);
    });
    c.addEventListener('mousemove', (ev) => {
      const rect = container.querySelector('.user-sparkline-container').getBoundingClientRect();
      const x = ev.clientX - rect.left;
      const y = ev.clientY - rect.top;
      tooltip.style.left = x + 'px';
      tooltip.style.top = (y - 10) + 'px';
    });
    c.addEventListener('mouseleave', (ev) => {
      tooltip.style.display = 'none';
      c.setAttribute('r', 3);
    });
  });

  // mobile toggle
  const wrapperNode = container.closest('.user-sparkline-wrapper');
  if (wrapperNode) {
    const toggle = wrapperNode.querySelector('.sparkline-toggle');
    if (toggle) {
      toggle.addEventListener('click', () => {
        wrapperNode.classList.toggle('expanded');
      });
    }
  }
}

function avatarGradient(seed) {
  const palette = ['#1f6feb','#7cc3ff','#ff7a7a','#ffb86b','#a78bfa','#34d399','#f97316','#06b6d4'];
  const s = String(seed || 'x');
  let h = 0;
  for (let i=0;i<s.length;i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  const c1 = palette[h % palette.length];
  const c2 = palette[(h + 3) % palette.length];
  return `background: linear-gradient(135deg, ${c1}, ${c2});`;
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str).replace(/[&<>\"'`]/g, function (s) {
    return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;' })[s];
  });
}

function renderUserInfo(userId) {
  const container = document.getElementById('userInfo');
  if (!container) return;
  if (!usersData || !Array.isArray(usersData)) {
    container.style.display = 'none';
    return;
  }
  const entry = usersData.find(u => u.user && String(u.user.id) === String(userId));
  if (!entry) {
    container.style.display = 'none';
    return;
  }

  const u = entry.user || {};
  const name = u.firstName ? (u.firstName + (u.lastName ? (' ' + u.lastName) : '')) : (u.name || u.loginName || 'Unbekannt');
  const login = u.loginName || '';
  const initials = ((u.firstName ? u.firstName[0] : '') + (u.lastName ? u.lastName[0] : '')).toUpperCase() || (login[0] || 'U').toUpperCase();

  const punkte = entry.punkte ?? '—';
  const rank = entry.rank ?? '—';
  const mitgliedSeit = entry.mitgliedSeit ?? '—';
  const guthaben = entry.guthaben ?? null;
  const teamValue = entry.teamValue ?? null;
  const lastPoints = entry.lastPoints ?? '—';
  const tactic = entry.tactic || entry.formation?.tactic || '—';

  const spark = buildPointsSparkline(entry.punkteHistorie);
  const avatarStyle = avatarGradient(u.id || u.loginName || name);

  container.innerHTML = `
    <div class="user-info-avatar" style="${avatarStyle}">${escapeHtml(initials)}</div>
    <div class="user-info-main">
      <div class="user-info-left">
        <div class="user-info-name">${escapeHtml(name)}</div>
        ${ login ? `<div class="user-info-login">@${escapeHtml(login)}</div>` : '' }
        <div class="user-badges">
          <div class="user-badge">Rang: ${escapeHtml(rank)}</div>
          <div class="user-badge">Taktik: ${escapeHtml(tactic)}</div>
        </div>
      </div>

      <div class="user-stats-group">
        <div class="user-stats-inner">
          <div class="user-stats-col">
            <div class="user-info-section-label">Leistung</div>
            <div class="user-stats-row">
              <div style="min-width:160px">
                <div class="user-info-label">Punkte (letzte)</div>
                <div class="user-stats-big">${escapeHtml(punkte)} ${lastPoints && lastPoints !== '—' ? `(<span class="user-info-value">${escapeHtml(lastPoints)}</span>)` : ''}</div>
              </div>
            </div>
          </div>
          <div class="section-separator" aria-hidden="true"></div>
          <div class="user-stats-col">
            <div class="user-info-section-label">Finanzen</div>
            <div class="user-stats-row">
              <div>
                <div class="user-info-label">Guthaben</div>
                <div class="user-info-value">${formatCurrencyEUR(guthaben)}</div>
              </div>
              <div>
                <div class="user-info-label">Teamwert</div>
                <div class="user-info-value">${formatCurrencyEUR(teamValue)}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="user-sparkline-wrapper">
        <button class="sparkline-toggle">Punkte Verlauf ▾</button>
        <div class="user-sparkline-container">${spark}<div class="user-sparkline-tooltip"></div></div>
      </div>
    </div>
  `;
  container.style.display = '';
  // attach interactive behaviour for sparkline (tooltip, toggle)
  try { attachSparklineInteractions(container); } catch (e) { console.warn('sparkline attach error', e); }
}
