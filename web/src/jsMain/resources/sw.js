// Offline-Speicher für Orbit (neue Version = neuer Cache-Name)
const CACHE = 'orbit-__VERSION__';
const FILES = ['./', 'index.html', 'orbit.js', 'manifest.webmanifest',
  'fonts/Outfit-ExtraBold.ttf', 'fonts/Outfit-Medium.ttf', 'fonts/Manrope-ExtraBold.ttf', 'fonts/Manrope-SemiBold.ttf',
  'icons/icon-180.png', 'icons/icon-192.png', 'icons/icon-512.png'];
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});
// Erst aus dem Netz (immer neueste Version), sonst aus dem Speicher
self.addEventListener('fetch', e => {
  e.respondWith(fetch(e.request).then(r => {
    const copy = r.clone();
    caches.open(CACHE).then(c => c.put(e.request, copy));
    return r;
  }).catch(() => caches.match(e.request)));
});
