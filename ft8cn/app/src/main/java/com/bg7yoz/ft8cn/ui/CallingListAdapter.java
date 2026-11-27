package com.bg7yoz.ft8cn.ui;
/**
 * 消息列表Adapter。使用此Adapter有解码界面、呼叫界面、网格追踪界面。
 * 不同周期背景不同。为了区分，共有4种背景颜色。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.opengl.Visibility;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.ArrayList;

public class CallingListAdapter extends RecyclerView.Adapter<CallingListAdapter.CallingListItemHolder> {
    public enum ShowMode{CALLING_LIST,MY_CALLING,TRACKER}
    private static final String TAG = "CallingListAdapter";
    private final MainViewModel mainViewModel;
    private final ArrayList<Ft8Message> ft8MessageArrayList;

    //한국어판(0.93-kr11): [미교신만] 보기.
    //  켜져 있으면 아직 교신하지 않은 국의 줄만 visibleMessages 에 담고, 목록 위치는 모두 이 목록을 기준으로 센다.
    private boolean unworkedOnly;
    private final ArrayList<Ft8Message> visibleMessages = new ArrayList<>();
    // 보여 줄 줄을 다시 굴라야 하는지, 그리고 마지막으로 고를 때의 원래 목록 크기
    private boolean visibleDirty = true;
    private int visibleSourceSize = -1;
    private final Context context;

    private final ShowMode showMode;
    private View.OnClickListener onItemClickListener;

    /**
     * 한국어판(0.93-kr7): 목록 줄을 한 번 톡 눌렀을 때의 동작(원터치 호출·응답).
     * 롱프레스 컨텍스트 메뉴는 그대로 두고, 짧은 탭만 이쪽으로 받는다.
     */
    public interface OnItemTapHandler {
        void onItemTap(int position);
    }

    private OnItemTapHandler onItemTapHandler;

    private final View.OnCreateContextMenuListener menuListener=new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

            //view.setTag(ft8Message);//把消息对象传递给上一级界面
            int postion= (int) view.getTag();
            if (postion==-1) return;
            Ft8Message ft8Message = messageAt(postion);
            if (ft8Message == null) return;

            //添加菜单的参数i1:组，i2:id值，i3:显示顺序
            if (!ft8Message.getCallsignTo().contains("...")//目标不能是自己
                    //&& !ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
                    && !GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())
                    && !(ft8Message.i3==0&&ft8Message.n3==0)) {
                if (!ft8Message.checkIsCQ()) {
                    if (showMode==ShowMode.CALLING_LIST) {//在消息列表中就可以显示这个菜单了
                        contextMenu.add(0, 0, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.tracking_receiver)
                                        , ft8Message.getCallsignTo(), ft8Message.toWhere))
                                .setActionView(view);
                    }
                    if (!mainViewModel.ft8TransmitSignal.isSynFrequency()) {//如果同频率的话，会与发送者同频，会影响发送者！！！
                        contextMenu.add(0, 1, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.calling_receiver)
                                        , ft8Message.getCallsignTo(), ft8Message.toWhere))
                                .setActionView(view);
                    }
                    //说明是对我呼叫，加上回复菜单
                    //if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)) {
                    if (GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())) {
                        contextMenu.add(0, 4, 0, String.format(
                                        GeneralVariables.getStringFromResource(R.string.reply_to)
                                        , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                                .setActionView(view);

                    }
                    if (showMode!=ShowMode.TRACKER) {
                        contextMenu.add(0, 5, 0
                                , String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                        , ft8Message.getCallsignTo())).setActionView(view);
                    }

                    //增加查询日志
                    contextMenu.add(0, 7, 0
                            , String.format(GeneralVariables.getStringFromResource(R.string.qsl_query_log_menu)
                                    , ft8Message.getCallsignTo())).setActionView(view);

                }
            }

            if (!ft8Message.getCallsignFrom().contains("...")
                    //&& !ft8Message.getCallsignFrom().equals(GeneralVariables.myCallsign)
                    && !GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignFrom())
                    && !(ft8Message.i3==0&&ft8Message.n3==0)) {
                if (showMode==ShowMode.CALLING_LIST) {//在消息列表中就可以显示这个菜单了
                    contextMenu.add(1, 2, 0, String.format(
                                    GeneralVariables.getStringFromResource(R.string.tracking)
                                    , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                            .setActionView(view);
                }
                contextMenu.add(1, 3, 0, String.format(
                                GeneralVariables.getStringFromResource(R.string.calling)
                                , ft8Message.getCallsignFrom(), ft8Message.fromWhere))
                        .setActionView(view);
                if (showMode!=ShowMode.TRACKER) {
                    contextMenu.add(1, 6, 0
                            , String.format(GeneralVariables.getStringFromResource(R.string.qsl_qrz_confirmation_s)
                                    , ft8Message.getCallsignFrom())).setActionView(view);
                }

                //增加查询日志
                contextMenu.add(0, 8, 0
                        , String.format(GeneralVariables.getStringFromResource(R.string.qsl_query_log_menu)
                                , ft8Message.getCallsignFrom())).setActionView(view);
            }

        }
    };



    public CallingListAdapter(Context context, MainViewModel mainViewModel
            , ArrayList<Ft8Message> messages, ShowMode showMode) {
        this.mainViewModel = mainViewModel;
        this.context = context;
        this.showMode=showMode;
        ft8MessageArrayList = messages;
        this.unworkedOnly = GeneralVariables.unworkedOnlyMode;
        rebuildVisibleMessages();
    }

    /**
     * 한국어판(0.93-kr11): [미교신만] 보기를 켜고 끈다.
     * 켜면 아직 교신하지 않은 국의 줄만 보이고, 끄면 전체 디코드 줄이 그대로 보인다.
     *
     * @param on 켤지(true) 끌지(false)
     */
    public void setUnworkedOnly(boolean on) {
        GeneralVariables.unworkedOnlyMode = on;
        this.unworkedOnly = on;
        refreshVisibleMessages();
    }

    /**
     * 한국어판(0.93-kr11): 보여 줄 줄을 다시 고르고 화면을 갱신한다.
     */
    public void refreshVisibleMessages() {
        rebuildVisibleMessages();
        notifyDataSetChanged();
    }

    /**
     * 한국어판(0.93-kr11): [미교신만] 보기가 켜져 있는지 돌려준다.
     *
     * @return 켜져 있으면 true
     */
    public boolean isUnworkedOnly() {
        return unworkedOnly;
    }

    /**
     * 한국어판(0.93-kr11): 실제로 보여 줄 줄만 골라 둔다(디코드 목록에서만 적용한다).
     */
    private void rebuildVisibleMessages() {
        visibleMessages.clear();
        for (int i = 0; i < ft8MessageArrayList.size(); i++) {
            Ft8Message message = ft8MessageArrayList.get(i);
            if (message == null) {
                continue;
            }
            if (isFiltering() && isWorkedMessage(message)) {
                continue;
            }
            visibleMessages.add(message);
        }
        visibleDirty = false;
        visibleSourceSize = ft8MessageArrayList.size();
    }

    /**
     * 한국어판(0.93-kr11): [미교신만] 보기를 지금 적용할지(디코드 목록이고, 켜져 있을 때).
     *
     * @return 적용하면 true
     */
    private boolean isFiltering() {
        return unworkedOnly && showMode == ShowMode.CALLING_LIST;
    }

    /**
     * 한국어판(0.93-kr11): 이미 교신한 국인지 확인한다.
     * 줄그음(setQueryHolderQSL_Callsign)과 같은 checkQSLCallsign 기준을 쓰고,
     * 내가 보낸 줄은 받는 쪽 콜사인으로 본다.
     *
     * @param message 디코드 줄
     * @return 교신한 적이 있으면 true
     */
    private boolean isWorkedMessage(Ft8Message message) {
        String callsign = otherCallsign(message);
        if (callsign == null || callsign.trim().isEmpty() || callsign.contains("...")) {
            return false;
        }
        return GeneralVariables.checkQSLCallsign(callsign);
    }

    /**
     * 한국어판(0.93-kr11): 이 줄이 가리키는 상대 콜사인(내 송신 줄이면 받는 쪽).
     *
     * @param message 디코드 줄
     * @return 상대 콜사인
     */
    private String otherCallsign(Ft8Message message) {
        String callsignFrom = message.getCallsignFrom();
        if (callsignFrom != null && GeneralVariables.checkIsMyCallsign(callsignFrom)) {
            return message.getCallsignTo();
        }
        return callsignFrom;
    }

    /**
     * 한국어판(0.93-kr11): 목록 위치(어댑터 위치)에 있는 줄을 돌려준다.
     *
     * @param position 목록에서의 위치
     * @return 디코드 줄(없으면 null)
     */
    private Ft8Message messageAt(int position) {
        if (position < 0 || position >= visibleMessages.size()) {
            return null;
        }
        return visibleMessages.get(position);
    }

    @NonNull
    @Override
    public CallingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view ;
        if (GeneralVariables.simpleCallItemMode) {
            view = layoutInflater.inflate(R.layout.call_list_holder_simple_item, parent, false);
        }else {
            view = layoutInflater.inflate(R.layout.call_list_holder_item, parent, false);
        }
        return new CallingListItemHolder(view,onItemClickListener,menuListener);
    }

    /**
     * 删除消息
     *
     * @param position 在列表中的位置
     */
    public void deleteMessage(int position) {
        Ft8Message message = messageAt(position);
        if (message == null) {
            return;
        }
        int sourceIndex = indexOfMessage(message);
        if (sourceIndex >= 0) {
            ft8MessageArrayList.remove(sourceIndex);
        }
        if (position < visibleMessages.size() && visibleMessages.get(position) == message) {
            visibleMessages.remove(position);
        }
        visibleDirty = true;
    }

    /**
     * 한국어판(0.93-kr11): 같은 줄(같은 객체)의 원래 목록 위치를 찾는다.
     *
     * @param message 찾을 줄
     * @return 원래 목록에서의 위치(없으면 -1)
     */
    private int indexOfMessage(Ft8Message message) {
        for (int i = 0; i < ft8MessageArrayList.size(); i++) {
            if (ft8MessageArrayList.get(i) == message) {
                return i;
            }
        }
        return -1;
    }

    public Ft8Message getMessageByPosition(int position){
        return messageAt(position);
    }

    /**
     * 通过holder获取消息
     *
     * @param holder holder
     * @return ft8message
     */
    public Ft8Message getMessageByViewHolder(RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() == -1) {
            return null;
        }
        return messageAt(holder.getAdapterPosition());
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull CallingListItemHolder holder, int position) {
        holder.callListHolderConstraintLayout.setTag(position);//设置layout的tag，为了识别消息的定位
        Ft8Message message = messageAt(position);
        if (message == null) {
            return;
        }
        holder.ft8Message = message;
        holder.showMode = showMode;//确定是消息列表还是关注消息的列表
        holder.isSyncFreq = mainViewModel.ft8TransmitSignal.isSynFrequency();//如果同频发射，就不显示呼叫接收者

        holder.callingUtcTextView.setText(UtcTimer.getTimeHHMMSS(holder.ft8Message.utcTime));
        //한국어판(0.93-kr7): 한 번 톡 누르면 이 줄의 국을 바로 부르거나 응답한다
        holder.onItemTapHandler = onItemTapHandler;
        //时序，包括颜色,
        holder.callingListSequenceTextView.setText(holder.ft8Message.getSequence() == 0 ? "0" : "1");
        holder.isWeakSignalImageView.setVisibility(holder.ft8Message.isWeakSignal ? View.VISIBLE:View.INVISIBLE);

        //한국어판(0.93-kr5): 행 색을 4개 시차가 아니라 뜻으로 나눈다.
        //  내 송신(TX QSO)=빨강, 내 CQ 송신(TX CQ)=호박, 나를 호출한 수신(RX)=노랑, CQ=초록, 그 밖의 교신=다크 네이비.
        //  (한국어판 0.93-kr12에서 CQ 송신을 호박색으로 떼고, 나를 부른 수신을 노랑으로 바꿨다)
        applyRowStyle(holder);

        holder.callingListIdBTextView.setText(holder.ft8Message.getdB());
        //时间偏移，如果超过1.0秒，-0.05秒，노란색으로 알린다
        holder.callListDtTextView.setText(holder.ft8Message.getDt());
        if (holder.ft8Message.time_sec > 1.0f || holder.ft8Message.time_sec < -0.05) {
            holder.callListDtTextView.setTextColor(context.getColor(R.color.list_warning_text_color));
        }

        holder.callingListFreqTextView.setText(holder.ft8Message.getFreq_hz());

        //查是不是通联过的呼号，获取是否存在holder.otherBandIsQso中
        setQueryHolderQSL_Callsign(holder);

        //在别的波段通联过的呼号，消息를 연하늘색으로 살짝 구분한다
        if (!holder.ft8Message.inMyCall() && holder.otherBandIsQso
                && holder.ft8Message.freq_hz > 0.01f) {
            holder.callListMessageTextView.setTextColor(
                    context.getColor(R.color.list_normal_accent_color));
        }


        //한국어판(0.93-kr10): 이 줄이 보이기 직전에 나라 배지까지 붙여 쓴다
        applyMessageText(holder);

        //载波频率
        holder.bandItemTextView.setText(BaseRigOperation.getFrequencyStr(holder.ft8Message.band));
        //计算距离
        holder.callingListDistTextView.setText(MaidenheadGrid.getDistStr(
                GeneralVariables.getMyMaidenheadGrid()
                , holder.ft8Message.getMaidenheadGrid(mainViewModel.databaseOpr)));
        holder.callingListCallsignToTextView.setText("");//被呼叫者
        holder.callingListCallsignFromTextView.setText("");//呼叫者

        //消息类型
        holder.callingListCommandIInfoTextView.setText(holder.ft8Message.getCommandInfo());
        if (!(holder.ft8Message.i3 == 1 || holder.ft8Message.i3 == 2)) {
            holder.callingListCommandIInfoTextView.setTextColor(holder.rowAccentColor);
        }

        //CQ 메시지 뒤에는 이제 따로 칠하지 않는다(행 색이 CQ를 알려 준다)
        if (holder.ft8Message.checkIsCQ()) {
            holder.ft8Message.toWhere = "";
        }


        if (holder.ft8Message.fromWhere != null) {
            holder.callingListCallsignFromTextView.setText(holder.ft8Message.fromWhere);
        } else {
            holder.callingListCallsignFromTextView.setText("");
        }

        if (holder.ft8Message.toWhere != null) {
            holder.callingListCallsignToTextView.setText(holder.ft8Message.toWhere);
        } else {
            holder.callingListCallsignToTextView.setText("");
        }

        //给没有通联过的分区打标记
        setToDxcc(holder);
        setFromDxcc(holder);


        //查询呼号归属地，为防止占用太多运算资源，当from为空是再做查询的工作
//        if (holder.ft8Message.fromWhere == null) {
//            setQueryHolderCallsign(holder);//查询呼号归属地
//        }

        if (holder.ft8Message.freq_hz <= 0.01f) {//这是发射界面
            holder.callingListIdBTextView.setVisibility(View.GONE);
            holder.callListDtTextView.setVisibility(View.GONE);
            holder.callingListFreqTextView.setText("TX");
            holder.bandItemTextView.setVisibility(View.GONE);
            holder.callingListDistTextView.setVisibility(View.GONE);
            holder.callingListCommandIInfoTextView.setVisibility(View.GONE);
            holder.callingUtcTextView.setVisibility(View.GONE);
            holder.callingListCallsignToTextView.setVisibility(View.GONE);
            holder.callingListCallsignFromTextView.setVisibility(View.GONE);
            holder.dxccToImageView.setVisibility(View.GONE);
            holder.ituToImageView.setVisibility(View.GONE);
            holder.cqToImageView.setVisibility(View.GONE);
            holder.dxccFromImageView.setVisibility(View.GONE);
            holder.ituFromImageView.setVisibility(View.GONE);
            holder.cqFromImageView.setVisibility(View.GONE);
        } else if (GeneralVariables.simpleCallItemMode){//简单列表模式
            holder.bandItemTextView.setVisibility(View.GONE);
            holder.callingListDistTextView.setVisibility(View.GONE);
            holder.callingListCommandIInfoTextView.setVisibility(View.GONE);
            holder.callingUtcTextView.setVisibility(View.GONE);
            holder.callingListCallsignToTextView.setVisibility(View.GONE);
            holder.dxccToImageView.setVisibility(View.GONE);
            holder.ituToImageView.setVisibility(View.GONE);
            holder.cqToImageView.setVisibility(View.GONE);
        }else {//标准列表模式
            holder.callingListIdBTextView.setVisibility(View.VISIBLE);
            holder.callListDtTextView.setVisibility(View.VISIBLE);
            holder.bandItemTextView.setVisibility(View.VISIBLE);
            holder.callingListDistTextView.setVisibility(View.VISIBLE);
            holder.callingListCommandIInfoTextView.setVisibility(View.VISIBLE);
            holder.callingUtcTextView.setVisibility(View.VISIBLE);
            holder.callingListCallsignToTextView.setVisibility(View.VISIBLE);
            holder.callingListCallsignFromTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 한국어판(0.93-kr5): 행 하나의 배경과 글씨 색을 한 번에 정한다.
     * 아이폰 FT8-Improved·WSJT-X와 같은 결로 CQ=초록, 나를 호출한 수신=노랑,
     * 내 송신=빨강, 그 밖의 교신=다크 네이비를 쓴다.
     * 한국어판(0.93-kr12): 내 송신을 다시 둘로 나눈다 — CQ 송신=호박색, 교신 송신(QSO)=빨강.
     *
     * @param holder 목록 행
     */
    private void applyRowStyle(@NonNull CallingListItemHolder holder) {
        //내가 보낸 메시지(송신 목록의 행이거나, 내 신호가 되돌아 들린 행)
        String callsignFrom = holder.ft8Message.getCallsignFrom();
        String callsignTo = holder.ft8Message.getCallsignTo();
        boolean isMyOwnMessage = callsignFrom != null && callsignTo != null
                && GeneralVariables.checkIsMyCallsign(callsignFrom)
                && !GeneralVariables.checkIsMyCallsign(callsignTo);
        boolean isTx = holder.ft8Message.freq_hz <= 0.01f || isMyOwnMessage;//내가 보낸 메시지
        //한국어판(0.93-kr12): 내가 보낸 줄이 CQ인지(CQ DS2YTO PM37) 아니면 교신용인지(DS2YTO BG5JQV -19)를 가른다
        boolean isTxCq = isTx && holder.ft8Message.callsignTo != null
                && holder.ft8Message.checkIsCQ();
        boolean isMine = !isTx && holder.ft8Message.inMyCall();//나를 부른 수신(RX)
        boolean isCq = !isTx && !isMine && holder.ft8Message.checkIsCQ();

        int backgroundResId;
        if (isTxCq) {
            //CQ 송신 = 호박색(흰 글씨)
            backgroundResId = R.drawable.calling_list_cell_tx_cq_style;
            holder.rowPrimaryColor = context.getColor(R.color.list_tx_cq_text_color);
            holder.rowSecondaryColor = context.getColor(R.color.list_tx_cq_muted_color);
            holder.rowAccentColor = context.getColor(R.color.list_tx_cq_accent_color);
        } else if (isTx) {
            //교신 송신(QSO) = 빨강(흰 글씨)
            backgroundResId = R.drawable.calling_list_cell_tx_style;
            holder.rowPrimaryColor = context.getColor(R.color.list_tx_text_color);
            holder.rowSecondaryColor = context.getColor(R.color.list_tx_muted_color);
            holder.rowAccentColor = context.getColor(R.color.list_tx_accent_color);
        } else if (isMine) {
            //나를 부른 수신 = 노랑(검은 글씨)
            backgroundResId = R.drawable.calling_list_cell_mine_style;
            holder.rowPrimaryColor = context.getColor(R.color.list_mine_text_color);
            holder.rowSecondaryColor = context.getColor(R.color.list_mine_muted_color);
            holder.rowAccentColor = context.getColor(R.color.list_mine_accent_color);
        } else if (isCq) {
            backgroundResId = R.drawable.calling_list_cell_cq_style;
            holder.rowPrimaryColor = context.getColor(R.color.list_cq_text_color);
            holder.rowSecondaryColor = context.getColor(R.color.list_cq_muted_color);
            holder.rowAccentColor = context.getColor(R.color.list_cq_accent_color);
        } else {
            backgroundResId = R.drawable.calling_list_cell_normal_style;
            holder.rowPrimaryColor = context.getColor(R.color.list_normal_text_color);
            holder.rowSecondaryColor = context.getColor(R.color.list_normal_muted_color);
            holder.rowAccentColor = context.getColor(R.color.list_normal_accent_color);
        }

        holder.callListHolderConstraintLayout.setBackgroundResource(backgroundResId);

        //본문은 밝게, 곁들인 숫자들은 한 단계 낮춰 눈이 덜 피로하게
        holder.callListMessageTextView.setTextColor(holder.rowPrimaryColor);
        holder.callingListSequenceTextView.setTextColor(holder.rowAccentColor);
        holder.callingListCommandIInfoTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingListIdBTextView.setTextColor(holder.rowSecondaryColor);
        holder.callListDtTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingListFreqTextView.setTextColor(holder.rowSecondaryColor);
        holder.bandItemTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingListDistTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingUtcTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingListCallsignFromTextView.setTextColor(holder.rowSecondaryColor);
        holder.callingListCallsignToTextView.setTextColor(holder.rowSecondaryColor);
        //메시지 글씨 뒤의 예전 노란 칠은 지운다
        holder.callListMessageTextView.setBackgroundResource(R.color.textview_none_color);
    }

    /**
     * 한국어판(0.93-kr10): 메시지 끝에 어느 나라인지 짧은 배지를 붙여 쓴다.
     * 예) {@code CQ HL5NTR PM46 [일본]}. 배지는 작고 행 색에 맞춘 밝은 글씨로, 줄바꿈을 만들지 않는다.
     * 통신한 적 있는 국(줄그음)일 때는 메시지 부분만 줄을 긋는다.
     *
     * @param holder 목록 행
     */
    private void applyMessageText(@NonNull CallingListItemHolder holder) {
        String message = holder.ft8Message.getMessageText(true);
        if (message == null) {
            message = "";
        }
        String country = countryBadgeText(holder.ft8Message);
        SpannableString text;
        if (country.isEmpty()) {
            text = new SpannableString(message);
        } else {
            String badge = " [" + country + "]";
            text = new SpannableString(message + badge);
            int start = message.length();
            int end = text.length();
            text.setSpan(new RelativeSizeSpan(0.8f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new ForegroundColorSpan(holder.rowAccentColor), start, end
                    , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (holder.strikeMessage && message.length() > 0) {
            text.setSpan(new StrikethroughSpan(), 0, message.length()
                    , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.callListMessageTextView.setText(text);
    }

    /**
     * 한국어판(0.93-kr10): 콜사인이 어느 나라인지 돌려준다. 내가 보낸 줄이면 받는 쪽 나라를 쓴다.
     *
     * @param message 메시지
     * @return 나라 이름(모르면 빈 문자열)
     */
    private String countryBadgeText(Ft8Message message) {
        if (message == null) {
            return "";
        }
        String callsignFrom = message.getCallsignFrom();
        boolean myMessage = callsignFrom != null
                && GeneralVariables.checkIsMyCallsign(callsignFrom);
        String where = myMessage ? message.toWhere : message.fromWhere;
        if (where == null || where.trim().isEmpty()) {
            where = myMessage ? message.fromWhere : message.toWhere;
        }
        if (where == null) {
            return "";
        }
        return where.trim();
    }

    private void setFromDxcc(@NonNull CallingListItemHolder holder) {

        if (holder.ft8Message.fromDxcc && holder.ft8Message.freq_hz > 0.01f) {
            holder.dxccFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.dxccFromImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.fromCq && holder.ft8Message.freq_hz > 0.01f) {
            holder.cqFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.cqFromImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.fromItu && holder.ft8Message.freq_hz > 0.01f) {
            holder.ituFromImageView.setVisibility(View.VISIBLE);
        } else {
            holder.ituFromImageView.setVisibility(View.GONE);
        }
    }

    private void setToDxcc(@NonNull CallingListItemHolder holder) {
        if (holder.ft8Message.toDxcc && holder.ft8Message.freq_hz > 0.01f) {
            holder.dxccToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.dxccToImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.toCq && holder.ft8Message.freq_hz > 0.01f) {
            holder.cqToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.cqToImageView.setVisibility(View.GONE);
        }

        if (holder.ft8Message.toItu && holder.ft8Message.freq_hz > 0.01f) {
            holder.ituToImageView.setVisibility(View.VISIBLE);
        } else {
            holder.ituToImageView.setVisibility(View.GONE);
        }
    }

    //检查是不是通联过的呼号
    private void setQueryHolderQSL_Callsign(@NonNull CallingListItemHolder holder) {
        //查是不是在本波段内通联成功过的呼号
        //한국어판(0.93-kr10): 줄그음은 메시지 칸 전체가 아니라 메시지 글자에만 넣는다(나라 배지는 살린다)
        holder.strikeMessage = GeneralVariables.checkQSLCallsign(holder.ft8Message.getCallsignFrom());
        holder.callListMessageTextView.setPaintFlags(
                holder.callListMessageTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.otherBandIsQso = GeneralVariables.checkQSLCallsign_OtherBand(holder.ft8Message.getCallsignFrom());
    }

    @Override
    public int getItemCount() {
        //한국어판(0.93-kr11): 새 디코드가 들어오거나 목록이 지워지면 이때 다시 고른다(2열·1열 모두 해당)
        if (visibleDirty || visibleSourceSize != ft8MessageArrayList.size()) {
            rebuildVisibleMessages();
        }
        return visibleMessages.size();
    }

    public void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * 한국어판(0.93-kr7): 목록 줄을 짧게 탭했을 때의 동작을 등록한다.
     * (setOnItemClickListener 를 쓰는 화면은 예전처럼 그 리스너가 우선한다)
     *
     * @param onItemTapHandler 탭 처리기
     */
    public void setOnItemTapHandler(OnItemTapHandler onItemTapHandler) {
        this.onItemTapHandler = onItemTapHandler;
    }

    static class CallingListItemHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "CallingListItemHolder";
        ConstraintLayout callListHolderConstraintLayout;
        TextView callingListIdBTextView, callListDtTextView, callingListFreqTextView,
                callListMessageTextView, callingListDistTextView, callingListSequenceTextView,
                callingListCallsignFromTextView, callingListCallsignToTextView
                , callingListCommandIInfoTextView,
                bandItemTextView, callingUtcTextView;
        ImageView dxccToImageView, ituToImageView, cqToImageView, dxccFromImageView
                , ituFromImageView, cqFromImageView,isWeakSignalImageView;
        public Ft8Message ft8Message;
        //boolean showFollow;
        ShowMode showMode;
        boolean isSyncFreq;
        boolean otherBandIsQso = false;
        //한국어판(0.93-kr10): 이 국과 이미 교신한 적이 있어 메시지에 줄을 그을지
        boolean strikeMessage = false;
        //한국어판(0.93-kr5): 행 색에 맞춘 글씨 색
        int rowPrimaryColor = 0;
        int rowSecondaryColor = 0;
        int rowAccentColor = 0;
        //한국어판(0.93-kr7): 줄을 짧게 탭했을 때 부를 처리기(어댑터가 바인딩할 때 넣어 준다)
        OnItemTapHandler onItemTapHandler;


        public CallingListItemHolder(@NonNull View itemView, View.OnClickListener listener
                    ,View.OnCreateContextMenuListener menuListener) {
            super(itemView);
            callListHolderConstraintLayout = itemView.findViewById(R.id.callListHolderConstraintLayout);
            callingListIdBTextView = itemView.findViewById(R.id.callingListIdBTextView);
            callListDtTextView = itemView.findViewById(R.id.callListDtTextView);
            callingListFreqTextView = itemView.findViewById(R.id.callingListFreqTextView);
            callListMessageTextView = itemView.findViewById(R.id.callListMessageTextView);
            callingListDistTextView = itemView.findViewById(R.id.callingListDistTextView);
            callingListSequenceTextView = itemView.findViewById(R.id.callingListSequenceTextView);
            callingListCallsignFromTextView = itemView.findViewById(R.id.callingListCallsignFromTextView);
            callingListCallsignToTextView = itemView.findViewById(R.id.callToItemTextView);
            callingListCommandIInfoTextView = itemView.findViewById(R.id.callingListCommandIInfoTextView);
            bandItemTextView = itemView.findViewById(R.id.bandItemTextView);
            callingUtcTextView = itemView.findViewById(R.id.callingUtcTextView);

            dxccToImageView = itemView.findViewById(R.id.dxccToImageView);
            ituToImageView = itemView.findViewById(R.id.ituToImageView);
            cqToImageView = itemView.findViewById(R.id.cqToImageView);
            dxccFromImageView = itemView.findViewById(R.id.dxccFromImageView);
            ituFromImageView = itemView.findViewById(R.id.ituFromImageView);
            cqFromImageView = itemView.findViewById(R.id.cqFromImageView);
            isWeakSignalImageView=itemView.findViewById(R.id.isWeakSignalImageView);

            dxccToImageView.setVisibility(View.GONE);
            ituToImageView.setVisibility(View.GONE);
            cqToImageView.setVisibility(View.GONE);
            dxccFromImageView.setVisibility(View.GONE);
            ituFromImageView.setVisibility(View.GONE);
            cqFromImageView.setVisibility(View.GONE);
            itemView.setTag(-1);
            if (listener != null) {
                itemView.setOnClickListener(listener);
            } else {
                //한국어판(0.93-kr7): 짧은 탭 = 원터치 호출/응답. 롱프레스 메뉴는 그대로 동작한다.
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        OnItemTapHandler handler = onItemTapHandler;
                        if (handler == null) return;
                        int position = getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return;
                        try {
                            handler.onItemTap(position);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "onItemTap: " + e.getMessage(), e);
                        }
                    }
                });
            }
            itemView.setOnCreateContextMenuListener(menuListener);

        }


    }
}
