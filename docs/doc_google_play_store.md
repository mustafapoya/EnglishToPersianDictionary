# English-Persian Dictionary — Google Play Store Listing & Publishing Kit

Everything needed to publish and maintain **English-Persian Dictionary** (`net.golbarg.engtoper`) on Google Play: product facts, release notes, store copy, Play Console settings, declarations and a pre-submission checklist.

* **Current app version:** `versionName 2.0` / `versionCode 7` (`app/build.gradle`); the previous release was 1.6 (6)
* **Target audience:** 13+ (not designed for children)
* **Support email:** `contact@golbarg.net`
* **Website:** `https://golbarg.net`
* **Privacy policy:** ⚠️ `https://golbarg.net/eng_to_per_dictionary/privacy_policy/` *(suggested address; confirm it, then use the same one everywhere)*
* **Backend:** none. The dictionary ships inside the app (`app/src/main/assets/eng_per.db`), and the app's only network traffic is Google AdMob and Google Play.

---

## 0. Product & Technical Overview

English-Persian Dictionary is a two-way, fully offline dictionary for Dari and Farsi speakers learning English. It looks up English words in Persian and Persian words in English, speaks English words aloud, and helps words stick with spaced-repetition flashcards, a quiz, a word of the day and a daily goal.

### Technical Profile
| Area | Specification & source |
|---|---|
| **Application ID** | `net.golbarg.engtoper` |
| **Current release** | Version `2.0` (build `7`), set in `app/build.gradle` |
| **SDK compatibility** | `minSdk 28` (Android 9.0) · `compileSdk 36` · `targetSdk 36` |
| **Dictionary data** | `eng_per.db` in assets (about 33 MB): **146,315** English entries (`phrase`) and **113,328** Persian entries (`phrase2`). Copied to the app's database folder on first launch (`OfflineDatabaseHandler`); saved words are stored in this database too. |
| **Storage** | SQLite: `eng_per.db` (dictionary and saved words) and `dictionary_db` (search history, flashcard progress). `SharedPreferences` `app_prefs` (settings, streak, ad and rating timings). Everything stays on the device. |
| **Accounts** | None. No sign-in and no personal data. |
| **Network** | No backend. Only the Google Mobile Ads SDK, the UMP consent SDK and Play In-App Review use the internet. |
| **Notifications** | One local daily reminder (off by default), scheduled with `AlarmManager` (`ReminderScheduler`), re-scheduled after reboot, app update and clock changes. Asks for `POST_NOTIFICATIONS` on Android 13+ when turned on. |
| **Monetization** | Free, with **Google AdMob ads**, all set in `ads/AdConfig.java` (`ADS_ENABLED`, `USE_TEST_ADS`, one line per placement with its real unit ID): adaptive banners on Dictionary and Saved; a native sponsored card on Home; a native ad card every 7 answers in Flashcards; an interstitial at the end of a flashcard session of 8+ cards (at most once per 10 minutes, never in the first 24 hours after install); and a rewarded ad that turns **all** ads off for 24 hours. Ad content is capped at **PG**. Consent through Google UMP (`ads/AdConsent.java`), with "Ad privacy choices" in Settings where required. |
| **Rating** | Google Play In-App Review (`util/InAppReview.java`) after a good flashcard session (≥ 5 cards, ≥ 70 % known first try), once the user has finished 3 sessions on 3 different days and installed at least 3 days ago; at most every 90 days, never right after an interstitial. "Rate the app" in Settings and About opens the Play Store page. |
| **Language & RTL** | The UI is in **English** and **Persian** (right to left), following the phone or chosen in Settings → App language (Android 13+ per-app language settings too, via `locales_config`). Light and dark themes. Persian copy stays neutral between Dari and Farsi. |

> **Store languages vs. app language.** The store listing is in English (the Play default) and Persian. The app itself is in both languages, and the dictionary works in both directions whichever UI language is chosen.

---

### Core Features

