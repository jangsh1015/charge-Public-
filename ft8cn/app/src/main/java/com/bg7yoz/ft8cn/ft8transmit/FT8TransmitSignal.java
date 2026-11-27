package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 与发射信号有关的类。包括分析通联过程的自动程序。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.OnUtcTimer;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FT8TransmitSignal {
    private static final String TAG = "FT8TransmitSignal";

    private boolean transmitFreeText = false;
    private String freeText = "FREE TEXT";

    private final DatabaseOpr databaseOpr;//配置信息，和相关数据的数据库
    private TransmitCallsign toCallsign;//目标呼号
    public MutableLiveData<TransmitCallsign> mutableToCallsign = new MutableLiveData<>();

    private int functionOrder = 6;
    public MutableLiveData<Integer> mutableFunctionOrder = new MutableLiveData<>();//指令的顺序变化
    private boolean activated = false;//是否处于可以发射的模式
    public MutableLiveData<Boolean> mutableIsActivated = new MutableLiveData<>();
    public int sequential;//发射的时序
    public MutableLiveData<Integer> mutableSequential = new MutableLiveData<>();
    private boolean isTransmitting = false;
    public MutableLiveData<Boolean> mutableIsTransmitting = new MutableLiveData<>();//是否处于发射状态
    public MutableLiveData<String> mutableTransmittingMessage = new MutableLiveData<>();//当前消息的内容

    //********************************************
    //한국어판: 간헐 CQ(주기적 Auto CQ) 상태
    //  - intervalCQBurstLeft  : 이번 CQ 묶음에서 남은 송신 횟수
    //  - intervalCQNextBurst  : 다음 CQ 묶음을 시작할 시각(ms)
    //  - intervalCQWaiting    : 지금은 수신만 하며 대기 중인지
    private int intervalCQBurstLeft = 0;
    private long intervalCQNextBurst = 0;
    private boolean intervalCQWaiting = false;
    private boolean intervalCQWatchdogWarned = false;
    private String intervalCQStatus = "";
    //화면에 표시할 간헐 CQ 상태 문자열(""이면 표시 안 함)
    public MutableLiveData<String> mutableIntervalCQStatus = new MutableLiveData<>();

    //********************************************
    //한국어판(0.93-kr3): 무응답으로 중단된 콜사인의 자동 재호출 방지
    //  - '무응답 N회 중단'(GeneralVariables.noReplyLimit)으로 호출이 중단되면
    //    그 콜사인(대문자)을 아래 맵에 넣고 30분 동안 추적 자동 호출에서 제외한다.
    //  - 화면에서 직접 터치해 부르거나 상대가 먼저 나를 부르면 기록과 관계없이 응답하고,
    //    그때는 기록도 지운다.
    //  - 30분이 지난 기록은 확인할 때와 1초마다 도는 하트비트에서 정리한다.
    private static final long NO_REPLY_COOLDOWN_MS = 30 * 60 * 1000L;//30분
    private static final ConcurrentHashMap<String, Long> noReplyCooldown
            = new ConcurrentHashMap<>();//콜사인(대문자) -> 만료 시각(ms)

    //********************************************
    //한국어판(0.93-kr5): 같은 상대에게 같은 단계의 메시지를 끝없이 되풀이하지 않게 하는 안전장치
    //  - 무응답 제한이 어떤 경로로든 동작하지 못하는 경우(상대가 리포트를 되풀이하는 등)에도
    //    같은 콜사인·같은 단계의 메시지를 '무응답 N회 중단' 횟수만큼 보내면 자동 호출을 멈추고
    //    CQ/대기로 돌아간다.
    private String repeatedTxCallsign = "";//되풀이 중인 상대
    private int repeatedTxOrder = -1;//되풀이 중인 메시지 단계
    private int repeatedTxCount = 0;//같은 상대·같은 단계로 보낸 횟수
    private static final int REPEAT_LIMIT_WHEN_IGNORED = 20;//무응답 제한이 '무시(0)'일 때의 상한

    //한국어판(0.93-kr5): 상대가 RR73/RRR/73으로 교신을 끝냈을 때,
    //  리포트를 더 보내지 않고 73을 한 번만 보낸 뒤 CQ로 돌아가기 위한 표시
    private boolean qsoCompleteAfterTransmit = false;

    //public MutableLiveData<Integer> currentOrder = new MutableLiveData<>();//当前要发射的指令

    //********************************************
    //此处的信息是用于保存QSL的
    private long messageStartTime = 0;//消息开始的时间
    private long messageEndTime = 0;//消息结束的时间
    private String toMaidenheadGrid = "";//目标的网格信息
    private int sendReport = 0;//我发送到对方的报告
    private int sentTargetReport = -100;//


    private int receivedReport = 0;//我接收到的报告
    private int receiveTargetReport = -100;//发送给对方的信号报告
    //********************************************
    private final OnTransmitSuccess onTransmitSuccess;//一般是用于保存QSL数据


    //防止播放中止，变量不能放在方法中
    private AudioAttributes attributes = null;
    private AudioFormat myFormat = null;
    private AudioTrack audioTrack = null;

    public UtcTimer utcTimer;


    public ArrayList<FunctionOfTransmit> functionList = new ArrayList<>();
    public MutableLiveData<ArrayList<FunctionOfTransmit>> mutableFunctions = new MutableLiveData<>();

    private final OnDoTransmitted onDoTransmitted;//一般是用于打开关闭PTT
    private final ExecutorService doTransmitThreadPool = Executors.newCachedThreadPool();
    private final DoTransmitRunnable doTransmitRunnable = new DoTransmitRunnable(this);

    static {
        System.loadLibrary("ft8cn");
    }

    /**
     * 发射模块的构造函数，需要两个回调，一个是当发射时（有两个动作，用于打开/关闭PTT），另一个时当成功时(用于保存QSL)。
     *
     * @param databaseOpr       数据库
     * @param doTransmitted     当发射前后时的回调
     * @param onTransmitSuccess 当发射成功时的回调
     */
    public FT8TransmitSignal(DatabaseOpr databaseOpr
            , OnDoTransmitted doTransmitted, OnTransmitSuccess onTransmitSuccess) {
        this.onDoTransmitted = doTransmitted;//用于打开关闭PTT的事件
        this.onTransmitSuccess = onTransmitSuccess;//用于保存QSL的事件
        this.databaseOpr = databaseOpr;

        setTransmitting(false);
        setActivated(false);


        //观察音量设置的变化
        GeneralVariables.mutableVolumePercent.observeForever(new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                if (audioTrack != null) {
                    audioTrack.setVolume(aFloat);
                }
            }
        });

        utcTimer = new UtcTimer(FT8Common.FT8_SLOT_TIME_M, false, new OnUtcTimer() {
            @Override
            public void doHeartBeatTimer(long utc) {
                //1초마다 간헐 CQ 남은 시간 표시를 갱신한다
                updateIntervalCQStatus();
                //한국어판(0.93-kr3): 30분이 지난 무응답 쿨다운 기록을 정리한다
                cleanupNoReplyCooldown();
            }

            //@RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void doOnSecTimer(long utc) {
                //한국어판: 간헐 CQ로 다음 주기를 기다리는 동안에는 송신 감시(Tx watchdog) 시간을 세지 않는다.
                //  대기 중에 감시가 만료되어 자동 송신이 꺼지고 빨간 확성기가 사라지는 것을 막는다.
                if (isIntervalCQWaitingIdle()) {
                    GeneralVariables.resetLaunchSupervision();
                }
                //超过自动监管时间，就停止
                if (GeneralVariables.isLaunchSupervisionTimeout()) {
                    setActivated(false);
                    updateIntervalCQStatus();
                    return;
                }
                if (UtcTimer.getNowSequential() == sequential && activated) {
                    if (GeneralVariables.myCallsign.length() < 3) {
                        //我的呼号不正确，不能发射！
                        ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
                        return;
                    }
                    //한국어판: 간헐 CQ 모드에서는 CQ 묶음 차례에만 송신하고 나머지는 수신만 한다
                    if (!intervalCQAllowTransmit()) {
                        updateIntervalCQStatus();
                        return;
                    }
                    doTransmit();//发射动作还是准确按时间来，延迟是音频信号的延迟
                }
                updateIntervalCQStatus();
            }
        });

        utcTimer.start();

    }

    /**
     * 立即发射
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void transmitNow() {
        if (GeneralVariables.myCallsign.length() < 3) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return;
        }
        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.adjust_call_target)
                , toCallsign.callsign));

        //把信号报告相关的复位
        resetTargetReport();

        if (UtcTimer.getNowSequential() == sequential) {
            if ((UtcTimer.getSystemTime() % 15000) < 2500) {
                setTransmitting(false);
                doTransmit();
            }
        }
    }

    //发射信号
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void doTransmit() {
        if (!activated) {
            return;
        }
        //한국어판(0.93-kr5): 같은 상대에게 같은 단계의 메시지를 되풀이하고 있지 않은지 먼저 확인한다.
        //  (되풀이 한도를 넘었으면 이번 송신을 하지 않고 CQ/대기로 돌아간다)
        if (isSameMessageRepeatLimitReached()) {
            return;
        }
        //检测是不是黑名单频率，WSPR-2的频率，频率=电台频率+声音频率
        if (BaseRigOperation.checkIsWSPR2(
                GeneralVariables.band + Math.round(GeneralVariables.getBaseFrequency()))) {
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(R.string.use_wspr2_error)
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
            setActivated(false);
            return;
        }
        Log.d(TAG, "doTransmit: 开始发射...");
        doTransmitThreadPool.execute(doTransmitRunnable);

        mutableFunctions.postValue(functionList);
    }

    /**
     * 设置呼叫，生成发射消息列表
     *
     * @param transmitCallsign 目标呼号
     * @param functionOrder    命令顺序
     * @param toMaidenheadGrid 目标网格
     */
    @SuppressLint("DefaultLocale")
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void setTransmit(TransmitCallsign transmitCallsign
            , int functionOrder, String toMaidenheadGrid) {

        messageStartTime = 0;//复位起始的时间
        //한국어판(0.93-kr5): 새 목표·새 단계를 잡았으면 '73 한 번 보내고 끝내기' 표시를 지운다
        qsoCompleteAfterTransmit = false;

        Log.d(TAG, "准备发射数据...");
        if (GeneralVariables.checkFun1(toMaidenheadGrid)) {
            this.toMaidenheadGrid = toMaidenheadGrid;
        } else {
            this.toMaidenheadGrid = "";
        }
        //한국어판(0.93-kr3): 직접 터치해 부르거나 상대가 먼저 불러 응답하는 경우에는
        //  무응답 쿨다운과 관계없이 정상적으로 부르고, 그 콜사인의 쿨다운은 지운다.
        clearNoReplyCooldown(transmitCallsign.callsign);

        mutableToCallsign.postValue(transmitCallsign);//设定呼叫的目标对象（含报告、时序，频率，呼号）
        toCallsign = transmitCallsign;//设定呼叫的目标
        //mutableToCallsign.postValue(toCallsign);//设定呼叫的目标

        if (functionOrder == -1) {//说明是回复消息
            //此时的toMaidenheadGrid是extraInfo
            this.functionOrder = GeneralVariables.checkFunOrderByExtraInfo(toMaidenheadGrid) + 1;
            if (this.functionOrder == 6) {//如果已经是73了，就改到消息1
                this.functionOrder = 1;
            }
        } else {
            this.functionOrder = functionOrder;//当前指令的序号
        }

        if (transmitCallsign.frequency == 0) {
            transmitCallsign.frequency = GeneralVariables.getBaseFrequency();
        }
        if (GeneralVariables.synFrequency) {//如果是同频发送，就与目标呼号频率一致
            setBaseFrequency(transmitCallsign.frequency);
        }

        sequential = (toCallsign.sequential + 1) % 2;//发射的时序
        mutableSequential.postValue(sequential);//通知发射时序改变
        generateFun();
        mutableFunctionOrder.postValue(functionOrder);

    }

    @SuppressLint("DefaultLocale")
    public void setBaseFrequency(float freq) {
        GeneralVariables.setBaseFrequency(freq);
        //写到数据中
        databaseOpr.writeConfig("freq", String.format("%.0f", freq), null);
    }

    /**
     * 根据消息号，生成对应的消息
     *
     * @param order 消息号
     * @return FT8消息
     */
    public Ft8Message getFunctionCommand(int order) {
        switch (order) {
            //发射模式1，BG7YOY BG7YOZ OL50
            case 1:
                resetTargetReport();//把给对方的信号报告记录复位成-100
                return new Ft8Message(1, 0, toCallsign.callsign, GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
            //发射模式2，BG7YOY BG7YOZ -10
            case 2:
                sentTargetReport = toCallsign.snr;

                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, toCallsign.getSnr());
            //发射模式3，BG7YOY BG7YOZ R-10
            case 3:
                sentTargetReport = toCallsign.snr;
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "R" + toCallsign.getSnr());
            //发射模式4，BG7YOY BG7YOZ RRR
            case 4:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "RR73");
            //发射模式5，BG7YOY BG7YOZ 73
            case 5:
                return new Ft8Message(1, 0, toCallsign.callsign
                        , GeneralVariables.myCallsign, "73");
            //发射模式6，CQ BG7YOZ OL50
            case 6:
                resetTargetReport();//把给对方的信号报告,接收到对方的信号报告记录复位成-100
                Ft8Message msg = new Ft8Message(1, 0, "CQ", GeneralVariables.myCallsign
                        , GeneralVariables.getMyMaidenhead4Grid());
                msg.modifier = GeneralVariables.toModifier;
                return msg;
        }

        return new Ft8Message("CQ", GeneralVariables.myCallsign
                , GeneralVariables.getMyMaidenhead4Grid());
    }

    /**
     * 生成指令序列
     */
    public void generateFun() {
        //ArrayList<FunctionOfTransmit> functions = new ArrayList<>();
        GeneralVariables.noReplyCount = 0;
        functionList.clear();
        for (int i = 1; i <= 6; i++) {
            if (functionOrder == 6) {//如果当前的指令序列是6(CQ)，那么就只生成一个消息
                functionList.add(new FunctionOfTransmit(6, getFunctionCommand(6), false));
                break;
            } else {
                functionList.add(new FunctionOfTransmit(i, getFunctionCommand(i), false));
            }
        }
        mutableFunctions.postValue(functionList);
        setCurrentFunctionOrder(functionOrder);//设置当前消息
    }

    /**
     * 为了最大限度兼容，把32位浮点转换成16位整型，有些声卡不支持32位的浮点。
     *
     * @param buffer 32位浮点音频
     * @return 16位整型
     */
    private short[] float2Short(float[] buffer) {
        short[] temp = new short[buffer.length + 8];//多出8个为0的数据包，是为了兼容QP-7C的RP2040音频判断
        for (int i = 0; i < buffer.length; i++) {
            float x = buffer[i];
            if (x > 1.0)
                x = 1.0f;
            else if (x < -1.0)
                x = -1.0f;
            temp[i] = (short) (x * 32767.0);
        }
        return temp;
    }

    private void playFT8Signal(Ft8Message msg) {

        if (GeneralVariables.connectMode == ConnectMode.NETWORK) {//网络方式就不播放音频了
            Log.d(TAG, "playFT8Signal: 进入网络发射程序，等待音频发送。");


            if (onDoTransmitted != null) {//处理音频数据，可以给ICOM的网络模式发送
                onDoTransmitted.onTransmitByWifi(msg);
            }


            long now = System.currentTimeMillis();
            while (isTransmitting) {//等待音频数据包发送完毕再退出，以触发afterTransmitting
                try {
                    Thread.sleep(1);
                    long current = System.currentTimeMillis() - now;
                    if (current > 13100) {//实际发射的时长
                        isTransmitting = false;
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "playFT8Signal: 退出网络音频发送。");
            afterPlayAudio();
            return;
        }

        //进入到CAT串口传输音频方式
        //2023-08-16 由DS1UFX提交修改（基于0.9版），用于(tr)uSDX audio over cat的支持。
        if (GeneralVariables.controlMode == ControlMode.CAT) {
            Log.d(TAG, "playFT8Signal: try to transmit over CAT");

            if (onDoTransmitted != null) {//处理音频数据，可以给truSDX的CAT模式发送
                if (onDoTransmitted.supportTransmitOverCAT()) {
                    onDoTransmitted.onTransmitOverCAT(msg);

                    long now = System.currentTimeMillis();
                    while (isTransmitting) {//等待音频数据包发送完毕再退出，以触发afterTransmitting
                        try {
                            Thread.sleep(1);
                            long current = System.currentTimeMillis() - now;
                            if (current > 13000) {//实际发射的时长
                                isTransmitting = false;
                                break;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "playFT8Signal: transmitting over CAT is finished.");
                    afterPlayAudio();
                    return;
                }
            }
        }


        //进入声卡模式
        float[] buffer;
        buffer = GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency()
                , GeneralVariables.audioSampleRate);
        if (buffer == null) {
            afterPlayAudio();
            return;
        }

        Log.d(TAG, String.format("playFT8Signal: 准备声卡播放....位数：%s,采样率：%d"
                , GeneralVariables.audioOutput32Bit ? "Float32" : "Int16"
                , GeneralVariables.audioSampleRate));
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        //myFormat = new AudioFormat.Builder().setSampleRate(FT8Common.SAMPLE_RATE)
        myFormat = new AudioFormat.Builder().setSampleRate(GeneralVariables.audioSampleRate)
                .setEncoding(GeneralVariables.audioOutput32Bit ? //浮点与整型
                        AudioFormat.ENCODING_PCM_FLOAT : AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int mySession = 0;
        audioTrack = new AudioTrack(attributes, myFormat
                , GeneralVariables.audioOutput32Bit ? GeneralVariables.audioSampleRate * 15 * 4
                : GeneralVariables.audioSampleRate * 15 * 2//浮点与整型
                , AudioTrack.MODE_STATIC
                , mySession);

        //区分32浮点和整型
        int writeResult;
        if (GeneralVariables.audioOutput32Bit) {
            writeResult = audioTrack.write(buffer, 0, buffer.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        } else {
            short[] audio_data = float2Short(buffer);
            writeResult = audioTrack.write(audio_data, 0, audio_data.length
                    , AudioTrack.WRITE_NON_BLOCKING);
        }

        if (buffer.length > writeResult) {
            Log.e(TAG, String.format("播放缓冲区不足：%d--->%d", buffer.length, writeResult));
        }

        //检查写入的结果，如果是异常情况，则直接需要释放资源
        if (writeResult == AudioTrack.ERROR_INVALID_OPERATION
                || writeResult == AudioTrack.ERROR_BAD_VALUE
                || writeResult == AudioTrack.ERROR_DEAD_OBJECT
                || writeResult == AudioTrack.ERROR) {
            //出异常情况
            Log.e(TAG, String.format("播放出错：%d", writeResult));
            afterPlayAudio();
            return;
        }
        audioTrack.setNotificationMarkerPosition(buffer.length);
        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack audioTrack) {
                afterPlayAudio();
            }

            @Override
            public void onPeriodicNotification(AudioTrack audioTrack) {

            }
        });
        if (audioTrack != null) {
            audioTrack.play();
            audioTrack.setVolume(GeneralVariables.volumePercent);//设置播放的音量
        }
    }

    /**
     * 播放完声音后的处理动作。包括回调onAfterTransmit,用于关闭PTT
     */
    private void afterPlayAudio() {
        if (onDoTransmitted != null) {
            onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
        }
        isTransmitting = false;
        mutableIsTransmitting.postValue(false);
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        //한국어판(0.93-kr5): 상대가 이미 RR73/73으로 교신을 끝냈으면 73을 한 번 보낸 것으로 마치고
        //  CQ/대기로 돌아간다(리포트를 다시 보내며 끝없이 부르지 않게).
        if (qsoCompleteAfterTransmit) {
            qsoCompleteAfterTransmit = false;
            Log.d(TAG, "afterPlayAudio: 교신을 마치고 CQ 상태로 돌아갑니다.");
            resetToCQ();
            setCurrentFunctionOrder(functionOrder);
            mutableToCallsign.postValue(toCallsign);
            mutableFunctionOrder.postValue(functionOrder);
        }
    }

    /**
     * 한국어판(0.93-kr5): 같은 상대에게 같은 단계의 메시지를 계속 보내고 있는지 확인한다.
     * 무응답 제한이 어떤 이유로든 동작하지 못해도(상대가 리포트를 되풀이하는 등),
     * 설정한 '무응답 N회 중단' 횟수만큼 같은 메시지를 보내면 호출을 멈춘다.
     *
     * @return true 이면 이번에는 송신하지 않는다
     */
    private boolean isSameMessageRepeatLimitReached() {
        if (transmitFreeText) {
            return false;
        }
        String target = (toCallsign == null || toCallsign.callsign == null)
                ? "" : toCallsign.callsign.trim();
        //CQ 나 목표가 정해지지 않은 때는 세지 않는다
        if (target.length() < 3 || "CQ".equalsIgnoreCase(target) || functionOrder == 6) {
            repeatedTxCallsign = target;
            repeatedTxOrder = functionOrder;
            repeatedTxCount = 0;
            return false;
        }
        if (!target.equals(repeatedTxCallsign) || functionOrder != repeatedTxOrder) {
            repeatedTxCallsign = target;
            repeatedTxOrder = functionOrder;
            repeatedTxCount = 0;
        }
        repeatedTxCount++;

        int limit = GeneralVariables.noReplyLimit > 0
                ? GeneralVariables.noReplyLimit : REPEAT_LIMIT_WHEN_IGNORED;
        if (limit < 3) {
            limit = 3;
        }
        if (repeatedTxCount <= limit) {
            return false;
        }

        Log.e(TAG, String.format(Locale.US
                , "isSameMessageRepeatLimitReached: %s 에게 %d단계 메시지를 %d번 보내 호출을 멈추고 CQ로 돌아갑니다."
                , target, functionOrder, repeatedTxCount));
        startNoReplyCooldown(target);
        repeatedTxCallsign = "";
        repeatedTxOrder = 6;
        repeatedTxCount = 0;
        qsoCompleteAfterTransmit = false;
        resetToCQ();
        setCurrentFunctionOrder(functionOrder);
        mutableToCallsign.postValue(toCallsign);
        mutableFunctionOrder.postValue(functionOrder);
        return true;
    }

    //当通联成功时的动作
    private void doComplete() {
        messageEndTime = UtcTimer.getSystemTime();//获取结束的时间

        //如对方没有网格，就从历史呼号与网格对应表中查找
        toMaidenheadGrid = GeneralVariables.getGridByCallsign(toCallsign.callsign, databaseOpr);

        if (messageStartTime == 0) {//如果起始时间没有，就取现在的
            messageStartTime = UtcTimer.getSystemTime();
        }


        //从历史记录中查信号报告
        //此处处理信号报告，是因为保存的信号报告经常与实际通联的信号报告不一致。
        //遍历接收到对方的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignFrom.equals(toCallsign.callsign)
                    && GeneralVariables.checkIsMyCallsign(message.callsignTo))) {
                    //&& message.callsignTo.equals(GeneralVariables.myCallsign))) {
                receiveTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }
        //遍历我发送给对方的信号报告
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message message = GeneralVariables.transmitMessages.get(i);
            if ((GeneralVariables.checkFun3(message.extraInfo)
                    || GeneralVariables.checkFun2(message.extraInfo))
                    && (message.callsignTo.equals(toCallsign.callsign)
                    && GeneralVariables.checkIsMyCallsign(message.callsignFrom))) {
                    //&& message.callsignFrom.equals(GeneralVariables.myCallsign))) {
                sentTargetReport = getReportFromExtraInfo(message.extraInfo);
                break;
            }
        }


        messageEndTime = UtcTimer.getSystemTime();
        if (onDoTransmitted != null) {//用于保存通联记录
            onTransmitSuccess.doAfterTransmit(new QSLRecord(
                    messageStartTime,
                    messageEndTime,
                    GeneralVariables.myCallsign,
                    GeneralVariables.getMyMaidenhead4Grid(),
                    toCallsign.callsign,
                    toMaidenheadGrid,
                    sentTargetReport != -100 ? sentTargetReport : sendReport,
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,//如果给对方的信号报告是不是-100，就用发给对方的信号报告记录
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency())
            ));

            GeneralVariables.addQSLCallsign(toCallsign.callsign);//把通联成功的呼号添加到列表中
            ToastMessage.show(String.format("QSO : %s , at %s", toCallsign.callsign
                    , BaseRigOperation.getFrequencyAllInfo(GeneralVariables.band)));
        }

    }

    /**
     * 设置当前要发射的指令顺序
     *
     * @param order 顺序
     */
    public void setCurrentFunctionOrder(int order) {
        functionOrder = order;
        for (int i = 0; i < functionList.size(); i++) {
            functionList.get(i).setCurrentOrder(order);
        }
        if (order == 1) {
            resetTargetReport();//复位信号报告
        }
        if (order == 4 || order == 5) {
            updateQSlRecordList(order, toCallsign);
        }
        mutableFunctions.postValue(functionList);
    }


    /**
     * 当目标是复合呼号（非标准信号），JTDX回复可能会缩短
     *
     * @param fromCall 对方的呼号
     * @param toCall   我的目标呼号
     * @return 是不是
     */
    private boolean checkCallsignIsCallTo(String fromCall, String toCall) {
        if (toCall.contains("/")) {//当对方的呼号在斜线时，JTDX会把/后面的字符去掉
            return toCall.contains(fromCall);
        } else {
            return fromCall.equals(toCall);
        }
    }

    /**
     * 检查消息中from中有目标呼号的数量。当有目标呼号呼叫我的消息，返回0，如果目标呼号呼叫别人，返回值应当大于1
     *
     * @param messages 消息列表
     * @return 0：有目标呼叫我的，1：没有任何目标呼号发出的消息，>1：有目标呼号呼叫别人的消息
     */
    private int checkTargetCallMe(ArrayList<Ft8Message> messages) {
        int fromCount = 1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//同一个时序下的消息不做解析
            if (toCallsign == null) {
                continue;
            }
            //if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                return 0;
            }
            if (checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                fromCount++;//计数器，from是目标呼号的情况
            }
        }
        return fromCount;
    }

    /**
     * 检测本消息列表中对方回复消息的序号，如果没有,返回-1
     *
     * @param messages 消息列表
     * @return 消息的序号
     */
    private int checkFunctionOrdFromMessages(ArrayList<Ft8Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.getSequence() == sequential) continue;//同一个时序下的消息不做解析
            if (toCallsign == null) {
                continue;
            }
            //是双方的呼叫信息
            //if (ft8Message.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(ft8Message.getCallsignTo())
                    && checkCallsignIsCallTo(ft8Message.getCallsignFrom(), toCallsign.callsign)) {
                //--TODO ----检查起始时间是不是0，如果是0，补充起始时间。因为有的呼叫会越过第一步

                //检测是不是对方给我的信号报告
                if (GeneralVariables.checkFun3(ft8Message.extraInfo)
                        || GeneralVariables.checkFun2(ft8Message.extraInfo)) {
                    //从消息中取信号报告，如果不正确（-100），那么就取消息中的信号报告
                    receivedReport = getReportFromExtraInfo(ft8Message.extraInfo);
                    receiveTargetReport = receivedReport;//对方给我的信号报告，要保存下来
                    if (receivedReport == -100) {//如果不正确，就取消息的报告
                        receivedReport = ft8Message.report;
                    }
                }
                sendReport = messages.get(i).snr;//把接收到的信号保存下来

                int order = GeneralVariables.checkFunOrder(ft8Message);//检查消息的序号
                if (order != -1) return order;//说明成功解析出序号
            }
        }

        return -1;//说明没找到消息
    }

    /**
     * 从扩展消息中获取对方给的信号报告，获取失败，值-100
     *
     * @param extraInfo 扩展消息
     * @return 信号报告
     */
    private int getReportFromExtraInfo(String extraInfo) {
        String s = extraInfo.replace("R", "").trim();
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -100;
        }
    }

    /**
     * 检查是不是属于排除的消息：
     * 1.与发射的时序相同
     * 2.不在相同的波段
     * 3.呼号是排除的字头
     *
     * @param msg 消息
     * @return 是/否
     */
    private boolean isExcludeMessage(Ft8Message msg) {
        return msg.getSequence() == sequential || msg.band != GeneralVariables.band
                || GeneralVariables.checkIsExcludeCallsign(msg.callsignFrom);
    }

    /**
     * 检查有没有人CQ我，或我关注的呼号在CQ
     *
     * @param messages 消息列表
     * @return false=没有符合的消息，TRUE=有符合的消息
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    private boolean checkCQMeOrFollowCQMessage(ArrayList<Ft8Message> messages) {
        //此message是刚刚解码出的消息
        //第一个循环与第二个循环都是检查是否有CQ我的消息。第一个循环是优先查询是不是我的目标呼号。
        // 目的是当多个目标呼叫我，我的回复不专一的问题。
        //检查CQ我，不是73，且是我呼叫的目标
        for (int i = messages.size() - 1; i >= 0; i--) {//此处是检查有没有CQ我。（TO:ME,且不能是73）
            Ft8Message msg = messages.get(i);
            if (isExcludeMessage(msg)) continue;//检查是不是属于排除的消息：
            if (toCallsign == null) break;

            //if (msg.getCallsignTo().equals(GeneralVariables.myCallsign)
            if (GeneralVariables.checkIsMyCallsign(msg.getCallsignTo())
                    && msg.getCallsignFrom().equals(toCallsign.callsign)//todo 注意测试复合呼号的情况
                    && !GeneralVariables.checkFun4_5(msg.extraInfo)) {//cq我、73/RR73이 아닌 응답、发送方是我关注的目标
                //设置发射之前，确定消息的序号，避免从头开始
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                                , msg.getSequence(), msg.snr)
                        , GeneralVariables.checkFunOrder(msg) + 1
                        , msg.extraInfo);
                return true;
            }
        }

        //检查CQ我，不是73，
        for (int i = messages.size() - 1; i >= 0; i--) {//此处是检查有没有CQ我。（TO:ME,且不能是73）
            Ft8Message msg = messages.get(i);
            if (isExcludeMessage(msg)) continue;//检查是不是属于排除的消息：
            //if ((msg.getCallsignTo().equals(GeneralVariables.myCallsign)
            //한국어판(0.93-kr5): 73/RR73/RRR은 교신을 끝낸 인사이므로, 여기에 리포트로 답하지 않는다.
            //  (그대로 두면 상대의 RR73을 받을 때마다 리포트를 다시 보내 끝없이 부르게 된다)
            if ((GeneralVariables.checkIsMyCallsign(msg.getCallsignTo())
                    && !GeneralVariables.checkFun4_5(msg.extraInfo))) {//cq我、73/RR73/RRR이 아닌 응답
                //한국어판(0.93-kr7): CQ 대기 중에 나를 부른 국을 찾았으면,
                //  이전 교신의 신호 기록을 먼저 비우고, 곧바로 그 국을 목표로 잡아
                //  다음 기회에 응답이 나가게 한다(예전에는 기록이 남아 단계가 엇갈렸다).
                resetTargetReport();
                Log.d(TAG, String.format(Locale.US
                        , "CQ 대기 중 호출 발견: %s (%s) -> 다음 슬롯에 응답합니다."
                        , msg.getCallsignFrom(), msg.getMessageText(true)));
                //设置发射之前，确定消息的序号，避免从头开始
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                                , msg.getSequence(), msg.snr)
                        , GeneralVariables.checkFunOrder(msg) + 1
                        , msg.extraInfo);
                mutableToCallsign.postValue(toCallsign);
                mutableFunctionOrder.postValue(functionOrder);
                return true;
            }
        }


        //如果不自动呼叫我关注的消息，就退出
        if (!GeneralVariables.autoCallFollow) {
            return false;
        }

        if (toCallsign == null) {
            return false;
        }
        //当已经有目标呼号的时候，不对关注的呼号做反应
        if (toCallsign.haveTargetCallsign()) {
            return false;
        }

        //我关注的呼号次之，！！！到关注的消息列表中找
        //此处是检查关注的呼号在CQ。（TO:CQ,且不能本次通联能成功的呼号）
        for (int i = GeneralVariables.transmitMessages.size() - 1; i >= 0; i--) {
            Ft8Message msg = GeneralVariables.transmitMessages.get(i);
            if (isExcludeMessage(msg)) continue;//检查是不是属于排除的消息：

            //处于CQ,FROM是我的关注呼号,并且不在通联成功的呼号列表中
            if ((msg.checkIsCQ()//在CQ
                    && ((GeneralVariables.autoCallFollow && GeneralVariables.autoFollowCQ)//自动呼叫CQ
                    || GeneralVariables.callsignInFollow(msg.getCallsignFrom()))//是我关注的
                    && !GeneralVariables.checkQSLCallsign(msg.getCallsignFrom())//之前没有联通成功过
                    && !GeneralVariables.checkIsMyCallsign(msg.callsignFrom))) {//不是我自己
                    //&& !msg.callsignFrom.equals(GeneralVariables.myCallsign))) {//不是我自己

                //한국어판(0.93-kr3): 무응답으로 중단됐던 국이라도 30분 안에 새 CQ를 내면
                //  자동으로 다시 부르지 않고 건너뛰어 다른 CQ 국을 찾는다.
                if (isNoReplyCoolingDown(msg.getCallsignFrom())) {
                    continue;
                }

                resetTargetReport();
                setTransmit(new TransmitCallsign(msg.i3, msg.n3, msg.getCallsignFrom(), msg.freq_hz
                        , msg.getSequence(), msg.snr), 1, msg.extraInfo);

                return true;
            }
        }

        return false;

    }


    public void updateQSlRecordList(int order, TransmitCallsign toCall) {
        if (toCall == null) return;
        if (toCall.callsign.equals("CQ")) return;

        QSLRecord record = GeneralVariables.qslRecordList.getRecordByCallsign(toCall.callsign);
        if (record == null) {
            toMaidenheadGrid = GeneralVariables.getGridByCallsign(toCallsign.callsign, databaseOpr);
            record = GeneralVariables.qslRecordList.addQSLRecord(new QSLRecord(
                    messageStartTime,
                    messageEndTime,
                    GeneralVariables.myCallsign,
                    GeneralVariables.getMyMaidenhead4Grid(),
                    toCallsign.callsign,
                    toMaidenheadGrid,
                    sentTargetReport != -100 ? sentTargetReport : sendReport,
                    receiveTargetReport != -100 ? receiveTargetReport : receivedReport,//如果给对方的信号报告是不是-100，就用发给对方的信号报告记录
                    "FT8",
                    GeneralVariables.band,
                    Math.round(GeneralVariables.getBaseFrequency()
                    )));
        }
        //根据消息序列更新内容
        switch (order) {
            case 1://更新网格，和对方消息的SNR
                record.setToMaidenGrid(toMaidenheadGrid);
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            case 2://更新对方返回的信号报告，和对方的信号报告
            case 3:
                record.setSendReport(sentTargetReport != -100 ? sentTargetReport : sendReport);
                record.setReceivedReport(receiveTargetReport != -100 ? receiveTargetReport : receivedReport);
                GeneralVariables.qslRecordList.deleteIfSaved(record);
                break;

            //当RR73或73的状态下，就保存日志。
            case 4:
            case 5:
                if (!record.saved) {
                    doComplete();//保存到数据库
                    record.saved = true;
                }

                break;
        }

    }

    /**
     * 从关注列表解码的消息中，此处是变化发射程序的入口
     *
     * @param msgList 消息列表
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void parseMessageToFunction(ArrayList<Ft8Message> msgList) {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        if (msgList.size() == 0) return;//没有消息解析，返回

        if (msgList.get(0).getSequence() == sequential) {
            return;
        }
        ArrayList<Ft8Message> messages = new ArrayList<>(msgList);//防止线程冲突


        int newOrder = checkFunctionOrdFromMessages(messages);//检查消息中对方回复的消息序号，-1为没有收到
        //한국어판(0.93-kr5): 응답이 와도 단계가 앞으로 나아가지 않으면(같은 리포트를 되풀이하는 등)
        //  무응답으로 센다. 예전에는 어떤 응답이든 횟수를 0으로 되돌려, 같은 단계를 끝없이 되풀이할 수 있었다.
        boolean noProgress = false;
        if (newOrder != -1) {//如果有消息序号，说明有回应
            if (newOrder >= functionOrder) {
                GeneralVariables.noReplyCount = 0;//단계가 앞으로 나아갔을 때만 횟수를 되돌린다
            } else {
                GeneralVariables.noReplyCount++;//제자리걸음하는 응답도 무응답으로 센다
                noProgress = true;
            }
        }

        //更新一下通联的列表检查是不是在通联列表中，如果没有记录下来，就保存
        updateQSlRecordList(newOrder, toCallsign);


        //한국어판(0.93-kr5): 상대가 RR73/RRR(4)로 교신을 끝냈으면 리포트를 더 보내지 않는다.
        //  예전에는 RR73을 받으면 5단계(73)로 넘어가기만 해서, 상태가 어긋나면
        //  R-11 같은 리포트를 끝없이 되풀이했다. 이제는 73을 한 번만 보내고 CQ로 돌아간다.
        if (newOrder == 4) {
            if (functionOrder >= 1 && functionOrder <= 4) {
                functionOrder = 5;//73을 한 번 보낸 뒤 CQ로 돌아간다
                qsoCompleteAfterTransmit = true;
                setCurrentFunctionOrder(functionOrder);
                mutableFunctionOrder.postValue(functionOrder);
            } else {
                qsoCompleteAfterTransmit = false;
                resetToCQ();
                setCurrentFunctionOrder(functionOrder);
                mutableFunctionOrder.postValue(functionOrder);
            }
            return;
        }

        //한국어판(0.93-kr5): 같은 리포트를 되풀이하는 응답이 '무응답 N회'를 넘으면 여기서 멈춘다
        if (noProgress && handleNoReplyLimitReached(messages)) {
            return;
        }

        // 判断通联成功：对方回73（5）||我是73（5），且对方没回（-1）
        // 或者我是RR73(4),且已经达到无回应阈值，且有无回应限制
        // 或我是RR73(4),且对方开始呼叫别人了,解决RR73卡死的问题
        //한국어판(0.93-kr7): '교신 완료'와 '대상을 포기함'을 나눠 본다.
        //  - 교신 완료(73/RR73 응답): 그대로 CQ로 돌아간다.
        //  - 포기: RR73 단계에서도 설정한 무응답 횟수에 이르면 멈추고, 그 국은 30분간 자동 재호출에서 뺀다.
        //    예전에는 RR73 단계만 횟수를 2배로 기다려, '무응답 3회'로 맞춰 두어도 6~7번 더 부르는 일이 있었다.
        boolean qsoFinished = (newOrder == 5//消息中目标回复我RR73了
                || (functionOrder == 5 && newOrder == -1));// 判断通联成功：对方回73（5）||我是73（5），且对方没回（-1）
        boolean targetGaveUp = (functionOrder == 4
                && ((GeneralVariables.noReplyLimit > 0
                && GeneralVariables.noReplyCount >= GeneralVariables.noReplyLimit)//설정한 무응답 횟수에 이름
                || checkTargetCallMe(messages) > 1//상대가 다른 국을 부르기 시작함(>1)
                || (GeneralVariables.noReplyLimit == 0
                && GeneralVariables.noReplyCount > 20)));//한도 '무시'일 때의 안전 상한
        if (qsoFinished || targetGaveUp) {
            if (targetGaveUp && !qsoFinished && toCallsign != null
                    && !"CQ".equals(toCallsign.callsign)) {
                startNoReplyCooldown(toCallsign.callsign);//한국어판(0.93-kr7): 포기한 국은 30분간 자동 재호출 제외
            }
            //进入到CQ状态
            resetToCQ();

            //加入检查消息中有没有呼叫我的，或关注的呼号在CQ
            checkCQMeOrFollowCQMessage(messages);
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            mutableFunctionOrder.postValue(functionOrder);
            return;
        }


        if (newOrder != -1) {//说明收到消息，且未完成通联
            //原来是newOrder == 1，但有的时候，对方直接给信号报告，也就是消息2.
            if (newOrder == 1 || newOrder == 2) {//说明是别人第一次回复我
                //한국어판(0.93-kr5): generateFun() 이 무응답 횟수를 0으로 지우므로,
                //  제자리걸음 응답일 때는 지운 횟수를 되살린다
                int noReplyCountBackup = GeneralVariables.noReplyCount;
                resetTargetReport();//把信号报告复位一下
                generateFun();
                if (noProgress) {
                    GeneralVariables.noReplyCount = noReplyCountBackup;
                }
            }
            functionOrder = newOrder + 1;//执行下一个序号的消息
            mutableFunctions.postValue(functionList);
            mutableFunctionOrder.postValue(functionOrder);
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            return;
        }


        //到此位置，我还没有在6号消息状态，检查看有没有人呼叫我
        // 2022-09-22如果这时有人呼叫我，或自动跟踪状态，我就设置新的发射消息列表
        if (checkCQMeOrFollowCQMessage(messages)) {
            return;
        }


        //到此位置，说明没有收到回复的消息
        //之明如果我是在CQ，那么newOrder必然是-1
        if (functionOrder == 6) {//我处于CQ状态
            checkCQMeOrFollowCQMessage(messages);
            return;
        }


        //到此位置，说明没有回应，错误次数要加1,弱信号检测不记无回应
        //한국어판(0.93-kr7): 이 슬롯에 보통 신호(1차 디코드)가 하나라도 있었으면 무응답으로 센다.
        //  예전에는 목록의 첫 메시지 하나만 보고 약한 신호라고 건너뛰어, 무응답 횟수가 모자라
        //  설정한 횟수(예: 3회)보다 더 부르는 일이 있었다.
        boolean decodedStrongSignal = false;
        for (Ft8Message message : messages) {
            if (message != null && !message.isWeakSignal) {
                decodedStrongSignal = true;
                break;
            }
        }
        if (decodedStrongSignal) {
            GeneralVariables.noReplyCount++;
        }
        //如果超出无反应限定值，复位到CQ状态
        //한국어판(0.93-kr5): 무응답 한도 처리를 한 곳으로 모았다(제자리걸음 응답에서도 같은 규칙을 쓴다)
        handleNoReplyLimitReached(messages);

    }

    /**
     * 한국어판(0.93-kr5): 무응답 횟수가 설정한 '무응답 N회 중단'에 이르렀으면
     * 자동 호출을 멈추고 CQ(또는 새 목표)로 돌아간다.
     *
     * @param messages 이번에 디코드된 메시지 목록
     * @return true 이면 호출을 중단했다
     */
    private boolean handleNoReplyLimitReached(ArrayList<Ft8Message> messages) {
        if (!(GeneralVariables.noReplyLimit > 0
                && GeneralVariables.noReplyCount >= GeneralVariables.noReplyLimit)) {
            return false;
        }
        //한국어판(0.93-kr3): 무응답으로 호출이 중단된 국은 30분 동안 자동 재호출에서 뺀다.
        if (toCallsign != null) {
            startNoReplyCooldown(toCallsign.callsign);
        }
        //한국어판(0.93-kr7): 새 목표가 없으면 확실히 CQ(대기)로 돌아간다.
        //  resetToCQ() 로 신호 기록과 간헐 CQ 상태까지 함께 정리하고, 화면에도 알린다.
        if (!getNewTargetCallsign(messages)) {//检查关注列表中的CQ消息，如果有新的目标，返回TRUE;
            resetToCQ();
            setCurrentFunctionOrder(functionOrder);//设置当前消息
            mutableToCallsign.postValue(toCallsign);
            mutableFunctionOrder.postValue(functionOrder);
            return true;
        }
        generateFun();
        setCurrentFunctionOrder(functionOrder);//设置当前消息
        mutableToCallsign.postValue(toCallsign);
        mutableFunctionOrder.postValue(functionOrder);
        return true;
    }

    /**
     * 检查关注列表中，有没有正在CQ的消息，且不是我现在的目标呼号
     *
     * @param messages 关注的消息列表
     * @return 目标呼号，没有返回NULL
     */
    public boolean getNewTargetCallsign(ArrayList<Ft8Message> messages) {
        if (toCallsign == null) return false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Ft8Message ft8Message = messages.get(i);
            if (ft8Message.band != GeneralVariables.band) {//如果消息不在相同的波段内，不理会
                continue;
            }
            //不是CQ,不理会
            if (!ft8Message.checkIsCQ()) {
                continue;
            }
            //한국어판(0.93-kr3): 무응답으로 중단된 국은 30분 동안 새 목표로 삼지 않는다.
            if (isNoReplyCoolingDown(ft8Message.getCallsignFrom())) {
                continue;
            }
            //不是当前的目标呼号，且之前没有通联成功过
            if ((!ft8Message.getCallsignFrom().equals(toCallsign.callsign)
                    && (!GeneralVariables.checkQSLCallsign(ft8Message.getCallsignFrom())))) //之前没有联通成功过
            {
                //한국어판(0.93-kr7): 새 목표를 잡을 때는 예전 대상의 주파수·시퀀스가 남지 않도록
                //  그 CQ 메시지의 값으로 목표를 다시 만든다(예전에는 콜사인만 바뀌어 송신 위치가 엇갈렸다).
                setTransmit(new TransmitCallsign(ft8Message.i3, ft8Message.n3
                                , ft8Message.getCallsignFrom(), ft8Message.freq_hz
                                , ft8Message.getSequence(), ft8Message.snr)
                        , 1, ft8Message.extraInfo);
                return true;
            }


        }
        return false;
    }

    public boolean isSynFrequency() {
        return GeneralVariables.synFrequency;
    }


    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        if (!this.activated) {//强制关闭发射
            setTransmitting(false);
        }
        mutableIsActivated.postValue(activated);
    }

    public boolean isTransmitting() {
        return isTransmitting;
    }

    public void setTransmitting(boolean transmitting) {
        if (GeneralVariables.myCallsign.length() < 3 && transmitting) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.callsign_error));
            return;
        }

        if (!transmitting) {//停止发射
            if (audioTrack != null) {
                if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.pause();
                }
                if (onDoTransmitted != null) {//通知一下，已经不发射了
                    onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
                }
            }
        }

        mutableIsTransmitting.postValue(transmitting);
        isTransmitting = transmitting;
    }

    //=================================================================
    // 한국어판: 간헐 CQ (주기적 Auto CQ)
    //  10분·20분·30분 같은 주기마다 CQ를 2~3번만 내고, 나머지 시간은
    //  송신하지 않고 수신만 하여 무전기 발열과 배터리를 아끼는 기능입니다.
    //  - 누군가 내 호출부호를 부르면 곧바로 평소 자동 응답으로 넘어갑니다.
    //  - 교신이 끝나면 다시 간헐 CQ로 돌아가 한 묶음을 내고 대기합니다.
    //=================================================================

    /**
     * 지금 내가 CQ를 내는 상태인지 확인한다.
     *
     * @return CQ 상태이면 true
     */
    private boolean isInCQState() {
        return functionOrder == 6 && toCallsign != null
                && "CQ".equals(toCallsign.callsign);
    }

    /**
     * 간헐 CQ로 다음 주기를 정상 대기(수신만)하고 있는지 확인한다.
     * 이때는 송신 감시 시간을 세지 않아야 자동 송신이 꺼지지 않는다.
     *
     * @return 대기 중이면 true
     */
    private synchronized boolean isIntervalCQWaitingIdle() {
        return activated && intervalCQWaiting
                && GeneralVariables.intervalCQMinutes > 0 && isInCQState();
    }

    /**
     * 간헐 CQ 주기를 밀리초로 돌려준다.
     *
     * @return 주기(ms)
     */
    private long intervalCQPeriodMillis() {
        return Math.max(1, GeneralVariables.intervalCQMinutes) * 60L * 1000L;
    }

    /**
     * 간헐 CQ 묶음 예약을 처음 상태로 되돌린다.
     * 다음 송신 차례에 곧바로 CQ를 낸 뒤 주기를 세기 시작한다.
     */
    public synchronized void resetIntervalCQSchedule() {
        intervalCQBurstLeft = 0;
        intervalCQNextBurst = 0;
        intervalCQWaiting = false;
        intervalCQWatchdogWarned = false;
    }

    /**
     * 이번 송신 차례에 송신해도 되는지 판단한다.
     * - 간헐 CQ가 꺼져 있으면: 항상 송신
     * - CQ 상태가 아니면(상대를 부르거나 교신 중): 항상 송신
     * - CQ 상태이고 간헐 CQ가 켜져 있으면: 묶음 차례에만 송신
     *
     * @return 송신해도 되면 true, 대기하면 false
     */
    public synchronized boolean intervalCQAllowTransmit() {
        //기능이 꺼져 있으면 원래대로 연속 CQ
        if (GeneralVariables.intervalCQMinutes <= 0) {
            intervalCQBurstLeft = 0;
            intervalCQNextBurst = 0;
            intervalCQWaiting = false;
            return true;
        }
        //CQ 상태가 아니면(상대 호출·교신 중) 제한하지 않는다
        if (!isInCQState()) {
            intervalCQBurstLeft = 0;
            intervalCQNextBurst = 0;
            intervalCQWaiting = false;
            return true;
        }

        long now = UtcTimer.getSystemTime();

        if (intervalCQBurstLeft > 0) {//묶음 송신 중
            intervalCQBurstLeft--;
            intervalCQWaiting = (intervalCQBurstLeft == 0);
            return true;
        }

        if (intervalCQNextBurst == 0 || now >= intervalCQNextBurst) {
            //새 묶음 시작: 첫 번째 CQ를 지금 내고, 나머지는 내 차례마다 낸다
            int count = Math.max(1, GeneralVariables.intervalCQCount);
            intervalCQBurstLeft = count - 1;
            intervalCQNextBurst = now + intervalCQPeriodMillis();
            intervalCQWaiting = (intervalCQBurstLeft == 0);
            checkIntervalCQWatchdog();
            return true;
        }

        //다음 묶음까지는 수신만 한다
        intervalCQWaiting = true;
        return false;
    }

    /**
     * 송신 감시(Tx watchdog)가 간헐 CQ 주기보다 짧으면 한 번 알려 준다.
     * 감시 시간이 짧으면 주기를 다 채우지 못하고 자동 송신이 멈춘다.
     */
    private void checkIntervalCQWatchdog() {
        if (intervalCQWatchdogWarned) {
            return;
        }
        if (GeneralVariables.launchSupervision <= 0) {
            return;
        }
        if (GeneralVariables.launchSupervision >= intervalCQPeriodMillis()) {
            return;
        }
        intervalCQWatchdogWarned = true;
        ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                R.string.interval_cq_watchdog_warning),
                GeneralVariables.launchSupervision / 60000));
    }

    /**
     * 화면 표시줄에 쓸 간헐 CQ 상태 문자열을 돌려준다. 없으면 빈 문자열.
     *
     * @return 상태 문자열
     */
    public String getIntervalCQStatus() {
        return intervalCQStatus;
    }

    /**
     * 간헐 CQ 상태 문자열을 갱신한다(1초마다 호출).
     * 묶음 송신 중이면 "CQ 2/3", 대기 중이면 "다음 CQ 4:32"를 보여 준다.
     */
    @SuppressLint("DefaultLocale")
    private synchronized void updateIntervalCQStatus() {
        String text = "";
        if (activated && GeneralVariables.intervalCQMinutes > 0 && isInCQState()) {
            int total = Math.max(1, GeneralVariables.intervalCQCount);
            if (intervalCQBurstLeft > 0) {
                text = String.format(GeneralVariables.getStringFromResource(
                        R.string.interval_cq_status_burst)
                        , GeneralVariables.intervalCQMinutes
                        , total - intervalCQBurstLeft + 1
                        , total);
            } else {
                long remain = intervalCQNextBurst - UtcTimer.getSystemTime();
                if (remain < 0) {
                    remain = 0;
                }
                long sec = remain / 1000;
                text = String.format(GeneralVariables.getStringFromResource(
                        R.string.interval_cq_status_wait)
                        , GeneralVariables.intervalCQMinutes
                        , String.format(Locale.US, "%d:%02d", sec / 60, sec % 60));
            }
        }
        if (!text.equals(intervalCQStatus)) {
            intervalCQStatus = text;
            mutableIntervalCQStatus.postValue(text);
        }
    }

    //=================================================================
    // 한국어판(0.93-kr3): 무응답 콜사인 자동 재호출 방지(30분 쿨다운)
    //=================================================================

    /**
     * 콜사인을 쿨다운 맵의 키로 쓸 수 있게 정리한다(앞뒤 공백 제거·대문자).
     *
     * @param callsign 콜사인
     * @return 정리된 콜사인(없으면 빈 문자열)
     */
    private static String normalizeNoReplyCallsign(String callsign) {
        if (callsign == null) {
            return "";
        }
        return callsign.trim().toUpperCase(Locale.US);
    }

    /**
     * 무응답으로 호출이 중단된 콜사인을 30분 쿨다운에 넣는다.
     * 이 콜사인은 30분 동안 추적 자동 호출에서 건너뛴다.
     *
     * @param callsign 중단된 상대 콜사인
     */
    private void startNoReplyCooldown(String callsign) {
        String key = normalizeNoReplyCallsign(callsign);
        if (key.length() < 3 || "CQ".equals(key)) {
            return;
        }
        noReplyCooldown.put(key, UtcTimer.getSystemTime() + NO_REPLY_COOLDOWN_MS);
        Log.d(TAG, "startNoReplyCooldown: " + key);
    }

    /**
     * 해당 콜사인이 지금 무응답 쿨다운 중인지 확인한다.
     * 30분이 지난 기록은 이때 지운다.
     *
     * @param callsign 확인할 콜사인
     * @return 쿨다운 중이면 true
     */
    private boolean isNoReplyCoolingDown(String callsign) {
        String key = normalizeNoReplyCallsign(callsign);
        if (key.length() < 3) {
            return false;
        }
        Long until = noReplyCooldown.get(key);
        if (until == null) {
            return false;
        }
        if (UtcTimer.getSystemTime() >= until) {
            noReplyCooldown.remove(key, until);
            return false;
        }
        return true;
    }

    /**
     * 콜사인의 무응답 쿨다운을 지운다.
     * 화면에서 직접 불렀거나 상대가 먼저 나를 불러 응답할 때 사용한다.
     *
     * @param callsign 지울 콜사인
     */
    private void clearNoReplyCooldown(String callsign) {
        String key = normalizeNoReplyCallsign(callsign);
        if (key.length() < 3 || "CQ".equals(key)) {
            return;
        }
        if (noReplyCooldown.remove(key) != null) {
            Log.d(TAG, "clearNoReplyCooldown: " + key);
        }
    }

    /**
     * 30분이 지난 무응답 쿨다운 기록을 정리한다(1초마다 호출).
     */
    private void cleanupNoReplyCooldown() {
        if (noReplyCooldown.isEmpty()) {
            return;
        }
        long now = UtcTimer.getSystemTime();
        for (Map.Entry<String, Long> entry : noReplyCooldown.entrySet()) {
            if (now >= entry.getValue()) {
                noReplyCooldown.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 复位发射程序到6,时序也会改变
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void restTransmitting() {
        if (GeneralVariables.myCallsign.length() < 3) {
            return;
        }
        resetIntervalCQSchedule();//간헐 CQ: 다음 차례에 바로 CQ를 낸다
        //要判断我的呼号类型，才能确定i3n3 !!!
        int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
        setTransmit(new TransmitCallsign(i3, 0, "CQ", UtcTimer.getNowSequential())
                , 6, "");

    }

    /**
     * 한국어판(0.93-kr7): 현재 송신 단계를 읽는다. 6은 CQ를 내며 응답을 기다리는 상태다.
     * (딥 디코드에서 풀린 '나를 부른 국'을 자동으로 물고 들어갈지 판단할 때 쓴다)
     *
     * @return 현재 송신 단계(1~6)
     */
    public int getFunctionOrder() {
        return functionOrder;
    }

    /**
     * 把给对方的信号记录复位成-100；
     */
    public void resetTargetReport() {
        receiveTargetReport = -100;
        sentTargetReport = -100;
    }

    /**
     * 复位发射程序到6，不会改变时序
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    public void resetToCQ() {
        resetTargetReport();
        resetIntervalCQSchedule();//간헐 CQ: 교신이 끝난 뒤 CQ를 한 묶음 내고 다시 대기한다
        if (toCallsign == null) {
            //要判断我的呼号类型，才能确定i3n3 !!!
            int i3 = GenerateFT8.checkI3ByCallsign(GeneralVariables.myCallsign);
            setTransmit(new TransmitCallsign(i3, 0, "CQ", (UtcTimer.getNowSequential() + 1) % 2)
                    , 6, "");
        } else {
            functionOrder = 6;
            toCallsign.callsign = "CQ";
            mutableToCallsign.postValue(toCallsign);//设定呼叫的目标
            generateFun();
        }
    }

    /**
     * 设置发射时间延迟，这个延迟时间，也是给上一个周期解码的一个时间
     *
     * @param sec 毫秒
     */
    public void setTimer_sec(int sec) {
        utcTimer.setTime_sec(sec);
    }

    public boolean isTransmitFreeText() {
        return transmitFreeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public void setTransmitFreeText(boolean transmitFreeText) {
        this.transmitFreeText = transmitFreeText;
        if (transmitFreeText) {
            ToastMessage.show(GeneralVariables.getStringFromResource(R.string.trans_free_text_mode));
        } else {
            ToastMessage.show((GeneralVariables.getStringFromResource(R.string.trans_standard_messge_mode)));
        }
    }


    private static class DoTransmitRunnable implements Runnable {
        FT8TransmitSignal transmitSignal;

        public DoTransmitRunnable(FT8TransmitSignal transmitSignal) {
            this.transmitSignal = transmitSignal;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            //todo 此处可能要修改，维护一个列表。把每个呼号，网格，时间，波段，记录下来
            if (transmitSignal.functionOrder == 1 || transmitSignal.functionOrder == 2) {//当消息处于1或2时，说明开始了通联
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }
            if (transmitSignal.messageStartTime == 0) {//如果起始时间没有，就取现在的
                transmitSignal.messageStartTime = UtcTimer.getSystemTime();
            }

            //用于显示将要发射的消息内容
            Ft8Message msg;
            if (transmitSignal.transmitFreeText) {
                msg = new Ft8Message("CQ", GeneralVariables.myCallsign, transmitSignal.freeText);
                msg.i3 = 0;
                msg.n3 = 0;
            } else {
                msg = transmitSignal.getFunctionCommand(transmitSignal.functionOrder);
            }
            msg.modifier = GeneralVariables.toModifier;

            if (transmitSignal.onDoTransmitted != null) {
                //此处用于处理PTT等事件
                transmitSignal.onDoTransmitted.onBeforeTransmit(msg, transmitSignal.functionOrder);
            }

            transmitSignal.isTransmitting = true;
            transmitSignal.mutableIsTransmitting.postValue(true);


            transmitSignal.mutableTransmittingMessage.postValue(String.format(" (%.0fHz) %s"
                    , GeneralVariables.getBaseFrequency()
                    , msg.getMessageText()));
            //生成信号
//            float[] buffer=GenerateFT8.generateFt8(msg, GeneralVariables.getBaseFrequency());
//            if (buffer==null) {
//                return;
//            }

            //电台动作可能有要有个延迟时间，所以时间并不一定完全准确
            try {//给电台一个100毫秒的响应时间
                Thread.sleep(GeneralVariables.pttDelay);//给PTT指令后，电台一个响应时间，默认100毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

//            if (transmitSignal.onDoTransmitted != null) {//处理音频数据，可以给ICOM的网络模式发送
//                transmitSignal.onDoTransmitted.onAfterGenerate(buffer);
//            }
            //播放音频
            //transmitSignal.playFT8Signal(buffer);
            transmitSignal.playFT8Signal(msg);
        }
    }
}
