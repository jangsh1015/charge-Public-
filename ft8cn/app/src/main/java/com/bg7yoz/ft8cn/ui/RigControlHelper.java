package com.bg7yoz.ft8cn.ui;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;

/**
 * 한국어판(0.93-kr15): 무전기 상태(SWR·Po·ALC) 표시와 TUNE·PWR 조작을 한 곳에 모은 도우미.
 *
 * 같은 단추가 스펙트럼 화면(미터 상자 옆)과 휴대폰 하단 상태 줄 두 곳에 나오므로,
 * 명령을 보내는 코드와 토스트 문구를 여기 한 곳에 두어 두 화면의 동작이 어긋나지 않게 한다.
 *
 * @author DS2YTO
 */
public class RigControlHelper {
    //SWR 색: 1.5 이하는 초록, 1.5~2.0은 호박, 2.0을 넘으면 빨강
    public static final int COLOR_SWR_GOOD = 0xFF00E676;
    public static final int COLOR_SWR_WARN = 0xFFFFC400;
    public static final int COLOR_SWR_BAD = 0xFFFF5252;
    //튜너가 도는 동안 SWR을 밝게 보여 주는 시간
    public static final long TUNE_WATCH_MS = 8000;
    //PWR 단추의 출력 선택지(%)
    public static final int[] RF_POWER_PRESETS = {10, 25, 50, 75, 100};
    //오토튠을 시작한 시각 + 감시 시간 (스펙트럼 화면과 하단 상태 줄이 함께 본다)
    private static long tuneWatchUntil = 0;

    private RigControlHelper() {
    }

    /**
     * 지금 오토튠이 도는 중인지(명령을 보낸 뒤 8초 동안) 돌려준다.
     * 그동안은 송신 전이라도 무전기가 SWR·Po·ALC를 계속 읽어 오고, 화면은 값을 밝게 보여 준다.
     *
     * @return 오토튠 감시 시간 안이면 true
     */
    public static boolean isTuneWatching() {
        return System.currentTimeMillis() < tuneWatchUntil;
    }

    /**
     * SWR 값에 맞는 글자 색을 돌려준다.
     *
     * @param swr 실제 SWR 값
     * @return 초록·호박·빨강 중 하나
     */
    public static int swrColor(float swr) {
        if (swr <= 1.5f) {
            return COLOR_SWR_GOOD;
        } else if (swr <= 2.0f) {
            return COLOR_SWR_WARN;
        } else {
            return COLOR_SWR_BAD;
        }
    }

    /**
     * SWR 표시 글자를 만든다. 측정값이 없으면 'SWR --'.
     *
     * @param swr 실제 SWR 값. 0 이하면 측정값 없음
     * @return 예: "SWR 1.2", "SWR --"
     */
    public static String formatSwr(float swr) {
        if (!(swr > 0f)) {
            return "SWR --";
        }
        return "SWR " + BaseRigOperation.getSwrStr(swr);
    }

    /**
     * 송신 중 읽은 출력(Po)·ALC 를 한 줄 글자로 만든다. 둘 다 없으면 빈 글자.
     *
     * @param po  출력 %. 음수면 모름
     * @param alc ALC %. 음수면 모름
     * @return 예: "Po 95% · ALC 35%", "Po 95%", ""
     */
    public static String formatMeters(int po, int alc) {
        StringBuilder meters = new StringBuilder();
        if (po >= 0) {
            meters.append(String.format(java.util.Locale.US, "Po %d%%", po));
        }
        if (alc >= 0) {
            if (meters.length() > 0) {
                meters.append(" · ");
            }
            meters.append(String.format(java.util.Locale.US, "ALC %d%%", alc));
        }
        return meters.toString();
    }

    /**
     * PWR 단추의 이름표를 만든다. 무전기에 걸린 출력을 모르면 그냥 'PWR'.
     *
     * @param percent 무전기에 설정된 출력 %. 음수면 모름
     * @return 예: "PWR 100%", "PWR"
     */
    public static String formatPwrLabel(int percent) {
        if (percent < 0) {
            return "PWR";
        }
        return String.format(java.util.Locale.US, "PWR %d%%", percent);
    }

    /**
     * 무전기에 안테나 튜너 오토튠(TUNE) 명령을 보낸다.
     * 튜너가 꺼져 있어도 되도록 무전기 쪽에서 튜너를 먼저 켜고 잠시 뒤 오토튠을 시작한다.
     *
     * @param context        토스트 문구를 읽을 화면
     * @param rig            지금 연결된 무전기
     * @param isTransmitting 지금 송신 중인지
     * @return 명령을 보냈으면 true (이미 토스트로 사정을 알린 경우는 false)
     */
    public static boolean doAutoTune(Context context, BaseRig rig, boolean isTransmitting) {
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(context.getString(R.string.tune_connect_first), true);
            return false;
        }
        if (isTransmitting) {
            ToastMessage.show(context.getString(R.string.tune_while_transmitting), true);
            return false;
        }
        if (!rig.startAutoTune()) {
            ToastMessage.show(context.getString(R.string.tune_not_supported), true);
            return false;
        }
        tuneWatchUntil = System.currentTimeMillis() + TUNE_WATCH_MS;
        ToastMessage.show(context.getString(R.string.tune_started), true);
        return true;
    }

    /**
     * PWR 단추를 누르면 출력 선택 창(10·25·50·75·100%)을 연다.
     * 지금 무전기에 걸린 출력에 표시를 해 둔다.
     *
     * @param context 메뉴를 띄울 화면
     * @param anchor  단추
     * @param rig     지금 연결된 무전기
     */
    public static void showRfPowerMenu(Context context, View anchor, BaseRig rig) {
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(context.getString(R.string.tune_connect_first), true);
            return;
        }
        rig.readRfPower();//창을 열 때마다 지금 출력을 다시 확인한다

        Integer value = GeneralVariables.mutableRfPower.getValue();
        int current = value == null ? -1 : value;

        PopupMenu popupMenu = new PopupMenu(context, anchor);
        Menu menu = popupMenu.getMenu();
        menu.setGroupCheckable(0, true, true);
        for (int i = 0; i < RF_POWER_PRESETS.length; i++) {
            int percent = RF_POWER_PRESETS[i];
            MenuItem item = menu.add(0, percent, i, percent + "%");
            item.setCheckable(true);
            item.setChecked(current >= 0 && Math.abs(current - percent) <= 2);
        }
        final BaseRig targetRig = rig;
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                applyRfPower(context, targetRig, item.getItemId());//메뉴 항목 id 가 곧 출력 %
                return true;
            }
        });
        popupMenu.show();
    }

    /**
     * 고른 출력을 무전기에 보낸다.
     *
     * @param context 메뉴를 띄운 화면
     * @param rig     지금 연결된 무전기
     * @param percent 출력 %
     * @return 설정했으면 true
     */
    public static boolean applyRfPower(Context context, BaseRig rig, int percent) {
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(context.getString(R.string.tune_connect_first), true);
            return false;
        }
        if (!rig.setRfPowerPercent(percent)) {
            ToastMessage.show(context.getString(R.string.rf_power_not_supported), true);
            return false;
        }
        ToastMessage.show(context.getString(R.string.rf_power_set, percent), true);
        return true;
    }
}
