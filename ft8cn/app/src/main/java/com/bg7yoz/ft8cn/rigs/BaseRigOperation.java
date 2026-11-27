package com.bg7yoz.ft8cn.rigs;
/**
 * 电台相关计算。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;

import java.util.Locale;

public class BaseRigOperation {
    @SuppressLint("DefaultLocale")
    public static String getFrequencyStr(long freq) {
        return String.format("%d.%03dMHz", freq / 1000000, (freq % 1000000) / 1000);
    }
    /**
     * 检查是不是在WSPR2的频段内
     *
     * @param freq 频率
     * @return 是否
     */
    public static boolean checkIsWSPR2(long freq) {
        //freq=电台频率+声音频率
        return (freq >= 137400 && freq <= 137600)       //2190m
                || (freq >= 475400 && freq <= 475600)   //630m
                || (freq >= 1838000 && freq <= 1838200)  //160m
                || (freq >= 3594000 && freq <= 3594200)  //80m
                || (freq >= 5288600 && freq <= 5288800)  //60m
                || (freq >= 7040000 && freq <= 7040200)  //40m
                || (freq >= 10140100 && freq <= 10140300)   //30m
                || (freq >= 14097000 && freq <= 14097200)   //20m
                || (freq >= 18106000 && freq <= 18106200)   //17m
                || (freq >= 21096000 && freq <= 21096200)   //15m
                || (freq >= 24926000 && freq <= 24926200)   //12m
                || (freq >= 28126000 && freq <= 28126200)   //10m
                || (freq >= 50294400 && freq <= 50294600)   //6m
                || (freq >= 70092400 && freq <= 70092600)   //4m
                || (freq >= 144489900 && freq <= 144490100) //2m
                || (freq >= 432301600 && freq <= 432301800) //70cm
                || (freq >= 1296501400 && freq <= 1296501600);//23cm
    }

    /**
     * 通过频率获取波长
     *
     * @param freq 频率
     * @return 波长
     */
    @SuppressLint("DefaultLocale")
    public static String getMeterFromFreq(long freq) {
        if (freq >= 135700 && freq <= 137800) {
            return "2200m";
        }  // 2200m
        else if (freq >= 472000 && freq <= 479000) {
            return "630m";
        }  // 160m
        else if (freq >= 1800000 && freq <= 2000000) {
            return "160m";
        }  // 160m
        else if (freq >= 3500000 && freq <= 4000000) {
            return "80m";
        }  //  80m
        else if (freq >= 5351500 && freq <= 5366500) {
            return "60m";
        }  //  80m
        else if (freq >= 7000000 && freq <= 7300000) {
            return "40m";
        }  //  40m
        else if (freq >= 10100000 && freq <= 10150000) {
            return "30m";
        }  //  30m
        else if (freq >= 14000000 && freq <= 14350000) {
            return "20m";
        }  //  20m
        else if (freq >= 18068000 && freq <= 18168000) {
            return "17m";
        }  //  17m
        else if (freq >= 21000000 && freq <= 21450000) {
            return "15m";
        }  //  15m
        else if (freq >= 24890000 && freq <= 24990000) {
            return "12m";
        }  //  12m
        else if (freq >= 28000000 && freq <= 29700000) {
            return "10m";
        }  //  10m
        else if (freq >= 50000000 && freq <= 54000000) {
            return "6m";
        }  //   6m
        else if (freq >= 144000000 && freq <= 148000000) {
            return "2m";
        }  //   2m
        else if (freq >= 220000000 && freq <= 225000000) {
            return "1.25m";
        }  //1.25m
        else if (freq >= 420000000 && freq <= 450000000) {
            return "70cm";
        }  //0.7m
        else if (freq >= 902000000 && freq <= 928000000) {
            return "33cm";
        }  //0.33m
        else if (freq >= 1240000000 && freq <= 1300000000) {
            return "23cm";
        }  //0.23m
        else {
            return calculationMeterFromFreq(freq);
        }//不在范围内，就计算一下
    }

    @SuppressLint("DefaultLocale")
    private static String calculationMeterFromFreq(Long freq) {
        if (freq == 0) return "";
        float meter = 300000000f / (float) freq;
        if (meter < 1) {//以厘米为单位
            return String.format("%dcm", Math.round(meter * 10) * 10);
        } else if (meter < 20) {//小于20米，以米为单位
            return String.format("%dm", Math.round(meter));
        } else {//大于20M,以10米为单位
            return String.format("%dm", Math.round(meter / 10) * 10);
        }
    }

    public static String getFrequencyAllInfo(long freq) {
        return String.format("%s (%s)", getFrequencyStr(freq), getMeterFromFreq(freq));
    }

    @SuppressLint("DefaultLocale")
    public static String getFrequencyFloat(long freq) {
        return String.format("%d.%06d", freq / 1000000, (freq % 1000000));
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("0x%02x ", data[i] & 0xff));
        }
        return s.toString();
    }

    /**
     * 한국어판(0.93-kr13): ICOM(CI-V 0x15 0x12)·시에구의 SWR 미터 원값(0~255)을 실제 SWR 값으로 바꾼다.
     * 눈금은 0=1.0, 48=1.5, 80=2.0, 120=3.0, 255=25.5 로 알려져 있어, 그 네 구간을 직선으로 잇는다.
     *
     * @param meter 미터 원값(0~255)
     * @return 실제 SWR 값(1.0 ~ 25.5)
     */
    public static float meterToSwr(int meter) {
        float m = meter;
        if (m < 0) return 0f;
        if (m > 255f) m = 255f;
        if (m <= 48f) return 1.0f + m * (0.5f / 48f);            //1.0 ~ 1.5
        if (m <= 80f) return 1.5f + (m - 48f) * (0.5f / 32f);    //1.5 ~ 2.0
        if (m <= 120f) return 2.0f + (m - 80f) * (1.0f / 40f);   //2.0 ~ 3.0
        return 3.0f + (m - 120f) * (22.5f / 135f);                //3.0 ~ 25.5
    }

    /**
     * 한국어판(0.93-kr13): SWR 값을 화면에 쓸 문자열로 바꿉니다. (예: 1.0, 1.2, 2.1 / 10 이상은 "10+")
     */
    public static String getSwrStr(float swr) {
        if (swr >= 10f) return "10+";
        return String.format(Locale.US, "%.1f", swr);
    }
}
