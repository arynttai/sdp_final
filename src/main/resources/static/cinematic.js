// Progressive motion: a photograph remains the reliable first frame.
function initializeCinematicHero() {
  const hero = document.querySelector('.cinematic-hero');
  if (!hero) return;
  const media = hero.querySelector('.hero-media');
  const image = hero.querySelector('#hero-cinematic-image');
  const toggle = document.querySelector('#motion-toggle');
  const toggleLabel = toggle?.querySelector('.motion-toggle-label');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const finePointer = matchMedia('(hover: hover) and (pointer: fine)');
  let enabled = !reducedMotion.matches;
  let visible = true;
  let frame = 0;
  let pointer = null;
  let video = hero.querySelector('#hero-film');
  let ready = false;
  let playPending = false;
  let playbackBlocked = false;
  let loadedSource = '';

  function updateButton() {
    hero.dataset.motion = enabled ? 'on' : 'off';
    if (!toggle) return;
    toggle.setAttribute('aria-pressed', String(enabled));
    const label = reducedMotion.matches ? 'Motion off' : enabled ? 'Pause motion' : 'Enable motion';
    toggle.setAttribute('aria-label', reducedMotion.matches ? 'Motion disabled by system preference' : enabled ? 'Pause cinematic motion' : 'Enable cinematic motion');
    toggle.disabled = reducedMotion.matches;
    if (toggleLabel) toggleLabel.textContent = label;
    else toggle.textContent = label;
    toggle.title = reducedMotion.matches ? 'Motion is disabled by your device’s reduced-motion preference.' : '';
  }

  function showVideo(show) {
    hero.dataset.videoVisible = String(show);
    if (!video) return;
    video.hidden = !show;
    video.style.display = show ? '' : 'none';
  }

  function mayPlay() {
    return enabled && !reducedMotion.matches && visible && !document.hidden;
  }

  function source() {
    return hero.dataset.videoSrc || video?.dataset.videoSrc || '';
  }

  function bindVideo(element) {
    element.classList.add('hero-film');
    element.id = 'hero-film';
    element.muted = true;
    element.defaultMuted = true;
    element.loop = true;
    element.playsInline = true;
    element.preload = 'none';
    element.setAttribute('aria-hidden', 'true');
    element.setAttribute('tabindex', '-1');
    if (image?.src) element.poster = image.src;
    element.addEventListener('canplay', () => {
      ready = true;
      hero.dataset.videoState = 'ready';
      synchronizePlayback();
    });
    element.addEventListener('playing', () => {
      if (!mayPlay()) { element.pause(); return; }
      hero.dataset.videoState = 'playing';
      showVideo(true);
    });
    element.addEventListener('pause', () => {
      if (hero.dataset.videoState !== 'error') hero.dataset.videoState = 'paused';
      if (!mayPlay()) showVideo(false);
    });
    element.addEventListener('error', () => {
      ready = false;
      playbackBlocked = true;
      hero.dataset.videoState = 'error';
      showVideo(false);
    });
    showVideo(false);
  }

  function synchronizePlayback() {
    const requestedSource = source();
    if (!requestedSource) {
      video?.pause();
      showVideo(false);
      hero.dataset.videoState = 'absent';
      return;
    }
    if (!video && media) {
      video = document.createElement('video');
      bindVideo(video);
      media.insertBefore(video, image || media.firstChild);
    }
    if (!video) return;
    if (!mayPlay()) {
      video.pause();
      showVideo(false);
      return;
    }
    if (loadedSource !== requestedSource) {
      ready = false;
      playbackBlocked = false;
      loadedSource = requestedSource;
      showVideo(false);
      hero.dataset.videoState = 'loading';
      video.src = requestedSource;
      video.load();
      return;
    }
    if (!ready || playbackBlocked || playPending || !video.paused) return;
    playPending = true;
    const playing = video.play();
    Promise.resolve(playing).catch(error => {
      if (error?.name === 'AbortError' && !mayPlay()) return;
      playbackBlocked = true;
      hero.dataset.videoState = 'paused';
      showVideo(false);
    }).finally(() => { playPending = false; });
  }

  function renderCamera() {
    frame = 0;
    const moving = enabled && !reducedMotion.matches && visible && !document.hidden;
    let x = 0, y = 0, progress = 0;
    if (moving) {
      const rect = hero.getBoundingClientRect();
      progress = Math.max(0, Math.min(1, -rect.top / Math.max(rect.height, 1)));
      if (pointer && finePointer.matches && rect.width && rect.height) {
        x = Math.max(-1, Math.min(1, (pointer.x - rect.left) / rect.width * 2 - 1)) * 14;
        y = Math.max(-1, Math.min(1, (pointer.y - rect.top) / rect.height * 2 - 1)) * 10;
      }
    }
    hero.style.setProperty('--hero-x', `${x.toFixed(2)}px`);
    hero.style.setProperty('--hero-y', `${y.toFixed(2)}px`);
    hero.style.setProperty('--hero-rx', `${(-y * 0.08).toFixed(2)}deg`);
    hero.style.setProperty('--hero-ry', `${(x * 0.08).toFixed(2)}deg`);
    hero.style.setProperty('--hero-scale', moving ? '1.04' : '1');
    hero.style.setProperty('--hero-scroll', progress.toFixed(3));
  }

  function queueCamera() {
    if (!frame) frame = requestAnimationFrame(renderCamera);
  }

  function resetCamera() {
    pointer = null;
    if (frame) cancelAnimationFrame(frame);
    frame = 0;
    renderCamera();
  }

  if (video) bindVideo(video);
  if (image) {
    const imageError = () => {
      const fallback = image.dataset.heroFallback;
      if (fallback && image.src !== new URL(fallback, document.baseURI).href) {
        image.src = fallback;
        hero.dataset.imageFallback = 'true';
        if (video) video.poster = image.src;
      } else {
        hero.dataset.imageFallback = 'unavailable';
      }
    };
    image.addEventListener('error', imageError);
    if (image.complete && !image.naturalWidth && image.getAttribute('src')) imageError();
  }

  hero.addEventListener('pointermove', event => {
    if (!enabled || reducedMotion.matches || !finePointer.matches || event.pointerType === 'touch') return;
    pointer = { x: event.clientX, y: event.clientY };
    queueCamera();
  }, { passive: true });
  hero.addEventListener('pointerleave', resetCamera, { passive: true });
  hero.addEventListener('pointercancel', resetCamera, { passive: true });
  window.addEventListener('scroll', () => { if (visible) queueCamera(); }, { passive: true });
  window.addEventListener('resize', resetCamera, { passive: true });
  window.addEventListener('blur', resetCamera);
  document.addEventListener('visibilitychange', () => { resetCamera(); synchronizePlayback(); });
  finePointer.addEventListener('change', resetCamera);
  reducedMotion.addEventListener('change', () => {
    if (reducedMotion.matches) enabled = false;
    updateButton(); resetCamera(); synchronizePlayback();
  });
  toggle?.addEventListener('click', () => {
    // Respect an active OS preference even when the button is activated.
    enabled = reducedMotion.matches ? false : !enabled;
    playbackBlocked = false;
    updateButton(); resetCamera(); synchronizePlayback();
  });

  if ('IntersectionObserver' in window) {
    new IntersectionObserver(entries => {
      visible = entries[0].isIntersecting;
      resetCamera(); synchronizePlayback();
    }, { threshold: 0.05 }).observe(hero);
  }
  new MutationObserver(() => synchronizePlayback()).observe(hero, {
    attributes: true, attributeFilter: ['data-video-src']
  });
  updateButton();
  resetCamera();
  synchronizePlayback();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeCinematicHero, { once: true });
} else {
  initializeCinematicHero();
}
