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
    private static final String FRAGMENT_TAG_SPLIT_SPECTRUM = "splitSpectrum";
    private static final String FRAGMENT_TAG_SPLIT_DECODE = "splitDecode";
    private static final String FRAGMENT_TAG_SPLIT_CALLING = "splitCalling";
    private static final float DEFAULT_SPLIT_WIDTH_RATIO = 0.55f;
    private static final float DEFAULT_SPLIT_HEIGHT_RATIO = 0.50f;
    private static final float SPLIT_WIDTH_RATIO_MIN = 0.30f;
    private static final float SPLIT_WIDTH_RATIO_MAX = 0.75f;
    private static final float SPLIT_HEIGHT_RATIO_MIN = 0.20f;
    private static final float SPLIT_HEIGHT_RATIO_MAX = 0.80f;

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
    private float splitWidthRatio = DEFAULT_SPLIT_WIDTH_RATIO;
    private float splitHeightRatio = DEFAULT_SPLIT_HEIGHT_RATIO;
    private static boolean tabletSplitActive = false;


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
                //한국어판(0.93-kr4): 태블릿 3분할에서는 세 패널(스펙트럼·디코드·호출)이 늘 함께 떠 있다.
                //  그래서 이 세 탭은 화면만 3분할로 되돌리고, QSO 로그·설정에서 왔으면 시작 화면으로 돌아온다.
                if (splitLayout != null && isSplitPanelTab(itemId)) {
                    NavDestination currentDestination = navController.getCurrentDestination();
                    if (currentDestination != null
                            && currentDestination.getId() != R.id.menu_nav_calling_list) {
                        navController.popBackStack(R.id.menu_nav_calling_list, false);
                    }
                    updateSplitVisibility(R.id.menu_nav_calling_list);
                    return true;
                }
                navController.navigate(itemId);
                //binding.navView.setLabelFor(item.getItemId());
                return true;
            }
        });

        //한국어판(0.93-kr4): 태블릿 가로 3분할 패널을 준비하고, 화면(목적지)에 따라 표시를 바꾼다.
        initTabletSplitLayout();
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller
                    , @NonNull NavDestination destination, @Nullable Bundle arguments) {
                updateSplitVisibility(destination.getId());
            }
        });

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
     * 태블릿 가로 3분할 패널을 준비한다.
     * 3분할을 쓰지 않는 구성(스마트폰 등)에서는 이전 구성에서 넘어온 패널 프래그먼트만 정리한다.
     */
    private void initTabletSplitLayout() {
        navHostContainer = findViewById(R.id.fragmentContainerView);
        View split = findViewById(R.id.splitLayout);
        if (!isTabletSplitConfiguration() || !(split instanceof LinearLayout)) {
            tabletSplitActive = false;
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
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment spectrumFragment = fragmentManager.findFragmentById(R.id.splitSpectrumContainer);
        Fragment decodeFragment = fragmentManager.findFragmentById(R.id.splitDecodeContainer);
        Fragment callingFragment = fragmentManager.findFragmentById(R.id.splitCallingContainer);
        if (spectrumFragment == null || decodeFragment == null || callingFragment == null) {
            //일부만 남아 있는 경우를 대비해 남은 패널은 지우고 세 패널을 새로 붙인다
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
        }

        SharedPreferences splitPreference = getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE);
        splitWidthRatio = clampRatio(splitPreference.getFloat(KEY_SPLIT_WIDTH_RATIO
                , DEFAULT_SPLIT_WIDTH_RATIO), SPLIT_WIDTH_RATIO_MIN, SPLIT_WIDTH_RATIO_MAX);
        splitHeightRatio = clampRatio(splitPreference.getFloat(KEY_SPLIT_HEIGHT_RATIO
                , DEFAULT_SPLIT_HEIGHT_RATIO), SPLIT_HEIGHT_RATIO_MIN, SPLIT_HEIGHT_RATIO_MAX);
        applySplitRatio();

        splitVerticalHandle.setOnTouchListener(new SplitHandleTouchListener(true));
        splitHorizontalHandle.setOnTouchListener(new SplitHandleTouchListener(false));

        tabletSplitActive = true;
        //표시 여부는 현재 목적지에 따라 updateSplitVisibility 가 정한다.
        splitLayout.setVisibility(View.GONE);
    }

    /**
     * 3분할을 쓰지 않는 구성으로 바뀌었을 때(회전 등) 남아 있는 패널 프래그먼트를 지운다.
     */
    private void removeSplitFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String[] tags = {FRAGMENT_TAG_SPLIT_SPECTRUM, FRAGMENT_TAG_SPLIT_DECODE
                , FRAGMENT_TAG_SPLIT_CALLING};
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
            transaction.commitNowAllowingStateLoss();
        }
    }

    /**
     * 현재 목적지에 따라 3분할 패널과 전체 화면(NavHost) 중 하나를 보여 준다.
     */
    private void updateSplitVisibility(int destinationId) {
        if (splitLayout == null) {
            return;
        }
        boolean showSplit = isSplitPanelTab(destinationId);
        splitLayout.setVisibility(showSplit ? View.VISIBLE : View.GONE);
        if (navHostContainer != null) {
            //3분할 패널을 볼 때는 뒤의 전체 화면은 보이지 않게만 둔다(프래그먼트 생명주기는 그대로)
            navHostContainer.setVisibility(showSplit ? View.INVISIBLE : View.VISIBLE);
        }
    }

    /**
     * 저장된 비율을 패널 가중치에 적용한다.
     */
    private void applySplitRatio() {
        if (splitLayout == null) {
            return;
        }
        setSplitWeight(splitSpectrumContainer, splitWidthRatio);
        setSplitWeight(splitRightColumn, 1f - splitWidthRatio);
        setSplitWeight(splitDecodeContainer, splitHeightRatio);
        setSplitWeight(splitCallingContainer, 1f - splitHeightRatio);
    }

    private void setSplitWeight(View view, float weight) {
        if (view == null) {
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
     * 조절한 비율을 저장한다. 앱을 다시 켜도 그대로 쓴다.
     */
    private void saveSplitRatio() {
        getSharedPreferences(PREF_TABLET_SPLIT, MODE_PRIVATE).edit()
                .putFloat(KEY_SPLIT_WIDTH_RATIO, splitWidthRatio)
                .putFloat(KEY_SPLIT_HEIGHT_RATIO, splitHeightRatio)
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
                        availablePx = splitRightColumn.getHeight() - splitHorizontalHandle.getHeight();
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