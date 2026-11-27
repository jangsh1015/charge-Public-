#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""0.93-kr9 변경(스펙트럼 3분할 디코드 칸 복구 · 도구 버튼은 스펙트럼 화면에서만 ·
하단 QSO 로그 탭 → 그리드·PSK 전용 화면)에 대한 추가 정적 검증.

check_source.py · check_kr4 · check_kr6 · check_kr7 · check_kr8 이 보지 않는 것들을 확인한다.
  1) 버전 표기가 0.93-kr9 (versionCode 9 이상)이고 문서·readme 에 내역이 맨 위에 있는지
  2) 3분할 레이아웃 세 변형: 오른쪽 열이 [디코드 / 경계선 / 호출] 세 자식이고,
     무게가 코드가 쓰는 소수 비율(0.5/0.5, 0.55/0.45)로 맞는지, kr8 의 전환 탭 흔적이 없는지
  3) MainActivity: 3분할을 열 때 두 칸을 되살리고 비율을 다시 맡기는지(디코드 칸 자가 복구)
  4) 도구 버튼(floatView)이 스펙트럼 화면에서만 보이는지
  5) 그리드·PSK 화면: 내 콜사인·그리드·밴드 카드, PSK Reporter 지도(WebView)·재시도·브라우저 열기,
     내 교신 지도·통계·ADIF 공유·QSO 로그 보기, 하단 탭 이름·항법 그래프·문자열·인터넷 권한
  6) 피클·AI 언급이 사용자 문서에 들어가지 않았는지
