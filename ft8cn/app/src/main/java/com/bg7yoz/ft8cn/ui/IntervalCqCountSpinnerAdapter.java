package com.bg7yoz.ft8cn.ui;
/**
 * 간헐 CQ 한 번에 보내는 CQ 횟수 목록 (1~5회).
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

public class IntervalCqCountSpinnerAdapter extends BaseAdapter {
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 5;
    private final Context mContext;

    public IntervalCqCountSpinnerAdapter(Context context) {
        mContext = context;
    }

    /**
     * 목록의 위치(index)에 해당하는 송신 횟수를 돌려준다.
     *
     * @param index 목록 위치
     * @return 송신 횟수
     */
    public static int getCountValue(int index) {
        int count = MIN_COUNT + index;
        if (count < MIN_COUNT) {
            return MIN_COUNT;
        }
        if (count > MAX_COUNT) {
            return MAX_COUNT;
        }
        return count;
    }

    /**
     * 송신 횟수에 해당하는 목록 위치를 돌려준다.
     *
     * @param count 송신 횟수
     * @return 목록 위치
     */
    public static int getPosition(int count) {
        int index = count - MIN_COUNT;
        if (index < 0) {
            return 0;
        }
        if (index > MAX_COUNT - MIN_COUNT) {
            return MAX_COUNT - MIN_COUNT;
        }
        return index;
    }

    @Override
    public int getCount() {
        return MAX_COUNT - MIN_COUNT + 1;
    }

    @Override
    public Object getItem(int i) {
        return getCountValue(i);
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
            textView.setText(String.format(GeneralVariables.getStringFromResource(
                    R.string.interval_cq_count_item), getCountValue(i)));
        }
        return view;
    }
}
