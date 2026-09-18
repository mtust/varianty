# Варіанти

Party-гра на кшталт Fibbage: ведучому показують факт і правильну відповідь, він
вигадує відповідь-пастку, гравці вигадують свої відповіді, а потім усі голосують
за ту, що здається правильною.

Правила підрахунку балів:
- вгадав правильну відповідь — **+2**
- хтось проголосував за твою відповідь — **+1** за кожен такий голос
- проголосував за пастку ведучого — **-1**

## Структура репозиторію

- `mobile/` — Expo (React Native + TypeScript) додаток для Android/iOS
- `backend/` — Spring Boot (Java 21, Maven) бекенд: кімнати в реальному часі
  через WebSocket/STOMP, гостьовий та email-логін, історія ігор у PostgreSQL

## Швидкий старт

### Backend

```bash
cd backend
docker compose up -d          # піднімає локальний PostgreSQL
./mvnw spring-boot:run
```

За замовчуванням слухає `http://localhost:8080`. Питання (факти) засіваються
автоматично при першому старті (`QuestionSeeder`).

### Mobile

```bash
cd mobile
cp .env.example .env          # вкажіть LAN-IP бекенду для тестування на фізичному телефоні
npm install
npm start
```

`EXPO_PUBLIC_API_BASE_URL`/`EXPO_PUBLIC_WS_URL` мають вказувати на IP машини з
бекендом (не `localhost`), якщо тестуєте на реальному телефоні в тій самій Wi-Fi мережі.

## Ігровий цикл (WebSocket, STOMP over `/ws`)

1. Хост: `POST /api/rooms` → отримує код кімнати
2. Усі: `CONNECT` з `Authorization: Bearer <jwt>`, підписка на `/topic/room/{code}`
3. Гравці приєднуються: `SEND /app/room/{code}/join`
4. Хост стартує: `SEND /app/room/{code}/start` → хост отримує факт+відповідь
   через `/user/queue/host-question`
5. Хост надсилає пастку: `SEND /app/room/{code}/host-trap`
6. Гравці надсилають відповіді: `SEND /app/room/{code}/answer`
7. Коли відповіли всі — старт голосування, варіанти в `/topic/room/{code}/voting`
8. Гравці голосують: `SEND /app/room/{code}/vote`
9. Коли проголосували всі — результати раунду в `/topic/room/{code}/results`
10. Хост переходить далі: `SEND /app/room/{code}/next-round`
