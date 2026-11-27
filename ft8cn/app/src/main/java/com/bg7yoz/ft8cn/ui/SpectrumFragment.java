package com.bg7yoz.ft8cn.ui;
/**
 * 频谱图的主界面。
 * @author BGY70Z
 * @date 2023-03-20
 */

import static android.view.MotionEvent.ACTION_UP;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentSpectrumBinding;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class SpectrumFragment extends Fragment {
    private static final String TAG = "SpectrumFragment";
    private FragmentSpectrumBinding binding;
    private MainViewModel mainViewModel;


    private int frequencyLineTimeOut = 0;//画频率线的时间量

    //한국어판(0.93-kr13): SWR 표시 색과 오토튠 감시 시간
    private static final int COLOR_SWR_GOOD = 0xFF00E676;//1.5 이하 : 초록
    private static final int COLOR_SWR_WARN = 0xFFFFC400;//1.5 ~ 2.0 : 호박
    private static final int COLOR_SWR_BAD = 0xFFFF5252;//2.0 초과 : 빨강
    private static final long TUNE_WATCH_MS = 8000;//튜너가 도는 동안 SWR을 밝게 보여 주는 시간
    private long tuneWatchUntil = 0;//오토튠을 시작한 시각 + 감시 시간


    static {
        System.loadLibrary("ft8cn");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainViewModel = MainViewModel.getInstance(this);
        binding = FragmentSpectrumBinding.inflate(inflater, container, false);
        //한국어판(0.93-kr5): 맨 위 세로 막대그래프(ColumnarView)를 없애 워터폴만 남겼다.
        binding.deNoiseSwitch.setChecked(mainViewModel.deNoise);//噪声抑制
        binding.waterfallView.setDrawMessage(false);
        setDeNoiseSwitchState();
        setMarkMessageSwitchState();
        initSwrAndTune();//한국어판(0.93-kr13): SWR 표시와 TUNE 버튼

        binding.rulerFrequencyView.setFreq(Math.round(GeneralVariables.getBaseFrequency()));
        mainViewModel.currentMessages=null;


        //原始频谱开关
        binding.deNoiseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.deNoise = b;
                setDeNoiseSwitchState();
                mainViewModel.currentMessages=null;
            }
        });
        //标记消息开关
        binding.showMessageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainViewModel.markMessage = b;
                setMarkMessageSwitchState();
            }
        });

        //当声音变化，画频谱
        mainViewModel.spectrumListener.mutableDataBuffer.observe(getViewLifecycleOwner(), new Observer<float[]>() {
            @Override
            public void onChanged(float[] floats) {
                drawSpectrum(floats);
            }
        });



        //观察解码的时长
        mainViewModel.ft8SignalListener.decodeTimeSec.observe(getViewLifecycleOwner(), new Observer<Long>() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onChanged(Long aLong) {
                binding.decodeDurationTextView.setText(String.format(
                        GeneralVariables.getStringFromResource(R.string.decoding_takes_milliseconds), aLong));
            }
        });
        //观察解码的变化
        mainViewModel.mutableIsDecoding.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                binding.waterfallView.setDrawMessage(!aBoolean);//false说明解码完毕
            }
        });


        //显示UTC时间
        mainViewModel.timerSec.observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                binding.timersTextView.setText(UtcTimer.getTimeStr(aLong));
                binding.freqBandTextView.setText(GeneralVariables.getBandString());
            }
        });


        //触摸频谱时的动作
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                frequencyLineTimeOut = 60;//显示频率线的时长：60*0.16

                binding.waterfallView.setTouch_x(Math.round(motionEvent.getX()));



                if (!mainViewModel.ft8TransmitSignal.isSynFrequency()
                        && (binding.waterfallView.getFreq_hz() > 0)
                        && (motionEvent.getAction() == ACTION_UP)
                ) {//如果时异频发射
                    mainViewModel.databaseOpr.writeConfig("freq",
                            String.valueOf(binding.waterfallView.getFreq_hz()),
                            null);
                    mainViewModel.ft8TransmitSignal.setBaseFrequency(
                            (float) binding.waterfallView.getFreq_hz());

                    binding.rulerFrequencyView.setFreq(binding.waterfallView.getFreq_hz());

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastMessage.show(String.format(
                                    GeneralVariables.getStringFromResource(R.string.sound_frequency_is_set_to)
                                    , binding.waterfallView.getFreq_hz()),true);
                        }
                    });
                }
                return false;
            }
        };

        binding.waterfallView.setOnTouchListener(touchListener);

        return binding.getRoot();
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
            binding.waterfallView.setTouch_x(-1);
        }
        if (mainViewModel.markMessage) {//是否标记消息
            binding.waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), mainViewModel.currentMessages);
        } else {
            binding.waterfallView.setWaveData(fft, UtcTimer.getNowSequential(), null);
        }
    }

    /**
     * 한국어판(0.93-kr13): 송신 중 실시간 SWR 표시와 안테나 튜너(TUNE) 버튼.
     * - 송신 중에는 밝게, 송신이 끝난 뒤에는 마지막 송신의 SWR을 흐리게 남긴다.
     * - 색: 1.5 이하는 초록, 1.5~2.0은 호박, 2.0을 넘으면 빨강.
     */
    private void initSwrAndTune() {
        mainViewModel.ft8TransmitSignal.mutableIsTransmitting.observe(getViewLifecycleOwner()
                , new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        updateSwrView();
                    }
                });

        GeneralVariables.mutableSwr.observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                updateSwrView();
            }
        });

        binding.tuneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doAutoTune();
            }
        });
    }

    private void updateSwrView() {
        float swr = GeneralVariables.mutableSwr.getValue() == null
                ? 0f : GeneralVariables.mutableSwr.getValue();
        if (swr <= 0f) {
            binding.swrTextView.setVisibility(View.GONE);
            return;
        }
        binding.swrTextView.setVisibility(View.VISIBLE);
        binding.swrTextView.setText("SWR " + BaseRigOperation.getSwrStr(swr));
        if (swr <= 1.5f) {
            binding.swrTextView.setTextColor(COLOR_SWR_GOOD);
        } else if (swr <= 2.0f) {
            binding.swrTextView.setTextColor(COLOR_SWR_WARN);
        } else {
            binding.swrTextView.setTextColor(COLOR_SWR_BAD);
        }
        //송신 중이거나 오토튠이 도는 중에만 또렷하게, 그 밖에는 직전 송신 값을 흐리게
        boolean live = mainViewModel.ft8TransmitSignal.isTransmitting()
                || System.currentTimeMillis() < tuneWatchUntil;
        binding.swrTextView.setAlpha(live ? 1.0f : 0.55f);
    }

    /**
     * 한국어판(0.93-kr13): 무전기에 안테나 튜너 오토튠(TUNE) 명령을 보낸다.
     */
    private void doAutoTune() {
        BaseRig rig = mainViewModel.baseRig;
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(getString(R.string.tune_connect_first), true);
            return;
        }
        if (mainViewModel.ft8TransmitSignal.isTransmitting()) {
            ToastMessage.show(getString(R.string.tune_while_transmitting), true);
            return;
        }
        if (!rig.startAutoTune()) {
            ToastMessage.show(getString(R.string.tune_not_supported), true);
            return;
        }
        tuneWatchUntil = System.currentTimeMillis() + TUNE_WATCH_MS;
        ToastMessage.show(getString(R.string.tune_started), true);
        updateSwrView();
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSwrView();
            }
        }, TUNE_WATCH_MS + 500);
    }

    private void setDeNoiseSwitchState() {
        if (mainViewModel.deNoise) {
            binding.deNoiseSwitch.setText(getString(R.string.de_noise));
        } else {
            binding.deNoiseSwitch.setText(getString(R.string.raw_spectrum_data));
        }
    }
    private void setMarkMessageSwitchState(){
        if (mainViewModel.markMessage) {
            binding.showMessageSwitch.setText(getString(R.string.markMessage));
        } else {
            binding.showMessageSwitch.setText(getString(R.string.unMarkMessage));
        }
    }

    public native void getFFTData(int[] data, int fftData[]);

    public native void getFFTDataFloat(float[] data ,int fftData[]);



    public native void getFFTDataRaw(int[] data, int fftData[]);
    public native void getFFTDataRawFloat(float[] data,int fftData[]);

}