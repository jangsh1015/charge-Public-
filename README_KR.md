# FT8CN 한국어판 (간헐 CQ 추가) — 빌드용 소스

안드로이드 FT8 앱 **FT8CN**(원본: [N0BOY/FT8CN](https://github.com/N0BOY/FT8CN), 개발 BG7YOZ, MIT 라이선스)의
**한국어 UI + 간헐 CQ(주기적 Auto CQ)** 개조판 소스입니다.
개인 사용 목적으로 만든 빌드이며, 원본의 라이선스와 저작권 표시(`LICENSE`)를 그대로 유지합니다.

앱 패키지 이름은 `com.bg7yoz.ft8cn.kr`, 앱 이름은 `FT8CN KR`, 버전은 `0.93-kr4` 입니다.
패키지 이름이 원본과 달라서 **원본 FT8CN 과 나란히 설치**할 수 있습니다.

---

## 1. 원본과 다른 점

### (1) 한국어 UI 전면 번역
- `ft8cn/app/src/main/res/values-ko/strings.xml` — 원본 `values/strings.xml`(영어)의 모든 번역 대상
  문자열(502개)을 한국어 아마추어무선 용어로 옮겼습니다. (CQ 호출, 송신/수신, 워터폴, SWR, 호출부호,
  그리드, 자동 응답, QSO 로그 등)
- `ft8cn/app/src/main/assets/*_ko.txt` — 앱 안의 도움말과 안내문 26개를 한국어로 새로 썼습니다.
  설정 화면의 ⓘ 버튼과 `정보` 창이 이 파일들을 엽니다.
  `readme_ko.txt`(정보 창)에는 원작(BG7YOZ / N0BOY)과 한국어 번역·간헐 CQ 수정(DS2YTO) 크레딧이 들어 있습니다.
  중국어 원문만 있던 `readme.txt`·`rigaddress.txt` 는 영어(`_en`)와 한국어(`_ko`) 판을 따로 두어,
  한국어·영어 사용자에게 중국어가 보이지 않게 했습니다.
- 언어는 휴대폰의 시스템 언어를 따릅니다. 휴대폰이 한국어이면 자동으로 한국어 화면이 됩니다.
  (안드로이드 13 이상에서는 설정 > 앱 > FT8CN KR > 언어에서 앱 언어만 따로 고를 수도 있습니다.)

### (2) 간헐 CQ (주기적 Auto CQ)
1층·포터블처럼 무전기 발열과 배터리가 걱정되는 자리에서, CQ를 계속 내지 않고
**일정 시간마다 2~3번만 CQ를 내고 나머지는 수신만** 하도록 만든 기능입니다.

- 설정 화면에 두 항목이 추가되었습니다.
  - **간헐 CQ** : `꺼짐(연속)` / `5분마다` / `10분마다` / `15분마다` / `20분마다` / `30분마다` / `60분마다`
  - **1회 CQ 송신** : 한 번에 내는 CQ 횟수 `1~5회` (기본 3회)
- 설정은 데이터베이스에 저장되어 앱을 다시 켜도 유지됩니다. (키: `intervalCQ`, `intervalCQCount`)

동작 방식

| 상태 | 송신 |
|---|---|
| 간헐 CQ 꺼짐 | 원본과 같이 CQ를 계속 냅니다. |
| CQ 묶음 차례 | 내 송신 차례마다 CQ를 정한 횟수만큼 냅니다. (FT8은 15초 슬롯의 내 차례에만 송신하므로 3회면 약 1분) |
| 대기 | 송신하지 않고 **수신·디코드만** 합니다. |
| 누군가 내 호출부호를 부르거나 응답 | **즉시** 평소 자동 응답(Auto Answer) 교신으로 넘어갑니다. |
| 교신 종료 | 다시 간헐 CQ로 돌아가 CQ 묶음을 한 번 내고 대기합니다. |

- 화면 위쪽 대상 표시줄에 `간헐 10분 4:32` 처럼 남은 시간이 표시됩니다.
- 송신 감시(Tx watchdog)가 주기보다 짧으면 대기 중에 자동 송신이 멈출 수 있어 경고를 한 번 보여 줍니다.
  `0.93-kr2`부터는 **간헐 CQ로 대기하는 동안은 송신 감시 시간을 세지 않으므로**, 감시를 짧게 두어도
  대기 중에 꺼지지 않습니다. 그래도 묶음 송신이 긴 경우를 대비해 주기보다 길게 두거나 `무시`로 두면 안전합니다.
- `CQ 자동 추적`/`추적 자동 호출`이 켜져 있으면 대기 중에도 새 국을 먼저 부를 수 있습니다.
  무전기를 조용히 두고 싶으면 두 기능을 끄세요.

### (3) 0.93-kr2에서 더한 것
- `정보` 창을 `readme_ko.txt` 로 연결했습니다. 면책·사용 안내와 함께 원작(BG7YOZ / N0BOY),
  한국어 번역·간헐 CQ 수정(DS2YTO), FT8/FT4 프로토콜(K9AN·G4WJS·K1JT) 크레딧이 들어 있습니다.
- 무전기 모델 목록(`rigaddress_ko.txt`)과 시리얼 연결 끊김 안내, 웹 UI 업로드 버튼 문구를 한국어로 바꿨습니다.
- 간헐 CQ 대기 중에는 송신 감시 시간을 세지 않아, 대기 중에 자동 송신이 꺼지지 않습니다.
- 호출 탭의 확성기 아이콘: 송신 중은 빨간색 깜빡임, **자동 송신이 켜진 대기 중은 주황색**, 꺼짐은 X 아이콘입니다.

### (4) 0.93-kr3에서 더한 것
- **무응답 재호출 방지(30분 쿨다운)**: `추적 자동 호출`이 부른 국이 `무응답 N회 중단`으로 끊기면,
  그 콜사인(대문자로 정리)을 30분 쿨다운 목록(`ConcurrentHashMap<String, Long>`)에 넣고
  30분 동안 추적 자동 호출에서 건너뜁니다. 같은 국이 계속 CQ를 내도 다시 물고 들어가지 않고
  다른 새 CQ 국을 찾습니다. 직접 콜사인을 눌러 부르거나 상대가 먼저 부르면 쿨다운과 관계없이
  응답하며(이때 쿨다운도 지움), 30분이 지난 기록은 확인할 때와 1초 하트비트에서 자동 정리합니다.
- `무응답 N회 중단` 설정은 원래 `무시(0)` ~ `30회`까지 고를 수 있어 `3회`·`5회`·`7회`도 그대로 선택할 수 있습니다.
- **초기 기본 주파수 40m**: `GeneralVariables.band`·`OperationBand.getDefaultBand()`·
  `DatabaseOpr` 의 `bandFreq` 기본값을 `7.074 MHz (40m)`로 바꾸고, 처음 설치해 저장된 밴드가 없을 때는
  `MainActivity.InitData()`에서 밴드 목록 위치(`bandListIndex`)도 기본 밴드로 맞춥니다.
  (대한민국 3급은 14 MHz 대역이 수신만 가능해, 새 기기에서 20m로 시작하던 것을 40m로 바꿈)

### (5) 0.93-kr4에서 더한 것
- **태블릿 가로 3분할**: `res/layout-sw600dp-land/main_activity.xml` 에 3분할 컨테이너를 두고,
  작은 화면 폭이 600dp 이상이면서 가로일 때만 켭니다(`MainActivity.isTabletSplitConfiguration()`).
  - 왼쪽: `SpectrumFragment`(스펙트럼·워터폴), 오른쪽 위: `CallingListFragment`(디코드 목록),
    오른쪽 아래: `MyCallingFragment`(호출). 태블릿 가로에서는 두 목록 프래그먼트가
    `layout-sw600dp-land/fragment_calling_list.xml`·`fragment_my_calling.xml`(스펙트럼 없는 목록 전용)을
    쓰므로 스펙트럼이 겹치지 않습니다. 스마트폰 세로·가로 화면의 기존 2분할은 그대로입니다.
  - 경계선 손잡이(16dp 터치 영역, 가운데 4dp 알약)를 끌어 비율을 바꿉니다. 스펙트럼 30~75%,
    디코드 20~80% 범위로 제한하고, `SharedPreferences`(`tabletSplit/splitWidthRatio`·`splitHeightRatio`)에
    저장해 재시작 후에도 유지합니다.
  - 하단 탭에서 디코드·호출·스펙트럼을 누르면 3분할을 보여 주고, QSO 로그·설정·QRZ 화면은
    기존처럼 전체 화면(NavHost)으로 엽니다. 3분할 패널 프래그먼트는 계속 살아 있어
    탭을 오가도 구성과 스펙트럼이 유지됩니다.
  - 회전 등으로 3분할 레이아웃이 아니게 되면 남은 패널 프래그먼트를 정리해
    `No view found for id` 오류가 나지 않게 했습니다.
- **워터폴·스펙트럼 색감**: `SpectrumColorTable` 을 새로 두고 256단계 색상표를 미리 계산합니다.
  잡음 구간은 `#04070F`~`#0E2755` 딥 네이비, 신호는 청록(`#18CEE2`) → 노랑(`#FFE03A`) →
  주황(`#FF9612`) → 빨강(`#FF2E2E`)으로 올라갑니다. `WaterfallView` 의 원색 파랑 계산을 이 표로 바꾸고,
  `ColumnarView` 는 상단 청록/하단 원색 파랑 그라데이션 대신 막대마다 레벨 색을 칠합니다.
- **IC-705 기준 초기 설정**: `GeneralVariables` 기본값을 `modelNo`=IC-705(무전기 목록에서 이름으로 찾고,
  없으면 2번), `civAddress`=0xA4, `baudRate`=115200, `noReplyLimit`=3 으로 바꿨습니다(SWR·ALC 경보는 원래 기본 ON).
  이미 쓰던 설치본도 혜택을 보도록 앱을 처음 켜는 때 `MainActivity.applyIc705PresetIfNeeded()` 가
  `model`·`instruction`·`civ`·`baudRate`·`swrSwitch`·`alcSwitch`·`noReplyLimit` 7개 값을 DB에 한 번만 넣습니다
  (`ic705Preset/version`=4 로 기록, 이후에는 사용자가 바꾼 값을 다시 덮지 않음).
  제어 방식(`ctrMode`)과 Icom LAN 주소·포트 설정은 건드리지 않습니다.

---

## 2. 빌드 방법 (우분투 / 윈도우 WSL Ubuntu)

```bash
git clone https://github.com/jangsh1015/charge-Public-.git ft8cn-kr
cd ft8cn-kr
chmod +x build_apk.sh
./build_apk.sh
```

스크립트가 자동으로 하는 일:

1. `openjdk-17-jdk`, `unzip`, `wget`, `curl` 설치 (이미 있으면 건너뜀)
2. Android SDK Command-line Tools 내려받아 `~/Android/Sdk/cmdline-tools/latest` 에 설치
3. `platform-tools`, `platforms;android-33`, `build-tools;33.0.2` 설치 + 라이선스 동의
4. `./gradlew assembleDebug` 실행 → `ft8cn/app/build/outputs/apk/debug/app-debug.apk`
5. APK 를 소스 폴더(`FT8CN_KR.apk`)와 윈도우 다운로드 폴더(`/mnt/c/Users/<사용자>/Downloads/FT8CN_KR.apk`)로 복사

### 빌드 요구 사항 (원본 `build.gradle` 기준)

| 항목 | 값 |
|---|---|
| Gradle | 7.5 (wrapper 자동 다운로드) |
| Android Gradle Plugin | 7.4.1 |
| JDK | **17** (AGP 7.4.1 은 JDK 11 이상 필요) |
| compileSdk / targetSdk | 33 |
| buildToolsVersion | 33.0.2 |
| minSdk | 23 |
| NDK / CMake | **필요 없음** (`app/libs/<abi>/libft8cn.so` 미리 빌드된 라이브러리 사용) |

### 설치

APK 를 휴대폰으로 옮겨 눌러 설치하거나, USB 디버깅을 켠 경우:

```bash
~/Android/Sdk/platform-tools/adb install -r ft8cn/app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. 변경한 파일 목록

새로 만든 파일

| 파일 | 내용 |
|---|---|
| `ft8cn/app/src/main/res/values-ko/strings.xml` | 한국어 문자열 전체 |
| `ft8cn/app/src/main/assets/*_ko.txt` (26개) | 한국어 도움말·정보 창 (원작/수정자 크레딧 포함) |
| `ft8cn/app/src/main/assets/readme_en.txt`, `rigaddress_en.txt` | 정보 창·무전기 목록의 영어판 (중국어 제거) |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqSpinnerAdapter.java` | 간헐 CQ 주기 목록 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqCountSpinnerAdapter.java` | 1회 CQ 횟수 목록 |
| `build_apk.sh` | 우분투/WSL 원클릭 빌드 스크립트 |
| `tools/check_source.py` | 빌드 전 정적 검사(Java 문법·XML·리소스 참조) |

고친 파일

| 파일 | 내용 |
|---|---|
| `ft8cn/app/build.gradle` | `applicationId` → `com.bg7yoz.ft8cn.kr`, `versionName` → `0.93-kr4`, `buildToolsVersion 33.0.2` |
| `ft8cn/app/src/main/res/values/strings.xml` | 앱 이름 `FT8CN KR`, 간헐 CQ 문자열 9개 추가 |
| `ft8cn/app/src/main/res/layout/fragment_config.xml` | 설정 화면에 `간헐 CQ`, `1회 CQ 송신` 두 줄 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java` | `intervalCQMinutes`, `intervalCQCount` 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java` | 설정 키 `intervalCQ`, `intervalCQCount` 읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java` | 간헐 CQ 판단 로직, 상태 표시, 송신 감시 경고 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | 설정 화면 두 항목 연결, 도움말 버튼 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/MyCallingFragment.java` | 대상 표시줄에 간헐 CQ 남은 시간 표시 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | (kr2) 정보 창을 로캘별 `readme_help` 리소스로 연결 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/RigNameList.java` | (kr2) 무전기 목록을 로캘별 `rig_address_file`(rigaddress_ko/en)로 읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableConnector.java` | (kr2) 시리얼 연결 끊김 안내를 리소스 문자열로 변경 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/html/LogHttpServer.java` | (kr2) 웹 UI 업로드 버튼 문구를 리소스 문자열로 변경 |
| `ft8cn/app/src/main/res/values*/strings.xml` | (kr2) `readme_help`, `rig_address_file`, `html_upload`, `serial_connection_lost` 추가 (zh 계열은 중국어 원문 파일 유지) |

## 4. 정적 검사

빌드 전에 아래 명령으로 소스를 점검할 수 있습니다 (파이썬 3 + `pip install javalang`).

```bash
python3 tools/check_source.py
```

검사 항목: 모든 XML well-formed, 모든 Java 파일 문법 파싱, `R.string`/`@string`/`R.id`/`@id`/`R.layout`
참조가 실제로 정의되어 있는지, `values-ko` 가 기본 `values` 와 이름·포맷 지정자(`%s`, `%d`, `%.1f`)가
정확히 맞는지.

## 5. 라이선스와 출처

- 원본: FT8CN — https://github.com/N0BOY/FT8CN (개발 BG7YOZ, MIT License)
- 이 소스는 원본 `release` 브랜치 커밋 `a98fbfa` (2025-01-05, 버전 0.93) 를 기준으로 개조했습니다.
- 이 개조판도 같은 MIT 라이선스를 따르며 `LICENSE` 파일을 그대로 두었습니다.
- FT8/FT4 프로토콜: Steve Franke (K9AN), Bill Somerville (G4WJS), Joe Taylor (K1JT).
- 아마추어무선 면허 없이 송신하는 것은 불법입니다. 각국 법규를 지켜 사용하세요.
