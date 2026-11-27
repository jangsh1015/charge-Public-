#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""0.93-kr8 변경(QSO 로그 실시간 갱신 · 로그 패널 바탕색 · 송신 감시 기본 무시 ·
[PSK] 버튼)에 대한 추가 정적 검증.

check_source.py · check_kr4.py · check_kr6.py · check_kr7.py 가 보지 않는 것들을 확인한다.
  1) 버전 표기가 0.93-kr8 (versionCode 8)이고 문서·readme 에 내역이 맨 위에 있는지
  2) QSO 로그 실시간 갱신 배선: DB 저장 알림(DatabaseOpr) → LiveData(MainViewModel) → 관찰·재조회(LogFragment)
     + 호출 탭 2분할이 보일 때의 재조회(MainActivity)
  3) 로그 목록 바탕색: fragment_log.xml 루트·splitLogContainer·values/values-night 색 정의
  4) 송신 감시(Tx watchdog) 기본값 0(무시) — GeneralVariables·DatabaseOpr·ConfigFragment
  5) [PSK] 버튼: fragment_log.xml 의 pskGridButton, LogFragment 의 다이얼로그·PSK Reporter 주소·
     예외 보호·인터넷 확인, 내 콜사인·그리드를 설정값에서 그때그때 읽는지
  6) 스펙트럼 3분할 [호출·송신]/[QSO 로그] 전환 탭은 0.93-kr9 에서 걷어 냈으므로,
     그 흔적(탭·겹친 컨테이너·전환 함수)이 남아 있지 않은지
  7) 피클·AI 언급이 사용자 문서에 들어가지 않았는지
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

errors = []


def err(msg):
    errors.append(msg)


def read(path):
    return io.open(path, encoding="utf-8").read()


def strip_comments(txt):
    # 줄 주석(/****... 처럼 // 뒤에 별표가 오는 줄)을 먼저 지워야 블록 주석 정규식이 그 별표를 /* 로 잘못 보지 않는다
    txt = re.sub(r"//[^\n]*", "", txt)
    return re.sub(r"/\*.*?\*/", "", txt, flags=re.S)


# --- 1) 버전·문서 -----------------------------------------------------------
# 0.93-kr9부터는 이 검사를 'kr8 이상'으로 본다(과거 버전 검사가 최신 빌드를 막지 않게).
gradle = read(os.path.join(ROOT, "ft8cn", "app", "build.gradle"))
m = re.search(r"versionCode\s+(\d+)", gradle)
if not m or int(m.group(1)) < 8:
    err("build.gradle versionCode 가 8 이상이 아님 (%s)" % (m.group(1) if m else "없음"))
m = re.search(r"versionName\s+'(0\.93-kr(\d+))'", gradle)
if not m or int(m.group(2)) < 8:
    err("build.gradle versionName 이 0.93-kr8 이상이 아님")

DOCS = [("readme_ko.txt", os.path.join(APP, "assets", "readme_ko.txt")),
        ("readme_en.txt", os.path.join(APP, "assets", "readme_en.txt")),
        ("README_KR.md", os.path.join(ROOT, "README_KR.md"))]
for name, path in DOCS:
    if "0.93-kr8" not in read(path):
        err("%s 에 0.93-kr8 내역이 없음" % name)

for name in ["readme_ko.txt", "readme_en.txt"]:
    txt = read(os.path.join(APP, "assets", name))
    versions = re.findall(r"^\s{2}(0\.93-kr(\d))\s*$", txt, flags=re.M)
    if not versions:
        err("%s 에서 버전 목록을 찾지 못함" % name)
    elif int(versions[0][1]) < 8:
        err("%s 의 첫 버전이 0.93-kr8 보다 낮음 (%s)" % (name, versions[0][0]))

# --- 2) QSO 로그 실시간 갱신 -------------------------------------------------
db = strip_comments(read(os.path.join(JAVA, "database", "DatabaseOpr.java")))
for need, why in [
    ("interface OnQsoLogChanged", "QSO 로그 변경 리스너 인터페이스가 없음"),
    ("public void setOnQsoLogChanged(", "리스너 등록 함수가 없음"),
    ("private void notifyQsoLogChanged(", "리스너를 부르는 함수가 없음"),
]:
    if need not in db:
        err("DatabaseOpr: " + why)

# 알림은 QSO 기록을 저장하는 한 곳(doInsertQSLData)에서 나가야 한다
m = re.search(r"public boolean doInsertQSLData\([\s\S]*?\n    \}", db)
if not m:
    err("DatabaseOpr.doInsertQSLData 를 찾지 못함")
