# Лабораторна робота №1 — Spring Security

**Тема:** Інформаційна система управління приватною клінікою
**Стек:** Spring Boot 3 · Spring Security (OIDC + JWT) · Spring Data JPA · Liquibase · MapStruct · Thymeleaf · PostgreSQL · Keycloak

## 1. Предметна область

Базовий фундамент системи управління клінікою: відділення, лікарі, пацієнти, записи на прийом.
У наступних лабораторних цей фундамент розширюється: медичні картки, призначення, аналізи, оплата, сповіщення, статистика.

## 2. Структура проєкту

```
clinic-service/
├── pom.xml
├── docker-compose.yml              # Postgres + Keycloak
├── keycloak/realm-export.json      # готовий realm з ролями і тестовими користувачами
├── postman/Clinic-Service-Lab1.postman_collection.json
└── src/main/
    ├── java/com/clinic/
    │   ├── entity/                 # Department, Doctor, Patient, Appointment
    │   ├── repository/             # Spring Data JPA
    │   ├── dto/                    # DTO для REST/UI
    │   ├── mapper/                 # MapStruct Entity <-> DTO
    │   ├── service/ + service/impl # бізнес-логіка + @PreAuthorize
    │   ├── controller/             # REST API (/api/**)
    │   ├── web/                    # Thymeleaf-контролери (/ui/**)
    │   ├── security/               # конвертери ролей Keycloak, SecurityUtils
    │   ├── config/SecurityConfig.java  # два SecurityFilterChain (OIDC + JWT)
    │   └── exception/              # обробка помилок
    └── resources/
        ├── application.yml
        ├── db/changelog/           # Liquibase-міграції (схема + seed-дані)
        ├── templates/              # прості UI-сторінки (Thymeleaf)
        └── static/css/style.css
```

## 3. Як відповідає критеріям прийнятності

| AC | Що саме реалізує |
|----|---|
| **AC1** — базовий CRUD | `Controller → Service → Repository → Entity/DTO` для Department/Doctor/Patient/Appointment, мапінг через **MapStruct** (`mapper/*`) |
| **AC2** — міграції | Схема БД повністю створюється **Liquibase** (`db/changelog/db.changelog-master.xml`), `spring.jpa.hibernate.ddl-auto=validate` — Hibernate нічого не створює сам |
| **AC3** — базовий UI | Thymeleaf-сторінки `/ui/departments`, `/ui/doctors`, `/ui/patients`, `/ui/appointments` з формами CRUD |
| **AC4** — OIDC для UI | `SecurityConfig.uiSecurityFilterChain` — `oauth2Login()`, редирект на Keycloak, сесія після логіну |
| **AC5** — JWT для API | `SecurityConfig.apiSecurityFilterChain` — `oauth2ResourceServer().jwt()`, `STATELESS`, працює з `curl`/Postman через `Authorization: Bearer <token>` |
| **AC6** — обмеження у SecurityFilterChain | Наприклад: `POST/PUT/DELETE /api/departments/**` → лише `ADMIN` (у `apiSecurityFilterChain`); без ролі — **403** ще до контролера |
| **AC7** — обмеження через @PreAuthorize | Наприклад: `DepartmentServiceImpl.create/update/delete` → `@PreAuthorize("hasRole('ADMIN')")`; `AppointmentServiceImpl.updateStatus` → `@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")`; `PatientServiceImpl` — перевірка власності запису пацієнта |
| **AC8** — готовність до захисту | `postman/Clinic-Service-Lab1.postman_collection.json` — готові запити для отримання токенів і демонстрації всіх сценаріїв |

## 4. Запуск

### Крок 1. Підняти інфраструктуру (Postgres + Keycloak)

```bash
docker compose up -d
```

Keycloak запуститься на `http://localhost:8081` і автоматично імпортує realm `clinic` з файлу `keycloak/realm-export.json`:

