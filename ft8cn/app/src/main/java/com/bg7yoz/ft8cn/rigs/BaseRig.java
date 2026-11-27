package com.bg7yoz.ft8cn.rigs;

import androidx.lifecycle.MutableLiveData;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.connector.BaseRigConnector;

/**
 * 电台的抽象类。
 * @author BGY70Z
 * @date 2023-03-20
 */
public abstract class BaseRig {
    private long freq;//当前频率值
    public MutableLiveData<Long> mutableFrequency = new MutableLiveData<>();
    private int controlMode;//控制模式
    private OnRigStateChanged onRigStateChanged;//当电台的一些状态发生变化的回调
    private int civAddress;//CIV地址
    private int baudRate;//波特率
    private boolean isPttOn=false;//ptt是否打开
    private BaseRigConnector connector = null;//连接电台的对象

    public abstract boolean isConnected();//确认电台是否连接

    public abstract void setUsbModeToRig();//设置电台上边带方式

    public abstract void setFreqToRig();//设置电台频率

    public abstract void onReceiveData(byte[] data);//当电台发送回数据的动作

    public abstract void readFreqFromRig();//从电台读取频率

    public abstract String getName();//获取电台的名字

    /**
     * 한국어판(0.93-kr13): 안테나 튜너 오토튠(TUNE)을 시작한다.
     * 지원하지 않는 무전기는 false 를 돌려주고, 화면에서는 지원하지 않는다고 알린다.
     *
     * @return 명령을 보냈으면 true
     */
    public boolean startAutoTune() {
        return false;
    }

    /**
     * 한국어판(0.93-kr13): 송신 중에 측정한 SWR 값을 화면에 알린다. 0 이하면 '측정값 없음'으로 본다.
     *
     * @param swr 실제 SWR 값
     */
    protected void postSwr(float swr) {
        GeneralVariables.mutableSwr.postValue(swr > 0f ? swr : 0f);
    }

    /**
     * 한국어판(0.93-kr14): 송신 중에 읽은 출력(Po)·ALC 를 %로 화면에 알린다. 음수는 '측정값 없음'.
     *
     * @param percent 0~100 %, 음수면 표시하지 않는다
     */
    protected void postPo(int percent) {
        GeneralVariables.mutablePo.postValue(percent < 0 ? -1 : Math.min(percent, 100));
    }

    protected void postAlc(int percent) {
        GeneralVariables.mutableAlc.postValue(percent < 0 ? -1 : Math.min(percent, 100));
    }

    /**
     * 한국어판(0.93-kr14): 무전기에 설정된 송신 출력 % 를 화면에 알린다. 음수는 '모름'.
     *
     * @param percent 0~100 %, 음수면 모름
     */
    protected void postRfPower(int percent) {
        GeneralVariables.mutableRfPower.postValue(percent < 0 ? -1 : Math.min(percent, 100));
    }

    /**
     * 한국어판(0.93-kr14): 무전기의 송신 출력(RF POWER)을 %로 맞춘다.
     * 지원하지 않는 무전기는 false 를 돌려주고, 화면에서는 지원하지 않는다고 알린다.
     *
     * @param percent 0~100 %
     * @return 명령을 보냈으면 true
     */
    public boolean setRfPowerPercent(int percent) {
        return false;
    }

    /**
     * 한국어판(0.93-kr14): 무전기에 설정된 송신 출력을 읽어 온다. 읽으면 true.
     * 읽은 값은 postRfPower 로 화면에 알린다.
     */
    public boolean readRfPower() {
        return false;
    }

    private final OnConnectReceiveData onConnectReceiveData = new OnConnectReceiveData() {
        @Override
        public void onData(byte[] data) {
            onReceiveData(data);
        }
    };

    public void setPTT(boolean on) {//设置PTT打开或关闭
        isPttOn=on;
        if (onRigStateChanged != null) {
            onRigStateChanged.onPttChanged(on);
        }
    }

//    public void sendWaveData(float[] data) {
//        //留给ICOM电台使用
//    }
    public void sendWaveData(Ft8Message message) {
        //留给ICOM电台使用
    }

    public long getFreq() {
        return freq;
    }

    public void setFreq(long freq) {
        if (freq == this.freq) return;
        if (freq == 0) return;
        if (freq == -1) return;
        mutableFrequency.postValue(freq);
        this.freq = freq;
        if (onRigStateChanged != null) {
            onRigStateChanged.onFreqChanged(freq);
        }
    }

    public void setConnector(BaseRigConnector connector) {
        this.connector = connector;

        this.connector.setOnRigStateChanged(onRigStateChanged);
        this.connector.setOnConnectReceiveData(new OnConnectReceiveData() {
            @Override
            public void onData(byte[] data) {
                onReceiveData(data);
            }
        });
    }

    public void setControlMode(int mode) {
        controlMode = mode;
        if (connector != null) {
            connector.setControlMode(mode);
        }
    }

    public int getControlMode() {
        return controlMode;
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }

    public BaseRigConnector getConnector() {
        return connector;
    }

    public OnRigStateChanged getOnRigStateChanged() {
        return onRigStateChanged;
    }

    public void setOnRigStateChanged(OnRigStateChanged onRigStateChanged) {
        this.onRigStateChanged = onRigStateChanged;
    }

    public int getCivAddress() {
        return civAddress;
    }

    public void setCivAddress(int civAddress) {
        this.civAddress = civAddress;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public boolean isPttOn() {
        return isPttOn;
    }


    /**
     * 2023-08-16 由DS1UFX提交修改（基于0.9版），增加(tr)uSDX audio over cat的支持。
     */
    public boolean supportWaveOverCAT() {
        return false;
    }

    public void onDisconnecting() {
        postSwr(0f);//한국어판(0.93-kr13): 연결을 끊으면 SWR 표시를 지운다
        postPo(-1);//한국어판(0.93-kr14): Po·ALC·출력 표시도 지운다
        postAlc(-1);
        postRfPower(-1);
    }

}
