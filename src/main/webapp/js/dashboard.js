(function () {
    const API = 'api/dashboard';
    const POLL_MS = 4000;
    let lineChart = null;
    let donutChart = null;

    const COLORS = ['#3b82f6', '#22a060', '#f59e0b', '#7c5ce0', '#d94b55', '#068da0'];
    const COLORS_COLORBLIND = ['#0072b2', '#e69f00', '#7b61a8', '#009e9a', '#6f7680', '#56b4e9'];

    function esc(s) {
        if (!s) return '';
        const d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    }

    function flash(el) {
        if (!el) return;
        el.style.transition = 'color 0.3s';
        el.style.color = '#4ade80';
        setTimeout(() => { el.style.color = ''; }, 600);
    }

    function updateKpis(kpis) {
        const map = {
            'kpi-productos': kpis.totalProductos,
            'kpi-unidades': kpis.unidadesStock,
            'kpi-valor': '$' + kpis.valorTotalK + 'K',
            'kpi-bajo': kpis.bajoStock,
            'sum-criticos': kpis.criticos,
            'sum-bajos': kpis.bajos,
            'sum-normales': kpis.normales
        };
        for (const [id, val] of Object.entries(map)) {
            const el = document.getElementById(id);
            if (el && el.textContent !== String(val)) {
                el.textContent = val;
                flash(el);
            }
        }
    }

    function updateAlertBadge(count) {
        document.querySelectorAll('[data-alert-badge]').forEach(el => {
            el.textContent = count;
            el.style.display = count > 0 ? '' : 'none';
        });
        const pill = document.getElementById('top-alert-text');
        if (pill) pill.textContent = count + ' alertas médicas';
    }

    function renderMovimientos(list) {
        const container = document.getElementById('live-movimientos');
        if (!container || !list) return;
        container.innerHTML = list.map(m => {
            const iconCls = m.tipo === 'ENTRADA' ? 'entrada' : (m.tipo === 'SALIDA' ? 'salida' : 'ajuste');
            const arrow = m.tipo === 'ENTRADA' ? '↑' : (m.tipo === 'SALIDA' ? '↓' : '↔');
            const tipoVisible = m.tipo === 'ENTRADA' ? '↓ Entrada' : (m.tipo === 'SALIDA' ? '↑ Salida' : '↔ Ajuste');
            const qtyCls = m.tipo === 'ENTRADA' ? 'up' : 'down';
            return `<div class="sc-mov-item">
                <div class="sc-mov-icon ${iconCls}" aria-hidden="true">${arrow}</div>
                <div class="sc-mov-info">
                    <div class="sc-mov-name">${esc(m.producto)}</div>
                    <div class="sc-mov-desc"><strong>${tipoVisible}</strong> · ${esc(m.motivo)} · ${esc(m.usuario || '')}</div>
                </div>
                <div>
                    <div class="sc-mov-qty ${qtyCls}">${esc(m.cantidad)}</div>
                    <div class="sc-mov-stock">${m.stockAntes} → ${m.stockDespues}</div>
                </div>
                <div class="sc-mov-date">${esc(m.fecha)}</div>
            </div>`;
        }).join('');
    }

    function renderAlertasStock(list) {
        const container = document.getElementById('live-alertas-stock');
        if (!container || !list) return;
        container.innerHTML = list.slice(0, 4).map(p => {
            const pct = p.minimo > 0 ? Math.min(100, (p.stock / p.minimo) * 100) : 0;
            const color = p.nivel === 'critico' ? 'red' : 'yellow';
            const alertaTexto = p.tipoAlerta === 'caducado'
                ? `✖ Lote ${esc(p.lote)} caducado`
                : p.tipoAlerta === 'proximo'
                    ? `▲ Próximo: caduca ${esc(p.fechaCaducidad)}`
                    : `${p.nivel === 'critico' ? '✖ Crítico' : '▲ Bajo'}: ${p.stock} / ${p.minimo} mín.`;
            return `<div class="sc-alert-item">
                <div class="sc-alert-row">
                    <div>
                        <div class="sc-alert-name">${esc(p.nombre)}</div>
                        <div class="sc-alert-sku">${esc(p.sku)}</div>
                    </div>
                    <div class="sc-alert-nums">${alertaTexto}</div>
                </div>
                <div class="sc-progress">
                    <div class="sc-progress-fill ${color}" style="width:${pct}%"></div>
                </div>
            </div>`;
        }).join('');
    }

    function renderRecomendaciones(list) {
        const container = document.getElementById('live-recomendaciones');
        if (!container || !list) return;
        container.innerHTML = list.map(r =>
            `<div class="sc-rec-item ${esc(r.nivel)}">
                <div class="sc-rec-title">${esc(r.titulo)}</div>
                <div class="sc-rec-text">${esc(r.texto)}</div>
            </div>`
        ).join('');
    }

    function updateCharts(data) {
        const labels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Hoy'];

        if (lineChart) {
            lineChart.data.datasets[0].data = data.entradasSemana;
            lineChart.data.datasets[1].data = data.salidasSemana;
            lineChart.update('none');
        }

        if (donutChart && data.categorias) {
            const total = data.categorias.reduce((s, c) => s + Number(c.unidades || 0), 0);
            donutChart.data.labels = data.categorias.map(c => `${c.nombre}: ${c.unidades} (${total ? Math.round(c.unidades * 100 / total) : 0}%)`);
            donutChart.data.datasets[0].data = data.categorias.map(c => c.unidades);
            donutChart.update('none');
        }
    }

    function chartTheme() {
        const dark = document.documentElement.classList.contains('a11y-dark');
        const colorblind = document.documentElement.classList.contains('a11y-colorblind');
        return { dark, text: dark ? '#edf1f5' : '#465a73', grid: dark ? '#52606d' : '#b9c6d4', colors: colorblind ? COLORS_COLORBLIND : COLORS };
    }

    function updateChartAccessibility() {
        const theme = chartTheme();
        if (lineChart) {
            lineChart.options.plugins.legend.labels.color = theme.text;
            lineChart.options.plugins.legend.labels.font = { size: 14, weight: '600' };
            lineChart.options.plugins.tooltip = { titleFont: { size: 14 }, bodyFont: { size: 14 } };
            lineChart.options.scales.x.ticks.color = theme.text; lineChart.options.scales.y.ticks.color = theme.text;
            lineChart.options.scales.x.ticks.font = { size: 14 }; lineChart.options.scales.y.ticks.font = { size: 14 };
            lineChart.options.scales.x.grid.color = theme.grid; lineChart.options.scales.y.grid.color = theme.grid;
            lineChart.data.datasets[0].borderColor = theme.colors[0]; lineChart.data.datasets[0].pointStyle = 'circle';
            lineChart.data.datasets[1].borderColor = theme.colors[1]; lineChart.data.datasets[1].pointStyle = 'triangle'; lineChart.data.datasets[1].borderDash = [8, 5];
            lineChart.update();
        }
        if (donutChart) {
            donutChart.options.plugins.legend.labels.color = theme.text;
            donutChart.options.plugins.legend.labels.font = { size: 14, weight: '600' };
            donutChart.options.plugins.tooltip = { titleFont: { size: 14 }, bodyFont: { size: 14 } };
            donutChart.data.datasets[0].backgroundColor = theme.colors;
            donutChart.data.datasets[0].borderColor = theme.dark ? '#20262e' : '#ffffff'; donutChart.data.datasets[0].borderWidth = 3;
            donutChart.update();
        }
    }

    function initCharts() {
        const lineCtx = document.getElementById('chartMovimientos');
        if (lineCtx && typeof Chart !== 'undefined') {
            lineChart = new Chart(lineCtx, {
                type: 'line',
                data: {
                    labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Hoy'],
                    datasets: [
                        {
                            label: 'Entradas',
                            data: [0, 0, 0, 0, 0, 0, 0],
                            borderColor: '#3b82f6',
                            backgroundColor: 'rgba(59,130,246,0.1)',
                            fill: true,
                            tension: 0.4,
                            pointRadius: 5,
                            pointStyle: 'circle'
                        },
                        {
                            label: 'Salidas',
                            data: [0, 0, 0, 0, 0, 0, 0],
                            borderColor: '#4ade80',
                            backgroundColor: 'rgba(74,222,128,0.08)',
                            fill: true,
                            tension: 0.4,
                            pointRadius: 5,
                            pointStyle: 'triangle',
                            borderDash: [8, 5]
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { labels: { color: '#465a73', boxWidth: 14, font: { size: 14, weight: '600' } } }, tooltip: { titleFont: { size: 14 }, bodyFont: { size: 14 } } },
                    scales: {
                        x: { ticks: { color: '#465a73', font: { size: 14 } }, grid: { color: '#b9c6d4' } },
                        y: { ticks: { color: '#465a73', font: { size: 14 } }, grid: { color: '#b9c6d4' }, beginAtZero: true }
                    }
                }
            });
        }

        const donutCtx = document.getElementById('chartCategorias');
        if (donutCtx && typeof Chart !== 'undefined') {
            donutChart = new Chart(donutCtx, {
                type: 'doughnut',
                data: {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: COLORS,
                        borderColor: '#ffffff',
                        borderWidth: 3
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '65%',
                    plugins: {
                        legend: {
                            position: 'right',
                            labels: { color: '#465a73', boxWidth: 14, font: { size: 14, weight: '600' } }
                        }
                    }
                }
            });
        }
    }

    async function poll() {
        try {
            const res = await fetch(API, { cache: 'no-store' });
            if (!res.ok) return;
            const data = await res.json();

            if (data.kpis) updateKpis(data.kpis);
            updateAlertBadge(data.alertasCount);
            renderMovimientos(data.ultimosMovimientos);
            renderAlertasStock(data.alertasStock);
            renderRecomendaciones(data.recomendaciones);
            updateCharts(data);

            const liveTime = document.getElementById('live-time');
            if (liveTime) {
                const t = new Date();
                liveTime.textContent = 'Actualizado ' + t.toLocaleTimeString('es-ES');
            }
        } catch (e) {
            console.warn('Dashboard poll error', e);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        initCharts();
        updateChartAccessibility();
        poll();
        setInterval(poll, POLL_MS);
    });
    window.addEventListener('inventario:accessibility-change', updateChartAccessibility);
})();
/* RNF-09: mejoras transversales para lectores de pantalla y teclado. */
document.addEventListener('DOMContentLoaded', function () {
  const main = document.querySelector('main');
  if (main) {
    if (!main.id) main.id = 'contenido-principal';
    if (!document.querySelector('.sc-skip-link')) {
      const skip = document.createElement('a');
      skip.className = 'sc-skip-link'; skip.href = '#' + main.id;
      skip.textContent = 'Saltar al contenido principal';
      document.body.insertBefore(skip, document.body.firstChild);
    }
    main.setAttribute('tabindex', '-1');
  }

  const nav = document.querySelector('.sc-nav');
  if (nav) nav.setAttribute('aria-label', 'Navegación principal');
  document.querySelectorAll('.sc-nav a.activo').forEach(a => a.setAttribute('aria-current', 'page'));

  document.querySelectorAll('table').forEach(function (tabla, i) {
    if (tabla.hasAttribute('aria-label') || tabla.querySelector('caption')) return;
    const seccion = tabla.closest('section, .sc-card, main');
    const titulo = seccion ? seccion.querySelector('h1,h2,h3') : null;
    tabla.setAttribute('aria-label', titulo ? titulo.textContent.trim() : 'Tabla de información ' + (i + 1));
  });
  document.querySelectorAll('button').forEach(function (boton) {
    const texto = boton.textContent.trim();
    if (!boton.getAttribute('aria-label') && (texto === '+' || texto === '🗑')) {
      boton.setAttribute('aria-label', texto === '+' ? 'Agregar producto' : 'Eliminar elemento');
    }
  });

  document.querySelectorAll('input:not([type="hidden"]), select, textarea').forEach(function (campo, i) {
    if (!campo.id) campo.id = 'campo-accesible-' + i;
    const contenedor = campo.closest('.sc-form-group, label');
    const etiqueta = contenedor ? contenedor.querySelector('label') : null;
    if (etiqueta && etiqueta !== campo.parentElement && !etiqueta.htmlFor) etiqueta.htmlFor = campo.id;
    if (!campo.getAttribute('aria-label') && !campo.getAttribute('aria-labelledby') && (!etiqueta || etiqueta === campo.parentElement)) {
      const texto = etiqueta ? etiqueta.textContent.trim() : campo.placeholder;
      if (texto) campo.setAttribute('aria-label', texto);
    }
  });

  document.querySelectorAll('.sc-error-box, .error').forEach(el => { el.setAttribute('role', 'alert'); el.setAttribute('aria-live', 'assertive'); });
  document.querySelectorAll('.sc-success-box, .aviso').forEach(el => { el.setAttribute('role', 'status'); el.setAttribute('aria-live', 'polite'); });
  document.querySelectorAll('.sc-modal-bg').forEach(function (fondo) {
    const modal = fondo.querySelector('.sc-modal');
    if (!modal) return;
    modal.setAttribute('role', 'dialog'); modal.setAttribute('aria-modal', 'true'); modal.setAttribute('tabindex', '-1');
    const titulo = modal.querySelector('h1,h2,h3');
    if (titulo) { if (!titulo.id) titulo.id = fondo.id + '-titulo'; modal.setAttribute('aria-labelledby', titulo.id); }
    let estabaAbierto = !fondo.classList.contains('sc-hidden');
    let disparador = null;
    new MutationObserver(function () {
      const abierto = !fondo.classList.contains('sc-hidden');
      if (abierto && !estabaAbierto) {
        /* En este bloque recuerdo el control que abrió el modal para restaurar el foco al cerrarlo. */
        disparador = document.activeElement;
        const primero = modal.querySelector('input:not([type="hidden"]),select,textarea,button,a[href]');
        (primero || modal).focus();
      }
      if (!abierto && estabaAbierto && disparador && typeof disparador.focus === 'function') disparador.focus();
      estabaAbierto = abierto;
    }).observe(fondo, { attributes: true, attributeFilter: ['class'] });
  });
  document.addEventListener('keydown', function (e) {
    const abierto = [...document.querySelectorAll('.sc-modal-bg')].find(m => !m.classList.contains('sc-hidden'));
    if (!abierto) return;
    if (e.key === 'Escape') { abierto.classList.add('sc-hidden'); return; }
    if (e.key === 'Tab') {
      const elementos = [...abierto.querySelectorAll('a[href],button:not([disabled]),input:not([disabled]):not([type="hidden"]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])')];
      if (!elementos.length) return;
      const primero = elementos[0], ultimo = elementos[elementos.length - 1];
      if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
      else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
    }
  });
});
