package com.bg7yoz.ft8cn;
/**
 * FT8CN程序的主Activity。本APP采用Fragment框架实现，每个Fragment实现不同的功能。
 * ----2022.5.6-----
 * 主要完成以下功能：
 * 1.生成MainViewModel实例。MainViewModel是用于整个生存周期，用于录音、解析等功能。
 * 2.录音、存储的权限申请。
 * 3.实现Fragment的导航管理。
 * 4.USB串口连接后的提示
 *
 * @author BG7YOZ
 * @date 2022.5.6
 */


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bg7yoz.ft8cn.bluetooth.BluetoothStateBroadcastReceive;
import com.bg7yoz.ft8cn.callsign.CallsignDatabase;
import com.bg7yoz.ft8cn.connector.CableSerialPort;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.database.OnAfterQueryConfig;
import com.bg7yoz.ft8cn.database.OperationBand;
import com.bg7yoz.ft8cn.database.RigNameList;
import com.bg7yoz.ft8cn.databinding.MainActivityBinding;
import com.bg7yoz.ft8cn.floatview.FloatView;
import com.bg7yoz.ft8cn.floatview.FloatViewButton;
import com.bg7yoz.ft8cn.grid_tracker.GridTrackerMainActivity;
import com.bg7yoz.ft8cn.log.ImportSharedLogs;
import com.bg7yoz.ft8cn.log.OnShareLogEvents;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.timer.UtcTimer;
import com.bg7yoz.ft8cn.ui.CallingListFragment;
import com.bg7yoz.ft8cn.ui.FreqDialog;
import com.bg7yoz.ft8cn.ui.LogFragment;
import com.bg7yoz.ft8cn.ui.MyCallingFragment;
import com.bg7yoz.ft8cn.ui.SetVolumeDialog;
import com.bg7yoz.ft8cn.ui.ShareLogsProgressDialog;
import com.bg7yoz.ft8cn.ui.SpectrumFragment;
import com.bg7yoz.ft8cn.ui.ToastMessage;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private BluetoothStateBroadcastReceive mReceive;
    private static final String TAG = "MainActivity";
    private MainViewModel mainViewModel;
    private NavController navController;
    private static boolean animatorRunned = false;
    //private boolean animationEnd = false;

    private MainActivityBinding binding;
    private FloatView floatView;

    private ShareLogsProgressDialog dialog = null;//生成共享log的对话框

    //한국어판(0.93-kr4): 태블릿 가로 3분할(왼쪽 스펙트럼 | 오른쪽 위 디코드·아래 호출) 패널
    private static final String PREF_TABLET_SPLIT = "tabletSplit";
    private static final String KEY_SPLIT_WIDTH_RATIO = "splitWidthRatio";
    private static final String KEY_SPLIT_HEIGHT_RATIO = "splitHeightRatio";
    private static final String KEY_SPLIT_PREFERRED = "splitPreferred";
    private static final String FRAGMENT_TAG_SPLIT_SPECTRUM = "splitSpectrum";
    private static final String FRAGMENT_TAG_SPLIT_DECODE = "splitDecode";
    private static final String FRAGMENT_TAG_SPLIT_CALLING = "splitCalling";
    private static final float DEFAULT_SPLIT_WIDTH_RATIO = 0.55f;
    private static final float DEFAULT_SPLIT_HEIGHT_RATIO = 0.50f;
    private static final float SPLIT_WIDTH_RATIO_MIN = 0.30f;
    private static final float SPLIT_WIDTH_RATIO_MAX = 0.75f;
    private static final float SPLIT_HEIGHT_RATIO_MIN = 0.20f;
    private static final float SPLIT_HEIGHT_RATIO_MAX = 0.80f;

    //한국어판(0.93-kr6): 태블릿 가로 호출 탭 2분할(왼쪽 호출 창 | 오른쪽 QSO 로그)
    private static final String KEY_CALL_LOG_RATIO = "callLogRatio";
    //한국어판(0.93-kr6): 마지막으로 보던 화면(디코드 단독 / 호출+로그 / 스펙트럼 3분할·단독)
    private static final String KEY_LAST_MODE = "lastMode";
    private static final String FRAGMENT_TAG_SPLIT_CALL_ONLY = "splitCallOnly";
    private static final String FRAGMENT_TAG_SPLIT_LOG = "splitLog";
    private static final float DEFAULT_CALL_LOG_RATIO = 0.50f;
    private static final float CALL_LOG_RATIO_MIN = 0.30f;
    private static final float CALL_LOG_RATIO_MAX = 0.70f;
    private static final String MODE_DECODE = "decode";
    private static final String MODE_MY_CALLING = "myCalling";
    private static final String MODE_SPECTRUM_SPLIT = "spectrumSplit";
    private static final String MODE_SPECTRUM_ONLY = "spectrumOnly";

    //한국어판(0.93-kr8): 스펙트럼 3분할의 오른쪽 아래 칸을 [호출·송신] ↔ [QSO 로그]로 바꾸는 탭
    private static final String KEY_SPLIT_CALL_TAB = "splitCallTab";
    private static final String FRAGMENT_TAG_SPLIT_CALLING_LOG = "splitCallingLog";

    //한국어판(0.93-kr4): IC-705 기준 초기 설정
    private static final String PREF_IC705_PRESET = "ic705Preset";
    private static final String KEY_IC705_PRESET_VERSION = "version";
    private static final int IC705_PRESET_VERSION = 4;
    private static final int DEFAULT_IC705_MODEL_INDEX = 2;
    private static final int DEFAULT_NO_REPLY_LIMIT = 3;

    private LinearLayout splitLayout;
    private LinearLayout splitRightColumn;
    private View splitSpectrumContainer;
    private View splitDecodeContainer;
    private View splitCallingContainer;
    private View splitVerticalHandle;
    private View splitHorizontalHandle;
    private View navHostContainer;
    //한국어판(0.93-kr6): 호출 탭 2분할(왼쪽 호출 창 | 오른쪽 QSO 로그)
    private LinearLayout splitCallLogLayout;
    private View splitCallingOnlyContainer;
    private View splitLogContainer;
    private View splitCallLogHandle;
    //한국어판(0.93-kr8): 3분할 오른쪽 아래 칸의 [호출·송신] ↔ [QSO 로그] 전환
    private View splitCallingToggleBar;
    private View splitCallingBody;
    private View splitCallingLogContainer;
    private TextView splitCallingTabCall;
    private TextView splitCallingTabLog;
    private boolean splitCallingLogShown = false;
    private float splitWidthRatio = DEFAULT_SPLIT_WIDTH_RATIO;
    private float splitHeightRatio = DEFAULT_SPLIT_HEIGHT_RATIO;
    private float callLogRatio = DEFAULT_CALL_LOG_RATIO;
    //한국어판(0.93-kr5): 3분할을 접고 스펙트럼만 전체로 보고 있는지
    private boolean splitSpectrumOnly = false;
    private static boolean tabletSplitActive = false;
    //한국어판(0.93-kr5): 지금 3분할 패널이 화면에 떠 있는지(단독 화면으로 볼 때는 false)
    private static boolean splitPanelVisible = false;
    //한국어판(0.93-kr6): 지금 호출+로그 2분할이 화면에 떠 있는지
    private static boolean splitCallLogVisible = false;


    String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO
            , Manifest.permission.ACCESS_COARSE_LOCATION
            , Manifest.permission.ACCESS_WIFI_STATE
            , Manifest.permission.BLUETOOTH
            , Manifest.permission.BLUETOOTH_ADMIN
            , Manifest.permission.MODIFY_AUDIO_SETTINGS
            , Manifest.permission.WAKE_LOCK
            , Manifest.permission.ACCESS_FINE_LOCATION};
    List<String> mPermissionList = new ArrayList<>();

    private static final int PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.RECORD_AUDIO
                    , Manifest.permission.ACCESS_COARSE_LOCATION
                    , Manifest.permission.ACCESS_WIFI_STATE
                    , Manifest.permission.BLUETOOTH
                    , Manifest.permission.BLUETOOTH_ADMIN
                    , Manifest.permission.BLUETOOTH_CONNECT
                    , Manifest.permission.MODIFY_AUDIO_SETTINGS
                    , Manifest.permission.WAKE_LOCK
                    , Manifest.permission.ACCESS_FINE_LOCATION};
        }

        checkPermission();
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //禁止休眠
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                , WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        GeneralVariables.getInstance().setMainContext(getApplicationContext());

        //判断是不是简体中文
        GeneralVariables.isTraditionalChinese =
                getResources().getConfiguration().locale.getDisplayCountry().equals("中國");

        //确定是不是中国、香港、澳门、台湾
        GeneralVariables.isChina = (getResources().getConfiguration().locale
                .getLanguage().toUpperCase().startsWith("ZH"));

        mainViewModel = MainViewModel.getInstance(this);
        binding = MainActivityBinding.inflate(getLayoutInflater());
        binding.initDataLayout.setVisibility(View.VISIBLE);//显示LOG页面
        setContentView(binding.getRoot());


        ToastMessage.getInstance();
        registerBluetoothReceiver();//注册蓝牙动作改变的广播
        if (mainViewModel.isBTConnected()) {
            mainViewModel.setBlueToothOn();
        }


        //观察DEBUG信息
        GeneralVariables.mutableDebugMessage.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (s.length() > 1) {
                    binding.debugLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.debugLayout.setVisibility(View.GONE);
                }
                binding.debugMessageTextView.setText(s);
            }
        });
        binding.debugLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.debugLayout.setVisibility(View.GONE);
            }
        });


        mainViewModel.mutableIsRecording.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    binding.utcProgressBar.setVisibility(View.VISIBLE);
                } else {
                    binding.utcProgressBar.setVisibility(View.GONE);
                }
            }
        });
        //观察时钟的变化，显示进度条
        mainViewModel.timerSec.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                if (mainViewModel.ft8TransmitSignal.sequential == UtcTimer.getNowSequential()
                        && mainViewModel.ft8TransmitSignal.isActivated()) {
                    binding.utcProgressBar.setBackgroundColor(getColor(R.color.calling_list_isMyCall_color));
                } else {
                    binding.utcProgressBar.setBackgroundColor(getColor(R.color.progresss_bar_back_color));
                }
                binding.utcProgressBar.setProgress((int) ((aLong / 1000) % 15));
            }
        });

        //添加点击发射消息提示窗口点击关闭动作
        binding.transmittingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.transmittingLayout.setVisibility(View.GONE);
            }
        });
        //清空缓存中的文件
        //deleteFolderFile(this.getCacheDir().getPath());

        //Log.e(TAG, this.getCacheDir().getPath());

        //用于Fragment的导航。
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.navView, navController);
        //此处增加回调是因为当APP主动navigation后，无法回到解码的界面
        binding.navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //Log.e(TAG, "onNavigationItemSelected: "+item.toString() );
                int itemId = item.getItemId();
                //한국어판(0.93-kr5): 태블릿 가로에서도 디코드 탭은 그 화면만 전체로 연다.
                //  스펙트럼 탭이 3분할을 열고, 3분할 상태에서 스펙트럼 탭을 한 번 더 누르면 접힌다.
                //한국어판(0.93-kr6): 호출 탭은 왼쪽 호출 창·오른쪽 QSO 로그 2분할로 연다.
                if (splitLayout != null && isSplitPanelTab(itemId)) {
                    NavDestination currentDestination = navController.getCurrentDestination();
                    if (itemId == R.id.menu_nav_spectrum && currentDestination != null
                            && currentDestination.getId() == R.id.menu_nav_spectrum) {
                        splitSpectrumOnly = !splitSpectrumOnly;//3분할 ↕ 스펙트럼 단독
                        updateSplitVisibility(itemId);
                        return true;
                    }
                    splitSpectrumOnly = false;
                    navigateToBottomTab(itemId);
                    updateSplitVisibility(itemId);
                    return true;
                }
                splitSpectrumOnly = false;
                navigateToBottomTab(itemId);
                //binding.navView.setLabelFor(item.getItemId());
                return true;
            }
        });

        //한국어판(0.93-kr4): 태블릿 가로 3분할 패널을 준비하고, 화면(목적지)에 따라 표시를 바꾼다.
        initTabletSplitLayout();
        //한국어판(0.93-kr5): 지난번에 쓰던 화면(3분할/단독)을 먼저 읽어 둔다.
        //  아래 목적지 리스너가 첫 콜백에서 이 값을 다시 쓰기 때문이다.
        //한국어판(0.93-kr6): 3분할·디코드 단독에 호출+로그 2분할까지 더해 '마지막 화면'으로 기억한다.
        final boolean splitPreferredOnLaunch = getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE)
                .getBoolean(KEY_SPLIT_PREFERRED, true);
        final String lastModeOnLaunch = getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE)
                .getString(KEY_LAST_MODE, null);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller
                    , @NonNull NavDestination destination, @Nullable Bundle arguments) {
                updateSplitVisibility(destination.getId());
            }
        });

        //한국어판(0.93-kr5): 지난번에 보던 화면으로 시작한다. (기록이 없으면 3분할)
        if (savedInstanceState == null && (splitLayout != null || splitCallLogLayout != null)) {
            String startMode = lastModeOnLaunch;
            if (startMode == null) {
                startMode = splitPreferredOnLaunch ? MODE_SPECTRUM_SPLIT : MODE_DECODE;
            }
            restoreStartMode(startMode);
        }

        //FT8CN Ver %s\nBG7YOZ\n%s
        binding.welcomTextView.setText(String.format(getString(R.string.version_info)
                , GeneralVariables.VERSION, GeneralVariables.BUILD_DATE));

        floatView = new FloatView(this, 32);
        if (!animatorRunned) {
            animationImage();
            animatorRunned = true;
        } else {
            binding.initDataLayout.setVisibility(View.GONE);

            InitFloatView();
        }
        //初始化数据
        InitData();


        //观察是不是flex radio
        mainViewModel.mutableIsFlexRadio.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    //添加flex配置按钮
                    floatView.addButton(R.id.flex_radio, "flex_radio", R.drawable.flex_icon
                            , new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    navController.navigate(R.id.flexRadioInfoFragment);
                                }
                            });
                } else {//删除flex配置按钮
                    floatView.deleteButtonByName("flex_radio");
                }
            }
        });

        //观察是不是xiegu radio
        mainViewModel.mutableIsXieguRadio.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    //添加xiegu配置按钮
                    floatView.addButton(R.id.xiegu_radio, "xiegu_radio", R.drawable.xiegulogo32
                            , new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    navController.navigate(R.id.xieguInfoFragment);
                                }
                            });
                } else {//删除xiegu配置按钮
                    floatView.deleteButtonByName("xiegu_radio");
                }
            }
        });

        //关闭串口设备列表按钮
        binding.closeSelectSerialPortImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.selectSerialPortLayout.setVisibility(View.GONE);
            }
        });

        //观察串口设备列表的变化
        mainViewModel.mutableSerialPorts.observe(this, new Observer<ArrayList<CableSerialPort.SerialPort>>() {
            @Override
            public void onChanged(ArrayList<CableSerialPort.SerialPort> serialPorts) {
                setSelectUsbDevice();
            }
        });

        //列USB设备列表
        mainViewModel.getUsbDevice();


        //设置发射消息框的动画
        binding.transmittingMessageTextView.setAnimation(AnimationUtils.loadAnimation(this
                , R.anim.view_blink));
        //观察发射的状态
        mainViewModel.ft8TransmitSignal.mutableIsTransmitting.observe(this,
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        if (aBoolean) {
                            binding.transmittingLayout.setVisibility(View.VISIBLE);
                        } else {
                            binding.transmittingLayout.setVisibility(View.GONE);
                        }
                    }
                });

        //观察发射内容的变化
        mainViewModel.ft8TransmitSignal.mutableTransmittingMessage.observe(this,
                new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        binding.transmittingMessageTextView.setText(s);
                    }
                });

        //判断导入共享log文件的工作线程还在，如果在，就显示对话框
        if (Boolean.TRUE.equals(mainViewModel.mutableImportShareRunning.getValue())) {
            showShareDialog();
        }else {
            //读取共享的文件
            doReceiveShareFile(getIntent());
        }

    }


    /**
     * 接收共享文件
     * @param intent intent
     */
    private void doReceiveShareFile(Intent intent) {
        Uri uri = (Uri) intent.getData();

        if (uri != null) {
            ImportSharedLogs importSharedLogs = null;
            //先显示导入log的对话框
            showShareDialog();
            try {

                importSharedLogs = new ImportSharedLogs(mainViewModel);
                Log.e(TAG,"开始导入。。。");
                mainViewModel.mutableImportShareRunning.setValue(true);
                importSharedLogs.doImport(getBaseContext().getContentResolver().openInputStream(uri)
                        ,new OnShareLogEvents() {
                    @Override
                    public void onPreparing(String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                    }

                    @Override
                    public void onShareStart(int count, String info) {
                        mainViewModel.mutableSharePosition.postValue(0);
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableImportShareRunning.postValue(true);
                        mainViewModel.mutableShareCount.postValue(count);
                    }

                    @Override
                    public boolean onShareProgress(int count, int position, String info) {
                        mainViewModel.mutableSharePosition.postValue(position);
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableShareCount.postValue(count);
                        return Boolean.TRUE.equals(mainViewModel.mutableImportShareRunning.getValue());
                    }

                    @Override
                    public void afterGet(int count, String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableImportShareRunning.postValue(false);
                    }

                    @Override
                    public void onShareFailed(String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                    }
                });
            } catch (IOException e) {
                mainViewModel.mutableImportShareRunning.postValue(false);
                Log.e(TAG,String.format("错误：%s",e.getMessage()));
                ToastMessage.show(e.getMessage());
            }
        } else {
            Log.e(TAG, "读文件类型时，文件没有找到。");
        }
    }


    //----------------------------------- 한국어판(0.93-kr4): 태블릿 가로 3분할 패널

    /**
     * 태블릿 가로(작은 화면 폭 600dp 이상) 구성인지 확인한다.
     * res/layout-sw600dp-land 와 같은 기준이다.
     */
    private boolean isTabletSplitConfiguration() {
        Configuration configuration = getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                && configuration.smallestScreenWidthDp >= 600;
    }

    /**
     * 3분할 패널로 볼 수 있는 탭(디코드·호출·스펙트럼)인지 확인한다.
     */
    private boolean isSplitPanelTab(int itemId) {
        return itemId == R.id.menu_nav_calling_list
                || itemId == R.id.menu_nav_mycalling
                || itemId == R.id.menu_nav_spectrum;
    }

    /**
     * 3분할 패널을 쓰고 있는지 돌려준다. (패널 안의 프래그먼트가 화면 이동 여부를 판단할 때 쓴다)
     */
    public static boolean isTabletSplitActive() {
        return tabletSplitActive;
    }

    /**
     * 한국어판(0.93-kr5): 3분할 패널이 지금 화면에 떠 있는지 돌려준다.
     * 디코드·호출 화면을 단독으로 볼 때는 false 이므로, 그때는 평소처럼 화면을 옮긴다.
     *
     * @return 3분할 패널이 보이면 true
     */
    public static boolean isSplitPanelVisible() {
        return splitPanelVisible;
    }

    /**
     * 한국어판(0.93-kr6): 호출 탭 2분할(왼쪽 호출 창 | 오른쪽 QSO 로그)이 떠 있는지 돌려준다.
     *
     * @return 호출+로그 2분할이 보이면 true
     */
    public static boolean isCallLogPanelVisible() {
        return splitCallLogVisible;
    }

    /**
     * 한국어판(0.93-kr6): 이 프래그먼트가 분할 패널(3분할·2분할) 안에 붙어 있는지 확인한다.
     * 패널 안의 목록은 한 칸이 좁으므로 디코드 탭의 2열 보기를 쓰지 않는다.
     *
     * @param fragmentTag 프래그먼트 태그
     * @return 분할 패널 안의 프래그먼트이면 true
     */
    public static boolean isSplitPanelFragment(@Nullable String fragmentTag) {
        return FRAGMENT_TAG_SPLIT_DECODE.equals(fragmentTag)
                || FRAGMENT_TAG_SPLIT_CALLING.equals(fragmentTag)
                || FRAGMENT_TAG_SPLIT_SPECTRUM.equals(fragmentTag)
                || FRAGMENT_TAG_SPLIT_CALL_ONLY.equals(fragmentTag)
                || FRAGMENT_TAG_SPLIT_CALLING_LOG.equals(fragmentTag)
                || FRAGMENT_TAG_SPLIT_LOG.equals(fragmentTag);
    }

    /**
     * 태블릿 가로 3분할 패널을 준비한다.
     * 3분할을 쓰지 않는 구성(스마트폰 등)에서는 이전 구성에서 넘어온 패널 프래그먼트만 정리한다.
     */
    private void initTabletSplitLayout() {
        navHostContainer = findViewById(R.id.fragmentContainerView);
        View split = findViewById(R.id.splitLayout);
        if (!isTabletSplitConfiguration() || !(split instanceof LinearLayout)) {
            tabletSplitActive = false;
            splitPanelVisible = false;
            splitCallLogVisible = false;
            splitCallLogLayout = null;
            removeSplitFragments();
            return;
        }
        splitLayout = (LinearLayout) split;
        splitRightColumn = findViewById(R.id.splitRightColumn);
        splitSpectrumContainer = findViewById(R.id.splitSpectrumContainer);
        splitDecodeContainer = findViewById(R.id.splitDecodeContainer);
        splitCallingContainer = findViewById(R.id.splitCallingContainer);
        splitVerticalHandle = findViewById(R.id.splitVerticalHandle);
        splitHorizontalHandle = findViewById(R.id.splitHorizontalHandle);
        if (splitRightColumn == null || splitSpectrumContainer == null
                || splitDecodeContainer == null || splitCallingContainer == null
                || splitVerticalHandle == null || splitHorizontalHandle == null) {
            splitLayout = null;
            tabletSplitActive = false;
            splitPanelVisible = false;
            removeSplitFragments();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment spectrumFragment = fragmentManager.findFragmentById(R.id.splitSpectrumContainer);
        Fragment decodeFragment = fragmentManager.findFragmentById(R.id.splitDecodeContainer);
        Fragment callingFragment = fragmentManager.findFragmentById(R.id.splitCallingContainer);
        if (spectrumFragment == null || decodeFragment == null || callingFragment == null) {
            //일부만 남아 있는 경우를 대비해 남은 패널은 지우고 세 패널을 새로 붙인다
            try {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                if (spectrumFragment != null) {
                    transaction.remove(spectrumFragment);
                }
                if (decodeFragment != null) {
                    transaction.remove(decodeFragment);
                }
                if (callingFragment != null) {
                    transaction.remove(callingFragment);
                }
                transaction.add(R.id.splitSpectrumContainer, new SpectrumFragment()
                        , FRAGMENT_TAG_SPLIT_SPECTRUM);
                transaction.add(R.id.splitDecodeContainer, new CallingListFragment()
                        , FRAGMENT_TAG_SPLIT_DECODE);
                transaction.add(R.id.splitCallingContainer, new MyCallingFragment()
                        , FRAGMENT_TAG_SPLIT_CALLING);
                transaction.commitNowAllowingStateLoss();
            } catch (Exception e) {
                //패널을 붙이지 못하면 예전처럼 전체 화면으로 동작하게 둔다
                Log.e(TAG, "initTabletSplitLayout: 3분할 패널을 붙이지 못했습니다. " + e.getMessage());
                splitLayout = null;
                tabletSplitActive = false;
                splitPanelVisible = false;
                removeSplitFragments();
                return;
            }
        }

        SharedPreferences splitPreference = getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE);
        splitWidthRatio = clampRatio(splitPreference.getFloat(KEY_SPLIT_WIDTH_RATIO
                , DEFAULT_SPLIT_WIDTH_RATIO), SPLIT_WIDTH_RATIO_MIN, SPLIT_WIDTH_RATIO_MAX);
        splitHeightRatio = clampRatio(splitPreference.getFloat(KEY_SPLIT_HEIGHT_RATIO
                , DEFAULT_SPLIT_HEIGHT_RATIO), SPLIT_HEIGHT_RATIO_MIN, SPLIT_HEIGHT_RATIO_MAX);
        applySplitRatio();

        splitVerticalHandle.setOnTouchListener(new SplitHandleTouchListener(true));
        splitHorizontalHandle.setOnTouchListener(new SplitHandleTouchListener(false));

        //한국어판(0.93-kr5): 경계선을 끌어 패널 폭이 바뀌면 도구 버튼도 왼쪽 패널 구석으로 따라간다
        splitSpectrumContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom
                    , int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                    updateFloatViewPlacement();
                }
            }
        });

        tabletSplitActive = true;
        //표시 여부는 현재 목적지에 따라 updateSplitVisibility 가 정한다.
        splitLayout.setVisibility(View.GONE);
        //한국어판(0.93-kr6): 호출 탭 2분할(호출 창 | QSO 로그) 패널도 함께 준비한다.
        initTabletCallLogLayout();
        //한국어판(0.93-kr8): 3분할 오른쪽 아래 칸의 [호출·송신] ↔ [QSO 로그] 전환 탭도 준비한다.
        initSplitCallingLogLayout();
    }

    /**
     * 한국어판(0.93-kr8): 태블릿 가로 3분할의 오른쪽 아래 칸을 [호출·송신] ↔ [QSO 로그]로 바꾼다.
     * 두 프래그먼트(MyCallingFragment·LogFragment)를 같은 칸에 겹쳐 두고 보이는 하나만 켜므로,
     * QSO 로그를 보는 동안에도 송신 상태와 호출 대상이 끊기지 않는다.
     */
    private void initSplitCallingLogLayout() {
        splitCallingToggleBar = findViewById(R.id.splitCallingToggleBar);
        splitCallingBody = findViewById(R.id.splitCallingBody);
        splitCallingLogContainer = findViewById(R.id.splitCallingLogContainer);
        splitCallingTabCall = findViewById(R.id.splitCallingTabCall);
        splitCallingTabLog = findViewById(R.id.splitCallingTabLog);
        if (splitCallingContainer == null || splitCallingBody == null
                || splitCallingLogContainer == null
                || splitCallingTabCall == null || splitCallingTabLog == null) {
            //전환 탭이 없는 레이아웃에서는 예전처럼 호출 창만 쓴다
            splitCallingBody = null;
            splitCallingLogContainer = null;
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment logFragment = fragmentManager.findFragmentById(R.id.splitCallingLogContainer);
        if (logFragment == null) {
            try {
                fragmentManager.beginTransaction()
                        .add(R.id.splitCallingLogContainer, new LogFragment()
                                , FRAGMENT_TAG_SPLIT_CALLING_LOG)
                        .commitNowAllowingStateLoss();
            } catch (Exception e) {
                //로그 패널을 붙이지 못하면 전환 탭 없이 호출 창만 쓴다
                Log.e(TAG, "initSplitCallingLogLayout: QSO 로그 패널을 붙이지 못했습니다. " + e.getMessage());
                splitCallingBody = null;
                splitCallingLogContainer = null;
                return;
            }
        }

        splitCallingLogShown = getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE)
                .getBoolean(KEY_SPLIT_CALL_TAB, false);
        splitCallingTabCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSplitCallingTab(false);
            }
        });
        splitCallingTabLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSplitCallingTab(true);
            }
        });
        applySplitCallingTab();
    }

    /**
     * 한국어판(0.93-kr8): 3분할 오른쪽 아래 칸에 QSO 로그를 보여 줄지(아니면 호출 창) 고른다.
     *
     * @param showLog QSO 로그를 보여 주려면 true
     */
    private void showSplitCallingTab(boolean showLog) {
        if (splitCallingContainer == null || splitCallingLogContainer == null) {
            return;
        }
        splitCallingLogShown = showLog;
        applySplitCallingTab();
        getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE).edit()
                .putBoolean(KEY_SPLIT_CALL_TAB, showLog)
                .apply();
        if (showLog) {
            //새 교신이 저장되는 대로 목록을 다시 읽으므로, 열자마자 지금 기록을 한 번 맞춘다
            refreshSplitCallingLogPanel();
        }
    }

    /**
     * 한국어판(0.93-kr8): 기억해 둔 전환 상태를 화면에 반영하고, 고른 탭을 밝게 표시한다.
     */
    private void applySplitCallingTab() {
        if (splitCallingContainer == null || splitCallingLogContainer == null) {
            return;
        }
        boolean showLog = splitCallingLogShown;
        splitCallingContainer.setVisibility(showLog ? View.GONE : View.VISIBLE);
        splitCallingLogContainer.setVisibility(showLog ? View.VISIBLE : View.GONE);
        styleSplitCallingTab(splitCallingTabCall, !showLog);
        styleSplitCallingTab(splitCallingTabLog, showLog);
    }

    /**
     * 한국어판(0.93-kr8): 전환 탭 하나의 모양(배경·글자색)을 고른 상태에 맞춘다.
     *
     * @param tab      탭(TextView)
     * @param selected 지금 고른 탭이면 true
     */
    private void styleSplitCallingTab(TextView tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.setBackgroundResource(selected
                ? R.drawable.split_tab_selected : R.drawable.split_tab_normal);
        tab.setTextColor(ContextCompat.getColor(this, selected
                ? R.color.split_tab_text_selected : R.color.split_tab_text_normal));
    }

    /**
     * 한국어판(0.93-kr8): 3분할의 QSO 로그 칸을 지금 검색 조건으로 다시 읽는다.
     * 패널 프래그먼트가 아직 없거나 다른 종류면 아무것도 하지 않는다.
     */
    private void refreshSplitCallingLogPanel() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_SPLIT_CALLING_LOG);
        if (fragment instanceof LogFragment) {
            ((LogFragment) fragment).refreshLogPanel();
        }
    }

    /**
     * 한국어판(0.93-kr6): 태블릿 가로 호출 탭 2분할 패널을 준비한다.
     * 왼쪽 splitCallingOnlyContainer 에 MyCallingFragment(호출 창),
     * 오른쪽 splitLogContainer 에 LogFragment(QSO 로그)를 붙인다.
     * 두 패널은 계속 살아 있으므로 탭을 오가도 목록과 검색 조건이 유지된다.
     */
    private void initTabletCallLogLayout() {
        View callLog = findViewById(R.id.splitCallLogLayout);
        if (!(callLog instanceof LinearLayout)) {
            splitCallLogLayout = null;
            splitCallLogVisible = false;
            return;
        }
        splitCallLogLayout = (LinearLayout) callLog;
        splitCallingOnlyContainer = findViewById(R.id.splitCallingOnlyContainer);
        splitLogContainer = findViewById(R.id.splitLogContainer);
        splitCallLogHandle = findViewById(R.id.splitCallLogHandle);
        if (splitCallingOnlyContainer == null || splitLogContainer == null
                || splitCallLogHandle == null) {
            splitCallLogLayout = null;
            splitCallLogVisible = false;
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment callingFragment = fragmentManager.findFragmentById(R.id.splitCallingOnlyContainer);
        Fragment logFragment = fragmentManager.findFragmentById(R.id.splitLogContainer);
        if (callingFragment == null || logFragment == null) {
            //일부만 남아 있는 경우를 대비해 남은 패널은 지우고 두 패널을 새로 붙인다
            try {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                if (callingFragment != null) {
                    transaction.remove(callingFragment);
                }
                if (logFragment != null) {
                    transaction.remove(logFragment);
                }
                transaction.add(R.id.splitCallingOnlyContainer, new MyCallingFragment()
                        , FRAGMENT_TAG_SPLIT_CALL_ONLY);
                transaction.add(R.id.splitLogContainer, new LogFragment()
                        , FRAGMENT_TAG_SPLIT_LOG);
                transaction.commitNowAllowingStateLoss();
            } catch (Exception e) {
                Log.e(TAG, "initTabletCallLogLayout: 호출·로그 패널을 붙이지 못했습니다. " + e.getMessage());
                splitCallLogLayout = null;
                splitCallLogVisible = false;
                return;
            }
        }

        callLogRatio = clampRatio(getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE)
                        .getFloat(KEY_CALL_LOG_RATIO, DEFAULT_CALL_LOG_RATIO)
                , CALL_LOG_RATIO_MIN, CALL_LOG_RATIO_MAX);
        applySplitRatio();
        splitCallLogHandle.setOnTouchListener(new CallLogHandleTouchListener());
        splitCallLogLayout.setVisibility(View.GONE);
    }

    /**
     * 3분할을 쓰지 않는 구성으로 바뀌었을 때(회전 등) 남아 있는 패널 프래그먼트를 지운다.
     */
    private void removeSplitFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String[] tags = {FRAGMENT_TAG_SPLIT_SPECTRUM, FRAGMENT_TAG_SPLIT_DECODE
                , FRAGMENT_TAG_SPLIT_CALLING, FRAGMENT_TAG_SPLIT_CALLING_LOG
                , FRAGMENT_TAG_SPLIT_CALL_ONLY, FRAGMENT_TAG_SPLIT_LOG};
        FragmentTransaction transaction = null;
        for (String tag : tags) {
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                if (transaction == null) {
                    transaction = fragmentManager.beginTransaction();
                }
                transaction.remove(fragment);
            }
        }
        if (transaction != null) {
            try {
                transaction.commitNowAllowingStateLoss();
            } catch (Exception e) {
                Log.e(TAG, "removeSplitFragments: 패널 프래그먼트를 지우지 못했습니다. " + e.getMessage());
            }
        }
    }

    /**
     * 현재 목적지에 따라 3분할 패널·호출+로그 2분할·전체 화면(NavHost) 중 하나를 보여 준다.
     *  - 스펙트럼 탭 : 3분할(한 번 더 누르면 스펙트럼 단독)
     *  - 호출 탭 : 왼쪽 호출 창 + 오른쪽 QSO 로그 (한국어판 0.93-kr6)
     *  - 디코드 탭·그 밖 : 그 화면만 전체로 (디코드 탭은 태블릿 가로에서 2열)
     */
    private void updateSplitVisibility(int destinationId) {
        if (splitLayout == null && splitCallLogLayout == null) {
            splitPanelVisible = false;
            splitCallLogVisible = false;
            return;
        }
        if (destinationId != R.id.menu_nav_spectrum) {
            splitSpectrumOnly = false;
        }
        boolean showSplit = splitLayout != null
                && (destinationId == R.id.menu_nav_spectrum) && !splitSpectrumOnly;
        //한국어판(0.93-kr6): 호출 탭은 왼쪽 호출 창·오른쪽 QSO 로그 2분할로 연다.
        //  디코드 목록에서 행을 눌러 호출 화면으로 넘어올 때도 같은 2분할이 열린다.
        boolean showCallLog = splitCallLogLayout != null
                && destinationId == R.id.menu_nav_mycalling;
        if (splitLayout != null) {
            splitLayout.setVisibility(showSplit ? View.VISIBLE : View.GONE);
        }
        if (splitCallLogLayout != null) {
            splitCallLogLayout.setVisibility(showCallLog ? View.VISIBLE : View.GONE);
            if (showCallLog && !splitCallLogVisible) {
                //한국어판(0.93-kr8): 다른 화면에서 검색어가 바뀌었거나, 숨은 동안 새 교신이 저장됐을 수 있으니
                //  보일 때마다 오른쪽 QSO 로그를 지금 조건으로 다시 읽는다.
                refreshCallLogPanel();
            }
        }
        splitPanelVisible = showSplit;
        splitCallLogVisible = showCallLog;
        //한국어판(0.93-kr8): 3분할에서 QSO 로그 칸을 보고 있으면, 다시 보일 때 목록을 맞춘다
        if (showSplit && splitCallingLogShown) {
            refreshSplitCallingLogPanel();
        }
        if (navHostContainer != null) {
            //3분할·2분할 패널을 볼 때는 뒤의 전체 화면은 보이지 않게만 둔다(프래그먼트 생명주기는 그대로)
            navHostContainer.setVisibility(showSplit || showCallLog ? View.INVISIBLE : View.VISIBLE);
        }
        //다음에 앱을 켰을 때도 같은 화면으로 시작한다
        //  (설정·로그처럼 잠깐 들른 화면은 기록하지 않는다)
        if (isSplitPanelTab(destinationId)) {
            saveLastMode(destinationId, showSplit || showCallLog);
        }
        //한국어판(0.93-kr5): 3분할일 때만 도구 버튼을 왼쪽 워터폴 쪽으로 옮긴다
        updateFloatViewPlacement();
    }

    /**
     * 한국어판(0.93-kr8): 호출 탭 2분할의 오른쪽 QSO 로그 패널을 새로 보일 때 호출한다.
     * 패널 프래그먼트가 아직 없거나 다른 종류면 아무것도 하지 않는다.
     */
    private void refreshCallLogPanel() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_SPLIT_LOG);
        if (fragment instanceof LogFragment) {
            ((LogFragment) fragment).refreshLogPanel();
        }
    }

    /**
     * 한국어판(0.93-kr6): 마지막으로 보던 화면을 기억한다.
     *  디코드 단독 / 호출+로그 2분할 / 스펙트럼 3분할 / 스펙트럼 단독
     *
     * @param destinationId 지금 목적지
     * @param panelShown    분할 패널이 떠 있으면 true
     */
    private void saveLastMode(int destinationId, boolean panelShown) {
        String mode;
        if (destinationId == R.id.menu_nav_spectrum) {
            mode = splitSpectrumOnly ? MODE_SPECTRUM_ONLY : MODE_SPECTRUM_SPLIT;
        } else if (destinationId == R.id.menu_nav_mycalling) {
            mode = MODE_MY_CALLING;
        } else {
            mode = MODE_DECODE;
        }
        getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE).edit()
                .putString(KEY_LAST_MODE, mode)
                .putBoolean(KEY_SPLIT_PREFERRED, panelShown)
                .apply();
    }

    /**
     * 한국어판(0.93-kr6): 앱을 켰을 때 기억해 둔 화면으로 시작한다.
     *
     * @param mode 저장해 둔 화면 모드
     */
    private void restoreStartMode(String mode) {
        splitSpectrumOnly = false;
        if (MODE_MY_CALLING.equals(mode) && splitCallLogLayout != null) {
            navigateToBottomTab(R.id.menu_nav_mycalling);
            updateSplitVisibility(R.id.menu_nav_mycalling);
            return;
        }
        if (MODE_SPECTRUM_SPLIT.equals(mode) && splitLayout != null) {
            navigateToBottomTab(R.id.menu_nav_spectrum);
            updateSplitVisibility(R.id.menu_nav_spectrum);
            return;
        }
        if (MODE_SPECTRUM_ONLY.equals(mode) && splitLayout != null) {
            splitSpectrumOnly = true;
            navigateToBottomTab(R.id.menu_nav_spectrum);
            updateSplitVisibility(R.id.menu_nav_spectrum);
            return;
        }
        //디코드 단독(기본)
        navigateToBottomTab(R.id.menu_nav_calling_list);
        updateSplitVisibility(R.id.menu_nav_calling_list);
    }

    /**
     * 하단 탭으로 화면을 바꿀 때, 뒤로가기 기록이 쌓이지 않도록 시작 화면(디코드)을 거쳐 이동한다.
     *
     * @param itemId 이동할 목적지
     */
    private void navigateToBottomTab(int itemId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == itemId) {
            return;//이미 그 화면
        }
        navController.popBackStack(R.id.menu_nav_calling_list, false);
        NavDestination afterPop = navController.getCurrentDestination();
        if (afterPop != null && afterPop.getId() == itemId) {
            return;
        }
        navController.navigate(itemId);
    }

    /**
     * 저장된 비율을 패널 가중치에 적용한다.
     */
    private void applySplitRatio() {
        if (splitLayout != null) {
            setSplitWeight(splitSpectrumContainer, splitWidthRatio);
            setSplitWeight(splitRightColumn, 1f - splitWidthRatio);
            setSplitWeight(splitDecodeContainer, splitHeightRatio);
            //한국어판(0.93-kr8): 오른쪽 아래 칸은 전환 탭을 포함한 묶음(splitCallingBody)의 높이 비율이다
            setSplitWeight(splitCallingBody != null ? splitCallingBody : splitCallingContainer
                    , 1f - splitHeightRatio);
        }
        //한국어판(0.93-kr6): 호출+로그 2분할의 좌우 비율
        if (splitCallLogLayout != null) {
            setSplitWeight(splitCallingOnlyContainer, callLogRatio);
            setSplitWeight(splitLogContainer, 1f - callLogRatio);
        }
    }

    private void setSplitWeight(View view, float weight) {
        if (view == null || !(view.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        if (params.weight != weight) {
            params.weight = weight;
            view.setLayoutParams(params);
        }
    }

    private float clampRatio(float ratio, float min, float max) {
        if (Float.isNaN(ratio)) {
            return min;
        }
        return Math.max(min, Math.min(max, ratio));
    }

    /**
     * 한국어판(0.93-kr8): 3분할 오른쪽 아래 칸의 전환 탭 높이(px). 탭이 없으면 0.
     *
     * @return 전환 탭 높이
     */
    private int splitCallingToggleBarHeight() {
        return splitCallingToggleBar == null ? 0 : splitCallingToggleBar.getHeight();
    }

    /**
     * 조절한 비율을 저장한다. 앱을 다시 켜도 그대로 쓴다.
     */
    private void saveSplitRatio() {
        getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE).edit()
                .putFloat(KEY_SPLIT_WIDTH_RATIO, splitWidthRatio)
                .putFloat(KEY_SPLIT_HEIGHT_RATIO, splitHeightRatio)
                .putFloat(KEY_CALL_LOG_RATIO, callLogRatio)
                .apply();
    }

    /**
     * 경계선 손잡이를 끌어 패널 비율을 바꾸는 처리.
     */
    private class SplitHandleTouchListener implements View.OnTouchListener {
        private final boolean vertical;
        private float startTouchPx = 0;
        private float startSizePx = 0;
        private float availablePx = 0;

        SplitHandleTouchListener(boolean vertical) {
            this.vertical = vertical;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (vertical) {
                        availablePx = splitLayout.getWidth() - splitVerticalHandle.getWidth();
                        startSizePx = splitSpectrumContainer.getWidth();
                        startTouchPx = event.getRawX();
                    } else {
                        //한국어판(0.93-kr8): 오른쪽 아래 칸의 전환 탭 높이는 비율 계산에서 뺀다
                        availablePx = splitRightColumn.getHeight() - splitHorizontalHandle.getHeight()
                                - splitCallingToggleBarHeight();
                        startSizePx = splitDecodeContainer.getHeight();
                        startTouchPx = event.getRawY();
                    }
                    return availablePx > 0;
                case MotionEvent.ACTION_MOVE:
                    if (availablePx <= 0) {
                        return false;
                    }
                    if (vertical) {
                        splitWidthRatio = clampRatio((startSizePx + event.getRawX() - startTouchPx)
                                / availablePx, SPLIT_WIDTH_RATIO_MIN, SPLIT_WIDTH_RATIO_MAX);
                    } else {
                        splitHeightRatio = clampRatio((startSizePx + event.getRawY() - startTouchPx)
                                / availablePx, SPLIT_HEIGHT_RATIO_MIN, SPLIT_HEIGHT_RATIO_MAX);
                    }
                    applySplitRatio();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (availablePx > 0) {
                        saveSplitRatio();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * 한국어판(0.93-kr6): 호출+로그 2분할의 경계선을 끌어 좌우 비율을 바꾸는 처리.
     */
    private class CallLogHandleTouchListener implements View.OnTouchListener {
        private float startTouchPx = 0;
        private float startSizePx = 0;
        private float availablePx = 0;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (splitCallLogLayout == null || splitCallLogHandle == null
                            || splitCallingOnlyContainer == null) {
                        return false;
                    }
                    availablePx = splitCallLogLayout.getWidth() - splitCallLogHandle.getWidth();
                    startSizePx = splitCallingOnlyContainer.getWidth();
                    startTouchPx = event.getRawX();
                    return availablePx > 0;
                case MotionEvent.ACTION_MOVE:
                    if (availablePx <= 0) {
                        return false;
                    }
                    callLogRatio = clampRatio((startSizePx + event.getRawX() - startTouchPx)
                            / availablePx, CALL_LOG_RATIO_MIN, CALL_LOG_RATIO_MAX);
                    applySplitRatio();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (availablePx > 0) {
                        saveSplitRatio();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * 한국어판(0.93-kr4): IC-705 기준 초기 설정을 앱을 처음 켜는 때 한 번만 넣는다.
     * 한 번 넣은 뒤에는 다시 건드리지 않으므로, 그 뒤에 바꾼 값은 그대로 유지된다.
     */
    private void applyIc705PresetIfNeeded() {
        SharedPreferences presetPreference = getSharedPreferences(PREF_IC705_PRESET, MODE_PRIVATE);
        if (presetPreference.getInt(KEY_IC705_PRESET_VERSION, 0) >= IC705_PRESET_VERSION) {
            return;
        }

        RigNameList rigNameList = RigNameList.getInstance(getBaseContext());
        int modelIndex = getIc705ModelIndex(rigNameList.rigList);
        RigNameList.RigName ic705 = rigNameList.getRigNameByIndex(modelIndex);

        GeneralVariables.modelNo = modelIndex;
        GeneralVariables.instructionSet = ic705.instructionSet;
        GeneralVariables.civAddress = ic705.address;
        GeneralVariables.baudRate = ic705.bauRate;
        GeneralVariables.swr_switch_on = true;
        GeneralVariables.alc_switch_on = true;
        GeneralVariables.noReplyLimit = DEFAULT_NO_REPLY_LIMIT;

        mainViewModel.databaseOpr.writeConfigs(new String[][]{
                {"model", String.valueOf(GeneralVariables.modelNo)},
                {"instruction", String.valueOf(GeneralVariables.instructionSet)},
                {"civ", GeneralVariables.getCivAddressStr()},
                {"baudRate", String.valueOf(GeneralVariables.baudRate)},
                {"swrSwitch", GeneralVariables.swr_switch_on ? "1" : "0"},
                {"alcSwitch", GeneralVariables.alc_switch_on ? "1" : "0"},
                {"noReplyLimit", String.valueOf(GeneralVariables.noReplyLimit)}
        });

        presetPreference.edit().putInt(KEY_IC705_PRESET_VERSION, IC705_PRESET_VERSION).apply();
        Log.e(TAG, String.format("applyIc705PresetIfNeeded: IC-705 초기 설정 적용 (model=%d, civ=%s, baud=%d)"
                , GeneralVariables.modelNo, GeneralVariables.getCivAddressStr()
                , GeneralVariables.baudRate));
    }

    /**
     * 무전기 목록에서 IC-705 의 위치를 찾는다. 없으면 기본 위치를 쓴다.
     */
    private int getIc705ModelIndex(ArrayList<RigNameList.RigName> rigList) {
        for (int i = 0; i < rigList.size(); i++) {
            String modelName = rigList.get(i).modelName;
            if (modelName != null && modelName.toUpperCase().contains("IC-705")) {
                return i;
            }
        }
        return DEFAULT_IC705_MODEL_INDEX;
    }

    /**
     * 添加浮动按钮
     */

    private void InitFloatView() {
        //floatView = new FloatView(this, 32);

        binding.container.addView(floatView);
        floatView.setButtonMargin(0);
        floatView.setFloatBoard(FloatView.FLOAT_BOARD.RIGHT);

        floatView.setButtonBackgroundResourceId(R.drawable.float_button_style);
        //动态添加按钮，建议使用静态的ID，静态ID在VALUES/FLOAT_BUTTON_IDS.XML中设置
        floatView.addButton(R.id.float_nav, "float_nav", R.drawable.ic_baseline_fullscreen_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FloatViewButton button = floatView.getButtonByName("float_nav");
                        if (binding.navView.getVisibility() == View.VISIBLE) {
                            binding.navView.setVisibility(View.GONE);
                            if (button != null) {
                                button.setImageResource(R.drawable.ic_baseline_fullscreen_exit_24);
                            }
                        } else {
                            binding.navView.setVisibility(View.VISIBLE);
                            if (button != null) {
                                button.setImageResource(R.drawable.ic_baseline_fullscreen_24);
                            }
                        }
                    }
                });
        floatView.addButton(R.id.float_freq, "float_freq", R.drawable.ic_baseline_freq_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new FreqDialog(binding.container.getContext(), mainViewModel).show();
                    }
                });

        floatView.addButton(R.id.set_volume, "set_volume", R.drawable.ic_baseline_volume_up_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new SetVolumeDialog(binding.container.getContext(), mainViewModel).show();
                    }
                });
        //打开网格追踪
        floatView.addButton(R.id.grid_tracker, "grid_tracker", R.drawable.ic_baseline_grid_tracker_24
                , new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), GridTrackerMainActivity.class);
                        startActivity(intent);
                    }
                });


