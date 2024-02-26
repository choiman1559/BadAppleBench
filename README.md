# BadApple_Java
Playes BadApple in java using JavaCV and FFmpeg.

<pre>
Usage: BadApple [-abcehlpqrsv] [-ad] [-ar] [-cn] [-bs=<bufferSize>]
                [-d=<delayMilliseconds>] [-dn=<delayNanoseconds>] [-f=ARCHIVE]
                [-t=<ratioValueResize>]
Prints ascii-ed "Bad Apple" video.
  -a, --audio               Play mp4 file's audio
      -ad, --auto-delay     (Experimental) Automatically determines delay length
      -ar, --auto-ratio     (Experimental) Automatically determines downscale
                              ratio
  -b, --buffer-output       use more buffer when print ascii
      -bs, --buffer-size=<bufferSize>
                            Size of Buffer, Default is 8192 bytes.
  -c, --clear               Clear terminal when refresh frame
      -cn, --clear-curses   Clear terminal using ncurses
  -d, --delay=<delayMilliseconds>
                            Set the delay between frames (milliseconds)
      -dn, --delay-nano=<delayNanoseconds>
                            Set the delay between frames (milliseconds)
  -e, --engine              Convert to Ascii art using my own engine
  -f, --file=ARCHIVE        target *.mp4 file to play
  -h, --help                Display a help message
  -l, --loop                Play video by loop
  -p, --pre-render          (Experimental) Pre-Render all the frames to ascii
                              before play the video (Warning: Requires a lot of
                              memory)
  -q, --print-color         print color as well as ascii texts
  -r, --resize              Set whether or not to resize the image
  -s, --sync-audio          Sync audio with video
  -t, --ratio=<ratioValueResize>
                            Aspect ratio value to downscale frames
  -v, --verbose             Print debug log under frame while playing video

</pre>

## Example:
Command #1: `java -jar ./BadApple.jar -a=true -v=true -b=true -bs=10000 -c=true -cn=false -e=true -l=false -p=false -r=true -t=3 -d=12 -s=true -q=true -ad=true -ar=false`

![image](https://github.com/choiman1559/BadApple_Java/assets/43315227/8f79c4ca-8480-46bf-80d1-fff27c2c0bd7)


Command #2: `java -jar ./BadApple.jar -a=true -v=true -b=true -bs=10000 -c=true -cn=false -e=true -l=false -p=false -r=true -t=5 -d=12 -s=true -q=true -ad=true -ar=true -f=<Your MP4 File Here>`

![image](https://github.com/choiman1559/BadApple_Java/assets/43315227/2a6b37aa-1470-45ed-888f-6821d533969f)
