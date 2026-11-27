package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.databinding.FragmentGridPskBinding;
import com.bg7yoz.ft8cn.grid_tracker.GridTrackerMainActivity;

import java.util.Locale;

/**
 * 한국어판(0.93-kr9): 하단 '그리드·PSK' 탭.
 *
 * <p>태블릿에서 한 화면으로 내 위치 정보를 확인하고 전파 상황을 보는 화면이다.
 * <ul>
 *     <li>위쪽 카드: 내 콜사인({@link GeneralVariables#myCallsign}) · 내 그리드(6자리/4자리) · 밴드/주파수</li>
 *     <li>PSK Reporter 전파 지도: 인터넷이 있으면 앱 안(WebView)에서 바로 열고,
 *     없으면 안내 문구와 [다시 시도] 버튼을 보여 준다. 브라우저로도 열 수 있다.</li>
 *     <li>내 교신 지도(GridTracker) · 교신 통계 · ADIF 공유 · QSO 로그 목록 보기</li>
 * </ul>
 * 스마트폰에서도 기존 QSO 로그를 잃지 않도록, 같은 화면 안에서 로그 목록으로 바꿔 볼 수 있다.
 *
 * @author BG7YOZ (한국어판 0.93-kr9)
 */
public class GridPskFragment extends Fragment {
    private static final String TAG = "GridPskFragment";

    //한국어판(0.93-kr9): PSK Reporter 실시간 지도 주소(내 콜사인으로 내 신호를 받은 곳을 본다)
    private static final String PSK_REPORTER_MAP_URL
            = "https://pskreporter.info/pskmap.html?preset&callsign=%1$s&txrx=tx&timerange=86400";
    private static final String FRAGMENT_TAG_GRID_LOG = "gridPskLog";

    private FragmentGridPskBinding binding;
    private MainViewModel mainViewModel;

