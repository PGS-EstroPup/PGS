PGS_Addons Fabric Mod Project (with Gradle wrapper scripts)
=========================================================

This project includes the Gradle wrapper *scripts* and configuration but does NOT include the
`gradle-wrapper.jar` binary. The jar is required for `./gradlew` to run; you have two easy options:

OPTION 1 (recommended if you can install Gradle temporarily):
--------------------------------------------------------------
1. Install Gradle on your system (download from https://gradle.org/releases or use package managers).
2. Run (in project root):
   gradle wrapper
   This generates gradle-wrapper.jar and the complete wrapper under gradle/wrapper/
3. Then run:
   ./gradlew build    (or gradlew.bat build on Windows)

OPTION 2 (download gradle-wrapper.jar manually):
-----------------------------------------------
You can download a standard gradle-wrapper.jar from a trustworthy Gradle installation or from
a project's gradle/wrapper directory online (for example, from the Gradle distribution or an open-source project's repo).
Place the downloaded file at:
   gradle/wrapper/gradle-wrapper.jar
Then run:
   ./gradlew build

OPTION 3 (if you prefer not to install Gradle):
-----------------------------------------------
If you cannot install Gradle, you can ask me to generate a bootstrap gradle-wrapper.jar here.
However, building that jar here may be limited by environment restrictions. Tell me if you'd like me to attempt that.

Build the mod:
--------------

1. Make sure Java 17 is installed and `java -version` shows Java 17.
2. From the project directory, run (Windows):
   gradlew.bat build
   or (macOS/Linux):
   ./gradlew build
3. The mod jar will be generated at:
   build/libs/pgs_addons-1.0.0.jar

Notes:
- The wrapper scripts included will check for gradle-wrapper.jar and will print an informative message if it's missing.
- If you want, I can try to produce gradle-wrapper.jar for you here — say 'yes, generate wrapper jar' and I'll attempt it (may fail depending on environment).

