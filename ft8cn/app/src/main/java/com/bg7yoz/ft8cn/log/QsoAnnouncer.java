package com.bg7yoz.ft8cn.log;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainActivity;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.callsign.CallsignDatabase;
import com.bg7yoz.ft8cn.callsign.CallsignInfo;
import com.bg7yoz.ft8cn.callsign.CountryNameKo;

import java.io.File;
import java.util.Locale;

/**
 * 한국어판(0.93-kr16): 교신이 끝나 기록으로 저장될 때 소리로 알려 주는 도우미.
 *
 * <p>FT8은 화면을 계속 보지 않아도 교신이 저절로 끝나는 일이 많다. 그래서 교신이 완료되면
 * 휴대전화의 음성 안내(TTS)로 <b>"HL5NTR, 일본과 교신에 성공하였습니다"</b>처럼 한 줄 읽어 주고,
 * 알림창에는 <b>"교신 성공: HL5NTR (일본)"</b>을 조용히 남긴다. 설정의
 * [교신 성공 알림]을 끄면 둘 다 하지 않는다.</p>
 *
 * <p>무전기에 USB 오디오나 블루투스로 붙여 쓰면 휴대전화의 소리가 그 길로 무전기에 들어가 전파로 나간다.
 * 그래서 그런 장치가 붙어 있을 때는 읽을 말을 파일로 만들어 <b>휴대전화 스피커로만</b> 들려준다.
 * 읽는 소리가 전파에 섞이지 않으면서 무전기 옆에서도 들리게 하려는 것이다.</p>
 *
 * @author DS2YTO (한국어판 0.93-kr16)
 */
public class QsoAnnouncer {
    private static final String TAG = "QsoAnnouncer";

    //알림 채널과 알림 번호. 한 건씩 쌓이지 않고 늘 마지막 교신만 알림창에 남긴다.
    private static final String CHANNEL_ID = "qso_success";
    private static final int NOTIFICATION_ID = 7300;
    private static final String UTTERANCE_FILE = "qso_success_file";
    private static final String UTTERANCE_SPEAK = "qso_success_speak";
    private static final String CACHE_FILE_NAME = "qso_announce.wav";

    //송신 중이면 이만큼(250ms × 40 = 10초)까지 기다렸다가 읽는다
    private static final int MAX_TRANSMIT_WAIT = 40;
    private static final long TRANSMIT_WAIT_MS = 250;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static Context appContext = null;
    private static TextToSpeech textToSpeech = null;
    private static boolean ttsReady = false;
    private static boolean useKoreanVoice = false;
    private static String pendingKorean = null;
    private static String pendingEnglish = null;
    private static String lastSpokenText = null;
    private static File announceFile = null;
    private static MediaPlayer player = null;

    /**
     * 송신 중인지 알려 주는 쪽. 음성 안내가 송신 중에 겹치지 않게 하려고 쓴다.
     */
    public interface TransmitState {
        boolean isTransmitting();
    }

    private QsoAnnouncer() {
    }

