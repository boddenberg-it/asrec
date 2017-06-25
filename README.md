# **A**ndroid **S**creen **REC**order

### Why?

Asrec is designed for QA engineers to easily document bugs more descriptive by attaching screenshots and videos.

### How does the UI look like?
![screenshot of asrec UI](https://blobb.me/boddenberg-it/asrec_screenshot.png)

### Nice, what do I need to get started?

Basically, an adb daemon must be installed on your machine. One can install it via [android-tools-adb](https://packages.debian.org/jessie/android-tools-adb) debian package. Furthermore Google also provides [SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html) for Linux, MacOS and Windows environments.

Then download the [latest release](https://github.com/boddenberg-it/asrec/releases/), unzip it and execute:
```java -jar asrec.jar```

Alternatively, one can clone this repository and execute:
```./asrec.groovy```

**Note**: Invoking asrec.jar only requires a JRE. Invoking asrec.groovy directly requires a Groovy and JDK installation.
