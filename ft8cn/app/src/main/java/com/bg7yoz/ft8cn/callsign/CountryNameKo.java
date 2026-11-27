package com.bg7yoz.ft8cn.callsign;

/**
 * 한국어판(0.93-kr10): CTY.DAT 의 영어 나라 이름을 한국어 짧은 이름으로 바꾼다.
 *
 * <p>호출부호 옆에 어느 나라인지 한눈에 보여 주기 위한 표다. 표에 없는 이름은
 * 원래 영어 이름을 그대로 돌려주므로, 나라가 늘어도 화면이 비지는 않는다.
 * 중국의 성(省)별 이름(예: {@code BeiJing China})은 모두 {@code 중국}으로 묶는다.
 *
 * @author BG7YOZ (한국어판 0.93-kr10)
 */
public class CountryNameKo {
    private static final java.util.HashMap<String, String> KO = new java.util.HashMap<>();

    static {
        // 아시아
        put("Republic of Korea", "한국");
        put("Korea, Republic of", "한국");
        put("South Korea", "한국");
        put("DPR of Korea", "북한");
        put("Japan", "일본");
        put("China", "중국");
        put("Taiwan China", "대만");
        put("Taiwan", "대만");
        put("Hong Kong China", "홍콩");
        put("Hong Kong", "홍콩");
        put("Macao", "마카오");
        put("Mongolia", "몽골");
        put("Vietnam", "베트남");
        put("Thailand", "태국");
        put("Philippines", "필리핀");
        put("Indonesia", "인도네시아");
        put("Malaysia", "말레이시아");
        put("West Malaysia", "말레이시아(서부)");
        put("East Malaysia", "말레이시아(동부)");
        put("Singapore", "싱가포르");
        put("Brunei Darussalam", "브루나이");
        put("India", "인도");
        put("Sri Lanka", "스리랑카");
        put("Nepal", "네팔");
        put("Bhutan", "부탄");
        put("Bangladesh", "방글라데시");
        put("Pakistan", "파키스탄");
        put("Afghanistan", "아프가니스탄");
        put("Iran", "이란");
        put("Iraq", "이라크");
        put("Saudi Arabia", "사우디아라비아");
        put("Kuwait", "쿠웨이트");
        put("Qatar", "카타르");
        put("Bahrain", "바레인");
        put("United Arab Emirates", "아랍에미리트");
        put("Oman", "오만");
        put("Yemen", "예멘");
        put("Jordan", "요르단");
        put("Israel", "이스라엘");
        put("Lebanon", "레바논");
        put("Syria", "시리아");
        put("Turkey", "터키");
        put("Asiatic Turkey", "터키(아시아)");
        put("European Turkey", "터키(유럽)");
        put("Cyprus", "키프로스");
        put("UK Base Areas on Cyprus", "키프로스 영국기지");
        put("Kazakhstan", "카자흐스탄");
        put("Uzbekistan", "우즈베키스탄");
        put("Kyrgyzstan", "키르기스스탄");
        put("Tajikistan", "타지키스탄");
        put("Turkmenistan", "투르크메니스탄");
        put("Azerbaijan", "아제르바이잔");
        put("Armenia", "아르메니아");
        put("Georgia", "조지아");
        put("Laos", "라오스");
        put("Cambodia", "캄보디아");
        put("Myanmar", "미얀마");
        put("Andaman & Nicobar Is.", "안다만·니코바르");
        put("Lakshadweep Islands", "락샤드위프");
        put("Ogasawara", "오가사와라");
        put("Minami Torishima", "미나미토리시마");
        put("Minami Tori Shima", "미나미토리시마");
        put("Spratly Islands China", "스프래틀리");
        put("Scarborough Reef China", "스카버러리프");
        put("Pratas Island", "프라타스");
        put("Palestine", "팔레스타인");
        put("Timor - Leste", "동티모르");
        put("Vienna Intl Ctr", "빈국제센터");
        put("United Nations HQ", "유엔본부");
        put("ITU HQ", "ITU본부");

        // 유럽
        put("European Russia", "러시아(유럽)");
        put("Asiatic Russia", "러시아(아시아)");
        put("Russia", "러시아");
        put("Russian Federation", "러시아");
        put("Kaliningrad", "칼리닌그라드");
        put("Franz Josef Land", "프란츠요제프");
        put("Fed. Rep. of Germany", "독일");
        put("Germany", "독일");
        put("France", "프랑스");
        put("England", "잉글랜드");
        put("Scotland", "스코틀랜드");
        put("Wales", "웨일스");
        put("Northern Ireland", "북아일랜드");
        put("Ireland", "아일랜드");
        put("Isle of Man", "맨섬");
        put("Jersey", "저지");
        put("Guernsey", "건지");
        put("Italy", "이탈리아");
        put("Sardinia", "사르데냐");
        put("Sicily", "시칠리아");
        put("African Italy", "아프리카 이탈리아");
        put("Spain", "스페인");
        put("Balearic Islands", "발레아레스");
        put("Canary Islands", "카나리아");
        put("Ceuta & Melilla", "세우타·멜리야");
        put("Portugal", "포르투갈");
        put("Azores", "아조레스");
        put("Madeira Islands", "마데이라");
        put("Netherlands", "네덜란드");
        put("Belgium", "벨기에");
        put("Luxembourg", "룩셈부르크");
        put("Switzerland", "스위스");
        put("Liechtenstein", "리히텐슈타인");
        put("Austria", "오스트리아");
        put("Czech Republic", "체코");
        put("Slovak Republic", "슬로바키아");
        put("Poland", "폴란드");
        put("Hungary", "헝가리");
        put("Romania", "루마니아");
        put("Bulgaria", "불가리아");
        put("Greece", "그리스");
        put("Crete", "크레타");
        put("Dodecanese", "도데카니사");
        put("Mount Athos", "아토스산");
        put("Croatia", "크로아티아");
        put("Slovenia", "슬로베니아");
        put("Bosnia-Herzegovina", "보스니아헤르체고비나");
        put("Serbia", "세르비아");
        put("Montenegro", "몬테네그로");
        put("North Macedonia", "북마케도니아");
        put("Albania", "알바니아");
        put("Republic of Kosovo", "코소보");
        put("Moldova", "몰도바");
        put("Ukraine", "우크라이나");
        put("Belarus", "벨라루스");
        put("Lithuania", "리투아니아");
        put("Latvia", "라트비아");
        put("Estonia", "에스토니아");
        put("Finland", "핀란드");
        put("Aland Islands", "올란드");
        put("Sweden", "스웨덴");
        put("Norway", "노르웨이");
        put("Svalbard", "스발바르");
        put("Bear Island", "베어섬");
        put("Jan Mayen", "얀마옌");
        put("Denmark", "덴마크");
        put("Faroe Islands", "페로제도");
        put("Iceland", "아이슬란드");
        put("Greenland", "그린란드");
        put("Malta", "몰타");
        put("Sov Mil Order of Malta", "몰타기사단");
        put("San Marino", "산마리노");
        put("Vatican City", "바티칸");
        put("Monaco", "모나코");
        put("Andorra", "안도라");
        put("Gibraltar", "지브롤터");
        put("Corsica", "코르시카");
        put("Market Reef", "마켓리프");

        // 아프리카
        put("Egypt", "이집트");
        put("Libya", "리비아");
        put("Tunisia", "튀니지");
        put("Algeria", "알제리");
        put("Morocco", "모로코");
        put("Western Sahara", "서사하라");
        put("Mauritania", "모리타니");
        put("Senegal", "세네갈");
        put("The Gambia", "감비아");
        put("Guinea", "기니");
        put("Guinea-Bissau", "기니비사우");
        put("Cape Verde", "카보베르데");
        put("Sierra Leone", "시에라리온");
        put("Liberia", "라이베리아");
        put("Cote d'Ivoire", "코트디부아르");
        put("Ghana", "가나");
        put("Togo", "토고");
        put("Benin", "베냉");
        put("Nigeria", "나이지리아");
        put("Niger", "니제르");
        put("Burkina Faso", "부르키나파소");
        put("Mali", "말리");
        put("Chad", "차드");
        put("Sudan", "수단");
        put("Republic of South Sudan", "남수단");
        put("Eritrea", "에리트레아");
        put("Djibouti", "지부티");
        put("Ethiopia", "에티오피아");
        put("Somalia", "소말리아");
        put("Kenya", "케냐");
        put("Uganda", "우간다");
        put("Tanzania", "탄자니아");
        put("Rwanda", "르완다");
        put("Burundi", "부룬디");
        put("Dem. Rep. of the Congo", "콩고민주공화국");
        put("Republic of the Congo", "콩고공화국");
        put("Gabon", "가봉");
        put("Equatorial Guinea", "적도기니");
        put("Annobon Island", "안노본");
        put("Sao Tome & Principe", "상투메프린시페");
        put("Cameroon", "카메룬");
        put("Central African Republic", "중앙아프리카공화국");
        put("Angola", "앙골라");
        put("Zambia", "잠비아");
        put("Malawi", "말라위");
        put("Mozambique", "모잠비크");
        put("Zimbabwe", "짐바브웨");
        put("Botswana", "보츠와나");
        put("Namibia", "나미비아");
        put("South Africa", "남아프리카공화국");
        put("Lesotho", "레소토");
        put("Kingdom of Eswatini", "에스와티니");
        put("Madagascar", "마다가스카르");
        put("Reunion Island", "레위니옹");
        put("Mauritius", "모리셔스");
        put("Rodriguez Island", "로드리게스");
        put("Agalega & St. Brandon", "아갈레가·세인트브랜던");
        put("Seychelles", "세이셸");
        put("Comoros", "코모로");
        put("Mayotte", "마요트");
        put("Juan de Nova, Europa", "후안데노바·유로파");
        put("Glorioso Islands", "글로리오소");
        put("Tromelin Island", "트로믈랭");
        put("Chagos Islands", "차고스");
        put("St. Helena", "세인트헬레나");
        put("Ascension Island", "어센션");
        put("Tristan da Cunha & Gough", "트리스탄다쿠냐·고프");
        put("Fernando de Noronha", "페르난두지노로냐");
        put("Trindade & Martim Vaz", "트린다데·마르팅바스");
        put("St. Peter & St. Paul", "세인트피터·폴");

        // 아메리카
        put("United States", "미국");
        put("USA", "미국");
        put("Alaska", "알래스카");
        put("Hawaii", "하와이");
        put("Canada", "캐나다");
        put("Mexico", "멕시코");
        put("Revillagigedo", "레비야히헤도");
        put("Guatemala", "과테말라");
        put("Belize", "벨리즈");
        put("El Salvador", "엘살바도르");
        put("Honduras", "온두라스");
        put("Nicaragua", "니카라과");
        put("Costa Rica", "코스타리카");
        put("Panama", "파나마");
        put("Cuba", "쿠바");
        put("Jamaica", "자메이카");
        put("Haiti", "아이티");
        put("Dominican Republic", "도미니카공화국");
        put("Puerto Rico", "푸에르토리코");
        put("US Virgin Islands", "미국령버진아일랜드");
        put("British Virgin Islands", "영국령버진아일랜드");
        put("Anguilla", "앵귈라");
        put("Antigua & Barbuda", "앤티가바부다");
        put("Montserrat", "몬트세랫");
        put("Guadeloupe", "과들루프");
        put("Dominica", "도미니카");
        put("Martinique", "마르티니크");
        put("St. Lucia", "세인트루시아");
        put("St. Vincent", "세인트빈센트");
        put("Grenada", "그레나다");
        put("Barbados", "바베이도스");
        put("Trinidad & Tobago", "트리니다드토바고");
        put("Aruba", "아루바");
        put("Curacao", "퀴라소");
        put("Bonaire", "보네르");
        put("Saba & St. Eustatius", "사바·세인트유스타티우스");
        put("Sint Maarten", "신트마르턴");
        put("St. Martin", "생마르탱");
        put("St. Barthelemy", "생바르텔레미");
        put("St. Kitts & Nevis", "세인트키츠네비스");
        put("Turks & Caicos Islands", "터크스케이커스");
        put("Bahamas", "바하마");
        put("Bermuda", "버뮤다");
        put("Cayman Islands", "케이맨");
        put("Colombia", "콜롬비아");
        put("San Andres & Providencia", "산안드레스·프로비덴시아");
        put("Venezuela", "베네수엘라");
        put("Aves Island", "아베스");
        put("Guyana", "가이아나");
        put("Suriname", "수리남");
        put("French Guiana", "프랑스령기아나");
        put("Ecuador", "에콰도르");
        put("Galapagos Islands", "갈라파고스");
        put("Peru", "페루");
        put("Brazil", "브라질");
        put("Bolivia", "볼리비아");
        put("Chile", "칠레");
        put("Easter Island", "이스터섬");
        put("Juan Fernandez Islands", "후안페르난데스");
        put("San Felix & San Ambrosio", "산펠릭스·산암브로시오");
        put("Argentina", "아르헨티나");
        put("Uruguay", "우루과이");
        put("Paraguay", "파라과이");
        put("Falkland Islands", "포클랜드");
        put("South Georgia Island", "사우스조지아");
        put("South Orkney Islands", "사우스오크니");
        put("South Shetland Islands", "사우스셰틀랜드");
        put("South Sandwich Islands", "사우스샌드위치");
        put("Guantanamo Bay", "관타나모");
        put("Navassa Island", "나바사");
        put("Desecheo Island", "데세체오");
        put("Sable Island", "세이블");

        // 오세아니아 · 태평양
        put("Australia", "호주");
        put("Lord Howe Island", "로드하우");
        put("New Zealand", "뉴질랜드");
        put("Chatham Islands", "채텀");
        put("Kermadec Islands", "케르마덱");
        put("N.Z. Subantarctic Is.", "뉴질랜드 남극제도");
        put("Papua New Guinea", "파푸아뉴기니");
        put("Solomon Islands", "솔로몬제도");
        put("Vanuatu", "바누아투");
        put("New Caledonia", "뉴칼레도니아");
        put("Chesterfield Islands", "체스터필드");
        put("Fiji", "피지");
        put("Rotuma Island", "로투마");
        put("Conway Reef", "콘웨이리프");
        put("Samoa", "사모아");
        put("American Samoa", "아메리칸사모아");
        put("Tonga", "통가");
        put("Niue", "니우에");
        put("Tokelau Islands", "토켈라우");
        put("Wallis & Futuna Islands", "왈리스푸투나");
        put("Tuvalu", "투발루");
        put("Kiribati", "키리바시");
        put("Western Kiribati", "키리바시(서부)");
        put("Central Kiribati", "키리바시(중부)");
        put("Eastern Kiribati", "키리바시(동부)");
        put("Banaba Island", "바나바");
        put("Nauru", "나우루");
        put("Marshall Islands", "마셜제도");
        put("Palau", "팔라우");
        put("Micronesia", "미크로네시아");
        put("Guam", "괌");
        put("Mariana Islands", "마리아나");
        put("Northern Mariana Islands", "마리아나");
        put("Wake Island", "웨이크");
        put("Midway Island", "미드웨이");
        put("Kure Island", "쿠레");
        put("Johnston Island", "존스턴");
        put("Palmyra & Jarvis Islands", "팔미라·자비스");
        put("Baker & Howland Islands", "베이커·하울랜드");
        put("Swains Island", "스웨인스");
        put("French Polynesia", "프랑스령폴리네시아");
        put("Austral Islands", "오스트랄");
        put("Marquesas Islands", "마르키즈");
        put("North Cook Islands", "쿡제도(북부)");
        put("South Cook Islands", "쿡제도(남부)");
        put("Pitcairn Island", "핏케언");
        put("Ducie Island", "듀시");
        put("Norfolk Island", "노퍽");
        put("Willis Island", "윌리스");
        put("Mellish Reef", "멜리시리프");
        put("Cocos (Keeling) Islands", "코코스(킬링)");
        put("Christmas Island", "크리스마스섬");
        put("Macquarie Island", "매쿼리");
        put("Nauru Island", "나우루");
        put("Auckland Islands", "오클랜드제도");
        put("Antarctica", "남극");
        put("Bouvet", "부베섬");
        put("Heard Island", "허드섬");
        put("Peter 1 Island", "피터1세섬");
        put("Pr. Edward & Marion Is.", "프린스에드워드·매리언");
        put("Crozet Island", "크로제");
        put("Kerguelen Islands", "케르겔렌");
        put("Amsterdam & St. Paul Is.", "암스테르담·세인트폴");
        put("St. Paul Island", "세인트폴");
        put("Macquarie", "매쿼리");
        put("Clipperton Island", "클리퍼턴");

        // 그 밖에 자주 보는 이름
        put("Cocos Island", "코코스섬");
        put("Lakshadweep", "락샤드위프");
        put("Glorioso", "글로리오소");
    }

    private static void put(String english, String korean) {
        if (english != null && korean != null) {
            KO.put(english, korean);
        }
    }

    private CountryNameKo() {
    }

    /**
     * 영어 나라 이름을 한국어 짧은 이름으로 바꾼다.
     *
     * @param english CTY.DAT 의 나라 이름(예: "Japan")
     * @return 한국어 이름(예: "일본"). 표에 없으면 들어온 이름 그대로.
     */
    public static String toKorean(String english) {
        if (english == null) {
            return "";
        }
        String name = english.trim();
        if (name.isEmpty()) {
            return "";
        }
        String korean = KO.get(name);
        if (korean != null) {
            return korean;
        }
        // 표에 없는 중국의 성(省) 이름은 모두 중국으로 묶는다
        if (name.endsWith(" China")) {
            return "중국";
        }
        return name;
    }
}
