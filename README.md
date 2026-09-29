# BatteryGuard

**Desarrollado por Enmanuel Gil · OptiSuite** — Android 8.0+ · gratis · sin anuncios · código abierto

Cuida la batería de tu Android con datos **reales**: salud medida con tus propias cargas, aviso para desenchufar al 80 %, alertas de calor y batería baja, y qué apps usas más. Sin inventar números: si Android no da un dato, BatteryGuard lo dice.

**Descarga:** [BatteryGuard.apk](https://github.com/EnMaNueL-G/BatteryGuard/releases/latest/download/BatteryGuard.apk) (se instala encima de la versión anterior).

---

## Qué hace

### 🔋 Batería en tiempo real
- Nivel, temperatura, voltaje y **corriente real** (mA que entran o salen) y **potencia** en vatios.
- **Tiempo restante / hasta el 100 %** calculado al ritmo actual (si no hay datos suficientes muestra «—», nunca una cifra inventada).
- Estado de salud según Android, **ciclos de carga** (Android 14+) y datos técnicos en bruto.
- Cada fabricante informa la corriente a su manera (µA o mA, signo al revés): BatteryGuard lo normaliza.

### ❤️ Salud real de la batería
- **Capacidad de fábrica** leída del perfil de energía del fabricante.
- **Capacidad actual** medida en tus cargas: mAh que entran al subir de ≤ 60 % a ≥ 80 % (mismo método que AccuBattery). Mediana de las últimas cargas, con filtros contra lecturas absurdas.
- Mientras no hay cargas medidas, una **estimación rápida** con el medidor de la batería, marcada como tal.

### 🔔 Avisos (monitor en segundo plano)
- **Límite de carga**: te avisa al llegar al 80 % (o el % que elijas). Parar ahí alarga la vida de la batería.
- **Batería caliente** (42 °C por defecto) y **batería baja** (opcional).
- El monitor **no hace sondeos**: solo reacciona cuando Android avisa de un cambio. Compatible con Android 15/16 (arranca al encender el móvil).

### 📱 Apps que más usas
Tiempo en pantalla y en segundo plano de las últimas 24 h. **Android no deja que una app mida la batería que gastan otras** (solo el sistema): el tiempo de uso es la mejor pista honesta. Botón al consumo real en los Ajustes de Android y, al tocar una app, a su información para restringirla.

### ⚙️ Avanzado (opcional, por ADB)
Desactivar la **búsqueda de Wi‑Fi y Bluetooth en segundo plano** (Android escanea para la ubicación aunque los tengas apagados). Se guarda el valor original y **«Restaurar» lo deja como estaba**.
```bash
adb shell pm grant com.enmanuelgil.batteryguard android.permission.WRITE_SECURE_SETTINGS
```

---

## Lo que BatteryGuard NO hace (a propósito)
«Matar procesos» o «limpiar RAM» **no ahorra batería** en Android moderno: el sistema vuelve a abrir esas apps y gasta más. La versión 1.0.0 lo prometía y no funcionaba; se ha quitado.

## Permisos
| Permiso | Para qué |
|---|---|
| Notificaciones | Monitor y avisos |
| Servicio en primer plano (specialUse) · arranque | Monitor en segundo plano, si lo activas |
| Acceso de uso (lo concedes tú) | Tiempo de uso por app |
| WRITE_SECURE_SETTINGS (ADB, opcional) | Búsqueda de redes en segundo plano |

No recoge datos, no tiene anuncios y no usa Internet.

---

## Cambios

### v1.1.0
- **Salud real** (antes siempre mostraba 85 %), **ciclos** reales (antes mostraba el % de carga), capacidad de fábrica y actual bien etiquetadas.
- **Aviso de límite de carga**, calor configurable y batería baja.
- Corriente y potencia reales con unidades normalizadas; tiempo restante sin cifras inventadas.
- «Consumo por app» → **tiempo de uso** honesto, sin apps repetidas, sin congelar la pantalla y con acceso a restringir cada app.
- Quitado el «Optimizar» que no hacía nada; los ajustes avanzados ahora tienen respaldo y restauración (antes cambiaba las animaciones sin avisar).
- Monitor ligero y compatible con Android 15/16; pide el permiso de notificaciones (en Android 13+ antes no se veía nada).
- Pruebas automáticas de los cálculos.

### v1.0.0
- Versión inicial.

---

## Compilar
Android Studio (JDK 17) · `gradlew assembleRelease` · pruebas: `gradlew testReleaseUnitTest`.

## Apoya el proyecto
- **Binance Pay ID:** `1165745950`
- **USDT (BSC · BEP-20):** `0xb6f6731a4ea87f8e1fd6f44f48b5bc4204571f08`

© 2026 Enmanuel Gil · OptiSuite — [github.com/EnMaNueL-G](https://github.com/EnMaNueL-G)
