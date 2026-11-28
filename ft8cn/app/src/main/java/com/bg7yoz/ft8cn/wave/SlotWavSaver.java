package com.bg7yoz.ft8cn.wave;

/**
 * 한국어판(0.93-kr18): 직전 15초 FT8 슬롯 오디오를 WAV 파일로 저장하고 공유한다.
 *
 * 목적
 *  - 무전기에서 실제로 들어온 15초 오디오를 WSJT-X 가 그대로 읽는 WAV(12 kHz, 16-bit signed
 *    little-endian, 1 channel)로 떠 두고, PC 의 WSJT-X 로 다시 디코드해 FT8CN 과 성능을 비교한다.
 *  - 그래서 파일 이름에 그 슬롯의 최저 SNR 과 디코드 국 수를 넣어, 나중에 "몇 dB 로 들어온
 *    파일인지" 골라 볼 수 있게 한다.
 *    예) FT8_7074kHz_20261128_101500Z_min-22dB_5dec.wav
 *        (시각은 WSJT-X 의 .wav 이름과 맞추기 위해 UTC 로 적는다)
 *
 * 흐름
 *  1) FT8SignalListener 가 슬롯 수신을 마치면 setSlot() 으로 오디오 15초를 넘긴다.
 *  2) 디코드가 끝나면 setDecodeResult() 로 최저 SNR 과 디코드 국 수를 넘긴다.
 *  3) 사용자가 [WAV] 단추를 누르면 saveAndShare() 가 파일을 만들고 공유 창을 띄운다.
 *
 * @author BGY70Z (한국어판 0.93-kr18)
 */

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SlotWavSaver {
    private static final String TAG = "SlotWavSaver";
    private static final SlotWavSaver instance = new SlotWavSaver();

    public static final int SAMPLE_RATE = 12000;//WSJT-X 와 같은 12 kHz
    public static final int SLOT_SECONDS = 15;//FT8 한 슬롯
    public static final int SLOT_SAMPLES = SAMPLE_RATE * SLOT_SECONDS;//180,000 샘플(약 360 KB)
    public static final String FOLDER = "FT8CN_WAV";//Download/FT8CN_WAV/
    public static final String AUTO_PREFIX = "AUTO_FT8_";//자동 저장 파일 이름 앞\uae00자
    public static final int AUTO_SNR_THRESHOLD = -22;//이 SNR(dB) 이하로 들어온 신호가 있으면 자동 저장
    public static final int AUTO_KEEP = 20;//자동 저장 파일을 최근 20개까지만 보관
    private static final String FILE_PROVIDER = "com.bg7yoz.ft8cn.kr.fileprovider";
    private static final long DECODE_WAIT_MS = 2500;//방금 끝난 슬롯의 디코드 결과를 잠깐 기다리는 시간

    private int dataLength = 0;//현재 채워진 샘플 수(0 이면 아직 슬롯이 없음)
    private short[] samples = null;//직전 슬롯의 16-bit 샘플(항상 SLOT_SAMPLES 크기)
    private long slotUtc = 0;//슬롯의 UTC 시각(밀리초)
    private long slotBand = 0;//슬롯을 받을 때의 무전기 주파수(Hz)
    private int minSnr = 0;//그 슬롯에서 디코드된 신호 중 가장 약한 SNR(dB)
    private int decodeCount = -1;//디코드된 국 수. -1 이면 아직 디코드 결과 없음

    private SlotWavSaver() {
    }

    public static SlotWavSaver getInstance() {
        return instance;
    }

    /**
     * 슬롯 수신이 끝나면 호출한다. 오디오를 16-bit 로 바꿔 보관한다.
     * (디코더가 원본 버퍼를 건드릴 수 있으므로 값을 복사해 둔다)
     *
     * @param data 12 kHz float 오디오(-1.0 ~ 1.0)
     * @param utc  슬롯의 UTC 시각(밀리초)
     * @param band 무전기 주파수(Hz)
     */
    public synchronized void setSlot(float[] data, long utc, long band) {
        if (data == null || data.length == 0) {
            return;
        }
        short[] temp = new short[SLOT_SAMPLES];
        int copyLen = Math.min(data.length, SLOT_SAMPLES);
        int srcStart = data.length - copyLen;//15초보다 길면 뒤쪽(최근) 15초를 쓴다
        int dstStart = SLOT_SAMPLES - copyLen;//15초가 안 되면 앞을 0(무음)으로 둔다
        for (int i = 0; i < copyLen; i++) {
            temp[dstStart + i] = floatToShort(data[srcStart + i]);
        }
        samples = temp;
        dataLength = SLOT_SAMPLES;
        slotUtc = utc;
        slotBand = band;
        minSnr = 0;
        decodeCount = -1;
    }

    /**
     * 그 슬롯의 디코드가 끝나면 호출한다.
     * 딥 디코드처럼 여러 번 불려도 가장 약한 SNR 과 가장 많은 국 수로 합친다.
     *
     * @param utc    슬롯의 UTC 시각(밀리초)
     * @param minSnr 그 슬롯에서 디코드된 신호의 최저 SNR(dB)
     * @param count  디코드된 국 수
     */
    public synchronized void setDecodeResult(long utc, int minSnr, int count) {
        if (utc != slotUtc) {//이미 다음 슬롯으로 넘어갔으면 버린다
            return;
        }
        if (decodeCount < 0) {
            this.minSnr = minSnr;
            this.decodeCount = count;
        } else {
            if (count > 0 && (this.decodeCount == 0 || minSnr < this.minSnr)) {
                this.minSnr = minSnr;
            }
            this.decodeCount = Math.max(this.decodeCount, count);
        }
    }

    /**
     * 저장할 15초 슬롯이 준비되어 있는지.
     */
    public synchronized boolean hasSlot() {
        return dataLength >= SLOT_SAMPLES && samples != null;
    }

    /**
     * 지금 슬롯의 디코드 결과가 아직 없는지.
     */
    public synchronized boolean isDecodePending() {
        return decodeCount < 0;
    }

    /**
     * [WAV] 단추를 눌렀을 때. 직전 15초를 WAV 로 저장하고 공유 창을 띄운다.
     * 파일 쓰기는 백그라운드에서 하고, 토스트·공유 창은 화면 스레드에서 띄운다.
     */
    public void saveAndShare(final Activity activity) {
        if (activity == null) {
            return;
        }
        if (!hasSlot()) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.wav_wait_slot));
            return;
        }
        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.wav_saving));

        new Thread(new Runnable() {
            @Override
            public void run() {
                short[] audio;
                long utc;
                long band;
                synchronized (SlotWavSaver.this) {
                    audio = samples;
                    utc = slotUtc;
                    band = slotBand;
                }
                if (audio == null) {
                    return;
                }

                //방금 슬롯이 끝나자마자 눌렀으면 디코드 결과가 아직 없다. 잠깐 기다려 본다.
                long deadline = System.currentTimeMillis() + DECODE_WAIT_MS;
                while (System.currentTimeMillis() < deadline) {
                    synchronized (SlotWavSaver.this) {
                        if (slotUtc != utc || decodeCount >= 0) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                int snr;
                int count;
                synchronized (SlotWavSaver.this) {
                    snr = minSnr;
                    count = (slotUtc == utc) ? decodeCount : -1;
                }

                final String fileName = buildFileName("", utc, band, snr, count);
                final SavedWav saved;
                try {
                    saved = writeToStorage(activity.getApplicationContext(), fileName, audio);
                } catch (Exception e) {
                    Log.e(TAG, "WAV 저장 실패: " + e.getMessage());
                    final String reason = e.getMessage() == null
                            ? e.getClass().getSimpleName() : e.getMessage();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastMessage.show(String.format(
                                    GeneralVariables.getStringFromResource(R.string.wav_save_failed)
                                    , reason));
                        }
                    });
                    return;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastMessage.show(String.format(
                                GeneralVariables.getStringFromResource(R.string.wav_saved)
                                , saved.displayPath));
                        share(activity, saved);
                    }
                });
            }
        }).start();
    }

    /**
     * 파일 이름을 만든다. 예) FT8_7074kHz_20261128_101500Z_min-22dB_5dec.wav
     * 디코드 결과가 없으면(디코드 전) _ndec 로 적는다.
     *
     * @param prefix 자동 저장이면 AUTO_PREFIX, 수동 저장이면 ""
     */
    private String buildFileName(String prefix, long utc, long band, int minSnr, int decodeCount) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = format.format(new Date(utc));
        int khz = (int) Math.round(band / 1000.0);
        String decodePart;
        if (decodeCount < 0) {
            decodePart = "_ndec";
        } else if (decodeCount == 0) {
            decodePart = "_0dec";
        } else {
            decodePart = String.format(Locale.US, "_min%ddB_%ddec", minSnr, decodeCount);
        }
        return String.format(Locale.US, "%sFT8_%dkHz_%sZ%s.wav", prefix, khz, time, decodePart);
    }

    /**
     * 한국어판(0.93-kr18): 약신호 자동 저장.
     * 그 슬롯에서 디코드된 신호 중 가장 약한 것이 -22 dB 이하이면, 사용자가 단추를 누르지 않아도
     * 조용히 Download/FT8CN_WAV/ 에 AUTO_FT8_…wav 로 저장한다. (공유 창은 띄우지 않는다)
     * 저장 뒤에는 자동 저장 파일이 20개를 넘지 않도록 오래된 것부터 지운다.
     *
     * 디코드가 끝난 스레드에서 바로 부르는 것을 전제로 한다(별도 스레드를 만들지 않는다).
     */
    public void autoSaveWeakSignal(Context context, long utc) {
        if (context == null || !GeneralVariables.autoSaveWeakWav) {
            return;
        }
        short[] audio;
        long band;
        int snr;
        int count;
        synchronized (this) {
            if (utc != slotUtc || decodeCount <= 0) {//이 슬롯의 디코드 결과가 아니거나 디코드 0건
                return;
            }
            if (minSnr > AUTO_SNR_THRESHOLD) {//-22 dB 보다 센 신호뿐이면 저장하지 않는다
                return;
            }
            audio = samples;
            band = slotBand;
            snr = minSnr;
            count = decodeCount;
        }
        if (audio == null) {
            return;
        }

        String fileName = buildFileName(AUTO_PREFIX, utc, band, snr, count);
        try {
            SavedWav saved = writeToStorage(context, fileName, audio);
            Log.d(TAG, "약신호 자동 저장(" + snr + "dB, " + count + "국): " + saved.displayPath);
        } catch (Exception e) {
            Log.e(TAG, "약신호 자동 저장 실패: " + e.getMessage());
            return;
        }
        pruneAutoFiles(context);
    }

    /**
     * 자동 저장 파일이 AUTO_KEEP 개를 넘으면 이름(UTC 시각) 순으로 오래된 것부터 지운다.
     * 수동 저장 파일(FT8_…wav)은 건드리지 않는다.
     */
    private void pruneAutoFiles(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pruneAutoFilesInMediaStore(context);
            } else {
                pruneAutoFilesInFolder(new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), FOLDER));
            }
        } catch (Exception e) {
            Log.e(TAG, "오래된 자동 저장 WAV 정리 실패: " + e.getMessage());
        }
    }

    private void pruneAutoFilesInMediaStore(Context context) {
        ContentResolver resolver = context.getContentResolver();
        String relPathLike = Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER + "%";
        Cursor cursor = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI
                , new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME}
                , MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ? AND "
                        + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?"
                , new String[]{relPathLike, AUTO_PREFIX + "%"}
                , MediaStore.MediaColumns.DISPLAY_NAME + " ASC");
        if (cursor == null) {
            return;
        }
        try {
            int over = cursor.getCount() - AUTO_KEEP;
            for (int i = 0; i < over && cursor.moveToNext(); i++) {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                String name = cursor.getString(
                        cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                resolver.delete(ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, id), null, null);
                Log.d(TAG, "오래된 자동 WAV 삭제: " + name);
            }
        } finally {
            cursor.close();
        }
    }

    private void pruneAutoFilesInFolder(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        java.util.ArrayList<String> autoNames = new java.util.ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith(AUTO_PREFIX)) {
                autoNames.add(f.getName());
            }
        }
        java.util.Collections.sort(autoNames);
        int over = autoNames.size() - AUTO_KEEP;
        for (int i = 0; i < over; i++) {
            File old = new File(dir, autoNames.get(i));
            if (old.delete()) {
                Log.d(TAG, "오래된 자동 WAV 삭제: " + autoNames.get(i));
            }
        }
    }

    /**
     * 기기 Download/FT8CN_WAV/ 에 WAV 를 쓴다.
     * 안드로이드 10(Q) 이상은 MediaStore, 그 아래는 공용 Downloads 폴더에 직접 쓴다.
     * 둘 다 안 되면 앱 전용 외부 폴더에라도 남긴다.
     */
    private SavedWav writeToStorage(Context context, String fileName, short[] audio) throws IOException {
        byte[] data = toLittleEndianBytes(audio);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SavedWav saved = writeToMediaStore(context, fileName, data);
            if (saved != null) {
                return saved;
            }
        } else {
            SavedWav saved = writeToPublicDownloads(context, fileName, data);
            if (saved != null) {
                return saved;
            }
        }
        return writeToAppFolder(context, fileName, data);
    }

    private SavedWav writeToMediaStore(Context context, String fileName, byte[] data) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS
                    + "/" + FOLDER);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return null;
            }
            OutputStream out = resolver.openOutputStream(uri);
            if (out == null) {
                resolver.delete(uri, null, null);
                return null;
            }
            try {
                writeWav(out, data);
            } finally {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return new SavedWav(uri, "Download/" + FOLDER + "/" + fileName);
        } catch (Exception e) {
            Log.e(TAG, "MediaStore 저장 실패: " + e.getMessage());
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private SavedWav writeToPublicDownloads(Context context, String fileName, byte[] data) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), FOLDER);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            File file = new File(dir, fileName);
            FileOutputStream out = new FileOutputStream(file);
            try {
                writeWav(out, data);
            } finally {
                out.close();
            }
            MediaScannerConnection.scanFile(context,
                    new String[]{file.getAbsolutePath()}, new String[]{"audio/wav"}, null);
            Uri uri = FileProvider.getUriForFile(context, FILE_PROVIDER, file);
            return new SavedWav(uri, "Download/" + FOLDER + "/" + fileName);
        } catch (Exception e) {
            Log.e(TAG, "공용 Downloads 저장 실패: " + e.getMessage());
            return null;
        }
    }

    private SavedWav writeToAppFolder(Context context, String fileName, byte[] data) throws IOException {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (base == null) {
            base = context.getFilesDir();
        }
        File dir = new File(base, FOLDER);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("폴더를 만들 수 없습니다: " + dir.getAbsolutePath());
        }
        File file = new File(dir, fileName);
        FileOutputStream out = new FileOutputStream(file);
        try {
            writeWav(out, data);
        } finally {
            out.close();
        }
        Uri uri = FileProvider.getUriForFile(context, FILE_PROVIDER, file);
        return new SavedWav(uri, file.getAbsolutePath());
    }

    /**
     * 공유 창(ACTION_SEND)을 띄운다. 취소해도 파일은 Download 폴더에 그대로 남는다.
     */
    private void share(Activity activity, SavedWav saved) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/wav");
            intent.putExtra(Intent.EXTRA_STREAM, saved.uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent,
                    GeneralVariables.getStringFromResource(R.string.wav_share_title)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "공유 창을 열 수 없음: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "공유 실패: " + e.getMessage());
        }
    }

    /**
     * float(-1.0~1.0) 을 16-bit signed 로 바꾼다. 넘치는 값은 잘라 낸다.
     */
    private static short floatToShort(float value) {
        float v = value;
        if (v > 1.0f) {
            v = 1.0f;
        } else if (v < -1.0f) {
            v = -1.0f;
        }
        int s = Math.round(v * 32767.0f);
        if (s > 32767) {
            s = 32767;
        } else if (s < -32768) {
            s = -32768;
        }
        return (short) s;
    }

    private static byte[] toLittleEndianBytes(short[] audio) {
        byte[] out = new byte[audio.length * 2];
        for (int i = 0; i < audio.length; i++) {
            out[i * 2] = (byte) (audio[i] & 0xFF);
            out[i * 2 + 1] = (byte) ((audio[i] >> 8) & 0xFF);
        }
        return out;
    }

    /**
     * 표준 RIFF PCM WAV(모노, 12 kHz, 16-bit) 로 쓴다. WSJT-X 가 그대로 읽는다.
     */
    private static void writeWav(OutputStream stream, byte[] pcm) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(stream, 64 * 1024);
        int byteRate = SAMPLE_RATE * 2;//모노 16-bit
        int dataSize = pcm.length;
        byte[] header = new byte[44];
        putAscii(header, 0, "RIFF");
        putIntLE(header, 4, 36 + dataSize);
        putAscii(header, 8, "WAVE");
        putAscii(header, 12, "fmt ");
        putIntLE(header, 16, 16);//fmt 청크 크기
        putShortLE(header, 20, 1);//PCM
        putShortLE(header, 22, 1);//모노
        putIntLE(header, 24, SAMPLE_RATE);
        putIntLE(header, 28, byteRate);
        putShortLE(header, 32, 2);//블록 정렬
        putShortLE(header, 34, 16);//비트 수
        putAscii(header, 36, "data");
        putIntLE(header, 40, dataSize);
        out.write(header);
        out.write(pcm);
        out.flush();
    }

    private static void putAscii(byte[] buf, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            buf[offset + i] = (byte) text.charAt(i);
        }
    }

    private static void putIntLE(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void putShortLE(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /**
     * 저장된 WAV 의 위치.
     */
    private static class SavedWav {
        final Uri uri;//공유에 쓰는 주소
        final String displayPath;//사용자에게 보여 줄 위치

        SavedWav(Uri uri, String displayPath) {
            this.uri = uri;
            this.displayPath = displayPath;
        }
    }
}
