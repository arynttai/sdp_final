(() => {
  'use strict';
  const storageKey = 'italiano-theme';
  const root = document.documentElement;
  const isTheme = value => value === 'light' || value === 'dark';

  function apply(theme) {
    root.dataset.theme = theme;
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.content = theme === 'light' ? '#faf7f1' : '#171410';
    const button = document.getElementById('theme-toggle');
    if (button) {
      const label = `Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`;
      button.setAttribute('aria-label', label);
      button.title = label;
    }
    document.querySelectorAll('a.receipt-link').forEach(link => {
      const url = new URL(link.href, location.href);
      if (url.origin === location.origin && url.searchParams.get('format') === 'html') {
        url.searchParams.set('theme', theme);
        link.href = url.pathname + url.search;
      }
    });
  }

  let initial = 'dark';
  try {
    const saved = localStorage.getItem(storageKey);
    if (isTheme(saved)) initial = saved;
  } catch (_) { /* Theme switching still works when storage is unavailable. */ }
  apply(initial);

  document.addEventListener('DOMContentLoaded', () => {
    apply(root.dataset.theme);
    document.getElementById('theme-toggle')?.addEventListener('click', () => {
      const next = root.dataset.theme === 'dark' ? 'light' : 'dark';
      apply(next);
      try { localStorage.setItem(storageKey, next); } catch (_) { /* Optional persistence. */ }
    });
  });
  window.addEventListener('storage', event => {
    if (event.key === storageKey) apply(isTheme(event.newValue) ? event.newValue : 'dark');
  });
})();