else:
    body = m.group(0)
    if "notifyQsoLogChanged()" not in body:
        err("DatabaseOpr.doInsertQSLData: 저장 후 QSO 로그 변경 알림이 없음")
    elif body.index("notifyQsoLogChanged()") > body.rindex("return true;"):
        err("DatabaseOpr.doInsertQSLData: 알림이 return 뒤에 있음")

vm = strip_comments(read(os.path.join(JAVA, "MainViewModel.java")))
if "MutableLiveData<Integer> mutableQsoLogUpdated" not in vm:
    err("MainViewModel: mutableQsoLogUpdated(LiveData) 가 없음")
if "databaseOpr.setOnQsoLogChanged(" not in vm:
    err("MainViewModel: DB 알림 리스너를 등록하지 않음")
m = re.search(r"setOnQsoLogChanged\([\s\S]{0,600}?postValue\(", vm)
if not m:
    err("MainViewModel: DB 알림을 LiveData(postValue)로 넘기지 않음")

logf = strip_comments(read(os.path.join(JAVA, "ui", "LogFragment.java")))
if "mutableQsoLogUpdated.observe(getViewLifecycleOwner()" not in logf:
    err("LogFragment: mutableQsoLogUpdated 를 observe 하지 않음")
m = re.search(r"private void refreshLogList\(\)\s*\{[\s\S]{0,600}?\n    \}", logf)
if not m:
    err("LogFragment: refreshLogList() 를 찾지 못함")
elif "queryByCallsign(mainViewModel.queryKey, 0)" not in m.group(0):
    err("LogFragment.refreshLogList: 목록을 처음부터 다시 읽지 않음")
if "public void refreshLogPanel()" not in logf:
    err("LogFragment: refreshLogPanel() 이 없음")
if "liveRefreshHandler.removeCallbacks" not in logf:
    err("LogFragment: 미뤄 둔 새로 고침을 지우지 않음")
if "public void onResume()" not in logf:
    err("LogFragment: onResume() 재조회가 없음")

main_code = strip_comments(read(os.path.join(JAVA, "MainActivity.java")))
m = re.search(r"private void refreshCallLogPanel\(\)\s*\{[\s\S]{0,600}?\n    \}", main_code)
if not m:
    err("MainActivity: refreshCallLogPanel() 을 찾지 못함")
elif "refreshLogPanel()" not in m.group(0):
    err("MainActivity.refreshCallLogPanel: LogFragment.refreshLogPanel() 을 부르지 않음")
if "refreshIfQueryChanged" in main_code:
    err("MainActivity: 예전 refreshIfQueryChanged 호출이 남아 있음")

# --- 3) 로그 목록 바탕색 -----------------------------------------------------
log_layout = read(os.path.join(RES, "layout", "fragment_log.xml"))
m = re.search(r'android:id="@\+id/logFrameLayout"[\s\S]{0,400}?android:background="([^"]+)"',
              log_layout)
if not m:
    err("fragment_log.xml 루트(logFrameLayout)에 바탕색이 없음")
elif m.group(1) != "@color/log_panel_background":
    err("fragment_log.xml 루트 바탕색이 log_panel_background 가 아님 (%s)" % m.group(1))

if 'name="log_panel_background"' not in read(os.path.join(RES, "values", "colors.xml")):
    err("values/colors.xml 에 log_panel_background 없음")
if 'name="log_panel_background"' not in read(os.path.join(RES, "values-night", "colors.xml")):
    err("values-night/colors.xml 에 log_panel_background 없음")

for variant in ["layout", "layout-land", "layout-sw600dp-land"]:
    p = os.path.join(RES, variant, "main_activity.xml")
    if not os.path.exists(p):
        err("%s/main_activity.xml 없음" % variant)
        continue
    txt = read(p)
    m = re.search(r'android:id="@\+id/splitLogContainer"[\s\S]{0,400}?android:background="([^"]+)"',
                  txt)
    if not m:
        err("%s: splitLogContainer 에 바탕색이 없음" % variant)
    elif m.group(1) != "@color/log_panel_background":
        err("%s: splitLogContainer 바탕색이 log_panel_background 가 아님 (%s)"
            % (variant, m.group(1)))

