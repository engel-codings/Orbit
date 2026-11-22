"""Screenshots der Web-Version wie auf einem iPhone (390x844)."""
import subprocess, sys, time, os
from playwright.sync_api import sync_playwright

dist, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
srv = subprocess.Popen([sys.executable, "-m", "http.server", "8765", "-d", dist],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1.5)
W, H = 390, 844
K = W / 1000.0            # Welt-Einheiten -> Bildschirm
HU = H / K                # Bildschirmhöhe in Welt-Einheiten
errors = []

def u(x, y):
    return x * K, y * K

try:
    with sync_playwright() as p:
        b = p.chromium.launch()
        ctx = b.new_context(viewport={"width": W, "height": H}, device_scale_factor=3, has_touch=True,
                            locale="de-DE", user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)")
        pg = ctx.new_page()
        pg.on("pageerror", lambda e: errors.append("PAGEERROR: " + str(e)))
        pg.on("console", lambda m: errors.append(m.type + ": " + m.text) if m.type == "error" else None)
        pg.goto("http://localhost:8765/index.html")
        time.sleep(3)

        def shot(name):
            pg.screenshot(path=f"{out}/{name}.png")

        def tap(x, y, wait=1.4):
            pg.touchscreen.tap(*u(x, y)); time.sleep(wait)

        def back():
            pg.keyboard.press("Escape"); time.sleep(1.2)

        pcy = HU * 0.43
        nav = lambda i: (100 + i * 200, HU - 300 + 65)
        shot("01_bonus")
        dlg_h = 160 + 2 * 62 + 190
        tap(500, (HU - dlg_h) / 2 + dlg_h - 165 + 60); shot("02_menu")
        tap(500, pcy); shot("03_karte")
        tap(500 + 0.783 * 250, HU - 450); shot("04_level1")
        tap(500, 1000, 0.4); shot("05_flug")
        time.sleep(2.5); shot("06_ende")
        pg.reload(); time.sleep(3)
        tap(*nav(0)); shot("07_shop")
        back()
        tap(*nav(1)); shot("08_missionen")
        back()
        tap(*nav(4)); shot("09_optionen")
        back()
        tap(500, pcy + 385); shot("10_endlos_auswahl")
        back()
        tap(500, pcy + 517); shot("11_gluecksrad")
        back()
        tap(*nav(4)); tap(500, 235 + 6 * 112 + 49); shot("12_optionen_EN")
        back(); shot("13_menu_EN")
        b.close()
finally:
    srv.terminate()

with open(f"{out}/fehler.txt", "w") as f:
    f.write("\n".join(errors) or "keine Fehler")
print("\n".join(errors) or "keine Fehler")
