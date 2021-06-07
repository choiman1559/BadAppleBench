# BadApple_Java
Playes BadApple in java using JavaCV and FFmpeg.

```
Usage: BadAppleJava [-achlrs] [-d=<delayMilliseconds>] [-f=ARCHIVE]
                    [-t=<ratioValueResize>]
Prints ascii-ed "Bad Apple" video.
  -a, --audio          Play mp4 file's audio
  -c, --clear          Clear terminal when refresh frame
  -d, --delay=<delayMilliseconds>
                       Set the delay between frames (milliseconds)
  -f, --file=ARCHIVE   target *.mp4 file to play
  -h, --help           Display a help message
  -l, --loop           Play video by loop
  -r, --resize         Set whether or not to resize the image
  -s, --sync           Sync audio with video
  -t, --ratio=<ratioValueResize>
                       Ratio value when resetting frame size
```

example:
`java -jar BadApple.main.jar -r -s=false -c=false -d 24 -t 4 -a=true`