//        floatView.addButton(R.id.flex_radio, "flex_radio", R.drawable.flex_icon
//                , new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        navController.navigate(R.id.flexRadioInfoFragment);
//                    }
//                });

        floatView.initLocation();
        //한국어판(0.93-kr5): 태블릿 3분할에서는 도구 버튼을 왼쪽 워터폴 쪽으로 옮긴다.
        updateFloatViewPlacement();
    }

    /**
     * 한국어판(0.93-kr5): 오른쪽에 떠 있는 도구 버튼(전체화면·주파수·볼륨·지구본)을
     * 태블릿 가로 3분할에서는 왼쪽 워터폴 쪽 구석으로 옮긴다.
     * 오른쪽 교신/디코드 목록을 가리지 않게 하기 위해서다.
     * 3분할이 아닐 때(폰 세로·로그·설정 화면 등)에는 예전처럼 오른쪽 가장자리에 둔다.
     */
    private void updateFloatViewPlacement() {
        if (floatView == null) {
            return;
        }
        boolean splitVisible = splitLayout != null
                && splitSpectrumContainer != null
                && splitLayout.getVisibility() == View.VISIBLE;
        if (!splitVisible) {
            floatView.setFloatBoard(FloatView.FLOAT_BOARD.RIGHT);
            floatView.initLocation();
            return;
        }
        //왼쪽 패널 안쪽에 두고, 끌어 옮겨도 왼쪽으로 붙게 한다
        floatView.setFloatBoard(FloatView.FLOAT_BOARD.LEFT);
        floatView.post(new Runnable() {
            @Override
            public void run() {
                placeFloatViewInSplitCorner();
            }
        });
    }

    /**
     * 왼쪽 워터폴 패널의 오른쪽 아래 구석에 도구 버튼을 세로로 쌓아 둔다.
     * 워터폴 아래쪽에 있는 스위치(잡음 제거·메시지 표시)는 가리지 않게 조금 띄운다.
     */
    private void placeFloatViewInSplitCorner() {
        if (floatView == null || splitLayout == null || splitSpectrumContainer == null
                || splitLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        View parent = (View) floatView.getParent();
        if (parent == null) {
            return;
        }
        int panelWidth = splitSpectrumContainer.getWidth();
        int panelHeight = splitSpectrumContainer.getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            return;//아직 화면 크기가 정해지지 않았으면 다음 배치 때 다시 한다
        }

        int[] panelLocation = new int[2];
        splitSpectrumContainer.getLocationInWindow(panelLocation);
        int[] parentLocation = new int[2];
        parent.getLocationInWindow(parentLocation);
        int panelRight = panelLocation[0] - parentLocation[0] + panelWidth;
        int panelBottom = panelLocation[1] - parentLocation[1] + panelHeight;

        int buttonPx = floatView.getWidth() > 0 ? floatView.getWidth() : dpToPx(32);
        int buttonCount = Math.max(1, floatView.getButtonCount());
        int gapPx = dpToPx(4);
        int stackHeight = buttonCount * buttonPx + (buttonCount - 1) * gapPx;

        int rightMarginPx = dpToPx(8);
        int bottomMarginPx = dpToPx(52);
        int left = panelRight - buttonPx - rightMarginPx;
        int top = panelBottom - stackHeight - bottomMarginPx;
        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }
        floatView.setLayoutLeftTop(left, top);
    }

    /**
     * dp 값을 화면 픽셀 값으로 바꾼다.
     *
     * @param dp dp 값
     * @return 픽셀 값
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * 初始化一些数据
     */
    private void InitData() {
        if (mainViewModel.configIsLoaded) return;//如果数据已经读取一遍了，就不用再读取了。

        //读取波段数据
        if (mainViewModel.operationBand == null) {
            mainViewModel.operationBand = OperationBand.getInstance(getBaseContext());
        }

        //한국어판(0.93-kr3): 처음 설치했을 때 기본 밴드는 40m(7.074MHz)이다.
        //  저장된 밴드가 없어 목록 위치가 정해지지 않았다면 기본 밴드의 위치를 찾아 둔다.
        if (GeneralVariables.bandListIndex < 0) {
            GeneralVariables.bandListIndex = OperationBand.getIndexByFreq(GeneralVariables.band);
        }

        mainViewModel.databaseOpr.getQslDxccToMap();

        //获取所有的配置参数
        mainViewModel.databaseOpr.getAllConfigParameter(new OnAfterQueryConfig() {
            @Override
            public void doOnBeforeQueryConfig(String KeyName) {

            }

            @Override
            public void doOnAfterQueryConfig(String KeyName, String Value) {
                mainViewModel.configIsLoaded = true;
                //한국어판(0.93-kr4): IC-705 기준 초기 설정(115200·CI-V A4·SWR/ALC 경보·무응답 3회)을 한 번만 넣는다
                applyIc705PresetIfNeeded();
                //此处梅登海德已经通过数据库得到了，但是如果GPS能获取到，还是用GPS的
                String grid = MaidenheadGrid.getMyMaidenheadGrid(getApplicationContext());
                if (!grid.equals("")) {//说明获取到了GPS数据
                    GeneralVariables.setMyMaidenheadGrid(grid);
                    //写到数据库中
                    mainViewModel.databaseOpr.writeConfig("grid", grid, null);
                }

                mainViewModel.ft8TransmitSignal.setTimer_sec(GeneralVariables.transmitDelay);
                //如果呼号、网格为空，就进入设置界面
                if (GeneralVariables.getMyMaidenheadGrid().equals("")
                        || GeneralVariables.myCallsign.equals("")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {//导航到设置页面
                            navController.navigate(R.id.menu_nav_config);
                        }
                    });
                }
            }
        });

        //把历史中通联成功的呼号与网格的对应关系
        new DatabaseOpr.GetCallsignMapGrid(mainViewModel.databaseOpr.getDb()).execute();

        mainViewModel.getFollowCallsignsFromDataBase();
        //打开呼号位置信息的数据库，目前是以内存数据库方式。
        if (GeneralVariables.callsignDatabase == null) {
            GeneralVariables.callsignDatabase = CallsignDatabase.getInstance(getBaseContext(), null, 1);
        }
    }


    /**
     * 显示生成log的对话框
     */
    private void showShareDialog() {
        dialog = new ShareLogsProgressDialog(
                binding.getRoot().getContext()
                , mainViewModel,true);

        dialog.show();
        mainViewModel.mutableSharePosition.postValue(0);
        mainViewModel.mutableShareInfo.postValue("");
        mainViewModel.mutableShareCount.postValue(0);
    }


    /**
     * 检查权限
     */
    private void checkPermission() {
        mPermissionList.clear();

        //判断哪些权限未授予
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        }

        //判断是否为空
        if (!mPermissionList.isEmpty()) {//请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
        }
    }


    /**
     * 响应授权
     * 这里不管用户是否拒绝，都进入首页，不再重复申请权限
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * 显示串口设备列表
     */
    public void setSelectUsbDevice() {
        ArrayList<CableSerialPort.SerialPort> ports = mainViewModel.mutableSerialPorts.getValue();
        binding.selectSerialPortLinearLayout.removeAllViews();
        for (int i = 0; i < ports.size(); i++) {//动态添加串口设备列表
            View layout = LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.select_serial_port_list_view_item, null);
            layout.setId(i);
            TextView textView = layout.findViewById(R.id.selectSerialPortListViewItemTextView);
            textView.setText(ports.get(i).information());
            binding.selectSerialPortLinearLayout.addView(layout);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //连接电台并做电台的频率设置等操作
                    mainViewModel.connectCableRig(getApplicationContext(), ports.get(view.getId()));
                    binding.selectSerialPortLayout.setVisibility(View.GONE);
                }
            });
        }

        //选择串口设备弹框
        if ((ports.size() >= 1) && (!mainViewModel.isRigConnected())) {
            binding.selectSerialPortLayout.setVisibility(View.VISIBLE);
        } else {//说明没有可以识别的驱动，不显示设备弹框
            binding.selectSerialPortLayout.setVisibility(View.GONE);
        }
    }