# --- 4) 송신 감시 기본값 '무시' ----------------------------------------------
gv = strip_comments(read(os.path.join(JAVA, "GeneralVariables.java")))
m = re.search(r"DEFAULT_LAUNCH_SUPERVISION\s*=\s*([^;]+);", gv)
if not m:
    err("GeneralVariables: DEFAULT_LAUNCH_SUPERVISION 을 찾지 못함")
elif m.group(1).strip() != "0":
    err("GeneralVariables: 송신 감시 기본값이 0(무시)이 아님 (%s)" % m.group(1).strip())
if not re.search(r"int launchSupervision\s*=\s*DEFAULT_LAUNCH_SUPERVISION\s*;", gv):
    err("GeneralVariables: launchSupervision 처음 값이 기본값을 쓰지 않음")

m = re.search(r'name\.equalsIgnoreCase\("launchSupervision"\)\)[\s\S]{0,900}?\n                \}', db)
if not m:
    err("DatabaseOpr: launchSupervision 설정 읽기를 찾지 못함")
else:
    body = m.group(0)
    if "DEFAULT_LAUNCH_SUPERVISION" not in body:
        err("DatabaseOpr: DB에 값이 없을 때 송신 감시 기본값을 쓰지 않음")
    if "LEGACY_DEFAULT_LAUNCH_SUPERVISION" not in body:
        err("DatabaseOpr: 예전 기본값(10분)을 '무시'로 읽지 않음")

if "LEGACY_DEFAULT_LAUNCH_SUPERVISION" not in gv:
    err("GeneralVariables: 예전 기본값(10분) 상수가 없음")

cfg = strip_comments(read(os.path.join(JAVA, "ui", "ConfigFragment.java")))
m = re.search(r"launchSupervisionSpinner\.setOnItemSelectedListener\([\s\S]{0,1200}?\n                \}\);", cfg)
if not m:
    err("ConfigFragment: 송신 감시 스피너 리스너를 찾지 못함")
else:
    body = m.group(0)
    if "timeOut == GeneralVariables.launchSupervision" not in body:
        err("ConfigFragment: 송신 감시 스피너가 값이 같을 때도 DB에 씀")

# 0이면 감시하지 않는다는 판단은 그대로여야 한다
signal = strip_comments(read(os.path.join(JAVA, "ft8transmit", "FT8TransmitSignal.java")))
if "GeneralVariables.launchSupervision <= 0" not in signal:
    err("FT8TransmitSignal: 송신 감시 0(무시) 처리 고리가 없음")


# --- 5) [PSK] 버튼(내 콜사인·그리드·PSK Reporter) ------------------------------
log_layout = read(os.path.join(RES, "layout", "fragment_log.xml"))
if 'android:id="@+id/pskGridButton"' not in log_layout:
    err("fragment_log.xml 에 pskGridButton 이 없음")
if "locationInMapImageButton" in log_layout:
    err("fragment_log.xml 에 예전 지도 버튼(locationInMapImageButton)이 남아 있음")
if "@string/psk_button_label" not in log_layout:
    err("fragment_log.xml: [PSK] 버튼 글자가 psk_button_label 이 아님")

if "binding.locationInMapImageButton" in logf:
    err("LogFragment: 예전 지도 버튼(locationInMapImageButton) 참조가 남아 있음")
if "binding.pskGridButton.setOnClickListener" not in logf:
    err("LogFragment: [PSK] 버튼 클릭 처리기가 없음")
for need, why in [
    ("private void showPskGridDialog()", "[PSK] 다이얼로그 함수가 없음"),
    ("GeneralVariables.myCallsign", "내 콜사인을 설정값에서 읽지 않음"),
    ("GeneralVariables.getMyMaidenheadGrid()", "내 그리드를 읽지 않음"),
    ("GeneralVariables.getMyMaidenhead4Grid()", "내 그리드(4자리)를 읽지 않음"),
    ("Intent.ACTION_VIEW", "PSK Reporter 를 브라우저(ACTION_VIEW)로 열지 않음"),
    ("GridTrackerMainActivity", "내 교신 지도로 가는 길이 사라짐"),
    ("private boolean hasInternet()", "인터넷 확인 함수가 없음"),
    ("psk_grid_no_internet", "인터넷이 없을 때의 안내 문구를 쓰지 않음"),
]:
    if need not in logf:
        err("LogFragment: " + why)

# 주석 지우기는 'https://' 의 // 까지 지우므로, 주소는 원본 파일에서 읽는다
raw_logf = read(os.path.join(JAVA, "ui", "LogFragment.java"))
m = re.search(r"PSK_REPORTER_MAP_URL\s*(?:\n\s*)?=\s*\"([^\"]+)\"", raw_logf)
if not m:
    err("LogFragment: PSK_REPORTER_MAP_URL 상수를 찾지 못함")
