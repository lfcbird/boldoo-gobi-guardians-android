# Android утсанд суулгах заавар

## Бэлэн APK суулгах

1. GitHub Actions-ийн хамгийн сүүлийн амжилттай build-ээс `boldoo-gobi-guardians-debug` artifact-ыг татна.
2. ZIP файлыг задлаад `app-debug.apk`-г Android утсандаа хуулна.
3. APK-г дарж нээнэ.
4. Анх удаа суулгаж байвал Files/Chrome аппын **Install unknown apps** зөвшөөрлийг түр идэвхжүүлнэ.
5. **Install** товчийг дарж суулгана.
6. **Boldoo: Gobi Guardians** аппыг нээнэ.

Тоглоом интернет шаарддаггүй. Android 6.0 ба түүнээс шинэ хувилбар дээр landscape буюу хөндлөн дэлгэцээр ажиллана.

## Android Studio-оор ажиллуулах

1. Repository-г clone хийнэ.
2. Android Studio-д төслийн үндсэн хавтсыг нээнэ.
3. Gradle sync дуусахыг хүлээнэ.
4. API 23+ emulator эсвэл Android утас сонгоно.
5. **Run** дарна.

## Удирдлага

- Зүүн/баруун сум: хөдөлнө
- Дээш сум: үсэрнэ; хурдан тавибал нам үсэрнэ
- Тусгай товч: Хулангийн хурд эсвэл агаарт Хавтгайн нүдэлт ашиглана
- Баруун дээд pause товч: тоглоомыг түр зогсооно
- Smogling-ийн дээрээс бууж дарвал устгана
- Checkpoint хүрсний дараа унавал тухайн цэгээс үргэлжилнэ

Тохиргооноос дуу, орчны салхи, чичиргээ, товчны тод байдал, зүүн гарын байрлалыг өөрчилж болно. Үе бүрийн шилдэг оноо, хугацаа, од болон нээгдсэн үе утсанд автоматаар хадгалагдана.

## Source-оос APK build хийх

```bash
python3 tools/validate_levels.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK нь `app/build/outputs/apk/debug/app-debug.apk` замд гарна. Release APK нийтлэхдээ өөрийн нууц signing key ашиглана; key болон APK-г repository-д commit хийхгүй.