//    /**
//     * 删除指定文件夹中的所有文件
//     *
//     * @param filePath 指定的文件夹
//     */
//    public static void deleteFolderFile(String filePath) {
//        try {
//            File file = new File(filePath);//获取SD卡指定路径
//            File[] files = file.listFiles();//获取SD卡指定路径下的文件或者文件夹
//            for (int i = 0; i < files.length; i++) {
//                if (files[i].isFile()) {//如果是文件直接删除
//                    File tempFile = new File(files[i].getPath());
//                    tempFile.delete();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void animationImage() {

        ObjectAnimator navigationAnimator = ObjectAnimator.ofFloat(binding.navView, "translationY", 200);
        navigationAnimator.setDuration(3000);
        navigationAnimator.setFloatValues(200, 200, 200, 0);


        ObjectAnimator hideLogoAnimator = ObjectAnimator.ofFloat(binding.initDataLayout, "alpha", 1f, 1f, 1f, 0);
        hideLogoAnimator.setDuration(3000);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(navigationAnimator, hideLogoAnimator);
        //animatorSet.playTogether(initPositionStrAnimator, logoAnimator, navigationAnimator, hideLogoAnimator);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //animationEnd = true;
                binding.initDataLayout.setVisibility(View.GONE);
                binding.utcProgressBar.setVisibility(View.VISIBLE);
                InitFloatView();//显示浮窗
                //binding.floatView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        animatorSet.start();
    }


    //此方法只有在android:launchMode="singleTask"模式下起作用
    @Override
    protected void onNewIntent(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            mainViewModel.getUsbDevice();
        }else {
            setIntent(intent);//因为处于单例模式，所以要更新一下intent
            doReceiveShareFile(getIntent());
        }
        super.onNewIntent(intent);
    }


    @Override
    public void onBackPressed() {
        if (navController.getGraph().getStartDestination() == navController.getCurrentDestination().getId()) {//说明是到最后一个页面了
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.exit_confirmation))
                    .setPositiveButton(getString(R.string.exit)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (mainViewModel.ft8TransmitSignal.isActivated()) {
                                        mainViewModel.ft8TransmitSignal.setActivated(false);
                                    }
                                    closeThisApp();//退出APP
                                }
                            }).setNegativeButton(getString(R.string.cancel)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
            builder.create().show();

        } else {//退出activity堆栈
            navController.navigateUp();
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }

    private void closeThisApp() {
        mainViewModel.ft8TransmitSignal.setActivated(false);
        if (mainViewModel.baseRig != null) {
            if (mainViewModel.baseRig.getConnector() != null) {
                mainViewModel.baseRig.getConnector().disconnect();
            }
        }

        mainViewModel.ft8SignalListener.stopListen();
        mainViewModel = null;
        System.exit(0);
    }


    /**
     * 注册蓝牙动作广播
     */
    private void registerBluetoothReceiver() {
        if (mReceive == null) {
            mReceive = new BluetoothStateBroadcastReceive(getApplicationContext(), mainViewModel);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        intentFilter.addAction(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(AudioManager.EXTRA_SCO_AUDIO_STATE);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.EXTRA_CONNECTION_STATE);
        intentFilter.addAction(BluetoothAdapter.EXTRA_STATE);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        registerReceiver(mReceive, intentFilter);
    }

    /**
     * 注销蓝牙动作广播
     */
    private void unregisterBluetoothReceiver() {
        if (mReceive != null) {
            unregisterReceiver(mReceive);
            mReceive = null;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBluetoothReceiver();
        //保证屏幕方向切换后，不会因为对话框导致闪退
        if (Boolean.TRUE.equals(mainViewModel.mutableImportShareRunning.getValue())) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }

        super.onDestroy();
    }


}