**Ролі:** `ADMIN`, `DOCTOR`, `PATIENT`
**Клієнт:** `clinic-app` / secret `clinic-app-secret` (confidential, дозволені authorization_code і password grant)

**Тестові користувачі:**

| Логін | Пароль | Роль |
|---|---|---|
| `admin1` | `admin123` | ADMIN |
| `doctor1` | `doctor123` | DOCTOR (лікар "Олена Коваленко", вже прив'язаний до запису лікаря в БД) |
| `patient1` | `patient123` | PATIENT (пацієнт "Марія Іваненко", вже прив'язана до запису пацієнта в БД) |
| `patient2` | `patient123` | PATIENT (пацієнт "Andrii Petrenko") |

> Зачекайте ~30-40 сек, поки Keycloak повністю підніметься, перш ніж запускати застосунок.

### Крок 2. Запустити застосунок

```bash
mvn spring-boot:run
```

Застосунок підніметься на `http://localhost:8080`, Liquibase автоматично створить схему і засіє тестові дані (2 відділення, 2 лікарі, 2 пацієнти, 1 запис на прийом).

### Крок 3. UI (OIDC)

Відкрийте `http://localhost:8080` → «Увійти» → форма логіну Keycloak → увійдіть як `admin1`/`admin123` (або `doctor1`, `patient1`) → після успішного логіну вас поверне назад із дійсною сесією (AC4).

### Крок 4. API (JWT)

Отримати токен напряму від Keycloak (Resource Owner Password grant, для тестування):

```bash
curl -X POST http://localhost:8081/realms/clinic/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=clinic-app&client_secret=clinic-app-secret&username=admin1&password=admin123"
```

Викликати захищений ендпоінт з отриманим `access_token` (AC5):

```bash
curl http://localhost:8080/api/departments \
  -H "Authorization: Bearer <access_token>"
```

### Крок 5. Демонстрація обмежень доступу

```bash
# AC6: спроба створити відділення без ролі ADMIN -> 403 Forbidden (SecurityFilterChain)
curl -i -X POST http://localhost:8080/api/departments \
  -H "Authorization: Bearer <patient_token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Хірургія"}'

# AC7: спроба змінити статус прийому як PATIENT -> 403 (@PreAuthorize у AppointmentServiceImpl)
curl -i -X PATCH http://localhost:8080/api/appointments/1/status \
  -H "Authorization: Bearer <patient_token>" \
  -H "Content-Type: application/json" \
  -d '{"status":"COMPLETED"}'
```

### Крок 6. Postman

Імпортуйте `postman/Clinic-Service-Lab1.postman_collection.json` — колекція вже містить:
- запити на отримання токенів для admin1/doctor1/patient1 (токени автоматично зберігаються у змінні колекції);
- CRUD-запити для всіх сутностей;
- окремі запити, що навмисно демонструють 401/403 (AC6, AC7).

## 5. Ключові архітектурні рішення

- **Два SecurityFilterChain** (`@Order(1)` для `/api/**` з JWT, `@Order(2)` для решти з OIDC) — дозволяють одночасно підтримувати сесійних UI-користувачів і stateless API-клієнтів в одному застосунку.
- **Єдина конвенція ролей** (`ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_PATIENT`) для обох механізмів автентифікації забезпечується кастомними `KeycloakRealmRoleConverter` (для JWT) та `KeycloakOidcUserService` (для OIDC) — тому сервісний шар і `@PreAuthorize` не потребують дублювання логіки під кожен спосіб входу.
- **`keycloak_user_id`** у `Patient`/`Doctor` — зв'язок бізнес-сутності з обліковим записом Keycloak, на основі якого перевіряється "власність" даних (пацієнт бачить лише свою картку, лікар — лише свої прийоми).

## 6. Наступні кроки (для майбутніх лабораторних)

- Електронні медичні картки, призначення, результати аналізів
- Оплата послуг
- Сповіщення (email/SMS)
- Дашборд статистики для ADMIN
