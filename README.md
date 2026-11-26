# জনসেবা লটারি — Android starter project

দুটি আলাদা Android অ্যাপ:
- `userApp`: Google sign-in, প্রোফাইল, অ্যাকাউন্টের active/blocked অবস্থা দেখা।
- `adminApp`: শুধু Firebase custom claim `admin=true` থাকা অ্যাডমিনের জন্য ইউজার তালিকা ও block/unblock।

**গুরুত্বপূর্ণ:** এই প্রজেক্টটি বিনামূল্যের/ডেমো ড্র-এর starter। বাস্তব অর্থে টিকিট বিক্রি, পেমেন্ট বা পুরস্কার বিতরণ চালুর আগে সংশ্লিষ্ট দেশের আইন, লাইসেন্স এবং পেমেন্ট প্রদানকারীর লিখিত অনুমোদন যাচাই করুন। এই starter-এ কোনো real-money lottery বা bKash collection নেই।

## প্রয়োজনীয়তা
- Android Studio (Ladybug বা নতুন)
- JDK 17
- Firebase project-এর Spark plan
- Firebase Authentication-এ Google provider enabled
- Cloud Firestore তৈরি
- Android app registration (দুটি package name আলাদা)

## Firebase সেটআপ
1. Firebase Console-এ project তৈরি করুন।
2. Authentication → Sign-in method → Google enable করুন।
3. Firestore Database তৈরি করুন।
4. Android app দুটো register করুন:
   - `com.jonseba.user`
   - `com.jonseba.admin`
5. প্রতিটি app-এর `google-services.json` আলাদা করে ডাউনলোড করে সংশ্লিষ্ট module-এর ফোল্ডারে রাখুন:
   - `userApp/google-services.json`
   - `adminApp/google-services.json`
6. Firebase project settings-এ Android app SHA-1 এবং SHA-256 fingerprints যোগ করুন (debug/release আলাদা)।
7. `userApp/src/main/res/values/strings.xml` এবং `adminApp/.../strings.xml`-এ `default_web_client_id`-এর জায়গায় Firebase Console-এর Web client ID বসান। সাধারণত Google provider enable করার পর Web client ID পাওয়া যায়।
8. Firestore → Rules-এ `firestore.rules`-এর নিয়ম publish করুন।

## অ্যাডমিন অ্যাক্সেস নিরাপত্তা
কোনো ইউজার নিজে admin হতে পারবে না। Firebase Admin SDK-সহ trusted server environment থেকে কেবল নির্দিষ্ট অ্যাডমিন UID-তে custom claim সেট করুন:
```js
await getAuth().setCustomUserClaims("ADMIN_UID_HERE", { admin: true });
```
এটি trusted server/Cloud Shell-এ চালাতে হবে; Android অ্যাপ থেকে কখনো custom claim দেওয়া যাবে না। এরপর অ্যাডমিনকে sign out করে আবার sign in করতে হবে।

## Build
Android Studio-তে project folder খুলুন, Gradle sync করুন, দুই module আলাদাভাবে Run করুন।
```bash
./gradlew :userApp:assembleDebug
./gradlew :adminApp:assembleDebug
```
APK output:
- `userApp/build/outputs/apk/debug/userApp-debug.apk`
- `adminApp/build/outputs/apk/debug/adminApp-debug.apk`

এই পরিবেশে APK compile করা হয়নি; এটি Android Studio-তে build করার source project।

## Firebase Spark সীমাবদ্ধতা
Firebase Authentication ও Firestore-এর free quotas প্রযোজ্য। Cloud Functions সাধারণত Blaze billing plan চায়, তাই এই starter Cloud Functions ব্যবহার করে না। Google sign-in-এর জন্য OAuth configuration এবং SHA fingerprints ঠিক থাকা জরুরি।
