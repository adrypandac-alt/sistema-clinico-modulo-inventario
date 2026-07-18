(function () {
    const API = 'api/dashboard';
    const POLL_MS = 4000;
    let lineChart = null;
    let donutChart = null;

    const COLORS = ['#3b82f6', '#4ade80', '#f59e0b', '#a78bfa', '#ef4444', '#06b6d4'];

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
            const qtyCls = m.tipo === 'ENTRADA' ? 'up' : 'down';
            return `<div class="sc-mov-item">
                <div class="sc-mov-icon ${iconCls}">${arrow}</div>
                <div class="sc-mov-info">
                    <div class="sc-mov-name">${esc(m.producto)}</div>
                    <div class="sc-mov-desc">${esc(m.motivo)} · ${esc(m.usuario || '')}</div>
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
                ? `Lote ${esc(p.lote)} caducado`
                : p.tipoAlerta === 'proximo'
                    ? `Caduca ${esc(p.fechaCaducidad)}`
                    : `${p.stock} / ${p.minimo} mín.`;
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
            donutChart.data.labels = data.categorias.map(c => c.nombre);
            donutChart.data.datasets[0].data = data.categorias.map(c => c.unidades);
            donutChart.update('none');
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
                            pointRadius: 3
                        },
                        {
                            label: 'Salidas',
                            data: [0, 0, 0, 0, 0, 0, 0],
                            borderColor: '#4ade80',
                            backgroundColor: 'rgba(74,222,128,0.08)',
                            fill: true,
                            tension: 0.4,
                            pointRadius: 3
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { labels: { color: '#8b97a8', boxWidth: 10 } } },
                    scales: {
                        x: { ticks: { color: '#6b7a90' }, grid: { color: '#1e2633' } },
                        y: { ticks: { color: '#6b7a90' }, grid: { color: '#1e2633' }, beginAtZero: true }
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
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '65%',
                    plugins: {
                        legend: {
                            position: 'right',
                            labels: { color: '#8b97a8', boxWidth: 10, font: { size: 11 } }
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
        poll();
        setInterval(poll, POLL_MS);
    });
})();
