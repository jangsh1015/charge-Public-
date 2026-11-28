package com.bg7yoz.ft8cn.ui;
/**
 * 包含瀑布图、频率柱状图、标尺的自定义控件。
 * @author BGY70Z
 * @date 2023-03-20
 */

import static android.view.MotionEvent.ACTION_UP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.timer.UtcTimer;

public class SpectrumView extends ConstraintLayout {
    private MainViewModel mainViewModel;
    private ColumnarView columnarView;
    private Switch controlDeNoiseSwitch;
    private Switch controlShowMessageSwitch;
    private WaterfallView waterfallView;
    private RulerFrequencyView rulerFrequencyView;
    private Fragment fragment;


    private int frequencyLineTimeOut = 0;//画频率线的时间量
    //한국어판(0.93-kr17): 물결 그림 위 주파수 커서를 미세 조정하기 위한 값
    private static final float TOUCH_DAMPING = 0.35f;
    private int smoothedTouchX = -1;

    static {
        System.loadLibrary("ft8cn");
    }



    public SpectrumView(@NonNull Context context) {
        super(context);
    }

    public SpectrumView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View view = (View) View.inflate(context, R.layout.spectrum_layout,this);
    }


    @SuppressLint("ClickableViewAccessibility")
    public void run(MainViewModel mainViewModel , Fragment fragment){
        this.mainViewModel = MainViewModel.getInstance(null);
        this.fragment=fragment;
        columnarView=findViewById(R.id.controlColumnarView);
        controlDeNoiseSwitch=findViewById(R.id.controlDeNoiseSwitch);
        waterfallView=findViewById(R.id.controlWaterfallView);
        rulerFrequencyView=findViewById(R.id.controlRulerFrequencyView);
        controlShowMessageSwitch=findViewById(R.id.controlShowMessageSwitch);


        setDeNoiseSwitchState();
        setMarkMessageSwitchState();

        rulerFrequencyView.setFreq(Math.round(GeneralVariables.getBaseFrequency()));
        mainViewModel.currentMessages=null;


        //原始频谱开关
        controlDeNoiseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.deNoise = b;
                setDeNoiseSwitchState();
                mainViewModel.currentMessages=null;
            }
        });
        //标记消息开关
        controlShowMessageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.markMessage = b;
                setMarkMessageSwitchState();
            }
        });

        //当声音变化，画频谱
        mainViewModel.spectrumListener.mutableDataBuffer.observe(fragment.getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] ints) {
                drawSpectrum(ints);
            }
        });


        //观察解码的变化
        mainViewModel.mutableIsDecoding.observe(fragment.getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                waterfallView.setDrawMessage(!aBoolean);//aBoolean==false说明解码完毕
            }
        });

        //触摸频谱时的动作
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                frequencyLineTimeOut = 60;//显示频率线的时长：60*0.16

                //한국어판(0.93-kr17): 손가락이 움직인 거리의 0.35배만 따라가게 해서
                //  주파수 커서가 휙휙 튀지 않고 정확한 Hz 로 맞춰지게 한다.
                //  처음 닿는 순간(ACTION_DOWN)만 손끝 자리로 곧장 옮긴다.
                int rawX = Math.round(motionEvent.getX());
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || smoothedTouchX < 0) {
                    smoothedTouchX = rawX;
                } else {
                    int step = Math.round((rawX - smoothedTouchX) * TOUCH_DAMPING);
                    if (step == 0 && rawX != smoothedTouchX) {//아주 조금 움직였을 때는 1px 씩
                        step = rawX > smoothedTouchX ? 1 : -1;
                    }
                    smoothedTouchX += step;
                }
                waterfallView.setTouch_x(smoothedTouchX);
                columnarView.setTouch_x(smoothedTouchX);
                //한국어판(0.93-kr17): 지금 커서가 가리키는 Hz 를 바로 계산해 둔다.
                //  화면을 그리는 onDraw 만 믿으면 손을 뗀 순간 한 칸 전 값이 남을 수 있고,
                //  처음 닿은 타이에는 아직 값이 없어 주파수가 저장되지도 않았다.
                final int freqHz = waterfallView.updateFreqHz();

                if (!mainViewModel.ft8TransmitSignal.isSynFrequency()
                        && (freqHz > 0)
                        && (motionEvent.getAction() == ACTION_UP)
                ) {//如果时异频发射
                    mainViewModel.databaseOpr.writeConfig("freq",
                            String.valueOf(freqHz),
                            null);
                    mainViewModel.ft8TransmitSignal.setBaseFrequency(
                            (float) freqHz);

                    rulerFrequencyView.setFreq(freqHz);

                    fragment.requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastMessage.show(String.format(
                                    GeneralVariables.getStringFromResource(R.string.sound_frequency_is_set_to)
                                    , freqHz),true);
                        }
                    });
                }
                return false;
            }
        };

        waterfallView.setOnTouchListener(touchListener);
        columnarView.setOnTouchListener(touchListener);


    }
    private void setDeNoiseSwitchState() {
        if (mainViewModel==null) return;
        controlDeNoiseSwitch.setChecked(mainViewModel.deNoise);
        if (mainViewModel.deNoise) {
            controlDeNoiseSwitch.setText(GeneralVariables.getStringFromResource(R.string.de_noise));
        } else {
            controlDeNoiseSwitch.setText(GeneralVariables.getStringFromResource(R.string.raw_spectrum_data));
        }
    }
    private void setMarkMessageSwitchState(){
        if (mainViewModel.markMessage) {
            controlShowMessageSwitch.setText(GeneralVariables.getStringFromResource(R.string.markMessage));
        } else {
            controlShowMessageSwitch.setText(GeneralVariables.getStringFromResource(R.string.unMarkMessage));
        }
    }




    public void drawSpectrum(float[] buffer) {
        if (buffer.length <= 0) {
            return;
        }
        int[] fft = new int[buffer.length / 2];
        if (mainViewModel.deNoise) {
            getFFTDataFloat(buffer, fft);
        } else {
            getFFTDataRawFloat(buffer, fft);
        }
        frequencyLineTimeOut--;
        if (frequencyLineTimeOut < 0) {
            frequencyLineTimeOut = 0;
        }
        //达到显示的时长，就取取消掉频率线
        if (frequencyLineTimeOut == 0) {
            waterfallView.setTouch_x(-1);
            columnarView.setTouch_x(-1);
        }
        columnarView.setWaveData(fft);
        if (mainViewModel.markMessage) {//是否标记消息
            waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), mainViewModel.currentMessages);
        } else {
            waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), null);
        }
    }


    public native void getFFTData(int[] data, int fftData[]);
    public native void getFFTDataFloat(float[] data, int fftData[]);

    public native void getFFTDataRaw(int[] data, int fftData[]);
    public native void getFFTDataRawFloat(float[] data, int fftData[]);


}
