# ADB Media Remote

ADB Media Remote is a simple application to remote control Android device media playback using ADB commands.

It is developed for and tested on Amazon Fire TV Stick Lite aka sheldon (Android 9), because this device does not have a TV remote with volume keys.
Newer android versions may not provide the media commandline tool.

Following commands are executed on pressing the corresponding button:

```bash
media dispatch [play-pause|next|previous|rewind|fast-forword]
media volume --get
media volume --set INDEX
```

<img src="screenshot.png" alt="screenshot" width="300"/>
