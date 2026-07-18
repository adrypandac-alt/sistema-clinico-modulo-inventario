(function () {
  'use strict';
  if (window.__inventarioA11yLoaded) return;
  window.__inventarioA11yLoaded = true;
  const KEY_FONT = 'inventario_font_size';
  const KEY_DARK = 'inventario_dark_mode';
  const KEY_COLOR = 'inventario_colorblind_mode';
  const levels = ['normal', 'large', 'xlarge'];

  function getBool(key) { return localStorage.getItem(key) === 'true'; }
  function getFont() { const value = localStorage.getItem(KEY_FONT); return levels.includes(value) ? value : 'normal'; }
  function apply(announce) {
    const root = document.documentElement;
    const font = getFont(), dark = getBool(KEY_DARK), color = getBool(KEY_COLOR);
    root.classList.toggle('a11y-font-large', font === 'large');
    root.classList.toggle('a11y-font-xlarge', font === 'xlarge');
    root.classList.toggle('a11y-dark', dark);
    root.classList.toggle('a11y-colorblind', color);
    root.dataset.a11yReady = 'true';
    updateButtons(font, dark, color);
    window.dispatchEvent(new CustomEvent('inventario:accessibility-change', { detail: { font, dark, color } }));
    if (announce) speak('Preferencias visuales actualizadas');
  }
  function updateButtons(font, dark, color) {
    document.querySelectorAll('[data-a11y="decrease"]').forEach(b => b.disabled = font === 'normal');
    document.querySelectorAll('[data-a11y="increase"]').forEach(b => b.disabled = font === 'xlarge');
    document.querySelectorAll('[data-a11y="dark"]').forEach(b => {
      b.setAttribute('aria-pressed', String(dark)); b.textContent = dark ? '☀ Modo claro' : '🌙 Modo oscuro';
      b.title = dark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
      b.setAttribute('aria-label', dark ? 'Desactivar modo oscuro y usar modo claro' : 'Activar modo oscuro');
    });
    document.querySelectorAll('[data-a11y="colorblind"]').forEach(b => {
      b.setAttribute('aria-pressed', String(color)); b.textContent = color ? '◉ Color normal' : '◉ Modo daltonismo';
      b.setAttribute('aria-label', color ? 'Desactivar modo daltonismo y usar color normal' : 'Activar modo daltonismo');
    });
  }
  function speak(text) {
    let live = document.getElementById('a11y-live');
    if (!live) { live = document.createElement('div'); live.id = 'a11y-live'; live.className = 'sc-sr-only'; live.setAttribute('aria-live', 'polite'); document.body.appendChild(live); }
    live.textContent = ''; setTimeout(() => live.textContent = text, 20);
  }
  function changeFont(delta) {
    const index = levels.indexOf(getFont());
    localStorage.setItem(KEY_FONT, levels[Math.max(0, Math.min(levels.length - 1, index + delta))]); apply(true);
  }
  function bind() {
    /*
     * En este bloque relaciono etiquetas y campos en todas las pantallas.
     * Como comparto este archivo entre JSP distintas, compruebo cada elemento
     * antes de asignar atributos y no dependo de una estructura única.
     */
    document.querySelectorAll('input:not([type="hidden"]), select, textarea').forEach((field, index) => {
      if (!field.id) field.id = 'a11y-field-' + index;
      const group = field.closest('.sc-form-group, label');
      const label = group?.querySelector('label');
      if (label && label !== field.parentElement && !label.htmlFor) label.htmlFor = field.id;
      if (field.required) field.setAttribute('aria-required', 'true');
    });

    /*
     * En este bloque muestro el error nativo junto al campo y muevo el foco
     * únicamente después de intentar enviar. JavaScript ayuda al usuario, pero
     * el servlet vuelve a validar porque el navegador puede ser manipulado.
     */
    document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => {
      const invalid = form.querySelector(':invalid');
      if (!invalid) return;
      event.preventDefault();
      showFieldError(invalid);
      invalid.focus();
    }));
    document.addEventListener('invalid', event => {
      if (event.target.matches('input,select,textarea')) showFieldError(event.target);
    }, true);
    document.querySelectorAll('input,select,textarea').forEach(field => field.addEventListener('input', () => {
      if (field.validity?.valid) clearFieldError(field);
    }));

    document.querySelectorAll('[data-a11y="decrease"]').forEach(b => b.addEventListener('click', () => changeFont(-1)));
    document.querySelectorAll('[data-a11y="increase"]').forEach(b => b.addEventListener('click', () => changeFont(1)));
    document.querySelectorAll('[data-a11y="dark"]').forEach(b => b.addEventListener('click', () => { localStorage.setItem(KEY_DARK, String(!getBool(KEY_DARK))); apply(true); }));
    document.querySelectorAll('[data-a11y="colorblind"]').forEach(b => b.addEventListener('click', () => { localStorage.setItem(KEY_COLOR, String(!getBool(KEY_COLOR))); apply(true); }));
    document.querySelectorAll('[data-a11y="menu"]').forEach(b => b.addEventListener('click', () => {
      const controls = document.getElementById('a11y-controls'); const open = controls.classList.toggle('a11y-open');
      b.setAttribute('aria-expanded', String(open)); if (open) controls.querySelector('button').focus();
    }));
    /* En esta condición cierro el panel con Escape y devuelvo el foco al botón que lo abrió. */
    document.addEventListener('keydown', e => { if (e.key === 'Escape') {
      const c = document.getElementById('a11y-controls'); const trigger = document.querySelector('[data-a11y="menu"]');
      if (c?.classList.contains('a11y-open')) { c.classList.remove('a11y-open'); trigger?.setAttribute('aria-expanded', 'false'); trigger?.focus(); }
    } });
    apply(false);
  }

  function showFieldError(field) {
    clearFieldError(field);
    const id = field.id + '-error';
    const error = document.createElement('div');
    error.id = id; error.className = 'a11y-field-error'; error.setAttribute('role', 'alert');
    error.textContent = '✖ ' + (field.validationMessage || 'Revise este campo.');
    field.setAttribute('aria-invalid', 'true'); field.setAttribute('aria-describedby', id);
    field.insertAdjacentElement('afterend', error);
  }
  function clearFieldError(field) {
    document.getElementById(field.id + '-error')?.remove();
    field.removeAttribute('aria-invalid');
    if ((field.getAttribute('aria-describedby') || '').endsWith('-error')) field.removeAttribute('aria-describedby');
  }

  /* Aplicación temprana para reducir el parpadeo de tema. */
  try { apply(false); } catch (_) {}
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', bind); else bind();
})();
