# ink-ms-accesibility

Microservicio de preferencias de accesibilidad, notificaciones y **comandos por voz**.

Puerto: `3004` · Base: MongoDB `accessibility_notifications_db`

## Endpoints

### Preferencias — `/api/preferences`
- `GET /api/preferences`
- `PUT /api/preferences` — incluye `voiceCommandsEnabled`, `ttsEnabled`, `voiceLanguage`

### Notificaciones — `/api/notifications`
(Ver controladores existentes.)

### Voz — `/api/voice`
- `GET /api/voice/commands` — glosario de frases
- `POST /api/voice/interpret` — `{ "input": "ir a eventos" }` → acción / ruta
- `POST /api/voice/log` — historial de ejecución
- `GET /api/voice/history`

El reconocimiento de audio ocurre en el navegador (Web Speech API). Este servicio interpreta, cataloga y registra.
