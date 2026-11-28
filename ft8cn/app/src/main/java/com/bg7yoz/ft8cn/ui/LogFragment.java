package com.bg7yoz.ft8cn.ui;
/**
 * 通联纪录的主界面。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.log.ShareLogs;
import com.bg7yoz.ft8cn.databinding.FragmentLogBinding;
import com.bg7yoz.ft8cn.grid_tracker.GridTrackerMainActivity;
import com.bg7yoz.ft8cn.html.LogHttpServer;
import com.bg7yoz.ft8cn.log.AdifImporter;
import com.bg7yoz.ft8cn.log.LogCallsignAdapter;
import com.bg7yoz.ft8cn.log.LogQSLAdapter;
import com.bg7yoz.ft8cn.log.OnQueryQSLCallsign;
import com.bg7yoz.ft8cn.log.OnQueryQSLRecordCallsign;
import com.bg7yoz.ft8cn.log.QSLCallsignRecord;
import com.bg7yoz.ft8cn.log.QSLRecordStr;
import com.bg7yoz.ft8cn.log.OnShareLogEvents;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;


public class LogFragment extends Fragment {
    private static final String TAG = "LogFragment";
    //한국어판(0.93-kr16): ADIF 불러오기 파일 창을 여는 요청 번호
    private static final int REQUEST_IMPORT_ADIF = 7108;
    //한국어판(0.93-kr17): [새 교신만] 이 마지막으로 내보낸 QSO id 를 담아 두는 곳
    private static final String PREF_EXPORT = "ft8cn_kr_export";
    private static final String PREF_LAST_EXPORT_ID = "last_adif_export_id";
    private FragmentLogBinding binding;
    private MainViewModel mainViewModel;

    private LogCallsignAdapter logCallsignAdapter;
    private LogQSLAdapter logQSLAdapter;
    private boolean loading = false;//防止滑动触发多次查询
    private int lastItemPosition;
    //한국어판(0.93-kr8): 목록을 마지막으로 읽은 때(밀리초). 화면을 연 직후 onResume 이 또 읽는 것을 막는다
    private long lastQueryTimeMs = 0;
    //한국어판(0.93-kr8): 새 교신이 저장됐다는 알림을 받으면 목록을 다시 읽는다(실시간 QSO 로그).
    //  여러 건이 잇따라 저장될 때는 마지막 한 번만 읽도록 조금 미룬다.
    private static final long LIVE_REFRESH_DELAY_MS = 250;
    //화면을 연 직후 onResume 이 목록을 또 읽지 않게 하는 여유 시간(ms)
    private static final long JUST_OPENED_GRACE_MS = 1500;
    private final Handler liveRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable liveRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshLogList();
        }
    };
    private ShareLogsProgressDialog dialog = null;//生成共享log的对话框

    //한국어판(0.93-kr8): PSK Reporter 실시간 지도 주소(내 콜사인으로 내 신호를 받은 곳을 본다)
    private static final String PSK_REPORTER_MAP_URL
            = "https://pskreporter.info/pskmap.html?preset&callsign=%1$s&txrx=tx&timerange=86400";


    public LogFragment() {
        // Required empty public constructor
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = MainViewModel.getInstance(this);

    }

    @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(getLayoutInflater());

        logCallsignAdapter = new LogCallsignAdapter(requireContext(), mainViewModel);
        logQSLAdapter = new LogQSLAdapter(requireContext(), mainViewModel);
        binding.logRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        setShowStyle();//设置显模式


        initRecyclerViewAction();//设置列表滑动动作


        //设置显示统计页面按钮
        binding.countImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCountFragment();
            }
        });

        binding.inputMycallEdit.setText(mainViewModel.queryKey);
        queryByCallsign(mainViewModel.queryKey, 0);

        //한국어판(0.93-kr8): 새 교신이 저장될 때마다(DB 저장 알림) 목록을 바로 새로 고친다.
        //  태블릿 가로 '호출' 탭의 오른쪽 QSO 로그 패널은 앱을 켜 둔 동안 계속 살아 있으므로,
        //  이 관찰이 없으면 새 교신이 저장돼도 예전 목록이 그대로 남는다.
        mainViewModel.mutableQsoLogUpdated.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer updated) {
                if (updated == null || updated <= 0) {
                    return;//처음 등록될 때 오는 현재 값
                }
                onQsoLogUpdated();
            }
        });

        mainViewModel.mutableQueryFilter.observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                queryByCallsign(mainViewModel.queryKey, 0);
            }
        });

        //输入条件监听
        binding.inputMycallEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mainViewModel.queryKey = editable.toString();
                queryByCallsign(mainViewModel.queryKey, 0);
            }
        });

        //过滤条件按钮
        binding.filterImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FilterDialog(requireContext(), mainViewModel).show();
            }
        });

        //导出按钮
        binding.exportImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getLocalIp() == null) {
                    new HelpDialog(requireContext(), requireActivity()
                            , GeneralVariables.getStringFromResource(R.string.export_null)
                            , false).show();
                } else {
                    new HelpDialog(requireContext(), requireActivity()
                            , String.format(GeneralVariables.getStringFromResource(R.string.export_info)
                            , getLocalIp(), LogHttpServer.DEFAULT_PORT)
                            , false).show();
                }

            }
        });

        //分享日志按钮
        binding.shareLogImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildShareLogs();
            }
        });

        binding.logViewStyleimageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainViewModel.logListShowCallsign = !mainViewModel.logListShowCallsign;
                setShowStyle();
                queryByCallsign(binding.inputMycallEdit.getText().toString(), 0);//偏移量0，就是重新查询
            }
        });

        //한국어판(0.93-kr8): 예전 '지도(핀)' 버튼 자리는 [PSK] 버튼이다.
        //  내 콜사인·그리드를 보여 주고, PSK Reporter 지도와 내 교신 지도로 이어진다.
        //  콜사인은 누르는 순간 GeneralVariables.myCallsign 을 읽으므로,
        //  설정에서 콜사인을 바꾸면 버튼도 바뀐 콜사인을 그대로 따라간다.
        binding.pskGridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPskGridDialog();
            }
        });

        //한국어판(0.93-kr16): 목록 아래 [ADIF 불러오기] 줄을 연결한다
        initAdifImportBar();
        //判断生成共享log文件的工作线程还在，如果在，就显示对话框
        if (Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue())) {
            showShareDialog();
        }

        return binding.getRoot();
    }

    /**
     * 한국어판(0.93-kr16): 목록 아래 [ADIF 불러오기] 줄 — 누르면 파일을 골라 QSO 기록에 넣는다.
     * 한국어판(0.93-kr17): 같은 줄에 [새 교신만] 단추를 더했다 — 마지막으로 내보낸 뒤에
     * 저장된 교신만 ADIF 로 만들어 공유한다.
     */
    private void initAdifImportBar() {
        if (binding == null) {
            return;
        }
        if (binding.adifImportLayout != null) {
            binding.adifImportLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pickAdifFile();
                }
            });
        }
        if (binding.adifExportNewButton != null) {
            binding.adifExportNewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    buildShareNewLogs();
                }
            });
        }
    }

    /**
     * 한국어판(0.93-kr17): [새 교신만] 이 마지막으로 내보낸 QSO id 를 읽는다.
     */
    private long getLastExportId() {
        SharedPreferences prefs = requireContext().getApplicationContext()
                .getSharedPreferences(PREF_EXPORT, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_LAST_EXPORT_ID, 0);
    }

    /**
     * 한국어판(0.93-kr17): 이번에 내보낸 데까지를 기준점으로 적어 둔다.
     */
    private void setLastExportId(long id) {
        SharedPreferences prefs = requireContext().getApplicationContext()
                .getSharedPreferences(PREF_EXPORT, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_LAST_EXPORT_ID, id).apply();
    }

    /**
     * 한국어판(0.93-kr17): 마지막으로 내보낸 뒤 새로 저장된 교신만 ADIF 로 만들어 공유한다.
     * 새 교신이 없으면 공유 창을 열지 않고 안내만 한다.
     * 다 내보낸 뒤에는 그 기준점을 마지막 QSO id 로 올려 두어, 다음부터는 그 뒤의 것만 나간다.
     */
    private void buildShareNewLogs() {
        if (binding == null || !isAdded()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ShareLogs shareLogs = new ShareLogs();
                SQLiteDatabase db = mainViewModel.databaseOpr.getDb();
                long afterId = getLastExportId();
                //로그 표를 비우거나 새로 만든 경우(가장 큰 id 가 기존 기준점보다 작음)에는 처음부터 내보낸다
                if (afterId > shareLogs.getMaxLogId(db)) {
                    afterId = 0;
                }
                final long baseId = afterId;
                final int newCount = shareLogs.countLogsAfter(db, baseId);
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        if (newCount <= 0) {
                            ToastMessage.show(getString(R.string.log_export_new_empty), true);
                            return;
                        }
                        //보통의 [공유] 와 같은 진행 창을 띄우고, 파일 만들기를 따로 스레드에서 한다
                        showShareDialog();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                shareNewLogs(shareLogs, baseId);
                            }
                        }).start();
                    }
                });
            }
        }).start();
    }

    /**
     * 한국어판(0.93-kr17): 새 교신만 파일로 쓰고 공유 창을 연다. 다 내보낸 뒤 기준점을 올린다.
     */
    private void shareNewLogs(final ShareLogs shareLogs, final long baseId) {
        File adiFile = GeneralVariables.writeToTempFile(requireContext()
                , "FT8CN"
                , ".txt"
                , "");

        //진행 콜백으로 실제로 파일에 쓴 건수를 센다(중간에 지운 기록이 있으면 id 차이와 다르다)
        final int[] written = new int[]{0};

        final long exportedId = shareLogs.doShareNewLogs(requireContext(), adiFile
                , GeneralVariables.getStringFromResource(R.string.log_export_new_share_title)
                , mainViewModel.databaseOpr.getDb()
                , baseId
                , adiFile
                , new OnShareLogEvents() {
                    @Override
                    public void onPreparing(String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                    }

                    @Override
                    public void onShareStart(int count, String info) {
                        mainViewModel.mutableSharePosition.postValue(0);
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableShareRunning.postValue(true);
                        mainViewModel.mutableShareCount.postValue(count);
                    }

                    @Override
                    public boolean onShareProgress(int count, int position, String info) {
                        written[0] = position;
                        mainViewModel.mutableSharePosition.postValue(position);
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableShareCount.postValue(count);
                        return Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue());
                    }

                    @Override
                    public void afterGet(int count, String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                        mainViewModel.mutableShareRunning.postValue(false);
                    }

                    @Override
                    public void onShareFailed(String info) {
                        mainViewModel.mutableShareInfo.postValue(info);
                    }
                });

        if (exportedId <= baseId) {
            return;//중간에 그만두었거나 새 기록이 없으면 기준점을 그대로 둔다
        }
        setLastExportId(exportedId);

        if (isAdded() && getActivity() != null) {
            final int done = written[0];
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isAdded()) {
                        return;
                    }
                    ToastMessage.show(getString(R.string.log_export_new_done, done), true);
                }
            });
        }
    }

    /**
     * 한국어판(0.93-kr16): ADIF 파일을 고르는 창을 연다.
     */
    private void pickAdifFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES
                    , new String[]{"text/plain", "text/x-adif", "application/octet-stream"});
            startActivityForResult(intent, REQUEST_IMPORT_ADIF);
        } catch (Exception e) {
            Log.e(TAG, "pickAdifFile: 파일 창을 열지 못했습니다: " + e.getMessage());
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("*/*");
                startActivityForResult(fallback, REQUEST_IMPORT_ADIF);
            } catch (Exception e2) {
                ToastMessage.show(getString(R.string.log_adif_import_fail), true);
            }
        }
    }

    /**
     * 한국어판(0.93-kr16): 고른 ADIF 파일을 읽어 QSO 기록에 넣는다.
     * 파일이 크면 시간이 걸리므로 따로 스레드에서 읽는다.
     *
     * @param uri 고른 파일
     */
    private void importAdifFile(final Uri uri) {
        if (uri == null || binding == null || !isAdded()) {
            return;
        }
        ToastMessage.show(getString(R.string.log_adif_import_running), true);
        final Context appContext = requireContext().getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] result = AdifImporter.importFrom(appContext, uri
                        , mainViewModel.databaseOpr);
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (binding == null) {
                            return;
                        }
                        if (result[0] == 0) {
                            ToastMessage.show(getString(R.string.log_adif_import_empty), true);
                            return;
                        }
                        ToastMessage.show(getString(R.string.log_adif_import_done
                                , result[1], result[0]), true);
                        //목록을 다시 읽어 새로 넣은 기록을 바로 보여 준다
                        refreshLogList();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_ADIF
                && resultCode == android.app.Activity.RESULT_OK && data != null) {
            importAdifFile(data.getData());
        }
    }


    /**
     * 한국어판(0.93-kr9): 지금 검색 조건의 교신 기록을 ADIF 파일로 만들어 공유한다.
     * '그리드·PSK' 탭의 [ADIF 공유] 버튼이 이 화면에 그대로 맡기도록 열어 둔 길이다.
     */
    public void shareAdifFile() {
        if (binding == null || !isAdded()) {
            return;
        }
        buildShareLogs();
    }

    /**
     * 显示生成log的对话框
     */
    private void showShareDialog() {
        mainViewModel.mutableShareRunning.setValue(true);
        dialog = new ShareLogsProgressDialog(
                binding.getRoot().getContext()
                , mainViewModel,false);

        dialog.show();
        mainViewModel.mutableSharePosition.postValue(0);
        mainViewModel.mutableShareInfo.postValue("");
        mainViewModel.mutableShareCount.postValue(0);
    }


    /**
     * 创建共享日志的数据文件
     */
    private void buildShareLogs() {

        //先显示生成log的对话框
        showShareDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {

                File adiFile = GeneralVariables.writeToTempFile(requireContext()
                        , "FT8CN"
                        , ".txt"
                        , "");


                new ShareLogs().doShareLogs(requireContext(), adiFile
                        , GeneralVariables.getStringFromResource(R.string.share_logs)
                        , mainViewModel.databaseOpr.getDb()
                        , mainViewModel.queryKey
                        , mainViewModel.queryFilter
                        , adiFile
                        , false
                        , new OnShareLogEvents() {
                            @Override
                            public void onPreparing(String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                            }

                            @Override
                            public void onShareStart(int count, String info) {
                                mainViewModel.mutableSharePosition.postValue(0);
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareRunning.postValue(true);
                                mainViewModel.mutableShareCount.postValue(count);
                            }

                            @Override
                            public boolean onShareProgress(int count, int position, String info) {
                                mainViewModel.mutableSharePosition.postValue(position);
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareCount.postValue(count);
                                return Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue());
                            }

                            @Override
                            public void afterGet(int count, String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                                mainViewModel.mutableShareRunning.postValue(false);
                            }

                            @Override
                            public void onShareFailed(String info) {
                                mainViewModel.mutableShareInfo.postValue(info);
                            }
                        });
            }
        }).start();
    }

    /**
     * 弹出菜单选项
     *
     * @param item item
     * @return item
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        //한국어판(0.93-kr6): 태블릿 가로 호출 탭 2분할에서는 QSO 로그 화면이 둘(패널·탭) 살아 있을 수 있다.
        //  그래서 컨텍스트 메뉴도 자기 목록에서 나온 항목에만 반응하게 한다.
        if (!isMyListItemView(item.getActionView())) {
            return super.onContextItemSelected(item);
        }
        int position = (Integer) item.getActionView().getTag();
        if (!mainViewModel.logListShowCallsign) {
            switch (item.getItemId()) {
                case 0:
                    logQSLAdapter.setRecordIsQSL(position, false);
                    logQSLAdapter.notifyItemChanged(position);
                    break;
                case 1:
                    logQSLAdapter.setRecordIsQSL(position, true);
                    logQSLAdapter.notifyItemChanged(position);
                    break;
                case 2:
                    showQrzFragment(logQSLAdapter.getRecord(position).getCall());
                    break;
                case 3:
                    Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
                    intent.putExtra("qslList", logQSLAdapter.getRecord(position));
                    startActivity(intent);
                    break;

            }
        } else {
            if (item.getItemId() == 2) {
                showQrzFragment(logCallsignAdapter.getRecord(position).getCallsign());
            }
        }

        return super.onContextItemSelected(item);
    }

//    private boolean itemIsOnScreen(View view) {
//        if (view != null) {
//            int width = view.getWidth();
//            int height = view.getHeight();
//            Rect rect = new Rect(0, 0, width, height);
//            return view.getLocalVisibleRect(rect);
//        }
//        return false;
//    }

    private void loadQueryData() {
        if ((!loading)) {
            if (mainViewModel.logListShowCallsign) {
                queryByCallsign(mainViewModel.queryKey, logCallsignAdapter.getItemCount());
            } else {
                queryByCallsign(mainViewModel.queryKey, logQSLAdapter.getItemCount());
            }
        }
    }

    /**
     * 设置列表滑动动作
     */
    private void initRecyclerViewAction() {

        binding.logRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int itemCount;
                if (mainViewModel.logListShowCallsign) {
                    itemCount = logCallsignAdapter.getItemCount();
                } else {
                    itemCount = logQSLAdapter.getItemCount();
                }
                if (newState == SCROLL_STATE_IDLE &&
                        lastItemPosition == itemCount) {
                    loadQueryData();

                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager manager = (LinearLayoutManager) layoutManager;
                    int firstVisibleItem = manager.findFirstVisibleItemPosition();
                    int l = manager.findLastCompletelyVisibleItemPosition();
                    lastItemPosition = firstVisibleItem + (l - firstVisibleItem) + 1;
                }
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.ANIMATION_TYPE_DRAG
                , ItemTouchHelper.END | ItemTouchHelper.START) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder
                    , @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.END) {
                    //做一个是否删除确认对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setIcon(null);
                    builder.setTitle(GeneralVariables.getStringFromResource(R.string.delete_confirmation));
                    builder.setMessage(GeneralVariables.getStringFromResource(R.string.are_you_sure_delete));
                    builder.setPositiveButton(GeneralVariables.getStringFromResource(R.string.ok_confirmed)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    logQSLAdapter.deleteRecord(viewHolder.getAdapterPosition());//删除日志
                                    logQSLAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                                }
                            });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            logQSLAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton(GeneralVariables.getStringFromResource(R.string.cancel)
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    logQSLAdapter.notifyDataSetChanged();
                                }
                            }).show();

                }

                if (direction == ItemTouchHelper.START) {
                    logQSLAdapter.setRecordIsQSL(viewHolder.getAdapterPosition()
                            , !logQSLAdapter.getRecord(viewHolder.getAdapterPosition()).isQSL);
                    logQSLAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                }
            }

            //判断列表格式，呼号列表
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder) {
                int swipeFlag;
                if (mainViewModel.logListShowCallsign) {
                    swipeFlag = 0;
                } else {
                    swipeFlag = ItemTouchHelper.START | ItemTouchHelper.END;
                }
                return makeMovementFlags(0, swipeFlag);
            }

            //制作删除背景的图标显示
            final Drawable delIcon = ContextCompat.getDrawable(requireActivity()
                    , R.drawable.log_item_delete_icon);
            final Drawable qslIcon = ContextCompat.getDrawable(requireActivity()
                    , R.drawable.ic_baseline_library_add_check_24);
            final Drawable background = new ColorDrawable(Color.LTGRAY);

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView
                    , @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY
                    , int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                Drawable icon;
                View itemView = viewHolder.itemView;
                if (dX > 0) {
                    icon = delIcon;
                } else {
                    icon = qslIcon;
                }

                int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                int iconLeft, iconRight, iconTop, iconBottom;
                int backTop, backBottom, backLeft, backRight;
                backTop = itemView.getTop();
                backBottom = itemView.getBottom();
                iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                iconBottom = iconTop + icon.getIntrinsicHeight();
                if (dX > 0) {
                    backLeft = itemView.getLeft();
                    backRight = itemView.getLeft() + (int) dX;
                    background.setBounds(backLeft, backTop, backRight, backBottom);
                    iconLeft = itemView.getLeft() + iconMargin;
                    iconRight = iconLeft + icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else if (dX < 0) {
                    backRight = itemView.getRight();
                    backLeft = itemView.getRight() + (int) dX;
                    background.setBounds(backLeft, backTop, backRight, backBottom);
                    iconRight = itemView.getRight() - iconMargin;
                    iconLeft = iconRight - icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else {
                    background.setBounds(0, 0, 0, 0);
                    icon.setBounds(0, 0, 0, 0);
                }
                background.draw(c);
                icon.draw(c);

            }
        }).attachToRecyclerView(binding.logRecyclerView);
    }


    /**
     * 한국어판(0.93-kr6): 이 항목 보기가 내 QSO 로그 목록에 속해 있는지 확인한다.
     *
     * @param view 메뉴를 연 항목 보기
     * @return 내 목록의 항목이면 true
     */
    private boolean isMyListItemView(View view) {
        if (view == null || binding == null) {
            return false;
        }
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == binding.logRecyclerView) {
                return true;
            }
            parent = parent instanceof View ? ((View) parent).getParent() : null;
        }
        return false;
    }

    /**
     * 한국어판(0.93-kr8): 호출 탭 2분할의 QSO 로그 패널이 다시 보일 때 부른다.
     * 다른 화면에서 검색어(호출부호)가 바뀌었을 수 있고, 숨어 있는 동안 새 교신이나 지운 기록이
     * 생겼을 수 있으므로 보일 때마다 지금 조건으로 목록을 한 번 다시 읽는다.
     */
    public void refreshLogPanel() {
        liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
        refreshLogList();
    }

    /**
     * 한국어판(0.93-kr8): DB에 새 교신이 저장됐을 때(자동 교신 완료·웹 UI 업로드·공유 로그 가져오기)
     * 목록을 바로 다시 읽어 화면에 나타낸다. 화면에 보이지 않을 때는 읽지 않고,
     * 다시 보일 때 refreshLogPanel() 이 맞춘다.
     */
    private void onQsoLogUpdated() {
        if (binding == null || !isAdded()) {
            return;
        }
        View view = getView();
        if (view == null || !view.isShown() || !isResumed()) {
            return;
        }
        liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
        liveRefreshHandler.postDelayed(liveRefreshRunnable, LIVE_REFRESH_DELAY_MS);
    }

    /**
     * 한국어판(0.93-kr8): 지금 검색 조건으로 목록을 처음(offset 0)부터 다시 읽는다.
     * 로그 탭과 호출 탭 패널은 보기 방식(연락처/기록)을 함께 쓰므로 보기 방식도 맞춘다.
     */
    private void refreshLogList() {
        if (binding == null || !isAdded()) {
            return;
        }
        setShowStyle();
        queryByCallsign(mainViewModel.queryKey, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        //한국어판(0.93-kr8): 앱을 다시 열거나 다른 화면에서 돌아오면 보고 있는 목록을 최신으로 맞춘다.
        //  화면을 처음 만들면서(onCreateView) 방금 읽었으면 건너뛴다.
        if (binding != null && !loading
                && System.currentTimeMillis() - lastQueryTimeMs > JUST_OPENED_GRACE_MS) {
            liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
            liveRefreshHandler.postDelayed(liveRefreshRunnable, LIVE_REFRESH_DELAY_MS);
        }
    }

    /**
     * 设置显示模式。通联的呼号和日志两种表现方式
     */
    @SuppressLint("NotifyDataSetChanged")
    private void setShowStyle() {

        if (mainViewModel.logListShowCallsign) {
            binding.logViewStyleimageButton.setImageResource(R.drawable.ic_baseline_assignment_ind_24);
            binding.logRecyclerView.setAdapter(logCallsignAdapter);
            logCallsignAdapter.notifyDataSetChanged();
            //한국어판(0.93-kr8): [PSK] 버튼은 두 보기 모두에서 쓴다(예전 지도 버튼과 달리 숨기지 않는다)
        } else {
            binding.logViewStyleimageButton.setImageResource(R.drawable.ic_baseline_assignment_24);
            binding.logRecyclerView.setAdapter(logQSLAdapter);
            logQSLAdapter.notifyDataSetChanged();
        }

    }

    /**
     * 查询日志
     *
     * @param callsign 呼号
     */
    private void queryByCallsign(String callsign, int offset) {
        loading = true;//开始读数据
        lastQueryTimeMs = System.currentTimeMillis();
        //分两种查询
        if (mainViewModel.logListShowCallsign) {
            if (offset == 0) {//说明是新增记录
                logCallsignAdapter.clearRecords();//清空记录
            }

            mainViewModel.databaseOpr.getQSLCallsignsByCallsign(false, offset, callsign, mainViewModel.queryFilter
                    , new OnQueryQSLCallsign() {
                        @Override
                        public void afterQuery(ArrayList<QSLCallsignRecord> records) {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    logCallsignAdapter.setQSLCallsignList(records);
                                    loading = false;
                                }
                            });
                        }
                    });
        } else {
            if (offset == 0) {//说明是新增记录
                logQSLAdapter.clearRecords();
            }
            mainViewModel.databaseOpr.getQSLRecordByCallsign(false, offset, callsign, mainViewModel.queryFilter
                    , new OnQueryQSLRecordCallsign() {
                        @Override
                        public void afterQuery(ArrayList<QSLRecordStr> records) {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    logQSLAdapter.setQSLList(records);
                                    loading = false;
                                }
                            });
                        }
                    });

        }
    }


    /**
     * 한국어판(0.93-kr8): [PSK] 버튼 - 내 콜사인·그리드를 보여 주고,
     * PSK Reporter 지도(내 신호를 받은 곳)와 내 교신 지도로 이어 준다.
     * 인터넷이 없는 IC-705 와이파이에 물려 있어도 안내만 하고 앱이 꺼지지 않는다.
     */
    private void showPskGridDialog() {
        if (binding == null || !isAdded()) {
            return;
        }
        final String callsign = getMyCallsignForPsk();
        String grid6 = safeString(GeneralVariables.getMyMaidenheadGrid());
        String grid4 = safeString(GeneralVariables.getMyMaidenhead4Grid());
        String gridText = grid6;
        if (!grid4.isEmpty() && !grid4.equalsIgnoreCase(grid6)) {
            gridText = grid6.isEmpty() ? grid4 : String.format(Locale.US, "%s (%s)", grid6, grid4);
        }
        if (gridText.isEmpty()) {
            gridText = "-";
        }

        StringBuilder message = new StringBuilder();
        if (callsign.isEmpty()) {
            message.append(getStringFromRes(R.string.psk_grid_no_callsign));
        } else {
            message.append(String.format(getStringFromRes(R.string.psk_grid_info)
                    , callsign, gridText));
        }
        if (!hasInternet()) {
            //인터넷이 없어도 여기서 막지 않고, 왜 지도가 안 열리는지 먼저 알려 준다
            message.append("\n\n").append(getStringFromRes(R.string.psk_grid_no_internet));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getStringFromRes(R.string.psk_grid_title));
        builder.setMessage(message.toString());
        builder.setPositiveButton(getStringFromRes(R.string.psk_reporter_open)
                , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openPskReporter(callsign);
                    }
                });
        builder.setNeutralButton(getStringFromRes(R.string.psk_grid_qso_map)
                , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openQsoMap();
                    }
                });
        builder.setNegativeButton(getStringFromRes(R.string.close), null);
        builder.show();
    }

    /**
     * 한국어판(0.93-kr8): 지금 설정된 내 콜사인. 설정에서 바꾸면 그 값을 그대로 쓴다.
     *
     * @return 대문자 콜사인(없으면 빈 문자열)
     */
    private String getMyCallsignForPsk() {
        String callsign = safeString(GeneralVariables.myCallsign).trim();
        return callsign.toUpperCase(Locale.US);
    }

    /**
     * 한국어판(0.93-kr8): PSK Reporter 지도를 브라우저로 연다.
     * 브라우저가 없거나 열지 못해도 앱이 꺼지지 않게 예외를 잡는다.
     *
     * @param callsign 내 콜사인
     */
    private void openPskReporter(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            showHelpMessage(getStringFromRes(R.string.psk_grid_no_callsign));
            return;
        }
        String url = String.format(PSK_REPORTER_MAP_URL, Uri.encode(callsign));
        openUrl(url, getStringFromRes(R.string.psk_grid_open_failed)
                + "\n\n" + getStringFromRes(R.string.psk_grid_no_internet));
    }

    /**
     * 한국어판(0.93-kr8): 내 교신 지도(GridTracker)를 연다. 예전 지도 버튼과 같은 화면이다.
     */
    private void openQsoMap() {
        try {
            Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
            intent.putExtra("qslAll", mainViewModel.queryKey);
            intent.putExtra("queryFilter", mainViewModel.queryFilter);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openQsoMap: 지도 화면을 열지 못했습니다. " + e.getMessage());
            showHelpMessage(getStringFromRes(R.string.psk_grid_open_failed));
        }
    }

    /**
     * 한국어판(0.93-kr8): 웹 주소를 브라우저로 연다. 실패하면 안내 창을 띄운다.
     *
     * @param url        열 주소
     * @param failMessage 열지 못했을 때 보여 줄 문구
     */
    private void openUrl(String url, String failMessage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openUrl: 브라우저를 열지 못했습니다. " + e.getMessage());
            showHelpMessage(failMessage);
        }
    }

    /**
     * 한국어판(0.93-kr8): 지금 인터넷이 잡히는지 본다. (IC-705 와이파이는 인터넷이 없다)
     * 권한이 없거나 확인할 수 없으면 '있다'로 보아 열기를 막지 않는다.
     *
     * @return 인터넷이 잡히면 true
     */
    private boolean hasInternet() {
        try {
            ConnectivityManager manager = (ConnectivityManager) requireContext()
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) {
                return true;
            }
            Network network = manager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            Log.e(TAG, "hasInternet: 인터넷 상태를 확인하지 못했습니다. " + e.getMessage());
            return true;
        }
    }

    /**
     * 한국어판(0.93-kr8): 긴 안내 문구를 도움말 창으로 보여 준다(브라우저를 열지 못했을 때 등).
     *
     * @param message 보여 줄 문구
     */
    private void showHelpMessage(String message) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        try {
            new HelpDialog(requireContext(), requireActivity(), message, false).show();
        } catch (Exception e) {
            Log.e(TAG, "showHelpMessage: 안내 창을 띄우지 못했습니다. " + e.getMessage());
            ToastMessage.show(message);
        }
    }

    /**
     * 한국어판(0.93-kr8): 문자열 리소스를 안전하게 읽는다.
     *
     * @param resId 문자열 리소스
     * @return 문자열(없으면 빈 문자열)
     */
    private String getStringFromRes(int resId) {
        try {
            return GeneralVariables.getStringFromResource(resId);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 한국어판(0.93-kr8): null 을 빈 문자열로 바꾼다.
     *
     * @param value 값
     * @return 빈 문자열이거나 원래 문자열
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 显示统计页面
     */
    private void showCountFragment() {
        //用于Fragment的导航。
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        navHostFragment.getNavController().navigate(R.id.countFragment);
    }

    /**
     * 显示QRZ查询界面
     *
     * @param callsign 呼号
     */
    private void showQrzFragment(String callsign) {
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        assert navHostFragment != null;//断言不为空
        Bundle bundle = new Bundle();
        bundle.putString(QRZ_Fragment.CALLSIGN_PARAM, callsign);
        navHostFragment.getNavController().navigate(R.id.QRZ_Fragment, bundle);
    }


    /**
     * 获取本机IP地址
     *
     * @return IP 地址
     */
    @Nullable
    private String getLocalIp() {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress == 0) {
            return null;
        }
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "." + (ipAddress >> 16 & 0xff)
                + "." + (ipAddress >> 24 & 0xff));
    }

    @Override
    public void onDestroy() {
        //한국어판(0.93-kr8): 미뤄 둔 목록 새로 고침이 화면이 사라진 뒤에 돌지 않게 지운다
        liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
        //判断生成共享log线程是否还在工作，如果还在工作，要销毁对话框，防止出现not attached to window manager错误
        if (Boolean.TRUE.equals(mainViewModel.mutableShareRunning.getValue())) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }

        super.onDestroy();
    }
}