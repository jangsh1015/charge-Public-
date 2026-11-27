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
