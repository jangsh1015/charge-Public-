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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.PopupMenu;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentSpectrumBinding;
import com.bg7yoz.ft8cn.rigs.BaseRig;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
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

    //한국어판(0.93-kr13): SWR 표시 색과 오토튠 감시 시간
    private static final int COLOR_SWR_GOOD = 0xFF00E676;//1.5 이하 : 초록
    private static final int COLOR_SWR_WARN = 0xFFFFC400;//1.5 ~ 2.0 : 호박
    private static final int COLOR_SWR_BAD = 0xFFFF5252;//2.0 초과 : 빨강
    private static final long TUNE_WATCH_MS = 8000;//튜너가 도는 동안 SWR을 밝게 보여 주는 시간
    private long tuneWatchUntil = 0;//오토튠을 시작한 시각 + 감시 시간
    //한국어판(0.93-kr14): PWR 단추의 출력 선택지(%)와,연결 뒤 출력을 읽어 오는 시각
    private static final int[] RF_POWER_PRESETS = {10, 25, 50, 75, 100};
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

        //한국어판(0.93-kr14): PWR 단추 → 출력 선택 창
        binding.pwrTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRfPowerMenu(view);
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
            binding.swrTextView.setText("SWR " + BaseRigOperation.getSwrStr(swr));
            if (swr <= 1.5f) {
                binding.swrTextView.setTextColor(COLOR_SWR_GOOD);
            } else if (swr <= 2.0f) {
                binding.swrTextView.setTextColor(COLOR_SWR_WARN);
            } else {
                binding.swrTextView.setTextColor(COLOR_SWR_BAD);
            }
        }

        //둘째 줄: Po 95% · ALC 35%
        StringBuilder meters = new StringBuilder();
        if (po >= 0) {
            meters.append(String.format(Locale.US, "Po %d%%", po));
        }
        if (alc >= 0) {
            if (meters.length() > 0) {
                meters.append(" · ");
            }
            meters.append(String.format(Locale.US, "ALC %d%%", alc));
        }
        if (meters.length() > 0) {
            binding.meterTextView.setVisibility(View.VISIBLE);
            binding.meterTextView.setText(meters.toString());
        } else {
            binding.meterTextView.setVisibility(View.GONE);
        }

        //송신 중이거나 오토튠이 도는 중에만 또렷하게, 그 밖에는 직전 송신 값을 흐리게
        boolean live = mainViewModel.ft8TransmitSignal.isTransmitting()
                || System.currentTimeMillis() < tuneWatchUntil;
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
     * 한국어판(0.93-kr14): PWR 단추를 누르면 출력 선택 창(10·25·50·75·100%)을 연다.
     * 지금 무전기에 걸린 출력에 표시를 해 둔다.
     */
    private void showRfPowerMenu(View anchor) {
        BaseRig rig = mainViewModel.baseRig;
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(getString(R.string.tune_connect_first), true);
            return;
        }
        rig.readRfPower();//창을 열 때마다 지금 출력을 다시 확인한다

        Integer value = GeneralVariables.mutableRfPower.getValue();
        int current = value == null ? -1 : value;

        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        Menu menu = popupMenu.getMenu();
        menu.setGroupCheckable(0, true, true);
        for (int i = 0; i < RF_POWER_PRESETS.length; i++) {
            int percent = RF_POWER_PRESETS[i];
            MenuItem item = menu.add(0, percent, i, percent + "%");
            item.setCheckable(true);
            item.setChecked(current >= 0 && Math.abs(current - percent) <= 2);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                applyRfPower(item.getItemId());//메뉴 항목 id 가 곧 출력 %
                return true;
            }
        });
        popupMenu.show();
    }

    /**
     * 한국어판(0.93-kr14): 고른 출력을 무전기에 보낸다.
     */
    private void applyRfPower(int percent) {
        BaseRig rig = mainViewModel.baseRig;
        if (rig == null || !rig.isConnected()) {
            ToastMessage.show(getString(R.string.tune_connect_first), true);
            return;
        }
        if (!rig.setRfPowerPercent(percent)) {
            ToastMessage.show(getString(R.string.rf_power_not_supported), true);
            return;
        }
        ToastMessage.show(getString(R.string.rf_power_set, percent), true);
        updatePwrLabel();
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
        updateMeterView();
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMeterView();
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