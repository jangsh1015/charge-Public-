package com.bg7yoz.ft8cn.rigs;

import android.util.Log;

public class IcomRigConstant {
    private static final String TAG = "IcomRigConstant";
    //LSB:0,USB:1,AM:2,CW:3,RTTY:4,FM:5,WFM:6,CW_R:7,RTTY_R:8,DV:17
    public static final int LSB = 0;
    public static final int USB = 1;
    public static final int AM = 2;
    public static final int CW = 3;
    public static final int RTTY = 4;
    public static final int FM = 5;
    public static final int WFM = 6;
    public static final int CW_R = 7;
    public static final int RTTY_R = 8;
    public static final int DV = 0x17;
    public static final int UNKNOWN = -1;


    public static final int swr_alert_max=120;//相当于3.0
    public static final int alc_alert_max=120;//超过，在表上显示红色

    //协谷6100的ALC值的原始值（0～255）在127±50都是最佳线性度范围，转换到0～120的线性范围就是36.17到83.19
    public static final int xiegu_alc_alert_max=84;//超过，在表上显示红色
    public static final int xiegu_alc_alert_min=36;//超过，在表上显示红色



    //PTT状态
    public static final int PTT_ON = 1;
    public static final int PTT_OFF = 0;

    //指令集
    public static final byte CMD_RESULT_OK = (byte) 0xfb;//
    public static final byte CMD_RESULT_FAILED = (byte) 0xfa;//

    public static final byte[] SEND_FREQUENCY_DATA = {0x00};//发送频率数据
    public static final byte CMD_SEND_FREQUENCY_DATA = 0x00;//发送频率数据

    public static final byte[] SEND_MODE_DATA = {0x01};//发送模式数据
    public static final byte CMD_SEND_MODE_DATA = 0x01;//发送模式数据

    public static final byte[] READ_BAND_EDGE_DATA = {0x02};//读频率的波段边界
    public static final byte CMD_READ_BAND_EDGE_DATA = 0x02;//读频率的波段边界

    public static final byte[] READ_OPERATING_FREQUENCY = {0x03};//发送模式数据
    public static final byte CMD_READ_OPERATING_FREQUENCY = 0x03;//发送模式数据

    public static final byte[] READ_OPERATING_MODE = {0x04};//读取操作模式
    public static final byte CMD_READ_OPERATING_MODE = 0x04;//读取操作模式

    public static final byte[] SET_OPERATING_FREQUENCY = {0x05};//设置操作的频率
    public static final byte CMD_SET_OPERATING_FREQUENCY = 0x05;//设置操作的频率

    public static final byte[] SET_OPERATING_MODE = {0x06};//设置操作的模式
    public static final byte CMD_SET_OPERATING_MODE = 0x06;//设置操作的模式

    public static final byte CMD_READ_METER = 0x15;//读meter
    public static final byte CMD_READ_METER_PO = 0x11;//读meter子命令，输出功率表(한국어판 0.93-kr14)
    public static final byte CMD_READ_METER_SWR = 0x12;//读meter子命令，驻波表
    public static final byte CMD_READ_METER_ALC = 0x13;//读meter子命令，ALC表
    //한국어판(0.93-kr14): 송신 출력(RF POWER) 읽기/설정
    public static final byte CMD_RF_POWER = 0x14;//0x14 는 각종 레벨 설정 명령
    public static final byte CMD_RF_POWER_LEVEL = 0x0A;//子命令 0x0A = RF POWER (0000~0255)
    public static final byte CMD_CONNECTORS = 0x1A;//Connector设置，读取
    public static final byte CMD_CONNECTORS_DATA_MODE = 0x05;//Connector设置，读取
    public static final int CMD_CONNECTORS_DATA_WLAN_LEVEL = 0x050117;//Connector设置，读取





    public static final byte CMD_COMMENT_1A = 0x1A;//1A指令
    public static final byte[] SET_READ_PTT_STATE = {0x1A, 0x00, 0x48};//读取或设置PTT状态,不建议使用

