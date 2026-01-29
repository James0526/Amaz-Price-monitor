# Amaz Price Monitor

Track up to 12 Amazon product links locally, show the latest price, and get alerts when a price drops.

## Features
- Stores up to 12 Amazon links on device (Room database).
- Hides empty slots automatically.
- Displays a short product title and latest price.
- Shows the last updated time at the top.
- Fetches current prices when the app opens.
- Optional price-drop notifications showing previous and new price.

## Setup
1. Open the project in Android Studio.
2. Add your API settings to `local.properties` (this file is ignored by git):

```
AMAZON_API_BASE_URL=https://your-price-api.example/  # keep trailing slash
AMAZON_API_KEY=your_private_key_here
```

3. Sync Gradle and run on a device/emulator.

## API Contract
The app calls:
```
GET {AMAZON_API_BASE_URL}/price?url=<amazon_product_url>
Header: x-api-key: <AMAZON_API_KEY>
```

Expected JSON response:
```json
{
  "title": "Product name",
  "price": "$19.99"
}
```

If your provider uses a different endpoint or response, update:
`app/src/main/java/com/example/amazpricetracker/data/network/AmazonPriceApi.kt`.

## Build APK
- Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**
- Or CLI (when Gradle wrapper is available):
```
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Using the App
1. Paste an Amazon product link.
2. Tap **Add** to save it.
3. Toggle **Price drop alert** to receive notifications.
4. Tap the trash icon to delete a link.

## Notes on API Key Security
The API key is read from `local.properties` and is not committed to git.  
For production security, route requests through your own backend so the key
is never shipped inside the APK.