"""
import io
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "ft8cn", "app", "src", "main")
RES = os.path.join(APP, "res")
JAVA = os.path.join(APP, "java", "com", "bg7yoz", "ft8cn")
A = "{http://schemas.android.com/apk/res/android}"

errors = []


def err(msg):
    errors.append(msg)


def read(path):
    return io.open(path, encoding="utf-8").read()


def strip_comments(txt):
    txt = re.sub(r"//[^\n]*", "", txt)
    return re.sub(r"/\*.*?\*/", "", txt, flags=re.S)


# --- 1) 버전·문서 -----------------------------------------------------------
gradle = read(os.path.join(ROOT, "ft8cn", "app", "build.gradle"))
m = re.search(r"versionCode\s+(\d+)", gradle)
if not m or int(m.group(1)) < 9:
    err("build.gradle versionCode 가 9 이상이 아님 (%s)" % (m.group(1) if m else "없음"))
m = re.search(r"versionName\s+'(0\.93-kr(\d+))'", gradle)
if not m or int(m.group(2)) < 9:
    err("build.gradle versionName 이 0.93-kr9 이상이 아님")

DOCS = [("readme_ko.txt", os.path.join(APP, "assets", "readme_ko.txt")),
        ("readme_en.txt", os.path.join(APP, "assets", "readme_en.txt")),
        ("README_KR.md", os.path.join(ROOT, "README_KR.md"))]
for name, path in DOCS:
    if "0.93-kr9" not in read(path):
        err("%s 에 0.93-kr9 내역이 없음" % name)

for name in ["readme_ko.txt", "readme_en.txt"]:
    txt = read(os.path.join(APP, "assets", name))
    versions = re.findall(r"^\s{2}(0\.93-kr(\d))\s*$", txt, flags=re.M)
    if not versions:
        err("%s 에서 버전 목록을 찾지 못함" % name)
    elif versions[0][0] != "0.93-kr9":
        err("%s 의 첫 버전이 0.93-kr9 가 아님 (%s)" % (name, versions[0][0]))

# --- 2) 3분할 레이아웃 -------------------------------------------------------
expect_weights = {
    "@+id/splitSpectrumContainer": "0.55",
    "@+id/splitRightColumn": "0.45",
    "@+id/splitDecodeContainer": "0.5",
    "@+id/splitCallingContainer": "0.5",
    "@+id/splitCallingOnlyContainer": "0.5",
    "@+id/splitLogContainer": "0.5",
}
for variant in ["layout", "layout-land", "layout-sw600dp-land"]:
    p = os.path.join(RES, variant, "main_activity.xml")
    if not os.path.exists(p):
        err("%s/main_activity.xml 없음" % variant)
        continue
    txt = read(p)
    for gone in ["splitCallingToggleBar", "splitCallingBody", "splitCallingTabCall",
                 "splitCallingTabLog", "splitCallingLogContainer"]:
        if ("@+id/" + gone) in txt:
            err("%s/main_activity.xml 에 걷어 낸 %s 가 남아 있음" % (variant, gone))
    try:
        root = ET.fromstring(txt)
    except Exception as e:  # noqa: BLE001
        err("%s/main_activity.xml XML 오류: %s" % (variant, e))
        continue
    elements = {}
    for el in root.iter():
        elements.setdefault(el.get(A + "id"), el)

    right = elements.get("@+id/splitRightColumn")
    if right is None:
        err("%s: splitRightColumn 을 찾지 못함" % variant)
    else:
        kids = [k.get(A + "id") for k in list(right)]
        want = ["@+id/splitDecodeContainer", "@+id/splitHorizontalHandle",
                "@+id/splitCallingContainer"]
        if kids != want:
            err("%s: 오른쪽 열의 자식이 %s 이어야 함 (실제: %s)" % (variant, want, kids))
        decode = elements.get("@+id/splitDecodeContainer")
        calling = elements.get("@+id/splitCallingContainer")
        for name, el in [("splitDecodeContainer", decode), ("splitCallingContainer", calling)]:
            if el is None:
                continue
            if el.get(A + "layout_height") != "0dp":
                err("%s: %s 의 높이는 0dp 여야 함 (%s)"
                    % (variant, name, el.get(A + "layout_height")))

    for key, weight in expect_weights.items():
        el = elements.get(key)
        if el is None:
            err("%s: %s 를 찾지 못함" % (variant, key))
            continue
        got = el.get(A + "layout_weight")
        if got != weight:
            err("%s: %s 의 layout_weight 가 %s 여야 함 (실제: %s)"
                % (variant, key, weight, got))

# --- 3) MainActivity: 3분할 복구 --------------------------------------------
main_code = strip_comments(read(os.path.join(JAVA, "MainActivity.java")))
for need, why in [
    ("private void showSplitDecodeAndCalling()", "3분할 두 칸을 되살리는 함수가 없음"),
    ("private void ensureSplitDecodeHeight()", "디코드 칸 높이 확인 함수가 없음"),
]:
    if need not in main_code:
        err("MainActivity: " + why)

m = re.search(r"private void showSplitDecodeAndCalling\(\)\s*\{([\s\S]{0,900}?)\n    \}", main_code)
if not m:
    err("MainActivity.showSplitDecodeAndCalling 본문을 찾지 못함")
else:
    body = m.group(1)
    for need, why in [
        ("splitDecodeContainer.setVisibility(View.VISIBLE)", "디코드 칸을 보이게 하지 않음"),
        ("splitCallingContainer.setVisibility(View.VISIBLE)", "호출 칸을 보이게 하지 않음"),
        ("applySplitRatio()", "높이 비율을 다시 맡기지 않음"),
        ("ensureSplitDecodeHeight()", "디코드 칸 높이를 확인하지 않음"),
    ]:
        if need not in body:
            err("MainActivity.showSplitDecodeAndCalling: " + why)

m = re.search(r"private void ensureSplitDecodeHeight\(\)\s*\{([\s\S]{0,1600}?)\n    \}", main_code)
if not m:
    err("MainActivity.ensureSplitDecodeHeight 본문을 찾지 못함")
else:
    body = m.group(1)
    for need, why in [
        ("splitDecodeContainer.getHeight()", "디코드 칸의 실제 높이를 읽지 않음"),
        ("DEFAULT_SPLIT_HEIGHT_RATIO", "접혔을 때 기본 비율로 되돌리지 않음"),
        ("requestLayout()", "다시 배치하지 않음"),
    ]:
        if need not in body:
            err("MainActivity.ensureSplitDecodeHeight: " + why)

m = re.search(r"private void applySplitRatio\(\)\s*\{([\s\S]{0,1400}?)\n    \}", main_code)
if not m:
    err("MainActivity.applySplitRatio 본문을 찾지 못함")
else:
    body = m.group(1)
    for need, why in [
        ("setSplitWeight(splitDecodeContainer, splitHeightRatio", "디코드 칸 비율을 맡기지 않음"),
        ("setSplitWeight(splitCallingContainer, 1f - splitHeightRatio", "호출 칸 비율을 맡기지 않음"),
    ]:
        if need not in body:
            err("MainActivity.applySplitRatio: " + why)
    # 두 칸을 항상 함께 맞춰야 단위가 어긋나지 않는다(0.93-kr8 의 디코드 칸 접힘 원인)
    if "splitCallingBody" in body:
        err("MainActivity.applySplitRatio: 걷어 낸 splitCallingBody 를 아직 씀")

m = re.search(r"private void setSplitWeight\(View view, float weight, boolean horizontal\)"
              r"\s*\{([\s\S]{0,1400}?)\n    \}", main_code)
if not m:
    err("MainActivity.setSplitWeight(View, float, boolean) 를 찾지 못함")
else:
    body = m.group(1)
    for need, why in [
        ("params.height = 0", "세로 칸의 길이를 0dp 로 맞추지 않음"),
        ("params.width = 0", "가로 칸의 길이를 0dp 로 맞추지 않음"),
    ]:
        if need not in body:
            err("MainActivity.setSplitWeight: " + why)

if "showSplitDecodeAndCalling()" not in main_code:
    err("MainActivity: 3분할을 보여 줄 때 디코드 칸을 되살리지 않음")

# --- 4) 도구 버튼은 스펙트럼 화면에서만 -------------------------------------
if "private void updateFloatViewVisibility(int destinationId)" not in main_code:
    err("MainActivity: 도구 버튼 가시성 함수(updateFloatViewVisibility)가 없음")
m = re.search(r"private void updateFloatViewVisibility\(int destinationId\)\s*\{([\s\S]{0,1600}?)\n    \}",
              main_code)
if m:
    body = m.group(1)
    if "R.id.menu_nav_spectrum" not in body:
        err("MainActivity.updateFloatViewVisibility: 스펙트럼 탭 판단이 없음")
    if "View.GONE" not in body:
        err("MainActivity.updateFloatViewVisibility: 다른 화면에서 감추지 않음")
    if "setVisibility(View.VISIBLE)" not in body:
        err("MainActivity.updateFloatViewVisibility: 스펙트럼 화면에서 보이게 하지 않음")
if "updateFloatViewVisibility(destinationId)" not in main_code:
    err("MainActivity.updateSplitVisibility: 화면을 바꿀 때 도구 버튼 가시성을 맞추지 않음")

# --- 5) 그리드·PSK 화면 ------------------------------------------------------
grid_java_path = os.path.join(JAVA, "ui", "GridPskFragment.java")
if not os.path.exists(grid_java_path):
    err("ui/GridPskFragment.java 가 없음")
else:
    raw_grid = read(grid_java_path)
    grid = strip_comments(raw_grid)
    for need, why in [
        ("public class GridPskFragment extends Fragment", "프래그먼트 선언이 없음"),
        ("GeneralVariables.myCallsign", "내 콜사인을 설정값에서 읽지 않음"),
        ("GeneralVariables.getMyMaidenheadGrid()", "내 그리드(6자리)를 읽지 않음"),
        ("GeneralVariables.getMyMaidenhead4Grid()", "내 그리드(4자리)를 읽지 않음"),
        ("GeneralVariables.getBandString()", "밴드·주파수를 읽지 않음"),
        ("WebView", "PSK Reporter 지도를 앱 안에서 열지 않음"),
        ("loadUrl", "지도 주소를 읽어 들이지 않음"),
        ("pskRetryButton", "다시 시도 버튼을 연결하지 않음"),
        ("pskOfflineLayout", "인터넷이 없을 때의 안내를 띄우지 않음"),
        ("Intent.ACTION_VIEW", "브라우저로 열기가 없음"),
        ("GridTrackerMainActivity", "내 교신 지도로 가는 길이 없음"),
        ("R.id.countFragment", "교신 통계 화면으로 가는 길이 없음"),
        ("shareAdifFile()", "ADIF 공유를 로그 화면에 맡기지 않음"),
        ("R.id.gridLogContainer", "QSO 로그 목록 자리가 없음"),
        ("new LogFragment()", "QSO 로그 목록을 붙이지 않음"),
        ("refreshLogPanel()", "QSO 로그 목록을 다시 읽지 않음"),
        ("destroy()", "WebView 를 화면이 사라질 때 정리하지 않음"),
        ("hasInternet()", "인터넷 확인이 없음"),
    ]:
        if need not in grid:
            err("GridPskFragment: " + why)
    m = re.search(r"PSK_REPORTER_MAP_URL\s*(?:\n\s*)?=\s*\"([^\"]+)\"", raw_grid)
    if not m:
        err("GridPskFragment: PSK_REPORTER_MAP_URL 상수를 찾지 못함")
    else:
        url = m.group(1)
        for part in ["pskreporter.info/pskmap.html", "callsign=%1$s", "txrx=tx", "timerange=86400"]:
            if part not in url:
                err("GridPskFragment: PSK Reporter 주소에 %s 가 없음 (%s)" % (part, url))
    if "R.id.menu_nav_config" not in grid:
        err("GridPskFragment: 설정 화면으로 가는 길이 없음")
    # 로그 화면에 공개 함수가 있어야 [ADIF 공유] 가 동작한다
    logf = strip_comments(read(os.path.join(JAVA, "ui", "LogFragment.java")))
    if "public void shareAdifFile()" not in logf:
        err("LogFragment: ADIF 공유 공개 함수(shareAdifFile)가 없음")

grid_layout_path = os.path.join(RES, "layout", "fragment_grid_psk.xml")
if not os.path.exists(grid_layout_path):
    err("layout/fragment_grid_psk.xml 이 없음")
else:
    layout_txt = read(grid_layout_path)
    for need in ["gridHeaderLayout", "myCallsignText", "myGridText", "bandFreqText",
                 "gridSettingsButton", "logToggleButton", "browserButton", "qsoMapButton",
                 "statsButton", "adifButton", "pskMapWebView", "pskOfflineLayout",
                 "pskOfflineText", "pskRetryButton", "logPanelLayout", "gridLogContainer"]:
        if ("@+id/" + need) not in layout_txt:
            err("fragment_grid_psk.xml 에 @+id/%s 없음" % need)
    if "<WebView" not in layout_txt:
        err("fragment_grid_psk.xml 에 WebView 가 없음")

nav = read(os.path.join(RES, "navigation", "main_navigation.xml"))
m = re.search(r"android:id=\"@\+id/menu_nav_history\"\s*\n\s*android:name=\"([^\"]+)\"", nav)
if not m:
    err("main_navigation.xml 에서 menu_nav_history 목적지를 찾지 못함")
elif m.group(1) != "com.bg7yoz.ft8cn.ui.GridPskFragment":
    err("menu_nav_history 목적지가 GridPskFragment 가 아님 (%s)" % m.group(1))

menu = read(os.path.join(RES, "menu", "bottom_nav_menu.xml"))
if "menu_nav_history" not in menu:
    err("bottom_nav_menu.xml 에 menu_nav_history 가 없음")

base_strings = read(os.path.join(RES, "values", "strings.xml"))
ko_strings = read(os.path.join(RES, "values-ko", "strings.xml"))
for name in ["grid_psk_no_callsign_short", "grid_psk_grid_value", "grid_psk_open_settings",
             "grid_psk_show_log", "grid_psk_show_map", "grid_psk_open_browser",
             "grid_psk_stats", "grid_psk_adif", "grid_psk_map_offline", "grid_psk_retry"]:
    if 'name="%s"' % name not in base_strings:
        err("values/strings.xml 에 %s 가 없음" % name)
    if 'name="%s"' % name not in ko_strings:
        err("values-ko/strings.xml 에 %s 가 없음" % name)

if re.search(r'name="nav_menu_title_history">[^<]*</string>', base_strings) is None:
    err("values/strings.xml 에 nav_menu_title_history 가 없음")
elif "PSK" not in re.search(r'name="nav_menu_title_history">([^<]*)</string>', base_strings).group(1):
    err("values/strings.xml 의 nav_menu_title_history 가 그리드·PSK 로 바뀌지 않음")
ko_title = re.search(r'name="nav_menu_title_history">([^<]*)</string>', ko_strings)
if not ko_title:
    err("values-ko/strings.xml 에 nav_menu_title_history 가 없음")
elif "그리드" not in ko_title.group(1) or "PSK" not in ko_title.group(1):
    err("values-ko/strings.xml 의 nav_menu_title_history 가 '그리드·PSK' 가 아님 (%s)"
        % ko_title.group(1))

manifest = read(os.path.join(APP, "AndroidManifest.xml"))
if "android.permission.INTERNET" not in manifest:
    err("AndroidManifest.xml 에 INTERNET 권한이 없음(앱 안 PSK Reporter 지도)")

# 옛 3분할 전환 탭 문자열은 걷어 냈다
for name in ["split_tab_calling", "split_tab_qso_log"]:
    if 'name="%s"' % name in base_strings:
        err("values/strings.xml 에 걷어 낸 %s 가 남아 있음" % name)
    if 'name="%s"' % name in ko_strings:
        err("values-ko/strings.xml 에 걷어 낸 %s 가 남아 있음" % name)

# --- 6) 처음 한 번 채우는 내 정보 프리셋 --------------------------------------
for need, why in [
    ('DEFAULT_MY_CALLSIGN = "DS2YTO"', "기본 콜사인 상수가 없음"),
    ('DEFAULT_MY_GRID = "PM37uu"', "기본 그리드 상수가 없음"),
    ('DEFAULT_ICOM_IP = "192.168.59.1"', "ICOM 네트워크 주소 상수가 없음"),
    ('DEFAULT_ICOM_USER = "DS2YTO"', "ICOM 사용자명 상수가 없음"),
    ("DEFAULT_CONNECT_MODE = ConnectMode.NETWORK", "기본 연결 방식(네트워크)이 없음"),
    ("private void applyMyStationPresetIfNeeded()", "내 정보 프리셋 함수가 없음"),
    ("isEmptyValue(GeneralVariables.myCallsign)", "콜사인이 비었을 때만 넣는지 확인할 수 없음"),
    ("isEmptyValue(GeneralVariables.getMyMaidenheadGrid())", "그리드가 비었을 때만 넣는지 확인할 수 없음"),
]:
    if need not in main_code:
        err("MainActivity: " + why)
if "icomPassword" in main_code[main_code.index("private void applyMyStationPresetIfNeeded()"):
                               main_code.index("private boolean isEmptyValue(")]:
    err("MainActivity.applyMyStationPresetIfNeeded: 비밀번호를 넣으면 안 됨(공개 저장소)")
if "IC705_PRESET_VERSION = 5" not in main_code and "IC705_PRESET_VERSION = 6" not in main_code:
    err("MainActivity: 프리셋 판 번호를 올리지 않아 기존 설치에 새 값이 들어가지 않음")

db_code = strip_comments(read(os.path.join(JAVA, "database", "DatabaseOpr.java")))
if 'equalsIgnoreCase("connectMode")' not in db_code:
    err("DatabaseOpr: connectMode 설정을 읽지 않음")
cfg_code = strip_comments(read(os.path.join(JAVA, "ui", "ConfigFragment.java")))
if 'writeConfig("connectMode"' not in cfg_code:
    err("ConfigFragment: 고른 연결 방식을 DB에 저장하지 않음")

# --- 7) 피클·AI 언급 금지 ----------------------------------------------------
for path in [os.path.join(APP, "assets", "readme_ko.txt"),
             os.path.join(APP, "assets", "readme_en.txt"),
             os.path.join(ROOT, "README_KR.md")]:
    txt = read(path)
    for word in ["피클", "Pickle", "픽클"]:
        if word in txt:
            err("%s 에 '%s' 언급이 있음" % (os.path.basename(path), word))

if errors:
    print("문제 %d건:" % len(errors))
    for e in errors:
        print("  [오류] " + e)
    sys.exit(1)
print("0.93-kr9 추가 검증 통과 (3분할 디코드 칸 복구, 도구 버튼은 스펙트럼 화면에서만, "
      "그리드·PSK 전용 화면)")
