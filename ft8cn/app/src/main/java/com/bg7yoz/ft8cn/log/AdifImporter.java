package com.bg7yoz.ft8cn.log;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * 한국어판(0.93-kr16) · ADIF 불러오기.
 *
 * <p>QRZ·LotW·N1MM·JTDX 같은 곳에서 받은 교신 기록(ADIF, 확장자 .adi)을 읽어
 * 앱의 QSO 기록에 넣는다. 이미 있는 기록(같은 콜사인·시각·모드)은
 * {@link DatabaseOpr#doInsertQSLData} 가 알아서 건너뛴다.</p>
 *
 * <p>이렇게 넣어 두면 예전에 교신한 국을 목록에서 줄그어 보여 주고, 자동 호출에서
 * 이미 한 국을 한 번 더 부르는 일도 줄어든다.</p>
 *
 * @author DS2YTO (한국어판 0.93-kr16)
 */
public class AdifImporter {
    private static final String TAG = "AdifImporter";

    private AdifImporter() {
    }

    /**
     * ADIF 파일을 읽어 QSO 기록에 넣는다.
     *
     * @param context  화면(파일을 읽는 데 쓴다)
     * @param uri      고른 파일
     * @param database DB
     * @return {읽은 기록 수, 새로 넣은 기록 수}
     */
    public static int[] importFrom(Context context, Uri uri, DatabaseOpr database) {
        int[] result = new int[]{0, 0};
        if (context == null || uri == null || database == null) {
            return result;
        }
        String text;
        try {
            text = readText(context, uri);
        } catch (Exception e) {
            Log.e(TAG, "importFrom: 파일을 읽지 못했습니다: " + e.getMessage());
            return result;
        }
        ArrayList<HashMap<String, String>> records = parse(text);
        result[0] = records.size();
        for (HashMap<String, String> map : records) {
            try {
                fillMissingBandFreq(map);
                fillMissingBand(map);
                normalizeBand(map);
                QSLRecord record = new QSLRecord(map);
                if (record.isInvalid) {
                    continue;
                }
                if (record.getToCallsign() == null || record.getToCallsign().trim().isEmpty()) {
                    continue;
                }
                if (database.doInsertQSLData(record, null)) {
                    result[1]++;
                }
            } catch (Exception e) {
                Log.e(TAG, "importFrom: 기록을 넣지 못했습니다: " + e.getMessage());
            }
        }
        //새로 넣은 기록이 있으면 '교신한 적 있는 콜사인' 표를 다시 읽는다
        try {
            if (result[1] > 0) {
                database.getAllQSLCallsigns();
            }
        } catch (Exception e) {
            Log.e(TAG, "importFrom: 콜사인 표를 다시 읽지 못했습니다: " + e.getMessage());
        }
        return result;
    }

    /**
     * 파일 내용을 글자로 읽는다.
     *
     * @param context 화면
     * @param uri     파일
     * @return 글자
     * @throws Exception 읽지 못했을 때
     */
    private static String readText(Context context, Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }
        reader.close();
        return builder.toString();
    }

    /**
     * ADIF 글자에서 기록 하나씩을 뽑아 낸다.
     *
     * @param text ADIF 글자
     * @return 기록 목록(각 기록은 필드 이름 → 값)
     */
    public static ArrayList<HashMap<String, String>> parse(String text) {
        ArrayList<HashMap<String, String>> records = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return records;
        }
        String upper = text.toUpperCase(Locale.US);
        int start = upper.indexOf("<EOH>");
        int i = start >= 0 ? start + 5 : 0;
        HashMap<String, String> record = new HashMap<>();
        while (i < text.length()) {
            int open = text.indexOf('<', i);
            if (open < 0) {
                break;
            }
            int close = text.indexOf('>', open);
            if (close < 0) {
                break;
            }
            String tag = text.substring(open + 1, close).trim();
            i = close + 1;
            if (tag.equalsIgnoreCase("EOR")) {
                if (!record.isEmpty()) {
                    records.add(record);
                    record = new HashMap<>();
                }
                continue;
            }
            if (tag.equalsIgnoreCase("EOH") || tag.isEmpty()) {
                continue;
            }
            String name = tag;
            int length = -1;
            int colon = tag.indexOf(':');
            if (colon > 0) {
                name = tag.substring(0, colon);
                int nextColon = tag.indexOf(':', colon + 1);
                String lengthText = nextColon > 0
                        ? tag.substring(colon + 1, nextColon) : tag.substring(colon + 1);
                try {
                    length = Integer.parseInt(lengthText.trim());
                } catch (NumberFormatException ignored) {
                    length = -1;
                }
            }
            String value;
            if (length >= 0 && i + length <= text.length()) {
                value = text.substring(i, i + length);
                i += length;
            } else {
                int next = text.indexOf('<', i);
                if (next < 0) {
                    next = text.length();
                }
                value = text.substring(i, next);
                i = next;
            }
            record.put(name.toUpperCase(Locale.US), value.trim());
        }
        if (!record.isEmpty()) {
            records.add(record);
        }
        return records;
    }

    /**
     * FREQ 만 있고 BAND 가 없는 파일(LotW·QRZ 일부)에서는 주파수로 밴드를 정해 준다.
     * 밴드 이름이 있어야 '이미 교신한 국' 표(밴드별)에 제대로 들어간다.
     *
     * @param map 기록
     */
    private static void fillMissingBand(HashMap<String, String> map) {
        String band = map.get("BAND");
        if (band != null && !band.trim().isEmpty()) {
            return;
        }
        String freq = map.get("FREQ");
        if (freq == null || freq.trim().isEmpty()) {
            return;
        }
        try {
            long hz = Math.round(Float.parseFloat(freq.trim()) * 1000000f);
            String meter = BaseRigOperation.getMeterFromFreq(hz);
            if (meter != null && !meter.isEmpty()) {
                map.put("BAND", meter.toLowerCase(Locale.US));
            }
        } catch (Exception e) {
            Log.e(TAG, "fillMissingBand: 주파수를 읽지 못했습니다: " + e.getMessage());
        }
    }

    /**
     * 밴드 이름을 소문자로 맞춘다. 앱은 {"20m" 처럼 소문자로 기록하고 찾는다.
     *
     * @param map 기록
     */
    private static void normalizeBand(HashMap<String, String> map) {
        String band = map.get("BAND");
        if (band != null && !band.trim().isEmpty()) {
            map.put("BAND", band.trim().toLowerCase(Locale.US));
        }
    }

    /**
     * FREQ 가 없고 BAND 만 있으면 흔한 밴드의 주파수를 채워 넣는다.
     * (거리를 계산하거나 밴드 이름을 보여 줄 때 필요하다)
     *
     * @param map 기록
     */
    private static void fillMissingBandFreq(HashMap<String, String> map) {
        if (map.containsKey("FREQ") || !map.containsKey("BAND")) {
            return;
        }
        String band = map.get("BAND").toLowerCase(Locale.US);
        String freq;
        switch (band) {
            case "160m": freq = "1.840"; break;
            case "80m": freq = "3.573"; break;
            case "60m": freq = "5.357"; break;
            case "40m": freq = "7.074"; break;
            case "30m": freq = "10.136"; break;
            case "20m": freq = "14.074"; break;
            case "17m": freq = "18.100"; break;
            case "15m": freq = "21.074"; break;
            case "12m": freq = "24.915"; break;
            case "10m": freq = "28.074"; break;
            case "6m": freq = "50.313"; break;
            case "2m": freq = "144.174"; break;
            default: return;
        }
        map.put("FREQ", freq);
    }
}
