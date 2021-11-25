# BadApple_Java
Playes BadApple in java using JavaCV and FFmpeg.

```
Usage: BadAppleJava [-acehlrs] [-d=<delayMilliseconds>]
                    [-dn=<delayNanoseconds>] [-f=ARCHIVE]
                    [-t=<ratioValueResize>]
Prints ascii-ed "Bad Apple" video.
  -a, --audio          Play mp4 file's audio
  -c, --clear          Clear terminal when refresh frame
  -d, --delay=<delayMilliseconds>
                       Set the delay between frames (milliseconds)
      -dn, --delay-nano=<delayNanoseconds>
                       Set the delay between frames (milliseconds)
  -e, --engine         Convert to Ascii art using my own engine
  -f, --file=ARCHIVE   target *.mp4 file to play
  -h, --help           Display a help message
  -l, --loop           Play video by loop
  -r, --resize         Set whether or not to resize the image
  -s, --sync           Sync audio with video
  -t, --ratio=<ratioValueResize>
                       Ratio value when resetting frame size
```

example:
`java -jar BadApple.main.jar -t 2 -s=false -d 27 -a=true -e`

<img src="https://i.ibb.co/qnPW1dZ/2021-06-07-12-36-51.png" alt="drawing" width="400"/> <img src="https://i.ibb.co/XYTXbhQ/2021-06-07-12-00-40.png" alt="drawing" width="400"/>