    private LogFragment logFragment;
    private boolean logShown = false;
    private boolean openLogOnStart = false;
    private boolean mapRequested = false;
    private String lastLoadedUrl = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = MainViewModel.getInstance(this);
        //한국어판(0.93-kr9): 디코드·호출 목록에서 '이 국의 교신 기록 보기'로 들어오면
        //  그리드·PSK 화면 대신 그 화면의 QSO 로그 목록을 바로 보여 준다
        Bundle args = getArguments();
        openLogOnStart = args != null && args.getBoolean("showLog", false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        binding = FragmentGridPskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //한국어판(0.93-kr9): 지난번에 로그 목록을 보고 있었거나, 목록에서 이 국의 교신 기록을
        //  보러 들어왔으면(QSO 로그 목록을 바로 연다) 화면이 다 그려진 뒤에 붙인다
        final boolean startWithLog = openLogOnStart || (savedInstanceState != null
                && savedInstanceState.getBoolean("gridPskLogShown", false));
        openLogOnStart = false;
        if (startWithLog) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (binding != null) {
                        showLogPanel(true);
                    }
                }
            });
        }

        binding.gridSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        binding.logToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogPanel(!logShown);
            }
        });

        binding.browserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPskReporter(getMyCallsign());
            }
        });

        binding.qsoMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openQsoMap();
            }
        });

        binding.statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCountFragment();
            }
        });

        binding.adifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareAdif();
            }
        });

        binding.pskRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPskMap(true);
            }
        });

        initWebView();
        updateHeader();

        //설정에서 콜사인·그리드를 바꾸거나 밴드를 옮기면 위쪽 카드도 따라 바뀐다
        GeneralVariables.mutableMyMaidenheadGrid.observe(getViewLifecycleOwner()
                , new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        updateHeader();
                    }
                });
        GeneralVariables.mutableBandChange.observe(getViewLifecycleOwner()
                , new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                        updateHeader();
                    }
                });
        mainViewModel.timerSec.observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                //주파수는 밴드 안에서도 조금씩 움직이므로, 글자가 달라졌을 때만 다시 쓴다
                updateBandText();
            }
        });

        loadPskMap(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeader();
        if (binding != null && binding.pskMapWebView != null && mapRequested) {
            binding.pskMapWebView.onResume();
        }
        if (logShown) {
            refreshLogPanel();
        }
    }

    @Override
    public void onPause() {
        if (binding != null && binding.pskMapWebView != null) {
            binding.pskMapWebView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("gridPskLogShown", logShown);
    }

    @Override
    public void onDestroyView() {
        //WebView 는 화면이 사라질 때 확실히 정리한다(메모리·소리 누수 방지)
        if (binding != null && binding.pskMapWebView != null) {
            try {
                binding.pskMapWebView.stopLoading();
                binding.pskMapWebView.loadUrl("about:blank");
                binding.pskMapWebView.setWebChromeClient(null);
                binding.pskMapWebView.setWebViewClient(null);
                binding.pskMapWebView.removeAllViews();
                binding.pskMapWebView.destroy();
            } catch (Exception e) {
                Log.e(TAG, "onDestroyView: WebView 를 정리하지 못했습니다. " + e.getMessage());
            }
        }
        mapRequested = false;
        super.onDestroyView();
        binding = null;
    }

    //--------------------------------------------------------------------------
    // 위쪽 카드(내 콜사인 · 그리드 · 밴드)

    /**
     * 한국어판(0.93-kr9): 위쪽 카드에 내 콜사인·그리드·밴드/주파수를 다시 쓴다.
     * 콜사인이 비어 있으면 설정 안내와 [설정 열기] 버튼을 보여 준다.
     */
    private void updateHeader() {
        if (binding == null) {
            return;
        }
        String callsign = getMyCallsign();
        if (callsign.isEmpty()) {
            binding.myCallsignText.setText(getStringFromRes(R.string.grid_psk_no_callsign_short));
            binding.gridSettingsButton.setVisibility(View.VISIBLE);
        } else {
            binding.myCallsignText.setText(callsign);
            binding.gridSettingsButton.setVisibility(View.GONE);
        }

        String grid6 = safeString(GeneralVariables.getMyMaidenheadGrid()).trim();
        String grid4 = safeString(GeneralVariables.getMyMaidenhead4Grid()).trim();
        String gridText;
        if (grid6.isEmpty()) {
            gridText = grid4.isEmpty() ? "-" : grid4;
        } else if (grid4.isEmpty() || grid4.equalsIgnoreCase(grid6)) {
            gridText = grid6;
        } else {
            gridText = String.format(Locale.US, "%s (%s)", grid6, grid4);
        }
        binding.myGridText.setText(String.format(getStringFromRes(R.string.grid_psk_grid_value)
                , gridText));

        updateBandText();
    }

    /**
     * 한국어판(0.93-kr9): 밴드·주파수 글자만 다시 쓴다(바뀐 것이 없으면 그대로 둔다).
     */
    private void updateBandText() {
        if (binding == null) {
            return;
        }
        String band = safeString(GeneralVariables.getBandString());
        if (!band.contentEquals(binding.bandFreqText.getText())) {
            binding.bandFreqText.setText(band);
        }
    }

    /**
     * 한국어판(0.93-kr9): 설정된 내 콜사인(대문자). 없으면 빈 문자열.
     *
     * @return 내 콜사인
     */
    private String getMyCallsign() {
        return safeString(GeneralVariables.myCallsign).trim().toUpperCase(Locale.US);
    }

    //--------------------------------------------------------------------------
    // PSK Reporter 지도(WebView)

    /**
     * 한국어판(0.93-kr9): PSK Reporter 지도를 앱 안에서 열 수 있게 WebView 를 준비한다.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebView webView = binding.pskMapWebView;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (binding == null) {
                    return;
                }
                //지도가 떴으면 안내 문구는 감춘다
                binding.pskOfflineLayout.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request
                    , WebResourceError error) {
                //바깥 틀(첫 화면)을 못 열었을 때만 안내를 띄운다(지도 안의 잔글자는 무시)
                if (request != null && !request.isForMainFrame()) {
                    return;
                }
                showOfflineMessage();
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description
                    , String failingUrl) {
                showOfflineMessage();
            }
        });
    }

    /**
     * 한국어판(0.93-kr9): PSK Reporter 지도를 (다시) 연다.
     * 인터넷이 없으면 지도를 여는 대신 안내 문구와 [다시 시도] 버튼을 보여 준다.
     *
     * @param forceAgain 이미 열어 둔 지도라도 새로 읽을지(true)
     */
    private void loadPskMap(boolean forceAgain) {
        if (binding == null) {
            return;
        }
        String callsign = getMyCallsign();
        if (callsign.isEmpty()) {
            binding.pskOfflineLayout.setVisibility(View.VISIBLE);
            binding.pskOfflineText.setText(getStringFromRes(R.string.psk_grid_no_callsign));
            return;
        }
        String url = String.format(PSK_REPORTER_MAP_URL, Uri.encode(callsign));
        if (!forceAgain && url.equals(lastLoadedUrl) && mapRequested) {
            return;//같은 주소로 이미 열어 두었으면 그대로 둔다
        }
        if (!hasInternet()) {
            showOfflineMessage();
            return;
        }
        lastLoadedUrl = url;
        mapRequested = true;
        binding.pskOfflineLayout.setVisibility(View.GONE);
        try {
            binding.pskMapWebView.loadUrl(url);
        } catch (Exception e) {
            Log.e(TAG, "loadPskMap: 지도를 열지 못했습니다. " + e.getMessage());
            showOfflineMessage();
        }
    }

    /**
     * 한국어판(0.93-kr9): 지도를 열지 못했을 때의 안내를 띄운다.
     */
    private void showOfflineMessage() {
        if (binding == null) {
            return;
        }
        mapRequested = false;
        binding.pskOfflineLayout.setVisibility(View.VISIBLE);
        binding.pskOfflineText.setText(getStringFromRes(R.string.grid_psk_map_offline));
    }

    /**
     * 한국어판(0.93-kr9): PSK Reporter 지도를 브라우저로 연다.
     *
     * @param callsign 내 콜사인
     */
    private void openPskReporter(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            ToastMessage.show(getStringFromRes(R.string.psk_grid_no_callsign));
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW
                    , Uri.parse(String.format(PSK_REPORTER_MAP_URL, Uri.encode(callsign))));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openPskReporter: 브라우저를 열지 못했습니다. " + e.getMessage());
            ToastMessage.show(getStringFromRes(R.string.psk_grid_open_failed));
        }
    }

    /**
     * 한국어판(0.93-kr9): 지금 인터넷이 잡히는지 본다. (IC-705 와이파이는 인터넷이 없다)
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

    //--------------------------------------------------------------------------
    // 내 교신 지도 · 통계 · ADIF · QSO 로그

    /**
     * 한국어판(0.93-kr9): 내 교신 지도(GridTracker)를 연다.
     */
    private void openQsoMap() {
        try {
            Intent intent = new Intent(requireContext(), GridTrackerMainActivity.class);
            intent.putExtra("qslAll", mainViewModel.queryKey);
            intent.putExtra("queryFilter", mainViewModel.queryFilter);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openQsoMap: 지도 화면을 열지 못했습니다. " + e.getMessage());
            ToastMessage.show(getStringFromRes(R.string.psk_grid_open_failed));
        }
    }

    /**
     * 한국어판(0.93-kr9): 교신 통계 화면을 연다.
     */
    private void showCountFragment() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                    .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
            if (navHostFragment == null) {
                return;
            }
            navHostFragment.getNavController().navigate(R.id.countFragment);
        } catch (Exception e) {
            Log.e(TAG, "showCountFragment: 통계 화면을 열지 못했습니다. " + e.getMessage());
        }
    }

    /**
     * 한국어판(0.93-kr9): 지금 조건의 교신 기록을 ADIF 파일로 만들어 공유한다.
     * 로그 목록(LogFragment)이 하는 일과 같으므로 그 화면에 그대로 맡긴다.
     */
    private void shareAdif() {
        LogFragment fragment = ensureLogFragment();
        if (fragment == null) {
            ToastMessage.show(getStringFromRes(R.string.psk_grid_open_failed));
            return;
        }
        fragment.shareAdifFile();
    }

    /**
     * 한국어판(0.93-kr9): 그리드·PSK 화면과 QSO 로그 목록을 바꿔 보여 준다.
     *
     * @param showLog QSO 로그 목록을 보여 주려면 true
     */
    private void showLogPanel(boolean showLog) {
        if (binding == null) {
            return;
        }
        logShown = showLog;
        if (showLog) {
            ensureLogFragment();
        }
        binding.logPanelLayout.setVisibility(showLog ? View.VISIBLE : View.GONE);
        binding.logToggleButton.setText(showLog
                ? getStringFromRes(R.string.grid_psk_show_map)
                : getStringFromRes(R.string.grid_psk_show_log));
        if (showLog) {
            refreshLogPanel();
            if (binding.pskMapWebView != null) {
                binding.pskMapWebView.onPause();
            }
        } else {
            loadPskMap(false);
            if (binding.pskMapWebView != null && mapRequested) {
                binding.pskMapWebView.onResume();
            }
        }
    }

    /**
     * 한국어판(0.93-kr9): QSO 로그 목록(LogFragment)을 처음 쓸 때 붙인다.
     * 한 번 붙이면 화면을 오가도 살아 있어 검색 조건과 목록이 그대로 남는다.
     *
     * @return 로그 목록 프래그먼트(붙이지 못하면 null)
     */
    @Nullable
    private LogFragment ensureLogFragment() {
        if (logFragment != null && logFragment.isAdded()) {
            return logFragment;
        }
        if (binding == null) {
            return logFragment;
        }
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment existing = fragmentManager.findFragmentByTag(FRAGMENT_TAG_GRID_LOG);
        if (existing instanceof LogFragment) {
            logFragment = (LogFragment) existing;
            return logFragment;
        }
        try {
            logFragment = new LogFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.gridLogContainer, logFragment, FRAGMENT_TAG_GRID_LOG)
                    .commitNowAllowingStateLoss();
            return logFragment;
        } catch (Exception e) {
            Log.e(TAG, "ensureLogFragment: 로그 목록을 붙이지 못했습니다. " + e.getMessage());
            logFragment = null;
            return null;
        }
    }

    /**
     * 한국어판(0.93-kr9): 로그 목록을 지금 검색 조건으로 다시 읽는다.
     */
    private void refreshLogPanel() {
        if (logFragment instanceof LogFragment) {
            logFragment.refreshLogPanel();
        }
    }

    /**
     * 한국어판(0.93-kr9): 설정 화면(내 콜사인·그리드)으로 간다.
     */
    private void openSettings() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                    .getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
            if (navHostFragment == null) {
                return;
            }
            navHostFragment.getNavController().navigate(R.id.menu_nav_config);
        } catch (Exception e) {
            Log.e(TAG, "openSettings: 설정 화면을 열지 못했습니다. " + e.getMessage());
        }
    }

    /**
     * 한국어판(0.93-kr9): 문자열 리소스를 안전하게 읽는다.
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
     * 한국어판(0.93-kr9): null 을 빈 문자열로 바꾼다.
     *
     * @param value 값
     * @return 빈 문자열이거나 원래 문자열
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
