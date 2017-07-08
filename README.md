# **A**ndroid **S**creen **REC**order

### Why?

Asrec shall help QA engineers to document bugs more descriptive by easily attaching screenshots, videos and logs.

### How does the UI look like?

Asrec will start in "normal mode" (left), which provides the three base functionalities. The "advanced mode" provides further functionalities, such as setting the battery level, de/increasing brightness, toggle charging and airplane mode,...

![screenshot of asrec UI](https://blobb.me/boddenberg-it/asrec_screenshot_v02.png)

### Nice, what do I need to get started?

Basically, an ADB daemon must be installed. One can install it via [android-tools-adb](https://packages.debian.org/jessie/android-tools-adb) debian package. Furthermore Google also provides [SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools.html) for Linux, MacOS and Windows environments.

Then download the [latest release](https://github.com/boddenberg-it/asrec/releases/), unzip it and execute:

```java -jar asrec.jar```

Alternatively, one can clone this repository and execute:

```./asrec.groovy```

**Note**: Invoking asrec.jar requires a JRE installation. Invoking asrec.groovy directly requires a Groovy and JDK installation.
