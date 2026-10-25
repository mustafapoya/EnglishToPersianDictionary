# English-Persian Dictionary — Google Play Store Kit

Everything to set or answer in Google Play Console for **English-Persian Dictionary** (`net.golbarg.engtoper`). The store text is in [002-store-listing.md](002-store-listing.md) and the privacy policy in [003-privacy-policy.md](003-privacy-policy.md).

| | |
| --- | --- |
| **Application ID** | `net.golbarg.engtoper` |
| **Current version** | `versionName 2.0` / `versionCode 7` — `app/build.gradle` (re-check before every upload) |
| **Store text** | [002-store-listing.md](002-store-listing.md): `en-US` (default), `fa-IR` |
| **Privacy policy** | [003-privacy-policy.md](003-privacy-policy.md), published at `https://golbarg.net/eng_to_per_dictionary/privacy_policy/` |
| **Support email** | `contact@golbarg.net` |
| **Website** | `https://golbarg.net` |
| **Target audience** | 13+ |
| **Ads** | Google AdMob, account `pub-8976959600358837` — **switched off in the current build**, see [§5](#5-ads--monetization) |

**Open items**
- ⚠️ Ads are off: `AdConfig.ADS_ENABLED = false`, `USE_TEST_ADS = true`. Decide whether 2.0 ships with ads. If yes, set `true` / `false`. If no, answer *Contains ads: No* and declare no AdMob data in §6.
- ⚠️ Publish [003](003-privacy-policy.md) at `https://golbarg.net/eng_to_per_dictionary/privacy_policy/` before the release: the app's Settings → Privacy policy row opens that page.
- ⚠️ Confirm in AdMob that all six unit IDs in `AdConfig.Placement` belong to the app `~3318589324`.
- ⚠️ Create and publish a GDPR message in AdMob → Privacy & messaging (until then UMP shows no form and EEA/UK users get limited ads).
- ⚠️ Publish `google.com, pub-8976959600358837, DIRECT, f08c47fec0942fa0` in `https://golbarg.net/app-ads.txt`.
- ⚠️ Backup: `allowBackup="true"` with empty rules. The 33 MB dictionary copy exceeds Auto Backup's 25 MB limit, so backups are likely skipped, saved words included (they live in that database). Decide whether to keep backup, and if so move saved words out of `eng_per.db` or exclude it.
- ⚠️ `android:usesCleartextTraffic="true"` is a leftover; the app makes no HTTP requests of its own, so it can go.

---

## 1. App facts

English-Persian Dictionary is a two-way, fully offline dictionary for Dari and Farsi speakers learning English. It looks up English words in Persian and Persian words in English, speaks English words aloud, and helps words stick with spaced-repetition flashcards, a quiz, a word of the day and a daily goal.

### Technical profile

| | |
| --- | --- |
| **SDK** | `minSdk 28` (Android 9) · `compileSdk 36` · `targetSdk 36` |
| **Content** | `eng_per.db` in assets (about 33 MB): 146,315 English entries (`phrase`) and 113,328 Persian entries (`phrase2`), copied to the database folder on first launch (`OfflineDatabaseHandler`); saved words are stored in this database too |
| **Network** | No backend. Only the Google Mobile Ads SDK, the UMP consent SDK and Play In-App Review use the internet |
| **Storage** | SQLite `eng_per.db` (dictionary, saved words) and `dictionary_db` (search history, flashcard progress); `SharedPreferences` `app_prefs` (settings, streak, rating timings) and `ads` (full-screen ad timing, ad-free pass). Everything stays on the device |
| **Accounts** | None. No sign-in and no personal data |
| **Notifications & widgets** | One local daily reminder (off by default), scheduled with `AlarmManager` (`ReminderScheduler`) and re-scheduled after reboot, app update and clock changes; asks for `POST_NOTIFICATIONS` on Android 13+ when turned on. No widget |
| **Play libraries** | In-app review (`util/InAppReview`): after a good flashcard session (≥ 5 cards, ≥ 70 % known first try), once 3 sessions on 3 different days are done and the app was installed ≥ 3 days ago; at most every 90 days, never right after an interstitial |
| **Monetization** | Free. Google AdMob ads (banners, native, interstitial, rewarded 24 h ad-free pass) when switched on in `AdConfig`; content capped at PG; consent through Google's UMP SDK |
| **Languages** | UI in English and Persian (right to left), following the phone or chosen in Settings → App language (also Android 13+ per-app language settings via `locales_config`). Light and dark themes. Persian copy stays neutral between Dari and Farsi |

### Features

- **First launch:** a splash screen prepares the offline dictionary, then a three-page intro (translate both ways; listen and save; learn every day), replayable from Settings.
- **Home:** greeting and search prompt, word of the day, daily goal with progress and streak, quick tools (English, Persian, Quiz, Random), mini stats, an **Explore words** list with shuffle.
- **Dictionary:**
  - One screen for **English → Persian** and **Persian → English**, with a swap button.
  - Results as you type: starts with, contains or exact match. It offers to switch direction when you type in the other script.
  - **"Did you mean…?"** for misspellings.
  - Voice search and recent searches per direction.
- **Word detail sheet:** grouped meanings, subject meanings and related phrases; pronounce, copy, share, bookmark, reverse search from a meaning.
- **Flashcards:**
  - Leitner spaced repetition (5 levels; 1, 3, 7, 14 and 30 days).
  - Decks: Due, Saved or Random.
  - Swipe for **Know it** / **Again**, with undo, and word-first or meaning-first.
  - A session summary with **Review missed** and **New session**.
- **Saved words:** searchable starred words in both languages, **Practise** as flashcards, remove with undo.
- **Quiz:** multiple-choice vocabulary quiz with a streak counter and final score.
- **Look up from any app:**
  - **Dictionary** in other apps' text-selection menu and share sheet (`LookupActivity`), with English inflections tried (studied → study).
  - App shortcuts: Search English, Search Persian, Review cards.
- **Daily reminder:** an optional notification at a chosen time with the word of the day and a **Review cards** action.
- **Settings:**
  - Theme and app language; default search direction and match; speaking speed and accent.
  - Daily goal, meaning first, reminder and its time.
  - No ads for 24 hours, ad privacy choices (where required); clear history, reset progress.
  - Show introduction, rate, share, feedback, privacy policy, About.
- **About:** logo, version, live entry counts, features, developer (Golbarg), website, email and Google Play links.

---

## 2. Store settings

| Setting | Value | Notes |
| --- | --- | --- |
| **App category** | Books & Reference | Where most dictionaries sit; *Education* is the alternative |
| **Tags** | Dictionary, Language learning, Vocabulary | Pick the closest from the Play Console list |
| **Contact email** | `contact@golbarg.net` | Same as Send feedback and the About screen |
| **Website** | `https://golbarg.net` | Same as the About screen |
| **Privacy policy URL** | `https://golbarg.net/eng_to_per_dictionary/privacy_policy/` | Text from [003](003-privacy-policy.md) |
| **Account deletion URL** | Not applicable | The app has no accounts |
| **Contains ads** | Yes, if 2.0 ships with ads (see Open items) | Banner, native, interstitial and rewarded |
| **In-app purchases** | No | The 24-hour ad-free pass is earned with a rewarded video, not bought |
| **Price** | Free | |

---

## 3. Target audience & content rating

- **Target age groups:** 13–15, 16–17, 18+. Don't select an age group under 13: that would put the app under the Families policy, which needs Families-certified ad settings this app doesn't use.
- **Appeals to children?** No: it's a general reference tool.
- **Content rating (IARC):**
  - Category *Reference, News, or Educational*.
  - No violence, sexual content, gambling, user interaction, user-generated content or location sharing.
  - A full dictionary also lists crude and offensive words with their meanings, so answer the language questions honestly from that.
  - Expected result: Everyone / PEGI 3, possibly Teen / PEGI 12 if the language answers call for it.

---

## 4. Graphics

| Asset | Size | Notes |
| --- | --- | --- |
| App icon | 512 × 512 PNG | |
| Feature graphic | 1024 × 500 | Text in [002](002-store-listing.md) |
| Phone screenshots | 1080 × 2400 (9:16 to 9:20) | Storyboard below; captions in [002](002-store-listing.md) |

### Screenshot storyboard

Take the English set with the app in English and the Persian set with the app in Persian.

| # | Screen | What to show | Caption |
| --- | --- | --- | --- |
| 1 | Home | Word of the day, daily goal and streak | 002 caption 1 |
| 2 | Dictionary | English → Persian results while typing | 002 caption 2 |
| 3 | Word detail sheet | Meanings and the pronounce button | 002 caption 3 |
| 4 | Dictionary | Persian → English | 002 caption 4 |
| 5 | Flashcards | A card mid-swipe with Again / Know it | 002 caption 5 |
| 6 | Lookup sheet | Over a browser or ebook page | 002 caption 6 |
| 7 | Saved words | The saved list | 002 caption 7 |
| 8 | Dark theme | Home or dictionary in dark | 002 caption 8 |

Take screenshots with `AdConfig.ADS_ENABLED = false` so no ad appears.

---

## 5. Ads & monetization

Ads are controlled from `ads/AdConfig` (standard ads package, see `docs/ads` in the MobileApps workspace). **The current build has them off** (`ADS_ENABLED = false`). Content is capped at PG.

| Placement | Format | Where | Rule |
| --- | --- | --- | --- |
| `DICTIONARY_BANNER` | Banner | Dictionary | Hidden while the keyboard is open |
| `SAVED_BANNER` | Banner | Saved words | |
| `HOME_NATIVE` | Native | Sponsored card at the end of Home | Kept across tab switches |
| `FLASHCARD_NATIVE` | Native | An ad card every 7 answers in Flashcards | Skipped if none has loaded |
| `SESSION_END_INTERSTITIAL` | Interstitial | End of a flashcard session | Only after 8+ answers, never in a new user's first 24 h, at most one full-screen ad per 10 minutes |
| `AD_FREE_REWARDED` | Rewarded | "No ads for 24 hours" (Home, Flashcards, Settings) | Opt-in; turns every ad off for 24 h |

### AdMob console

| Item | Status |
| --- | --- |
| App ID `ca-app-pub-8976959600358837~3318589324` | ✅ in `AndroidManifest.xml` |
| Six ad units | ✅ in `AdConfig`; ⚠️ confirm they belong to this app |
| GDPR message | ⚠️ create and publish |
| `app-ads.txt` | ⚠️ publish |
| Store listing linked in AdMob | After the release |

---

## 6. Play Console declarations

### App access
All features are available without special access.

### Ads
Contains ads: Yes if 2.0 ships with ads (see Open items), otherwise No.

### Advertising ID
Yes when ads are on. It is used for Advertising or marketing (Mobile Ads SDK; the manifest declares `com.google.android.gms.permission.AD_ID`).

### Data safety

With ads on:

| Question / data type | Answer | Purpose | Optional? |
| --- | --- | --- | --- |
| Does the app collect or share user data? | Yes, only through the Google Mobile Ads SDK | The app has no backend, account or analytics | |
| Is data encrypted in transit? | Yes | The AdMob, UMP and Play SDKs use HTTPS | |
| Can users request deletion? | No in-app request | Nothing is held off the device; Settings → Data and clearing app data remove what's on it | |
| Device or other IDs | Collected and shared | Advertising or marketing, analytics, fraud prevention, security and compliance | No |
| Approximate location (from IP) | Collected and shared | Advertising, analytics, fraud prevention | No |
| App interactions | Collected and shared | Advertising, analytics | No |
| Diagnostics | Collected | Analytics | No |
| Audio, personal info, contacts, photos, files, financial info | Not collected | Voice search hands off to the phone's speech recognizer through an intent; the app only receives text | |

With ads off: *No data collected or shared*. Check the rows against [Google's AdMob Data safety guidance](https://support.google.com/admob/answer/11128406) when you fill in the form.

### Permissions

| Permission | Why | Play Console form needed? |
| --- | --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Ads | No |
| `POST_NOTIFICATIONS` | Daily reminder, Android 13+ | No |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule the reminder | No |
| `com.google.android.gms.permission.AD_ID` | AdMob | Advertising ID declaration (above) |

No foreground services and no exact alarms: the reminder uses an inexact `setWindow` alarm.

### Other declarations
Government, financial, health, news, COVID-19, data deletion: Not applicable.

---

## 7. Names & content rights

The app name and listing describe the app generically ("English-Persian Dictionary") and use no third-party trademarks. The dictionary data ships inside the app.

---

## 8. Release checklist

- [ ] `versionCode` raised and `versionName` set in `app/build.gradle`; header of this file updated
- [ ] Release notes written in every language in [002](002-store-listing.md)
- [ ] Ads: `AdConfig.ADS_ENABLED` / `USE_TEST_ADS` set for release; Logcat shows `Ads: on, REAL units` (or `Ads: off`). Never tap your own live ads
- [ ] [003](003-privacy-policy.md) still matches the app; if it changed, **Last updated** set and the web page republished
- [ ] Data safety and permissions in §6 still match the merged release manifest and the ads decision
- [ ] Screenshots and captions still match the app; every screen checked in English and Persian (right to left), light and dark, at a larger font size
- [ ] In-app review checked on a build from the Internal testing track (it never appears in a build from Android Studio)
- [ ] Optional: enable R8 (`minifyEnabled true`, `shrinkResources true`) and retest; most of the size is the dictionary database, which R8 doesn't shrink
- [ ] Signed release bundle (`gradlew :app:bundleRelease`, with JDK 21) uploaded to Internal testing first
- [ ] Release history below updated

---

## 9. Release history

| Version | versionCode | Date | Highlights |
| --- | --- | --- | --- |
| 2.0 | 7 | Not released yet | New design in English and Persian, new home with word of the day and daily goal, spaced-repetition flashcards, look up from any app, reminders, shortcuts, 24 h ad-free pass |
| 1.6 | 6 | — | Previous release |
