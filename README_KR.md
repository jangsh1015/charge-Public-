# FT8CN 한국어판 (간헐 CQ 추가) — 빌드용 소스

안드로이드 FT8 앱 **FT8CN**(원본: [N0BOY/FT8CN](https://github.com/N0BOY/FT8CN), 개발 BG7YOZ, MIT 라이선스)의
**한국어 UI + 간헐 CQ(주기적 Auto CQ)** 개조판 소스입니다.
개인 사용 목적으로 만든 빌드이며, 원본의 라이선스와 저작권 표시(`LICENSE`)를 그대로 유지합니다.

앱 패키지 이름은 `com.bg7yoz.ft8cn.kr`, 앱 이름은 `FT8CN KR`, 버전은 `0.93-kr1` 입니다.
패키지 이름이 원본과 달라서 **원본 FT8CN 과 나란히 설치**할 수 있습니다.

---

## 1. 원본과 다른 점

### (1) 한국어 UI 전면 번역
- `ft8cn/app/src/main/res/values-ko/strings.xml` — 원본 `values/strings.xml`(영어)의 모든 번역 대상
  문자열(502개)을 한국어 아마추어무선 용어로 옮겼습니다. (CQ 호출, 송신/수신, 워터폴, SWR, 호출부호,
  그리드, 자동 응답, QSO 로그 등)
- `ft8cn/app/src/main/assets/*_ko.txt` — 앱 안의 도움말 24개를 한국어로 새로 썼습니다.
  설정 화면의 ⓘ 버튼이 이 파일들을 엽니다.
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
- 송신 감시(Tx watchdog)가 주기보다 짧으면 자동 송신이 도중에 멈추므로, 그때는 설정에서
  송신 감시를 주기보다 길게 하거나 `무시`로 두세요. (조건이 맞으면 앱이 한 번 경고합니다.)
- `CQ 자동 추적`/`추적 자동 호출`이 켜져 있으면 대기 중에도 새 국을 먼저 부를 수 있습니다.
  무전기를 조용히 두고 싶으면 두 기능을 끄세요.

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
| `ft8cn/app/src/main/assets/*_ko.txt` (24개) | 한국어 도움말 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqSpinnerAdapter.java` | 간헐 CQ 주기 목록 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/IntervalCqCountSpinnerAdapter.java` | 1회 CQ 횟수 목록 |
| `build_apk.sh` | 우분투/WSL 원클릭 빌드 스크립트 |
| `tools/check_source.py` | 빌드 전 정적 검사(Java 문법·XML·리소스 참조) |

고친 파일

| 파일 | 내용 |
|---|---|
| `ft8cn/app/build.gradle` | `applicationId` → `com.bg7yoz.ft8cn.kr`, `versionName` → `0.93-kr1`, `buildToolsVersion 33.0.2` |
| `ft8cn/app/src/main/res/values/strings.xml` | 앱 이름 `FT8CN KR`, 간헐 CQ 문자열 9개 추가 |
| `ft8cn/app/src/main/res/layout/fragment_config.xml` | 설정 화면에 `간헐 CQ`, `1회 CQ 송신` 두 줄 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java` | `intervalCQMinutes`, `intervalCQCount` 추가 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java` | 설정 키 `intervalCQ`, `intervalCQCount` 읽기 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java` | 간헐 CQ 판단 로직, 상태 표시, 송신 감시 경고 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` | 설정 화면 두 항목 연결, 도움말 버튼 |
| `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/MyCallingFragment.java` | 대상 표시줄에 간헐 CQ 남은 시간 표시 |

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
