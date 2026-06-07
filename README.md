# BatteryGuard

**Desarrollado por Enmanuel Gil**
Versión 1.0.0 | Android 8.0+ (API 26) | Sin dependencias externas

Aplicación Android que analiza la salud real de tu batería, identifica las apps que más la drenan y aplica optimizaciones para extender su duración. Información clara, en tiempo real y sin necesidad de root.

---

## Características

### Panel Principal en Tiempo Real
- Indicador circular animado con nivel de batería actual
- Temperatura en tiempo real con alertas visuales
- Voltaje instantáneo
- Tiempo restante estimado (o tiempo de carga si está conectado)
- Estado de salud de la batería (Buena / Sobrecalentada / Muerta / etc.)
- Ciclos de carga (Android 14+ nativo)
- Capacidad actual estimada en mAh

### Análisis de Apps por Consumo
- Lista las apps que más batería consumieron en las últimas 24 horas
- Tiempo en primer plano y en background por app
- Barra de progreso coloreada por nivel de impacto
- Requiere permiso de acceso al historial de uso (se solicita desde la app)

### Optimización con un Toque
- Mata procesos en background para liberar CPU y reducir consumo
- Fuerza Garbage Collection del sistema
- Reduce animaciones (requiere activación avanzada por ADB)
- Desactiva WiFi scan pasivo

### Monitor en Background
- Notificación persistente con nivel y temperatura en tiempo real
- Alerta automática cuando la temperatura supera 45°C
- Se activa automáticamente al encender el dispositivo

---

## Instalación

1. Descarga `BatteryGuard-v1.0.0.apk` desde [Releases](https://github.com/EnMaNueL-G/BatteryGuard/releases)
2. En el teléfono: **Ajustes → Seguridad → Instalar apps de origen desconocido → Activar**
3. Abre el APK e instala
4. La app funciona de inmediato

---

## Activar Optimización Avanzada (Opcional)

Para desbloquear control de animaciones y WiFi scan: ejecuta **una sola vez** desde PC con el teléfono conectado por USB.

### Activar Depuración USB (si no está activa)
1. **Ajustes → Acerca del teléfono** → Toca "Número de compilación" 7 veces
2. **Ajustes → Opciones de desarrollador** → Activa "Depuración USB"
3. Conecta el teléfono al PC — toca "Permitir" en el popup

### Comando ADB
```bash
adb shell pm grant com.enmanuelgil.batteryguard android.permission.WRITE_SECURE_SETTINGS
```

> ⚠️ Xiaomi MIUI V14 / HyperOS bloquea este permiso. La app funciona igual en modo básico.

---

## Consejos para maximizar la batería

- Carga entre **20% y 80%** — evita llegar a 0% o 100% constantemente
- Temperaturas superiores a **45°C dañan permanentemente** las células de la batería
- Pantalla y WiFi son los mayores consumidores — reduce brillo y desactiva cuando no uses
- El modo oscuro en pantallas OLED ahorra hasta un 15% de batería
- Mantener muchas apps abiertas en background aumenta el consumo aunque no las uses

---

## Compatibilidad

| Dispositivo | Android | Básico | Avanzado |
|-------------|---------|--------|----------|
| Samsung Galaxy S21 | Android 15 | ✅ | ✅ |
| Samsung Galaxy J7 Prime | Android 8.1 | ✅ | ✅ |
| Xiaomi Redmi Note 12 | Android 14 | ✅ | ✅ |
| Xiaomi con MIUI V14 | Android 13 | ✅ | ⚠️ ADB bloqueado |

---

## Permisos

| Permiso | Para qué |
|---------|----------|
| `KILL_BACKGROUND_PROCESSES` | Matar apps en background al optimizar |
| `FOREGROUND_SERVICE` | Monitor en background |
| `POST_NOTIFICATIONS` | Alertas de temperatura |
| `RECEIVE_BOOT_COMPLETED` | Auto-inicio al encender |
| `PACKAGE_USAGE_STATS` | Ver qué apps consumen más batería |
| `WRITE_SECURE_SETTINGS` *(ADB, opcional)* | Animaciones, WiFi scan |

---

## Apoya el Proyecto

BatteryGuard es **gratuita, sin anuncios y de código abierto**.

**Binance Pay ID:** `1140153333`
**BSC BEP20:** `0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1`

---

## Créditos

**Desarrollado por:** Enmanuel Gil
**UI:** Jetpack Compose con Material Design 3 — tema verde oscuro
**Compatibilidad:** Android 8.0 — Android 15

---

*BatteryGuard v1.0.0 — Cuida tu batería, sin complicaciones*
