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

  0.93-kr14
   1. Tapping TUNE now switches the radio's antenna tuner (TUNER) on before starting auto tune.
      - With the tuner off the auto tune command was simply ignored. The app now sends 1C 01 01 (tuner on)
        and, about 0.12 s later, 1C 01 02 (start auto tune). The IC-705 function menu no longer has to be
        set to TUNER first.
      - The screen never freezes while the two commands are being sent.
      - No ALC warning is raised during the 8 s the tuner runs, because a high ALC is normal while tuning.
   2. While transmitting, the RF output (Po) and ALC are shown in % next to SWR.
      - They appear on a second, smaller line under SWR, e.g. `Po 95% · ALC 35%`. The radio is read every
        0.5 s during transmission (ICOM).
      - Po is CI-V 15 11, converted to % with the ICOM scale (0 = 0%, 143 = 50%, 213 = 100%).
      - ALC is CI-V 15 13 (0 to 120), shown as 0 to 100%.
      - After a transmission the last values stay on screen dimmed, and they are cleared when the radio
        is disconnected.
   3. A PWR (transmit power) button now sits next to TUNE.
      - One tap offers 10% / 25% / 50% / 75% / 100%. (ICOM CI-V 14 0A, the 0-255 value in 2-byte BCD)
      - The chosen level is set on the radio at once and the app says "Radio RF power set to N% ."
      - The button shows the current level on a small line under PWR, e.g. `100%`. It is read again at the
        start of every transmission, so a level changed with the knob on the radio is reflected on the
        next transmission.
      - Radios without RF power control (Yaesu, Kenwood and others) say they are not supported.

  0.93-kr13
   1. The live SWR of the radio is shown on screen while transmitting.
      - A small SWR badge appears at the top of the waterfall, between the UTC clock and the
        frequency/band text. (e.g. SWR 1.2)
      - While transmitting, the app reads the SWR from the radio every 0.5 s, so the number keeps
        up in real time. The same works with an IC-705 over a USB cable or over Wi-Fi
        (Elecraft and Xiegu are supported too).
      - The colour shows how safe it is:
        1.5 or lower green / 1.5 to 2.0 amber / over 2.0 red. If it turns red, check the antenna and coax.
      - When the transmission ends, the last SWR stays on screen dimmed, so you can still see how
        that 13 s transmission went after it finishes.
      - The badge disappears when the radio is switched off or disconnected.
   2. A TUNE (auto tuner) button now sits next to the SWR badge.
      - One tap sends the ICOM CI-V command 1C 01 02 (start auto tune) to the radio
        (IC-705 with AH-705, IC-7300 and others).
      - The SWR is read for 8 seconds while the tuner is running, so you can watch it drop right there.
      - It refuses to run while transmitting or when no radio is connected, and says why.
      - Radios without auto tune (Yaesu, Kenwood and others) say they are not supported.
      - On an IC-705 the antenna tuner (TUNER) must be switched on in the radio's function menu
        for TUNE to work.
        (From 0.93-kr14 the app switches the tuner on itself when TUNE is tapped.)

  0.93-kr12
   1. The message lists (decode and call) now use five row colours.
      - Stations calling me : pink is now yellow. (e.g. DS2YTO BG5JQV -23)
        A bright yellow row with near-black text, so the dB and time numbers beside it stay crisp.
      - My CQ transmissions (TX CQ) : amber (dark orange), a new colour. (e.g. CQ DS2YTO PM37)
      - My QSO transmissions (TX QSO) : red, as before. (e.g. DS2YTO BG5JQV -19)
      - Someone else's CQ is still green and other traffic is still dark navy.
      - You can now tell at a glance whether you are calling CQ or working a QSO.
   2. The waterfall traces are a little brighter.
      - With clean wired USB audio there is almost no noise, so signals looked faint and empty
        slots were hard to find.
      - Noise and empty frequencies stay dark navy, and only the signal range
        (blue, sky blue, cyan, yellow, orange) is about 25-30% brighter and more vivid.
        Red is raised slightly.
      - The background does not get brighter, so the screen stays easy on the eyes and only
        signals stand out.

  0.93-kr11
   1. Fixed the Display options dialog on the [Grid / PSK] screen.
      - The dialog could be taller than the screen, so [Save and Close] at the bottom was cut off,
        and the [X] at the top right did not close it. The dialog now always fits on the screen,
        and a long list scrolls inside it.
      - The [X] button at the top right is now 34x34, and a [Close] button was added beside it.
        [X] and [Close] always close the dialog, and it opens normally again from
        [Settings] then [Display options].
      - The Display options dialog no longer appears by itself when the screen opens.
   2. On the first run the map shows grids and SNR.
      - 'Show grid', 'Show snr' and 'SNR in LogBook' are switched on and saved once.
        (If you turn them off in Display options, the app leaves your choice alone.)
   3. The default lookup is the 40m band and FT8 mode.
      - Applied once; when you choose another band or mode in [Settings], your choice is kept.
   4. A [Unworked] button was added to the top row of the decode screen.
      - Press it to see only stations you have not worked yet (the same test as the struck-through
        lines), and press it again to see every decode.
      - It is off (show all) by default, and new decodes follow the filter at once on both the
        one-column phone list and the two-column tablet list.

  0.93-kr10
   1. The [Grid / PSK] screen is now 'map first'.
      - The multi-line card and the big button row at the top are folded into a single
        40dp toolbar: my call sign, grid and band on the left; [My map] [Stats] [ADIF]
        [Refresh] [Browser] [QSO log] chips on the right. The map now takes more than
        90% of the screen height.
      - Inside the PSK Reporter map the site header (OVERVIEW, MAP, DEVELOPER, STATS,
        FAQ, ABOUT US, CONTACT), the search form, the statistics lines and the footer
        note are folded away, so the map fills almost the whole screen.
      - [Settings] at the top right of the map opens the search form over the map;
        [Close] folds it again.
   2. The English labels of the PSK Reporter map are now Korean.
      - Search form: band / show / sent-rcvd / callsign / mode / last / Go, the values
        (all bands, signals, countries, grid), the time ranges, [Display options],
        [Permalink] and [Legend].
      - Display options dialog: hide statistics, show grid, show snr, hide connecting
        lines, map type and the rest.
      - The window that opens when a map marker is tapped: received, sent from, location,
        frequency, distance, rig, antenna and radio.
      - The 'Could not fetch current data (Website problem?)' note is also Korean and
        shown as one line under the map.
   3. The (0 null) error that left the map blank is much less likely.
      - The app now uses a common Chrome user agent and allows cookies (third-party
        included), storage and mixed content.
   4. The decode and calling lists show which country the other station is in, as a
      Korean badge next to the call sign.
      - e.g. CQ HL5NTR PM46 [Japan], DS2YTO JA1ABC -10 [Japan]
      - Country names come from CTY.DAT and are shown as short Korean names (Korea,
        United States, Japan, China, Russia, Germany, Australia and many more).
      - Lines I sent show the receiving station's country; when the country is unknown
        no badge is added.
      - The strike-through for a station already worked now covers only the message so
        that it does not cross out the country badge.
   5. A [Refresh] chip was added to the [Grid / PSK] toolbar.

  0.93-kr9
   1. The spectrum three-pane view on a tablet in landscape is back to three panes:
      spectrum | decode / call (TX). In 0.93-kr8 the bottom-right pane carried [Call] and
      [QSO log] tabs, the height weights of the panes were mixed up (one pane used a
      0.5 ratio while the other kept the old 50), so the top-right decode pane collapsed
      to about 1% height and disappeared. The tabs are gone, and every time the three-pane view
      opens the app sets the height ratio of the two panes again and checks that the decode
      pane really got its share, restoring it if it collapsed.
   2. The round purple tool buttons (full screen, frequency, volume, globe) that used to
      follow you onto the Call, Grid/PSK and Settings screens now appear only on the
      spectrum screen, so they no longer cover a list. If you leave full screen (hidden
      bottom tabs) for another screen, the bottom tabs come back by themselves.
   3. The bottom [QSO Logs] tab is now the [Grid / PSK] tab, so the bottom tabs no longer
      overlap: Decode (two columns) / Call (calling + QSO log) / Spectrum (three panes) /
      Grid / PSK (its own screen) / Settings.
      - The top shows my call sign, my grid (6 characters, with the 4-character one too)
        and the band/frequency in large type. If no call sign is set, an [Open settings]
        button takes you straight to Settings.
      - The PSK Reporter map (stations that reported receiving my signal in the last
        24 hours) opens right inside the app. Without internet it shows a note and
        [Retry], and [Open in browser] opens the map in the phone browser.
      - [My QSO map], [QSO stats], [Share ADIF] and [QSO log] sit on one row. [QSO log]
        switches this screen to the existing log list (search, filter, export and sharing
        as before), so a phone does not lose the log features.
   4. The app now asks for internet access so it can show the PSK Reporter map inside
      the app. (It is a permission that is not prompted for at install time.)
   5. A first-run preset fills in settings only once, and only where they are still empty.
      - My call sign DS2YTO, my grid PM37uu
      - ICOM network (RS-BA1): address 192.168.59.1, user name DS2YTO, connection mode Network
      - No password is included. Enter it in Settings when you first connect.
      Anything you have already set in Settings is kept and never overwritten.

  0.93-kr8
   1. On a tablet in landscape, the QSO log on the right of the Call tab now updates by itself
      each time a contact completes (once 73 has been exchanged). Before, the list was read
      only once when the app started, so later contacts never appeared.
      Records uploaded through the web UI or imported from a shared log show up right away too.
   2. The background of the QSO log panel on the Call tab now matches the QSO log tab.
      Before, dark text sat on a dark background, so the area under the search bar looked black
      and empty.
   3. Coming back to the Call tab or reopening the app reads the right-hand QSO log once more,
      so changes made while it was off screen (new or deleted records) are reflected as well.
   4. The TX watchdog (the time limit on automatic transmission) now defaults to Ignore (no limit).
      With auto answer or CQ left on, the app keeps transmitting until you stop it, with nothing
      to set up. A time chosen in Settings is still used as it is, and a value left by the old
      default (10 minutes) is read as Ignore as well.
   5. The map (pin) button at the top of the QSO log screen is now a [PSK] button.
      Tapping it shows my call sign and my grid locator (for example PM37uu).
      [Open PSK Reporter] brings up a live map of the stations that reported receiving my signal
      in the last 24 hours, and [My QSO map] opens the same map screen as before.
      The call sign is read from Settings every time, so changing it there changes the button too.
      On the IC-705 Wi-Fi (which has no internet) it only shows a note and the app does not close.
   6. On a tablet in landscape, the spectrum three-pane view now has [Call] and [QSO log] tabs
      above the bottom-right pane. One tap turns that pane into the QSO log, and the transmit
      state and call target keep running while the log is shown. The log refreshes by itself
      when a contact completes, and the tab you used last is remembered.

  0.93-kr7
   1. Tap a row once to call or answer that station right away. A pink row (a station
      calling you) answers at the right step of the QSO, while a green CQ or any other
      station is called at once, with the callsign filled into the Target box of the
      Calling window. Long-press still opens the same menu as before.
      (Your own TX rows and free-text/telemetry rows do not react to a tap.)
   2. While you are sending CQ or auto-following CQs, a station calling you that only
      appears in the deep decode is no longer missed, so the next slot does not send CQ
      again. A slightly late decode is tolerated as well.
   3. Tapping a row in the two-column or three-pane tablet view no longer crashes the app
      when the screen has no calling page to switch to.
   4. "Stop after N unanswered calls" now follows the configured count exactly.
      - Slots that decoded late or only in the deep decode are counted as unanswered,
        so the app no longer calls a station more times than the setting.
      - The RR73 step also stops at the same count, and that station is excluded from
        automatic re-calls for 30 minutes.
      - When it gives up, the report record and interval CQ state are reset together and
        the app returns to CQ (waiting).

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