    public static final byte[] READ_TRANSCEIVER_STATE = {0x1A, 0x00, 0x48};//读取电台发射状态
    public static final byte[] SET_TRANSCEIVER_STATE_ON = {0x1C, 0x00, 0x01};//设置电台处于发射状态TX
    public static final byte[] SET_TRANSCEIVER_STATE_OFF = {0x1C, 0x00, 0x00};//设置电台关闭发射状态RX
    //한국어판(0.93-kr13): 안테나 튜너(IC-705 + AH-705, IC-7300 등) 자동 조정(TUNE)
    public static final byte[] SET_TUNER_TUNE = {0x1C, 0x01, 0x02};//튜너 오토튠 시작
    public static final byte[] SET_TUNER_ON = {0x1C, 0x01, 0x01};//튜너 켜기(참고용)
    public static final byte[] READ_TRANSMIT_FREQUENCY = {0x1C, 0x03};//读取电台发射时的频率

    public static String getModeStr(int mode) {
        switch (mode) {
            case LSB:
                return "LSB";
            case USB:
                return "USB";
            case AM:
                return "AM";
            case CW:
                return "CW";
            case RTTY:
                return "RTTY";
            case FM:
                return "FM";
            case CW_R:
                return "CW_R";
            case RTTY_R:
                return "RTTY_R";
            case DV:
                return "DV";
            default:
                return "UNKNOWN";
        }
    }