else:
    url = m.group(1)
    for part in ["pskreporter.info/pskmap.html", "callsign=%1$s", "txrx=tx", "timerange=86400"]:
        if part not in url:
            err("LogFragment: PSK Reporter 주소에 %s 가 없음 (%s)" % (part, url))

# 브라우저·지도 화면 열기는 예외를 잡아 둔다(인터넷이 없는 IC-705 와이파이에서도 앱이 꺼지지 않게)
for fname, label in [("openUrl", "브라우저 열기"), ("openQsoMap", "내 교신 지도 열기")]:
    m = re.search(r"private void " + fname + r"\([\s\S]{0,1200}?\n    \}", logf)
    if not m:
        err("LogFragment: %s 함수를 찾지 못함" % fname)
    elif "catch (Exception e)" not in m.group(0):
        err("LogFragment: %s 에 예외 보호(catch)가 없음" % label)

base_strings = read(os.path.join(RES, "values", "strings.xml"))
ko_strings = read(os.path.join(RES, "values-ko", "strings.xml"))
for name in ["psk_button_label", "psk_grid_title", "psk_grid_info", "psk_grid_no_callsign",
             "psk_grid_no_internet", "psk_reporter_open", "psk_grid_qso_map",
             "psk_grid_open_failed"]:
    if 'name="%s"' % name not in base_strings:
        err("values/strings.xml 에 %s 가 없음" % name)
    if 'name="%s"' % name not in ko_strings:
        err("values-ko/strings.xml 에 %s 가 없음" % name)

# --- 6) 3분할 [호출·송신]/[QSO 로그] 전환 탭은 0.93-kr9 에서 걷어 냄 -------------------
A = "{http://schemas.android.com/apk/res/android}"
for variant in ["layout", "layout-land", "layout-sw600dp-land"]:
    p = os.path.join(RES, variant, "main_activity.xml")
    if not os.path.exists(p):
        err("%s/main_activity.xml 없음" % variant)
        continue
    txt = read(p)
    for gone in ["splitCallingToggleBar", "splitCallingBody", "splitCallingTabCall",
                 "splitCallingTabLog", "splitCallingLogContainer"]:
        if ("@+id/" + gone) in txt:
            err("%s/main_activity.xml 에 0.93-kr9 에서 걷어 낸 %s 가 남아 있음" % (variant, gone))
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
        continue
    right_kids = [k.get(A + "id") for k in list(right)]
    for need in ["@+id/splitDecodeContainer", "@+id/splitHorizontalHandle",
                 "@+id/splitCallingContainer"]:
        if need not in right_kids:
            err("%s: 오른쪽 열의 자식에 %s 없음 (실제: %s)" % (variant, need, right_kids))
    decode = elements.get("@+id/splitDecodeContainer")
    calling = elements.get("@+id/splitCallingContainer")
    for name, el in [("splitDecodeContainer", decode), ("splitCallingContainer", calling)]:
        if el is None:
            continue
        if el.get(A + "layout_height") != "0dp":
            err("%s: %s 의 높이는 0dp 여야 함 (%s)" % (variant, name, el.get(A + "layout_height")))
        if el.get(A + "layout_weight") is None:
            err("%s: %s 에 layout_weight 가 없음" % (variant, name))

for gone, why in [
    ("private void initSplitCallingLogLayout()", "3분할 로그 칸 준비 함수가 남아 있음"),
    ("FRAGMENT_TAG_SPLIT_CALLING_LOG", "3분할 로그 칸 태그가 남아 있음"),
    ("private void showSplitCallingTab(boolean showLog)", "호출·로그 전환 함수가 남아 있음"),
    ("private void applySplitCallingTab()", "전환 반영 함수가 남아 있음"),
    ("private void refreshSplitCallingLogPanel()", "3분할 로그 칸 재읽기 함수가 남아 있음"),
    ("KEY_SPLIT_CALL_TAB", "마지막으로 보던 탭 기억이 남아 있음"),
]:
    if gone in main_code:
        err("MainActivity: 0.93-kr9 에서 걷어 낸 " + why)

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
print("0.93-kr8 추가 검증 통과 (QSO 로그 실시간 갱신, 로그 패널 바탕색, 송신 감시 기본 무시, "
      "[PSK] 버튼, 3분할 전환 탭 걷어 냄)")
