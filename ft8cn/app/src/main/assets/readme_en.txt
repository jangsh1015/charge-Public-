Disclaimer

  FT8CN aims to learn how to decode, transmit FT8 signal for research purposes,
  which is not responsible for the consequences caused by the user's operation.
  Please comply with local laws and regulations when using FT8CN.
  Considering the performance and endurance limitations of the mobile phone,
  the processing of the signal adopts lightweight operations instead of deep decoding
  and other processing.
  Please click "FAQ" if you have good suggestions or questions.


Credits

  Original : BG7YOZ / N0BOY (FT8CN)
             https://github.com/N0BOY/FT8CN
             MIT License (see LICENSE)

  Korean translation and intermittent CQ feature : DS2YTO

  FT8/FT4 protocol : Steve Franke (K9AN), Bill Somerville (G4WJS), Joe Taylor (K1JT)

  The full list of original contributors is in readme.txt (Chinese original).


Korean edition changes

  0.93-kr6
   1. On a tablet in landscape, the Decode tab now shows the decode list in two columns
      (left and right), so twice as many stations fit on one screen. The narrow list inside
      the three-pane view stays single-column.
   2. On a tablet in landscape, the Call tab now opens the calling window on the left and
      the QSO log on the right, side by side. Drag the divider to change the widths (30-70%);
      the ratio is kept after a restart.
   3. Tapping a station in the decode list to call it opens the same two-pane view.
   4. The app reopens on the screen you left (decode only / call+log / spectrum three-pane or
      spectrum only). The QSO log and Settings tabs remain full screen, and the Spectrum tab
      remains the three-pane view.
   5. The QSO log title bar now uses the same dark indigo tone as the decode and call lists.

  0.93-kr5
   1. No more endless repeats of the same message to the same station. After 'no reply for N'
      (default 3) attempts of the same message, automatic calling stops and returns to CQ/idle
      even while sending a signal report (R-11). Only an answer that moves the QSO forward
      resets that counter. If the other station sends RR73/RRR, one final 73 is sent and the
      QSO is closed (a 73/RR73/RRR message is never answered with a report).
   2. Contact list colours now follow iPhone FT8-Improved / WSJT-X: CQ = dark green,
      a reply addressed to me = dark pink, my own transmission (TX) = dark red,
      everything else = dark navy. Dark rows with bright text replace the old bright blue.
   3. The vertical bar graph above the waterfall is gone; the waterfall now fills the area
      right below the ruler down to the bottom of the screen.
   4. In the tablet landscape three-pane view the tool buttons (full screen, frequency,
      volume, grid tracker) move to the bottom-right corner of the left waterfall pane
      instead of covering the lists on the right.
   5. In tablet landscape the bottom tabs choose the layout: [Spectrum] opens the three-pane
      view (and pressing it again collapses to the spectrum alone), [Decode] opens only the
      decode list, and [Calling] opens only the calling window. The last layout is remembered
      after a restart.
   6. A failed time sync now shows a short Korean message instead of the raw English
      network error.

  0.93-kr4
   1. On a tablet held in landscape the screen is split into three panes:
      spectrum/waterfall on the left, the decode list at the top right, and the calling
      window at the bottom right. Drag the handle (pill) on either divider to resize,
      and the sizes are remembered after the app restarts.
      (spectrum 30-75%, decode 20-80%)
   2. Softer spectrum colours: the noise background is a deep navy instead of bright blue,
      while real signals stand out in cyan, yellow, orange and red.
   3. Out-of-the-box settings for the IC-705: radio model ICOM IC-705 (CI-V A4),
      115200 baud, SWR/ALC alarms on, and auto-call stops after 3 unanswered calls.
      These are applied once on first launch; later changes are kept as they are.

  0.93-kr3
   1. When auto-follow gives up on a station for no reply, that callsign is excluded
      from automatic re-calls for 30 minutes. (Tapping the callsign yourself, or the
      station calling you first, works as before; after 30 minutes it can be called again.)
   2. A fresh install now starts on 40m (7.074 MHz).

  0.93-kr2
   1. The About window is translated into Korean, with original/editor credits.
   2. Chinese text in the radio model list and in some error messages is localized.
   3. Interval CQ waiting time no longer counts toward the TX watchdog,
      so automatic TX does not stop while waiting for the next CQ burst.
   4. The speaker icon on the calling tab shows TX (red, blinking),
      armed/waiting (orange) and off (X) states.

  0.93-kr1
   1. Full Korean UI and help texts.
   2. Added intermittent CQ (periodic Auto CQ).

Based on original FT8CN 0.93 (2025-01-04).
