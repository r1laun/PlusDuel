# Гайд для новичка: публикация PlusDuel в Google Play (TWA)

Актуально на 2026 год. Действуй по порядку, каждый шаг проверяй.

---

## 0. Что у тебя уже есть

| Файл | Где | Зачем |
|------|-----|-------|
| `PlusDuel.aab` | `PlusDuel - Google Play package/` | Сам билд для Console (НЕ APK) |
| `signing.keystore` + пароль `dZ7m_mr5tdh_` | там же | Подпись апдейтов. Храни в надёжном месте, без него не обновить приложение |
| `assetlinks.json` | `client/public/.well-known/` | Связка сайта и приложения, уже на сервере и отдаёт 200 |
| `privacy.html` | `client/public/` | Политика конфиденциальности, уже задеплоена |
| `feature-graphic-1024x500.png` | `play-store/` | Баннер для стора |
| `icon-512.png` | `client/public/` | Иконка 512×512 для стора |
| `store-listing-en.txt` | `play-store/` | Готовые тексты описания |
| `data-safety-draft.txt` | `play-store/` | Готовые ответы для Data safety |

Пакет: `com.onrender.plusduel.twa` — его уже не сменить без нового приложения.

---

## 1. Создать приложение в Play Console

1. Открой https://play.google.com/console
2. Прими соглашение разработчика ($25 единоразово, если ещё не оплачено)
3. **Create app**
4. Заполни:
   - **App name:** `PlusDuel`
   - **Default language:** English
   - **App or Game:** Game
   - **Free or Paid:** Free
5. Отметь чекбоксы: *Yes, I have complied with US export laws* (если верно), *Yes, this app meets the Content Guidelines* и *I confirm this app meets the target audience requirements*
6. Нажми **Create app**

---

## 2. Пройти чеклист «Set up your app»

Главная страница приложения → **Set up your app** (или боковая панель → **App content**).

### 2.1 Privacy policy
- Нажми **Start**
- **Privacy policy URL:** `https://plusduel.onrender.com/privacy.html`
- Убедись что ссылка открывается в браузере и не 404

### 2.2 App access
- Выбери **All functionality is available without special access**

### 2.3 Ads
- Выбери **No, my app does not contain ads**

### 2.4 Content rating
- Нажми **Start**
- Ответь на вопросы IARC. Для PlusDuel:
  - **Violence:** No
  - **Profanity:** No
  - **Sexual content:** No
  - **Gambling:** No
  - **Horror:** No
  - **Controlled substances:** No
  - **Crime:** No
  - **Online interactions:** Yes (игрок общается с другими через ник)
- В конце будет Everyone / Low Maturity

### 2.5 Target audience
- Выбери **13+** (или 18+ если хочешь перестраховаться)
- Обязательно укажи что приложение не предназначено для детей

### 2.6 Data safety
- Нажми **Start**
- Следуй `play-store/data-safety-draft.txt` — там готовые ответы на каждый вопрос

### 2.7 Category & contacts
- **Category:** Game > Puzzle
- **Tags (необязательно):** math, puzzle, multiplayer, duel
- **Contact email:** `nuraly2907@icloud.com`
- **Website:** `https://plusduel.onrender.com/`

---

## 3. Store listing

Боковое меню → **Main store listing**.

### Обязательные поля:
- **App name:** `PlusDuel`
- **Short description (80 символов):**
  ```
  1v1 math duels: build the target from every digit. Faster wins!
  ```
- **Full description (4000 символов):** скопируй из `play-store/store-listing-en.txt`

### Графика:
- **App icon (512×512):** `client/public/icon-512.png`
- **Feature graphic (1024×500):** `play-store/feature-graphic-1024x500.png`
- **Phone screenshots (мин. 2):** открой игру на телефоне, сними главный экран, дуэль и соло-режим. Сохрани в `play-store/screenshots/`

---

## 4. Залить AAB