    /**
     * 교신이 완료되어 기록으로 저장됐을 때 부른다.
     *
     * @param callsign      상대 호출부호
     * @param transmitState 송신 중인지 물어볼 곳 (없으면 null)
     */
    public static void announce(final String callsign, final TransmitState transmitState) {
        if (callsign == null) {
            return;
        }
        final String call = callsign.trim();
        if (call.isEmpty() || "CQ".equalsIgnoreCase(call)) {
            return;
        }
        if (!GeneralVariables.enableQsoAnnounce) {//설정에서 꺼 두었으면 아무 것도 하지 않는다
            return;
        }
        final Context context = GeneralVariables.getMainContext();
        if (context == null) {
            return;
        }
        appContext = context;

        //나라 이름(교신 기록에 붙은 CTY.DAT 정보)을 찾는 일은 DB를 읽으므로 작업 스레드에서 한다
        new Thread(new Runnable() {
            @Override
            public void run() {
                String countryEn = "";
                String countryKo = "";
                try {
                    CallsignDatabase database = GeneralVariables.callsignDatabase;
                    if (database != null) {
                        CallsignInfo info = database.getCallInfo(call);
                        if (info != null && info.CountryNameEn != null) {
                            countryEn = info.CountryNameEn.trim();
                            countryKo = CountryNameKo.toKorean(countryEn).trim();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "announce: 나라 이름을 찾지 못했습니다: " + e.getMessage());
                }

                final String koreanText = buildKoreanText(call, countryKo);
                final String englishText = buildEnglishText(call, countryEn);
                final String notificationText = buildNotificationText(call, countryKo, countryEn);

                postNotification(context, notificationText);
                speakWhenIdle(koreanText, englishText, transmitState, 0);
            }
        }).start();
    }

    /**
     * 음성 안내 말을 만든다. 예) "HL5NTR, 일본과 교신에 성공하였습니다"
     *
     * @param callsign  상대 호출부호
     * @param countryKo 나라 이름(모르면 빈 문자열)
     * @return 읽을 글
     */
    private static String buildKoreanText(String callsign, String countryKo) {
        if (countryKo == null || countryKo.isEmpty()) {
            return callsign + "과 교신에 성공하였습니다";
        }
        return callsign + ", " + withGwa(countryKo) + " 교신에 성공하였습니다";
    }

    /**
     * 영어 화면일 때 읽을 글. 예) "QSO completed with HL5NTR, Japan"
     *
     * @param callsign  상대 호출부호
     * @param countryEn 나라 이름(모르면 빈 문자열)
     * @return 읽을 글
     */
    private static String buildEnglishText(String callsign, String countryEn) {
        if (countryEn == null || countryEn.isEmpty()) {
            return "QSO completed with " + callsign;
        }
        return "QSO completed with " + callsign + ", " + countryEn;
    }

    /**
     * 알림창에 남길 한 줄. 예) "교신 성공: HL5NTR (일본)"
     *
     * @param callsign  상대 호출부호
     * @param countryKo 나라 이름(한국어)
     * @param countryEn 나라 이름(영어)
     * @return 알림 글
     */
    private static String buildNotificationText(String callsign, String countryKo, String countryEn) {
        String country = GeneralVariables.isKorean ? countryKo : countryEn;
        if (country == null) {
            country = "";
        }
        String title = GeneralVariables.getStringFromResource(R.string.qso_announce_notify_title);
        if (country.isEmpty()) {
            return title + ": " + callsign;
        }
        return title + ": " + callsign + " (" + country + ")";
    }

    /**
     * 나라 이름 뒤에 받침에 맞는 조사를 붙인다(일본과 / 캐나다와).
     *
     * @param word 나라 이름
     * @return 조사가 붙은 이름
     */
    private static String withGwa(String word) {
        String name = word == null ? "" : word.trim();
        if (name.isEmpty()) {
            return name;
        }
        char last = 0;
        for (int i = name.length() - 1; i >= 0; i--) {//괄호 등으로 끝나면 그 앞 한글을 본다
            char c = name.charAt(i);
            if (c >= 0xAC00 && c <= 0xD7A3) {
                last = c;
                break;
            }
        }
        if (last == 0) {//한글이 아니면 기본형을 쓴다
            return name + "과";
        }
        boolean hasFinalConsonant = ((last - 0xAC00) % 28) != 0;
        return name + (hasFinalConsonant ? "과" : "와");
    }

    /**
     * 알림창에 조용히 한 줄 남긴다(소리는 음성 안내가 맡으므로 알림 자체는 소리를 내지 않는다).
     *
     * @param context 화면
     * @param text    남길 글
     */
    private static void postNotification(Context context, String text) {
        try {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID
                        , GeneralVariables.getStringFromResource(R.string.qso_announce_channel)
                        , NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(GeneralVariables.getStringFromResource(
                        R.string.qso_announce_channel_desc));
                channel.setSound(null, null);
                channel.enableVibration(false);
                manager.createNotificationChannel(channel);
            }
            //안드로이드 13(API 33)부터는 알림 권한을 받아야 알림창에 뜬다. 없으면 조용히 넘어간다.
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context
                    , "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_qso_success_24)
                    .setContentTitle(GeneralVariables.getStringFromResource(
                            R.string.qso_announce_notify_title))
                    .setContentText(text)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent);
            Notification notification = builder.build();
            notification.sound = null;
            notification.defaults = 0;
            manager.notify(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "postNotification: 알림을 남기지 못했습니다: " + e.getMessage());
        }
    }

    /**
     * 송신이 끝나기를 잠깐 기다렸다가 음성으로 읽어 준다.
     *
     * @param koreanText  한국어 문장
     * @param englishText 영어 문장
     * @param state       송신 상태
     * @param attempt     지금까지 기다린 횟수
     */
    private static void speakWhenIdle(final String koreanText, final String englishText
            , final TransmitState state, final int attempt) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state != null && state.isTransmitting() && attempt < MAX_TRANSMIT_WAIT) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            speakWhenIdle(koreanText, englishText, state, attempt + 1);
                        }
                    }, TRANSMIT_WAIT_MS);
                    return;
                }
                speak(koreanText, englishText);
            }
        });
    }

    /**
     * 음성 안내를 준비하고 읽는다. 처음 부를 때만 TTS를 만든다.
     *
     * @param koreanText  한국어 문장
     * @param englishText 영어 문장
     */
    private static void speak(String koreanText, String englishText) {
        pendingKorean = koreanText;
        pendingEnglish = englishText;
        if (textToSpeech == null) {
            initTextToSpeech();
            return;//초기화가 끝나면 onInit 에서 읽는다
        }
        if (!ttsReady) {
            return;//아직 준비 중이면 onInit 이 읽는다
        }
        speakNow();
    }

    /**
     * TTS 엔진을 만든다. 목소리는 화면 언어를 따른다(한국어 화면이면 한국어 목소리).
     */
    private static void initTextToSpeech() {
        if (appContext == null) {
            return;
        }
        textToSpeech = new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "onInit: 음성 안내를 시작하지 못했습니다: " + status);
                    ttsReady = false;
                    return;
                }
                useKoreanVoice = false;
                if (GeneralVariables.isKorean) {
                    int result = textToSpeech.setLanguage(Locale.KOREAN);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //한국어 목소리가 없는 휴대전화면 영어 문장을 읽는다
                        Log.e(TAG, "onInit: 한국어 목소리가 없어 영어로 읽습니다");
                        textToSpeech.setLanguage(Locale.US);
                    } else {
                        useKoreanVoice = true;
                    }
                } else {
                    textToSpeech.setLanguage(Locale.US);
                }
                textToSpeech.setSpeechRate(0.95f);
                textToSpeech.setPitch(1.0f);
                try {
                    //그냥 읽을 때(USB 오디오가 없을 때)는 알림 쪽 길로 보내, 송신음과 섞이지 않게 한다
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    textToSpeech.setAudioAttributes(attributes);
                } catch (Exception e) {
                    Log.e(TAG, "onInit: 소리 길을 정하지 못했습니다: " + e.getMessage());
                }
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (!UTTERANCE_FILE.equals(utteranceId)) {
                            return;
                        }
                        final File file = announceFile;
                        announceFile = null;
                        if (file == null) {
                            return;
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                playOnSpeaker(file);
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "onError: 읽을 말을 만들지 못했습니다: " + utteranceId);
                        if (!UTTERANCE_FILE.equals(utteranceId)) {
                            return;
                        }
                        //파일을 만들지 못했으면 그냥 TTS로 읽는다
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                speakDirect();
                            }
                        });
                    }
                });
                ttsReady = true;
                speakNow();
            }
        });
    }

    /**
     * 기다리던 문장을 읽는다. 무전기에 USB 오디오로 붙어 있으면 스피커로만 들려준다.
     */
    private static void speakNow() {
        if (textToSpeech == null || !ttsReady) {
            return;
        }
        String text = useKoreanVoice ? pendingKorean : pendingEnglish;
        if (text == null || text.isEmpty()) {
            return;
        }
        pendingKorean = null;
        pendingEnglish = null;
        lastSpokenText = text;

        //무전기(USB·블루투스)로 소리가 새어 나가지 않게, 파일로 만들어 휴대전화 스피커로 생생한다
        if (isExternalAudioConnected() && synthesizeToFile(text)) {
            return;
        }
        speakDirect();
    }

    /**
     * 읽을 말을 wav 파일로 만든다. 다 되면 onDone 에서 스피커로 재생한다.
     *
     * @param text 읽을 글
     * @return 파일 만들기를 시작했으면 true
     */
    private static boolean synthesizeToFile(String text) {
        if (appContext == null || textToSpeech == null) {
            return false;
        }
        try {
            File file = new File(appContext.getCacheDir(), CACHE_FILE_NAME);
            if (file.exists() && !file.delete()) {
                Log.e(TAG, "synthesizeToFile: 예전 파일을 지우지 못했습니다");
            }
            if (textToSpeech.synthesizeToFile(text, null, file, UTTERANCE_FILE)
                    != TextToSpeech.SUCCESS) {
                return false;
            }
            announceFile = file;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "synthesizeToFile: 파일을 만들지 못했습니다: " + e.getMessage());
            return false;
        }
    }

    /**
     * TTS 목소리를 그대로 낸다(기본 길).
     */
    private static void speakDirect() {
        String text = lastSpokenText;
        if (textToSpeech == null || !ttsReady || text == null || text.isEmpty()) {
            return;
        }
        try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_SPEAK);
        } catch (Exception e) {
            Log.e(TAG, "speakDirect: 읽지 못했습니다: " + e.getMessage());
        }
    }

    /**
     * wav 파일을 휴대전화 스피커로 재생한다.
     *
     * @param file 읽을 말이 담긴 파일
     */
    private static void playOnSpeaker(final File file) {
        if (file == null || !file.exists() || appContext == null) {
            speakDirect();
            return;
        }
        releasePlayer();
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setDataSource(file.getAbsolutePath());
            AudioDeviceInfo speaker = findBuiltinSpeaker();
            if (speaker != null) {
                mediaPlayer.setPreferredDevice(speaker);//무전기가 아니라 휴대전화 스피커로
            }
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.release();
                    if (player == mediaPlayer) {
                        player = null;
                    }
                    if (file.exists()) {
                        file.delete();
                    }
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
            player = mediaPlayer;
        } catch (Exception e) {
            Log.e(TAG, "playOnSpeaker: 스피커로 재생하지 못했습니다: " + e.getMessage());
            releasePlayer();
            if (file.exists()) {
                file.delete();
            }
            speakDirect();
        }
    }

    /**
     * 휴대전화 스피커 말고 다른 곳(무전기에 붙인 USB 오디오·블루투스)으로 소리가 나는지 본다.
     *
     * @return 다른 곳으로 나가는 중이면 true
     */
    private static boolean isExternalAudioConnected() {
        if (appContext == null) {
            return false;
        }
        try {
            AudioManager manager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            if (manager == null) {
                return false;
            }
            AudioDeviceInfo[] devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_USB_DEVICE
                        || type == AudioDeviceInfo.TYPE_USB_HEADSET
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isExternalAudioConnected: 출력 장치를 보지 못했습니다: " + e.getMessage());
        }
        return false;
    }

    /**
     * 휴대전화의 기본 스피커를 찾는다.
     *
     * @return 스피커 (없으면 null)
     */
    private static AudioDeviceInfo findBuiltinSpeaker() {
        if (appContext == null) {
            return null;
        }
        try {
            AudioManager manager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            if (manager == null) {
                return null;
            }
            AudioDeviceInfo[] devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return device;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findBuiltinSpeaker: 스피커를 찾지 못했습니다: " + e.getMessage());
        }
        return null;
    }

    /**
     * 재생 중인 소리를 거두고 임시 파일을 지운다.
     */
    private static void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
        if (announceFile != null) {
            if (announceFile.exists()) {
                announceFile.delete();
            }
            announceFile = null;
        }
    }

    /**
     * 앱을 닫을 때 TTS 자원을 돌려준다.
     */
    public static void shutdown() {
        stopSpeaking();
        ttsReady = false;
        if (textToSpeech != null) {
            try {
                textToSpeech.shutdown();
            } catch (Exception e) {
                Log.e(TAG, "shutdown: 음성 안내를 닫지 못했습니다: " + e.getMessage());
            }
            textToSpeech = null;
        }
    }

    /**
     * 지금 읽고 있는(또는 기다리고 있는) 안내를 멈춘다. 설정에서 알림을 끄면 부른다.
     */
    public static void stopSpeaking() {
        pendingKorean = null;
        pendingEnglish = null;
        releasePlayer();
        if (textToSpeech != null && ttsReady) {
            try {
                textToSpeech.stop();
            } catch (Exception e) {
                Log.e(TAG, "stopSpeaking: 멈추지 못했습니다: " + e.getMessage());
            }
        }
    }
}