    public static byte[] setPTTState(int ctrAddr, int rigAddr, int state) {
        //1C指令，例如PTT ON：FE FE A1 E0 1C 00 01 FD
        byte[] data = new byte[8];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) 0x1c;//主指令代码
        data[5] = (byte) 0x00;//子指令代码
        data[6] = (byte) state;//状态 01=tx 00=rx
        data[7] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr13): 안테나 튜너 오토튠(TUNE) 시작 명령을 만든다.
     * FE FE &lt;rigAddr&gt; &lt;ctrAddr&gt; 1C 01 02 FD (IC-705 + AH-705, IC-7300 등)
     *
     * @param ctrAddr 내 주소
     * @param rigAddr 무전기 주소
     * @return 명령 데이터 패킷
     */
    public static byte[] setTuneStart(int ctrAddr, int rigAddr) {
        byte[] data = new byte[8];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = SET_TUNER_TUNE[0];//主指令代码1C
        data[5] = SET_TUNER_TUNE[1];//子指令代码01(튜너)
        data[6] = SET_TUNER_TUNE[2];//状态 02=튜닝 시작
        data[7] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr14): 안테나 튜너를 켜는(TUNER ON) 명령을 만든다.
     * FE FE &lt;rigAddr&gt; &lt;ctrAddr&gt; 1C 01 01 FD
     * 튜너가 꺼져 있으면 1C 01 02(오토튠 시작)가 먹지 않으므로 먼저 이 명령을 보낸다.
     *
     * @param ctrAddr 내 주소
     * @param rigAddr 무전기 주소
     * @return 명령 데이터 패킷
     */
    public static byte[] setTunerOn(int ctrAddr, int rigAddr) {
        byte[] data = new byte[8];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = SET_TUNER_ON[0];//主指令代码1C
        data[5] = SET_TUNER_ON[1];//子指令代码01(튜너)
        data[6] = SET_TUNER_ON[2];//상태 01=튜너 켜기
        data[7] = (byte) 0xfd;
        return data;
    }

    /**
     * 读驻波表
     *
     * @param ctrAddr 我的地址
     * @param rigAddr 电台地址
     * @return 指令数据包
     */
    public static byte[] getSWRState(int ctrAddr, int rigAddr) {
        //1C指令，例如PTT ON：FE FE A1 E0 15 12 FD
        byte[] data = new byte[7];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_READ_METER;//主指令代码
        data[5] = (byte) CMD_READ_METER_SWR;//子指令代码SWR
        data[6] = (byte) 0xfd;
        return data;
    }

    /**
     * 读ALC表
     *
     * @param ctrAddr 我的地址
     * @param rigAddr 电台地址
     * @return 指令数据包
     */
    public static byte[] getALCState(int ctrAddr, int rigAddr) {
        //1C指令，例如PTT ON：FE FE A1 E0 15 12 FD
        byte[] data = new byte[7];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_READ_METER;//主指令代码
        data[5] = (byte) CMD_READ_METER_ALC;//子指令代码ALC
        data[6] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr14): 출력(Po) 미터를 읽는 명령을 만든다.
     * FE FE &lt;rigAddr&gt; &lt;ctrAddr&gt; 15 11 FD (응답: 15 11 &lt;BCD 상위&gt; &lt;BCD 하위&gt; FD, 0~255)
     *
     * @param ctrAddr 내 주소
     * @param rigAddr 무전기 주소
     * @return 명령 데이터 패킷
     */
    public static byte[] getPOState(int ctrAddr, int rigAddr) {
        byte[] data = new byte[7];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_READ_METER;
        data[5] = (byte) CMD_READ_METER_PO;
        data[6] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr14): 정수(0~255)를 ICOM CI-V 2바이트 BCD 로 바꾼다. (상위 바이트가 앞)
     * 이 클래스의 twoByteBcdToInt 와 짝을 이룬다.
     *
     * @param value 0~9999
     * @return {상위 BCD, 하위 BCD}
     */
    public static byte[] intToTwoByteBcd(int value) {
        if (value < 0) value = 0;
        if (value > 9999) value = 9999;
        byte high = (byte) (((value / 1000) % 10) << 4 | ((value / 100) % 10));
        byte low = (byte) (((value / 10) % 10) << 4 | (value % 10));
        return new byte[]{high, low};
    }

    /**
     * 한국어판(0.93-kr14): 송신 출력을 %로 맞추는 명령(14 0A)을 만든다.
     * FE FE &lt;rigAddr&gt; &lt;ctrAddr&gt; 14 0A &lt;BCD 상위&gt; &lt;BCD 하위&gt; FD
     * 값은 0~255(255 = 100%)이며, 예를 들어 100% 는 02 55, 50% 는 01 28, 10% 는 00 26 이다.
     *
     * @param ctrAddr 내 주소
     * @param rigAddr 무전기 주소
     * @param percent 0~100 %
     * @return 명령 데이터 패킷
     */
    public static byte[] setRfPower(int ctrAddr, int rigAddr, int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        int value = Math.round(percent * 255f / 100f);
        byte[] bcd = intToTwoByteBcd(value);
        byte[] data = new byte[9];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = CMD_RF_POWER;
        data[5] = CMD_RF_POWER_LEVEL;
        data[6] = bcd[0];
        data[7] = bcd[1];
        data[8] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr14): 무전기에 설정된 송신 출력을 읽는 명령(14 0A)을 만든다.
     * 데이터 없이 보내면 14 0A &lt;BCD 상위&gt; &lt;BCD 하위&gt; 로 응답한다.
     *
     * @param ctrAddr 내 주소
     * @param rigAddr 무전기 주소
     * @return 명령 데이터 패킷
     */
    public static byte[] getRfPower(int ctrAddr, int rigAddr) {
        byte[] data = new byte[7];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = CMD_RF_POWER;
        data[5] = CMD_RF_POWER_LEVEL;
        data[6] = (byte) 0xfd;
        return data;
    }

    /**
     * 한국어판(0.93-kr14): CI-V 14 0A 값(0~255)을 출력 % 로 바꾼다.
     */
    public static int rfPowerValueToPercent(int value) {
        if (value <= 0) return 0;
        if (value >= 255) return 100;
        return Math.round(value * 100f / 255f);
    }

    public static byte[] getConnectorWLanLevel(int ctrAddr, int rigAddr){
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 17 FD
        byte[] data = new byte[9];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_CONNECTORS;//主指令代码1A
        data[5] = (byte) CMD_CONNECTORS_DATA_MODE;//WLan level
        data[6] = (byte) 0x01;
        data[7] = (byte) 0x17;
        data[8] = (byte) 0xfd;
        return data;
    }

    public static byte[] setConnectorWLanLevel(int ctrAddr, int rigAddr,int level){
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 17 FD
        byte[] data = new byte[11];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_CONNECTORS;//主指令代码1A
        data[5] = (byte) CMD_CONNECTORS_DATA_MODE;//子指令代码ALC
        data[6] = (byte) 0x01;
        data[7] = (byte) 0x17;
        data[8] = (byte)  (level >> 8 & 0xff);
        data[9] = (byte) (level &0xff);
        data[10] = (byte) 0xfd;
        return data;
    }

    //设置数据通讯方式
    public static byte[] setConnectorDataMode(int ctrAddr, int rigAddr,byte mode){
        //1A指令，例如DATA MODE=WLAN：FE FE A1 E0 1A 05 01 19 FD
        byte[] data = new byte[10];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) CMD_CONNECTORS;//主指令代码1A
        data[5] = (byte) CMD_CONNECTORS_DATA_MODE;//子指令代码ALC
        data[6] = (byte) 0x01;//
        data[7] = (byte) 0x19;//
        data[8] = (byte) mode;//数据连接的方式
        data[9] = (byte) 0xfd;
        return data;
    }
    public static byte[] setOperationMode(int ctrAddr, int rigAddr, int mode) {
        //06指令，例如USB=01：FE FE A1 E0 06 01 FD
        byte[] data = new byte[8];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) 0x06;//指令代码
        data[5] = (byte) mode;//USB=01
        data[6] = (byte) 0x01;//fil1
        data[7] = (byte) 0xfd;
        return data;
    }

    public static byte[] setOperationDataMode(int ctrAddr, int rigAddr, int mode) {
        //26指令，例如USB-D=01：FE FE A1 E0 26 01 01 01 FD
        byte[] data = new byte[10];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;//70
        data[3] = (byte) ctrAddr;//E0
        data[4] = (byte) 0x26;//指令代码
        data[5] = (byte) 0x00;//指令代码
        data[6] = (byte) mode;//USB=01
        data[7] = (byte) 0x01;//data模式
        data[8] = (byte) 0x01;//fil1
        data[9] = (byte) 0xfd;
        return data;
    }

    public static byte[] setReadFreq(int ctrAddr, int rigAddr) {
        //06指令，例如USB=01：FE FE A1 E0 06 01 FD
        byte[] data = new byte[6];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) 0x03;//指令代码
        data[5] = (byte) 0xfd;
        return data;
    }


    public static byte[] setOperationFrequency(int ctrAddr, int rigAddr, long freq) {
        //05指令，例如14.074M：FE FE A4 E0 05 00 40 07 14 00 FD
        byte[] data = new byte[11];
        data[0] = (byte) 0xfe;
        data[1] = (byte) 0xfe;
        data[2] = (byte) rigAddr;
        data[3] = (byte) ctrAddr;
        data[4] = (byte) 0x05;//指令代码
        data[5] = (byte) (((byte) (freq % 100 / 10) << 4) + (byte) (freq % 10));
        data[6] = (byte) (((byte) (freq % 10000 / 1000) << 4) + (byte) (freq % 1000 / 100));
        data[7] = (byte) (((byte) (freq % 1000000 / 100000) << 4) + (byte) (freq % 100000 / 10000));
        data[8] = (byte) (((byte) (freq % 100000000 / 10000000) << 4) + (byte) (freq % 10000000 / 1000000));
        data[9] = (byte) (((byte) (freq / 1000000000) << 4) + (byte) (freq % 1000000000 / 100000000));
        data[10] = (byte) 0xfd;

        Log.d(TAG, "setOperationFrequency: " + BaseRig.byteToStr(data));
        return data;
    }

    public static int twoByteBcdToInt(byte[] data) {
        if (data.length < 2) return 0;
        return (int) (data[1] & 0x0f)//取个位
                + ((int) (data[1] >> 4) & 0xf) * 10//取十位
                + (int) (data[0] & 0x0f) * 100//百位
                + ((int) (data[0] >> 4) & 0xf) * 1000;//千位

    }
    public static int twoByteBcdToIntBigEnd(byte[] data) {
        if (data.length < 2) return 0;
        return (int) (data[0] & 0x0f)//取个位
                + ((int) (data[0] >> 4) & 0xf) * 10//取十位
                + (int) (data[1] & 0x0f) * 100//百位
                + ((int) (data[1] >> 4) & 0xf) * 1000;//千位

    }
}