#### 0. First launch
* A splash screen prepares the offline dictionary (copies the database the first time), then a three-page intro (translate both ways; listen and save; learn every day) with Skip, Next and Get started. It can be replayed from Settings.

#### 1. Home
* A greeting and a search prompt, the **word of the day**, the **daily goal** with progress and streak, quick tools (English, Persian, Quiz, Random), mini stats (saved · learned), and an **Explore words** list with shuffle.

#### 2. Dictionary
* One screen for both directions, **English → Persian** and **Persian → English**, with a swap button. Results appear as you type, matching **starts with**, **contains** or **exact match**.
* If you type in the other script, it offers to switch direction. If nothing is found, **"Did you mean…?"** suggests close spellings.
* **Voice search** (English or Persian, through the phone's speech recognizer) and **recent searches** per direction.
* **Word detail sheet:** grouped meanings, subject meanings and related phrases; **pronounce**, **copy**, **share**, **bookmark** and **reverse search** from a meaning.

#### 3. Flashcards (Cards tab)
* Spaced repetition (Leitner, 5 levels; intervals 1, 3, 7, 14 and 30 days).
* Decks: **Due** today, **Saved** words, or **Random** words. Tap to flip; swipe toward the end for **Know it**, the other way for **Again**, or use the buttons. Undo the last answer.
* Show the word first or the meaning first (Settings). A session summary shows known, missed and learned, with **Review missed** and **New session**.

#### 4. Saved words
* Every starred word, searchable, in both languages, with **Practise** to study them as flashcards; remove with undo.

#### 5. Quiz
* A quick multiple-choice vocabulary quiz with a streak counter and a final score.

#### 6. Look up from any app
* **Dictionary** in other apps' text-selection menu and share sheet (`LookupActivity`): the word's detail sheet opens over the current app. English inflections are tried (studied → study, notes → note); with no exact match, or for a long selection, the full dictionary opens searching for it.
* **App shortcuts** (long-press the icon): Search English, Search Persian, Review cards.

#### 7. Daily reminder
* An optional notification at a chosen time with the word of the day and a **Review cards** action.

#### 8. Settings and About
* **Appearance:** theme (system, light, dark) and app language.
* **Dictionary:** default search direction and search match.
* **Pronunciation:** speaking speed and American or British accent, with a sample.
* **Learning:** daily goal, meaning first, daily reminder and its time.
* **Ads:** no ads for 24 hours (rewarded video), ad privacy choices (where required).
* **Data:** clear search history, reset flashcard progress.
* **Support & info:** show introduction, rate the app, share the app, send feedback, About.
* **About:** logo, version, live entry counts, features, developer (Golbarg), website, email and Google Play links.

---

## 1. Release Notes (Version 2.0)

> **Play Console limit:** 500 characters per language. Both languages are also in [store_listing_translations.md](store_listing_translations.md).

### English (en-US)
```
📖 Version 2.0, a whole new dictionary
• A fresh design, in light and dark, in English or Persian
• New home: word of the day, daily goal and streak
• Flashcards with spaced repetition: swipe to answer
• Look up a word from any app: select it and tap Dictionary
• "Did you mean…?" for misspelled words
• Daily reminder and app icon shortcuts
• Watch one video to turn off ads for 24 hours
• Many fixes and improvements
```
*(417 characters)*

### Persian / فارسی (fa-IR)
```
📖 نسخهٔ ۲.۰، دیکشنری‌ای سراپا تازه
• طراحی تازه، با پوستهٔ روشن و تیره، به فارسی یا انگلیسی
• صفحهٔ خانهٔ تازه: واژه روز، هدف روزانه و روزهای پیاپی
• فلش‌کارت با تکرار فاصله‌دار: برای پاسخ، کارت را بکشید
• جستجوی واژه از هر برنامه: انتخابش کنید و «دیکشنری» را بزنید
• پیشنهاد «منظورتان این بود؟» برای واژه‌های اشتباه
• یادآور روزانه و میان‌برهای آیکون برنامه
• با دیدن یک ویدیو، ۲۴ ساعت بدون تبلیغ
• رفع اشکال‌ها و بهبودهای فراوان
```
*(430 characters)*

---

## 2. Store Listing Copy (English, default)

> 💡 The Persian listing (title, short description, full description and release notes) is in
> 👉 [store_listing_translations.md](store_listing_translations.md)

### App Title
> **Limit:** 30 characters. Same as the app's name on the phone.
```
English-Persian Dictionary
```
*(26 characters)*

### Short Description
> **Limit:** 80 characters. Front-loaded for search.
```
Offline English ⇄ Persian dictionary with pronunciation and smart flashcards
```
*(76 characters)*

### Full Description
> **Limit:** 4,000 characters.

```
English-Persian Dictionary is a fast, two-way dictionary that works completely offline. Look up English words in Persian and Persian words in English, hear how they are pronounced, and make new words stick with flashcards and short daily practice. Made for Dari and Farsi speakers, students, and anyone learning English.

━━━━━━━━━━━━━━━━━━━━
📖 TWO-WAY, FULLY OFFLINE
━━━━━━━━━━━━━━━━━━━━
• More than 140,000 English and 110,000 Persian entries, all on your phone. No internet needed.
• Results appear as you type: words that start with, contain or exactly match your search.
• Every meaning clearly grouped, with subject meanings and related phrases.
• "Did you mean…?" suggestions when a word is misspelled.
• Type in either language: the app notices and offers to switch direction.
• Voice search, recent searches, and a reverse search from any meaning.

━━━━━━━━━━━━━━━━━━━━
🔊 PRONUNCIATION
━━━━━━━━━━━━━━━━━━━━
• Hear any English word in an American or British accent, at slow, normal or fast speed.

━━━━━━━━━━━━━━━━━━━━
🃏 FLASHCARDS THAT REMEMBER FOR YOU
━━━━━━━━━━━━━━━━━━━━
• Swipe one way for "Know it", the other for "Again".
• Spaced repetition brings each word back just in time: tomorrow, in 3 days, a week, two weeks, a month.
• Study the cards due today, your saved words, or random words, with the word or the meaning first.
• A daily goal and a streak keep you going.

━━━━━━━━━━━━━━━━━━━━
⭐ SAVE, QUIZ & WORD OF THE DAY
━━━━━━━━━━━━━━━━━━━━
• Star words to keep them in your own searchable list, ready to practise.
• A new word of the day and quick vocabulary quizzes.
• An optional daily reminder at the time you choose.

━━━━━━━━━━━━━━━━━━━━
⚡ RIGHT WHERE YOU READ
━━━━━━━━━━━━━━━━━━━━
• Select a word in any app (a browser, an ebook, a chat) and tap "Dictionary" to see its meaning without leaving that app. "studied" finds "study", too.
• Or share text to the dictionary from any app.
• Long-press the app icon to jump straight to an English or Persian search, or to your flashcards.
• Copy or share any word with its meaning.

━━━━━━━━━━━━━━━━━━━━
🌙 COMFORTABLE TO USE
━━━━━━━━━━━━━━━━━━━━
• Use the app in English or Persian, right to left in Persian.
• Light and dark themes.
• No account and no sign-in: your saved words and progress stay on your phone.

The app is free and shows a few ads, kept out of your way. Watch one short video to turn off every ad for 24 hours.

Download English-Persian Dictionary and keep every word you need in your pocket, online or off.
```
*(2,494 characters)*

---

## 3. Store Listing Configuration & Categorization

| Field | Value | Notes |
|---|---|---|
| **App category** | **Books & Reference** *(recommended)* | Where most dictionaries sit; *Education* is the alternative. Your choice. |
| **Tags** | *Dictionary, Language learning, Vocabulary* *(recommended)* | Pick the closest from the Play Console tag list. |
| **Contact email** | `contact@golbarg.net` | Same address as Send feedback and the About screen. |
| **Website** | `https://golbarg.net` | Same as the About screen. |
| **Privacy policy URL** | ⚠️ `https://golbarg.net/eng_to_per_dictionary/privacy_policy/` | Publish the text of [privacy_policy.md](privacy_policy.md) (English and Persian) there. It describes the app as it ships: no account, no backend, data kept on the device, AdMob ads with UMP consent. |
| **Account deletion URL** | Not applicable | The app has no accounts. |
| **Contains ads** | **Yes** | Google AdMob: banner, native, interstitial and rewarded ads. |
| **In-app purchases** | **No** | No billing library. The 24-hour ad-free pass is earned by a rewarded video, not bought. |
| **Price** | **Free** | |

### Target Audience & Content Rating
* **Target age:** **13 and older.** Do not select any age group under 13: that would put the app under the Families policy, which needs Families-certified ad settings this app doesn't use. Answer *No* to "could the app unintentionally appeal to children?"; it is a general reference tool.
* **Content rating questionnaire:** category *Reference, News, or Educational*. No violence, no sexual content, no gambling, no user-to-user interaction, no user-generated content, no location sharing. A full dictionary also lists crude and offensive words with their meanings; answer the questions about language honestly from that. Expected rating: **Everyone / PEGI 3** (possibly *Teen / PEGI 12* if the language answers call for it). The final rating comes from the questionnaire.

---

## 4. Screenshot Storyboard (8 Phone Screenshots)

Capture on a phone at 1080 × 2400 (or any 9:16 to 9:20 phone size), with real content. Take the English set with the app in English and the Persian set with the app in Persian, and put each caption in the language of its listing. Turn ads off for the captures (an active 24-hour ad-free pass, or `ADS_ENABLED = false` in a local build).

| Slot | Screen | English caption | Persian caption |
|:---:|---|---|---|
| **1** | Home: word of the day, daily goal and streak | *Learn a new word every day* | *هر روز یک واژهٔ تازه* |
| **2** | Dictionary: English → Persian results while typing | *English ⇄ Persian, fully offline* | *انگلیسی ⇄ فارسی، کاملاً آفلاین* |
| **3** | Word detail sheet with meanings and the pronounce button | *Every meaning, with pronunciation* | *همهٔ معنی‌ها، با تلفظ* |
| **4** | Dictionary: Persian → English | *Search in Persian too* | *به فارسی هم جستجو کنید* |
| **5** | Flashcards: a card mid-swipe with Again / Know it | *Flashcards that remember for you* | *فلش‌کارت‌هایی که به خاطر می‌سپارند* |
| **6** | Lookup sheet over a browser or ebook page | *Look up words in any app* | *جستجوی واژه در هر برنامه* |
| **7** | Saved words list | *Keep the words you want to learn* | *واژه‌هایی را که می‌خواهید نگه دارید* |
| **8** | Home or dictionary in dark theme | *Comfortable in light and dark* | *راحت در پوستهٔ روشن و تیره* |

---

## 5. Play Console Declarations & Data Safety

### Data Safety Form

| Section / question | Answer | Why |
|---|---|---|
| **Does the app collect or share user data?** | **Yes** | Only through the Google Mobile Ads SDK, for ads. The app itself has no backend, account or analytics. |
| **Is data encrypted in transit?** | **Yes** | The AdMob, UMP and Play SDKs use HTTPS. The app makes no requests of its own. |
| **Can users request deletion?** | **No in-app request** | The app holds no user data off the device; Settings → Data and clearing app data remove what is on it. Ad data is held by Google and managed through Google's controls (reset advertising ID, opt out of personalised ads). |
| **Device or other IDs** | **Collected and shared** | Advertising ID, by AdMob. Purposes: advertising or marketing, analytics, fraud prevention, security and compliance. Not optional. |
| **Location → Approximate location** | **Collected and shared** | Inferred from the IP address by AdMob. Purposes: advertising, analytics, fraud prevention. |
| **App activity → App interactions** | **Collected and shared** | Ad views and taps, by AdMob. Purposes: advertising, analytics. |
| **App info and performance → Diagnostics** | **Collected** | Ad SDK diagnostics, by AdMob. Purpose: analytics. |
| **Audio, personal info, contacts, photos, files, financial info** | **Not collected** | No such permissions. Voice search hands off to the phone's speech recognizer through an intent; the app only receives text and never leaves the phone with it. Searches, saved words and progress never leave the device. |

> These rows follow Google's published AdMob data disclosure; check them against [the current AdMob guidance for the Data safety form](https://support.google.com/admob/answer/11128406) when you fill it in, since Google updates it.

### Other Declarations
* **Advertising ID:** **Yes**, the app uses it (the manifest declares `com.google.android.gms.permission.AD_ID`). Purpose: advertising or marketing.
* **Foreground services:** none.
* **Exact alarms:** not used. The reminder uses an inexact `setWindow` alarm, so no `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` declaration is needed.
* **Sensitive permissions:** none. Declared permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` (ads), `POST_NOTIFICATIONS` (reminder, Android 13+), `RECEIVE_BOOT_COMPLETED` (re-schedule the reminder), `AD_ID`.
* **News app / Government app / Financial features / Health:** No.

---

## 6. Pre-Submission Checklist

- [x] **Version:** `versionCode 7` / `versionName 2.0` in `app/build.gradle`.
- [ ] **Real ads:** set `AdConfig.USE_TEST_ADS = false` before building the release (it is `true` now); `AdConfig.ADS_ENABLED = true` (it is `false` now). The real AdMob app ID `ca-app-pub-8976959600358837~3318589324` is already in `AndroidManifest.xml`. Never tap your own live ads; test with `USE_TEST_ADS = true` or add your phone as a test device in AdMob.
- [ ] **Ad units:** confirm in AdMob that all six real unit IDs in `AdConfig.Placement` belong to the app `~3318589324`.
- [ ] **EU/UK consent:** create and publish a GDPR message in AdMob → Privacy & messaging for this app. The UMP SDK is already in the app, but until a message exists it shows no form ("no form(s) configured") and EEA/UK users get limited ads.
- [ ] **app-ads.txt:** publish `google.com, pub-8976959600358837, DIRECT, f08c47fec0942fa0` at `https://golbarg.net/app-ads.txt` (the website listed in Play Console).
- [x] **Advertising ID:** declared (`AD_ID` permission kept); answer *Yes* in Play Console.
- [ ] **Privacy policy page:** confirm the ⚠️ items in [privacy_policy.md](privacy_policy.md) and the URL above, then publish it there.
- [ ] **Privacy policy inside the app:** Google Play's User Data policy asks for the policy to be reachable in the app as well as in Play Console. The app has no link yet; add one (e.g. a *Privacy policy* row in Settings → Support & info and in About, opened with `AppLinks.openUrl`).
- [ ] **In-app review:** check the rating sheet on a build installed from the Internal testing track or internal app sharing (it never appears in a build from Android Studio).
- [ ] **Cleartext traffic (optional):** `android:usesCleartextTraffic="true"` in the manifest is a leftover; the app makes no HTTP requests of its own, so it can be removed.
- [ ] **Backup (optional):** `allowBackup="true"` with empty backup rules. The 33 MB dictionary copy is larger than Android Auto Backup's 25 MB limit, so backups of the app are likely skipped, saved words included (they live in that database). Decide whether to keep backup and, if so, move saved words out of `eng_per.db` or exclude it.
- [x] **Store copy:** English (default) and Persian in this file and [store_listing_translations.md](store_listing_translations.md).
- [ ] **RTL & themes:** check every screen in Persian (right to left) and English, in light and dark, and at a larger font size.
- [ ] **Shrinking (optional):** `minifyEnabled false` in the `release` build type. Enable R8 (`minifyEnabled true`, `shrinkResources true`) and retest before upload if a smaller download matters; most of the size is the dictionary database, which R8 does not shrink.
- [ ] **Signing:** build the release bundle with the upload key.
- [ ] **App bundle:** run `gradlew :app:bundleRelease` with JDK 21 (the Gradle in this project does not run on newer JDKs) and upload the `.aab` to the Internal testing track first.
