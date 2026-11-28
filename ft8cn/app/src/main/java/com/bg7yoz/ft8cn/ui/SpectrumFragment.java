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
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class SpectrumFragment extends Fragment {
    private static final String TAG = "SpectrumFragment";
    private FragmentSpectrumBinding binding;
    private MainViewModel mainViewModel;


    private int frequencyLineTimeOut = 0;//画频率线的时间量

    //한국어판(0.93-kr15): SWR 색·오토튠 감시 시간·TUNE·PWR 조작은 RigControlHelper 로 옮겨
    //  휴대폰 하단 상태 줄과 같은 코드를 쓴다.
    private static final long RF_POWER_READ_DELAY_MS = 2500;


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
     * 한국어판(0.93-kr14): 출력(Po)·ALC 표시와 송신 출력(PWR) 단추를 더했다.
     * - 송신 중에는 밝게, 송신이 끝난 뒤에는 마지막 송신의 값을 흐리게 남긴다.
     * - 색: 1.5 이하는 초록, 1.5~2.0은 호박, 2.0을 넘으면 빨강.
     */
    private void initSwrAndTune() {
        mainViewModel.ft8TransmitSignal.mutableIsTransmitting.observe(getViewLifecycleOwner()
                , new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        updateMeterView();
                    }
                });

        GeneralVariables.mutableSwr.observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                updateMeterView();
            }
        });

        //한국어판(0.93-kr14): 출력(Po)과 ALC 표시
        GeneralVariables.mutablePo.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                updateMeterView();
            }
        });
        GeneralVariables.mutableAlc.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                updateMeterView();
            }
        });
        //한국어판(0.93-kr14): PWR 단추에 보이는 무전기 출력
        GeneralVariables.mutableRfPower.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                updatePwrLabel();
            }
        });

        binding.tuneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doAutoTune();
            }
        });

        //한국어판(0.93-kr14): PWR 단추 → 출력 선택 창 (0.93-kr15: 하단 상태 줄과 같은 도우미를 쓴다)
        binding.pwrTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RigControlHelper.showRfPowerMenu(requireActivity(), view, mainViewModel.baseRig);
            }
        });

        updateMeterView();
        updatePwrLabel();

        //한국어판(0.93-kr14): 이미 연결되어 있으면 무전기에 걸린 출력을 한 번 읽어 PWR 단추에 보여 준다
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                BaseRig rig = mainViewModel.baseRig;
                if (rig != null && rig.isConnected()) {
                    rig.readRfPower();
                }
            }
        }, RF_POWER_READ_DELAY_MS);
    }

    /**
     * 한국어판(0.93-kr13/kr14): SWR·Po·ALC 미터 상자를 갱신한다.
     */
    private void updateMeterView() {
        float swr = GeneralVariables.mutableSwr.getValue() == null
                ? 0f : GeneralVariables.mutableSwr.getValue();
        Integer poValue = GeneralVariables.mutablePo.getValue();
        Integer alcValue = GeneralVariables.mutableAlc.getValue();
        int po = poValue == null ? -1 : poValue;
        int alc = alcValue == null ? -1 : alcValue;

        //한국어판(0.93-kr14): SWR·Po·ALC 중 하나라도 읽은 값이 있어야 상자를 보여 준다
        boolean hasAny = (swr > 0f) || (po >= 0) || (alc >= 0);
        binding.meterBox.setVisibility(hasAny ? View.VISIBLE : View.GONE);
        if (!hasAny) {
            return;
        }

        if (swr <= 0f) {
            binding.swrTextView.setVisibility(View.GONE);
        } else {
            binding.swrTextView.setVisibility(View.VISIBLE);
            binding.swrTextView.setText(RigControlHelper.formatSwr(swr));
            binding.swrTextView.setTextColor(RigControlHelper.swrColor(swr));
        }

        //둘째 줄: Po 95% · ALC 35%
        String meters = RigControlHelper.formatMeters(po, alc);
        if (meters.length() > 0) {
            binding.meterTextView.setVisibility(View.VISIBLE);
            binding.meterTextView.setText(meters);
        } else {
            binding.meterTextView.setVisibility(View.GONE);
        }

        //송신 중이거나 오토튠이 도는 중에만 또렷하게, 그 밖에는 직전 송신 값을 흐리게
        boolean live = mainViewModel.ft8TransmitSignal.isTransmitting()
                || RigControlHelper.isTuneWatching();
        binding.meterBox.setAlpha(live ? 1.0f : 0.55f);
    }

    /**
     * 한국어판(0.93-kr14): PWR 단추의 이름표를 무전기에 걸린 출력으로 바꾼다. (예: PWR / 100%)
     * 모르면 그냥 PWR 로 둔다. 좁은 화면에서도 들어가도록 출력은 둘째 줄에 작게 쓴다.
     */
    private void updatePwrLabel() {
        Integer value = GeneralVariables.mutableRfPower.getValue();
        if (value == null || value < 0) {
            binding.pwrTextView.setText("PWR");
        } else {
            binding.pwrTextView.setText(String.format(Locale.US, "PWR\n%d%%", value));
        }
    }

    /**
     * 한국어판(0.93-kr13): 무전기에 안테나 튜너 오토튠(TUNE) 명령을 보낸다.
     * 한국어판(0.93-kr15): 명령을 보내는 일은 하단 상태 줄과 같은 RigControlHelper 가 맡는다.
     */
    private void doAutoTune() {
        if (!RigControlHelper.doAutoTune(requireActivity(), mainViewModel.baseRig
                , mainViewModel.ft8TransmitSignal.isTransmitting())) {
            return;//이미 토스트로 사정을 알렸다
        }
        updateMeterView();
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMeterView();
            }
        }, RigControlHelper.TUNE_WATCH_MS + 500);
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