package com.bg7yoz.ft8cn.ui;
/**
 * 간헐 CQ 주기 목록 (꺼짐(연속) / 5~60분).
 * FT8CN 한국어판에서 추가한 기능입니다.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

public class IntervalCqSpinnerAdapter extends BaseAdapter {
    //0은 꺼짐(연속), 나머지는 분 단위 주기
    private static final int[] MINUTES = {0, 5, 10, 15, 20, 30, 60};
    private final Context mContext;

    public IntervalCqSpinnerAdapter(Context context) {
        mContext = context;
    }

    /**
     * 목록의 위치(index)에 해당하는 주기(분)를 돌려준다. 0이면 기능 꺼짐.
     *
     * @param index 목록 위치
     * @return 주기(분)
     */
    public static int getMinutes(int index) {
        if (index < 0 || index >= MINUTES.length) {
            return 0;
        }
        return MINUTES[index];
    }

    /**
     * 주기(분)에 해당하는 목록 위치를 돌려준다.
     *
     * @param minutes 주기(분)
     * @return 목록 위치, 없으면 0
     */
    public static int getPosition(int minutes) {
        for (int i = 0; i < MINUTES.length; i++) {
            if (MINUTES[i] == minutes) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public int getCount() {
        return MINUTES.length;
    }

    @Override
    public Object getItem(int i) {
        return MINUTES[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"DefaultLocale", "ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        view = inflater.inflate(R.layout.no_reply_limit_spinner_item, null);
        if (view != null) {
            TextView textView = (TextView) view.findViewById(R.id.noReplyLimitCountItemTextView);
            if (i == 0) {
                textView.setText(GeneralVariables.getStringFromResource(R.string.interval_cq_period_off));
            } else {
                textView.setText(String.format(GeneralVariables.getStringFromResource(
                        R.string.interval_cq_minutes), MINUTES[i]));
            }
        }
        return view;
    }
}
