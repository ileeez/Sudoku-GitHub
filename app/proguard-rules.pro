# Sudoku Cat release rules
-keep class com.sudokucat.app.data.local.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