1. Боковое меню → **Release** → **Testing** → **Internal testing**
2. Нажми **Create new release**
3. **Upload AAB:** выбери `PlusDuel - Google Play package/PlusDuel.aab`
4. После загрузки → **Save** → **Review release** → **Start rollout to Internal testing**
5. **Выбери тестировщиков:** добавь свой email в список

---

## 5. Play App Signing (важно!)

После первой загрузки Google включит Play App Signing. Это значит что Google переподпишет твой AAB своим ключом.

**Сразу после публикации:**
1. Боковое меню → **Release** → **Setup** → **App signing**
2. Скопируй **SHA-256 App signing key**
3. Открой `client/public/.well-known/assetlinks.json`
4. Вставь новый отпечаток вторым элементом в массив:
   ```json
   [
     {
       "relation": ["delegate_permission/common.handle_all_urls"],
       "target": {
         "namespace": "android_app",
         "package_name": "com.onrender.plusduel.twa",
         "sha256_cert_fingerprints": [
           "F8:C7:DB:4F:93:21:A1:5E:60:C4:CA:77:BC:47:81:5D:63:D6:FF:01:03:B8:80:EE:7D:9F:93:F9:AF:F4:11:EA",
           "ВСТАВЬ_НОВЫЙ_ОТПЕЧАТОК_СЮДА"
         ]
       }
     }
   ]
   ```
5. Запушь + передеплой
6. Проверь что файл отдаёт 200 и содержит оба отпечатка

---

## 6. Closed testing (обязательно для новых аккаунтов)

Google требует минимум 12 тестеров × 14 дней для новых аккаунтов разработчика, прежде чем допустить в Production.

1. **Release → Testing → Closed testing → Create track**
2. Залей тот же AAB (или новый если исправил что-то)
3. **Testers** → добавь email-ы минимум 12 человек (друзья, форумы, Reddit r/AndroidGaming, Discord)
4. Разошли им opt-in ссылку: `https://play.google.com/apps/internaltesting/com.onrender.plusduel.twa`
5. **Жди 14 дней** с момента публикации релиза для закрытого тестирования
6. После 14 дней → нажми **Promote release → Production**

---

## 7. Production

1. **Release → Production → Create release**
2. Залей финальный AAB
3. Проверь что чеклист «Set up your app» полностью зелёный
4. **Review release** → **Start rollout to Production**
5. Google проверяет обычно 1–7 дней (иногда до 14)

---

## Чеклист перед отправкой в Production

- [ ] Privacy policy открывается по URL
- [ ] Ссылка на Privacy есть внутри приложения (футер)
- [ ] Content rating пройден
- [ ] Data safety заполнена
- [ ] Store listing: название, описания, иконка, баннер, 2+ скриншота
- [ ] AAB загружен в Internal testing
- [ ] assetlinks.json содержит оба отпечатка (TWA + Play App Signing)
- [ ] 12+ тестеров прошли Closed testing 14 дней
- [ ] Package name `com.onrender.plusduel.twa` совпадает с assetlinks.json

---

## Если что-то пойдёт не так

| Проблема | Решение |
|----------|---------|
| Console не даёт добавить приложение | Оплати $25, подтверди личность (нужен паспорт/права) |
| AAB не загружается | Убедись что файл `.aab`, не `.apk` |
| TWA открывается с URL-баром браузера | Не добавлен второй отпечаток Play App Signing в assetlinks.json |
| Google отклоняет описание | Убери слова "бесплатно/бесконечно/без рекламы" если не соответствует действительности |
| Data safety не проходит | Сверь с `privacy.html` — форма должна отражать реальные практики |
| Production недоступна | Заверши Closed testing (12 тестеров × 14 дней) |
| Потерял keystore | Приложение не обновить, только удалить и создать новое с другим package |

---

*Гайд актуален для Play Console на 2026. Интерфейс Google может слегка меняться — ищи похожие пункты если названия отличаются.*
