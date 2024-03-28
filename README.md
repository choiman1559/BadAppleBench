# BadAppleMark

`Bad Apple!! + BenchMark`

Play a video file converted to text using JavaCV and FFMPEG in the terminal to benchmark the performance of the terminal emulator. </br>
It is recommended that the quality of the original video used is 720P or lower. </br>

## Requirement

Minimum requirement:
 - Sound output device that supports MPEG AAC audio codec
 - Terminal emulator that supports ANSI text-ed color
 - Java 8+ Runtime
 - 1GB or more of free memory

Recommended requirement:
 - Processor with 4 or more threads
 - 2GB or more of free memory
 - Terminal emulator with GPU acceleration

## Usage
<pre>
<pre>BadApple-Java, Written By.Choiman1559, Version: 2.1.0
Usage: <b>BadApple</b> [<font color="#A2734C">-abcehlmpqrsv</font>] [<font color="#A2734C">-ad</font>] [<font color="#A2734C">-ar</font>] [<font color="#A2734C">-cn</font>] [<font color="#A2734C">-bs</font>=<i>&lt;bufferSize&gt;</i>]
                [<font color="#A2734C">-d</font>=<i>&lt;delayMilliseconds&gt;</i>] [<font color="#A2734C">-dn</font>=<i>&lt;delayNanoseconds&gt;</i>] [<font color="#A2734C">-f</font>=<i>ARCHIVE</i>]
                [<font color="#A2734C">-mo</font>=<i>ARCHIVE</i>] [<font color="#A2734C">-t</font>=<i>&lt;ratioValueResize&gt;</i>]
Prints ascii-ed &quot;Bad Apple&quot; video.
  <font color="#A2734C">-a</font>, <font color="#A2734C">--audio</font>               Play mp4 file&apos;s audio
      <font color="#A2734C">-ad, --auto-delay</font>     (Experimental) Automatically determines delay length
      <font color="#A2734C">-ar, --auto-ratio</font>     (Experimental) Automatically determines downscale
                              ratio
  <font color="#A2734C">-b</font>, <font color="#A2734C">--buffer-output</font>       use more buffer when print ascii
      <font color="#A2734C">-bs, --buffer-size</font>=<i>&lt;bufferSize&gt;</i>
                            Size of Buffer, Default is 8192 bytes.
  <font color="#A2734C">-c</font>, <font color="#A2734C">--clear</font>               Clear terminal when refresh frame
      <font color="#A2734C">-cn, --clear-curses</font>   Clear terminal using ncurses
  <font color="#A2734C">-d</font>, <font color="#A2734C">--delay</font>=<i>&lt;delayMilliseconds&gt;</i>
                            Set the delay between frames (milliseconds)
      <font color="#A2734C">-dn, --delay-nano</font>=<i>&lt;delayNanoseconds&gt;</i>
                            Set the delay between frames (milliseconds)
  <font color="#A2734C">-e</font>, <font color="#A2734C">--engine</font>              Convert to Ascii art using my own engine
  <font color="#A2734C">-f</font>, <font color="#A2734C">--file</font>=<i>ARCHIVE</i>        target *.mp4 file to play
  <font color="#A2734C">-h</font>, <font color="#A2734C">--help</font>                Display a help message
  <font color="#A2734C">-l</font>, <font color="#A2734C">--loop</font>                Play video by loop
  <font color="#A2734C">-m</font>, <font color="#A2734C">--benchmark</font>           Record and measure the performance of terminal
                              emulators.
      <font color="#A2734C">-mo, --bench-output</font>=<i>ARCHIVE</i>
                            Destination folder to save analysis results
  <font color="#A2734C">-p</font>, <font color="#A2734C">--pre-render</font>          (Experimental) Pre-Render all the frames to ascii
                              before play the video (Warning: Requires a lot of
                              memory)
  <font color="#A2734C">-q</font>, <font color="#A2734C">--print-color</font>         print color as well as ascii texts
  <font color="#A2734C">-r</font>, <font color="#A2734C">--resize</font>              Set whether to resize the image
  <font color="#A2734C">-s</font>, <font color="#A2734C">--sync-audio</font>          Sync audio with video
  <font color="#A2734C">-t</font>, <font color="#A2734C">--ratio</font>=<i>&lt;ratioValueResize&gt;</i>
                            Aspect ratio value to downscale frames
  <font color="#A2734C">-v</font>, <font color="#A2734C">--verbose</font>             Print debug log under frame while playing video</pre>
</pre>

## Examples

### Example 01
Command: `java -jar ./BadApple.jar -a=true -v=true -b=true -bs=10000 -c=true -cn=false -e=true -l=false -p=false -r=true -t=3 -d=12 -s=true -q=true -ad=true -ar=false`

Original Video: [Bad Apple!!](https://www.youtube.com/watch?v=i41KoE0iMYU)

![image](https://github.com/choiman1559/BadApple_Java/assets/43315227/8f79c4ca-8480-46bf-80d1-fff27c2c0bd7)

Frame Analytics Data:
![frameRate_BadApple](https://github.com/choiman1559/BadAppleMark/assets/43315227/256df070-ebef-404c-828e-2f69fd0fc210)

Original Video: [Never Gonna Give You Up](https://www.youtube.com/watch?v=dQw4w9WgXcQ) ~LOL~

### Example 02 
Command: `java -jar ./BadApple.jar -a=true -v=true -b=true -bs=10000 -c=true -cn=false -e=true -l=false -p=false -r=true -t=5 -d=12 -s=true -q=true -ad=true -ar=true -f=<Your MP4 File Here>`

![image](https://github.com/choiman1559/BadApple_Java/assets/43315227/2a6b37aa-1470-45ed-888f-6821d533969f)

Frame Analytics Data:
![frameRate_Rick](https://github.com/choiman1559/BadAppleMark/assets/43315227/9f8905d0-f6e5-4144-b877-597c0b8340ae)


### Example 03
Command: `java -jar ./BadApple.jar -a=true -v=true -b=true -bs=10000 -c=true -cn=false -e=true -l=false -p=false -r=true -t=4 -d=12 -s=true -q=true -ad=true -ar=false -f=<Your MP4 File Here>`

Original Video: [言って｡](https://youtu.be/F64yFFnZfkI)

![image](https://github.com/choiman1559/BadApple_Java/assets/43315227/f8b31211-8ed5-4806-adec-870534f3de54)

Frame Analytics Data:
![frameRate_TellMe](https://github.com/choiman1559/BadAppleMark/assets/43315227/298fc113-a033-4128-a71f-475fbdcb9408)
