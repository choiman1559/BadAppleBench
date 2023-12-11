# BadApple_Java
Playes BadApple in java using JavaCV and FFmpeg.

<pre>
Usage: <b>BadAppleJava</b> [<font color="#C4A000">-abcehlpqrs</font>] [<font color="#C4A000">-d</font>=<i>&lt;delayMilliseconds&gt;</i>]
                    [<font color="#C4A000">-dn</font>=<i>&lt;delayNanoseconds&gt;</i>] [<font color="#C4A000">-f</font>=<i>ARCHIVE</i>]
                    [<font color="#C4A000">-t</font>=<i>&lt;ratioValueResize&gt;</i>]
Prints ascii-ed &quot;Bad Apple&quot; video.
  <font color="#C4A000">-a</font>, <font color="#C4A000">--audio</font>           Play mp4 file&apos;s audio
  <font color="#C4A000">-b</font>, <font color="#C4A000">--buffer-output</font>   use more buffer when print ascii
  <font color="#C4A000">-c</font>, <font color="#C4A000">--clear</font>           Clear terminal when refresh frame
  <font color="#C4A000">-d</font>, <font color="#C4A000">--delay</font>=<i>&lt;delayMilliseconds&gt;</i>
                        Set the delay between frames (milliseconds)
      <font color="#C4A000">-dn, --delay-nano</font>=<i>&lt;delayNanoseconds&gt;</i>
                        Set the delay between frames (milliseconds)
  <font color="#C4A000">-e</font>, <font color="#C4A000">--engine</font>          Convert to Ascii art using my own engine
  <font color="#C4A000">-f</font>, <font color="#C4A000">--file</font>=<i>ARCHIVE</i>    target *.mp4 file to play
  <font color="#C4A000">-h</font>, <font color="#C4A000">--help</font>            Display a help message
  <font color="#C4A000">-l</font>, <font color="#C4A000">--loop</font>            Play video by loop
  <font color="#C4A000">-p</font>, <font color="#C4A000">--render</font>          Pre-Render the image before play the video
  <font color="#C4A000">-q</font>, <font color="#C4A000">--print-color</font>     print color as well as ascii texts
  <font color="#C4A000">-r</font>, <font color="#C4A000">--resize</font>          Set whether or not to resize the image
  <font color="#C4A000">-s</font>, <font color="#C4A000">--sync</font>            Sync audio with video
  <font color="#C4A000">-t</font>, <font color="#C4A000">--ratio</font>=<i>&lt;ratioValueResize&gt;</i>
                        Ratio value when resetting frame size
                        </pre>

example:
`java -jar BadApple.main.jar -t 2 -s=false -d 27 -a=true -e`

<img src="https://i.ibb.co/qnPW1dZ/2021-06-07-12-36-51.png" alt="drawing" width="400"/> <img src="https://i.ibb.co/XYTXbhQ/2021-06-07-12-00-40.png" alt="drawing" width="400"/>
