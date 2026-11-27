package com.bg7yoz.ft8cn.ui;
/**
 * 워터폴·스펙트럼 막대에 쓰는 레벨별 색상표.
 *
 * 한국어판(0.93-kr4): 쨍한 원색 파랑 배경이 눈에 부담이 된다는 요청에 맞춰
 *   낮은 레벨(잡음·빈 주파수)은 어두운 딥 네이비(#04070F ~ #0E2755)로 낮추고,
 *   실제 신호가 있는 중·고레벨은 밝은 청록 → 노랑 → 주황 → 빨강으로 대비가
 *   뚜렷하게 올라가도록 256단계 색상을 미리 계산해 둔다.
 *
 * 한국어판(0.93-kr12): 케이블 USB 오디오로 잡음 바닥이 거의 없어지자 신호 자취가
 *   너무 흐리게 보인다는 요청에 맞춰, 잡음 구간(0~132단계)은 그대로 두고
 *   신호 구간(146~236단계)만 약 25~30% 밝고 선명하게 올렸다.
 *   빈 주파수는 여전히 어두운 네이비이고, 실제 신호만 또렷하게 떠오른다.
 *
 * @author DS2YTO
 */
public final class SpectrumColorTable {

    /**
     * 색상이 바뀌는 레벨 위치(0~255). stops[i] ~ stops[i+1] 구간을 colors[i] → colors[i+1] 로 보간한다.
     */
    private static final int[] STOPS = {
            0, 64, 96, 120, 132, 146, 160, 178, 198, 218, 236, 255};

    /**
     * STOPS 위치의 색상(ARGB).
     */
    private static final int[] COLORS = {
            0xFF04070F, // 0   거의 검정 (kr12 그대로)
            0xFF060D1E, // 64  아주 어두운 네이비 (kr12 그대로)
            0xFF091528, // 96  딥 네이비 (kr12 그대로)
            0xFF0C1E3C, // 120 잡음 바닥 (kr12 그대로)
            0xFF0E2755, // 132 어두운 딥 블루(잡음 상단) (kr12 그대로)
            0xFF165FA5, // 146 파랑 (kr12: #12518C 에서 약 40% 밝게)
            0xFF1498CD, // 160 하늘색 (kr12: #1086B4 에서 약 30% 밝게)
            0xFF1EE0F0, // 178 밝은 청록 (kr12: #18CEE2 에서 밝게)
            0xFF94F6FA, // 198 아주 밝은 하늘색 (kr12: #7DF0F6 에서 밝게)
            0xFFFFEB5A, // 218 노랑 (kr12: #FFE03A 에서 밝게)
            0xFFFFA72C, // 236 주황 (kr12: #FF9612 에서 밝게)
            0xFFFF3838  // 255 빨강 (kr12: #FF2E2E 에서 조금 밝게)
    };

    private static final int[] LEVEL_COLORS = buildLevelColors();

    private SpectrumColorTable() {
    }

    /**
     * 레벨(0~255)에 해당하는 색상을 돌려준다.
     *
     * @param level 스펙트럼 데이터 값
     * @return ARGB 색상
     */
    public static int getColor(int level) {
        if (level < 0) {
            return LEVEL_COLORS[0];
        }
        if (level > 255) {
            return LEVEL_COLORS[255];
        }
        return LEVEL_COLORS[level];
    }

    /**
     * 256단계 색상표를 만든다.
     */
    private static int[] buildLevelColors() {
        int[] table = new int[256];
        int stop = 0;
        for (int level = 0; level < 256; level++) {
            while (stop < STOPS.length - 2 && level > STOPS[stop + 1]) {
                stop++;
            }
            int from = STOPS[stop];
            int to = STOPS[stop + 1];
            float rate = to == from ? 0f : (float) (level - from) / (float) (to - from);
            table[level] = blend(COLORS[stop], COLORS[stop + 1], rate);
        }
        return table;
    }

    /**
     * 두 색상을 rate 비율로 섞는다.
     */
    private static int blend(int colorFrom, int colorTo, float rate) {
        int red = channel(colorFrom, 16) + Math.round((channel(colorTo, 16) - channel(colorFrom, 16)) * rate);
        int green = channel(colorFrom, 8) + Math.round((channel(colorTo, 8) - channel(colorFrom, 8)) * rate);
        int blue = channel(colorFrom, 0) + Math.round((channel(colorTo, 0) - channel(colorFrom, 0)) * rate);
        return 0xFF000000 | (clamp255(red) << 16) | (clamp255(green) << 8) | clamp255(blue);
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static int clamp255(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }
}
