package com.bg7yoz.ft8cn.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareLogs {
    private static final String TAG = "ShareLogs";
    private boolean isCancel=false;
    //한국어판(0.93-kr17): 이번에 내보낸 기록 중 가장 큰 QSO id (새 교신만 내보내기의 기준점)
    private long lastExportedId = 0;

    /**
     * 한국어판(0.93-kr17): afterId 를 주면 그 id 보다 뒤에 저장된 교신만 고른다.
     *
     * @param queryFilter 0: 전체, 1: QSL 받은 것, 2: 못 받은 것
     * @param afterId     이 id 보다 큰 QSO 만 (0 이면 전부)
     */
    private String makeSQL(int queryFilter, long afterId) {
        String filterStr;
        switch (queryFilter) {
            case 1:
                filterStr = "and((q.isQSL =1)or(q.isLotW_QSL =1))\n";
                break;
            case 2:
                filterStr = "and((q.isQSL =0)and(q.isLotW_QSL =0))\n";
                break;
            default:
                filterStr = "";
        }
        String afterStr = afterId > 0 ? String.format("and(q.id > %d)\n", afterId) : "";
        return " FROM QSLTable AS q \n" +
                "WHERE ((CALL LIKE ?)OR(station_callsign LIKE ?))\n" +
                filterStr +
                afterStr;

    }


    @SuppressLint("Range")
    private int getCount(SQLiteDatabase db, String queryKey, int queryFilter, long afterId) {
        String sql = makeSQL(queryFilter, afterId);
        String key = "%" + queryKey + "%";
        Cursor cursor = db.rawQuery("SELECT COUNT(*) AS C " + sql, new String[]{key, key});
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("C"));

        cursor.close();
        return count;
    }

    private Cursor getData(SQLiteDatabase db, String queryKey, int queryFilter, long afterId) {
        String sql = makeSQL(queryFilter, afterId);
        String key = "%" + queryKey + "%";
        return db.rawQuery("SELECT * " + sql, new String[]{key, key});
    }

    /**
     * 한국어판(0.93-kr17): 주어진 id 보다 뒤에 저장된 교신 수 (새 교신만 내보낼 때 미리 확인용)
     */
    public int countLogsAfter(SQLiteDatabase db, long afterId) {
        return getCount(db, "", 0, afterId);
    }

    /**
     * 한국어판(0.93-kr17): QSO 표에 든 가장 큰 id. 표를 비우거나 새로 만들었는지 가늠하는 데 쓴다.
     */
    @SuppressLint("Range")
    public long getMaxLogId(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT MAX(id) AS M FROM QSLTable", null);
        long maxId = 0;
        if (cursor.moveToFirst()) {
            maxId = cursor.getLong(cursor.getColumnIndex("M"));
        }
        cursor.close();
        return maxId;
    }

    /**
     * 把日志数据写入到文件中，用于分享等处理
     *
     * @param db             数据库
     * @param queryKey       关键词
     * @param queryFilter    过滤条件
     * @param adiFile        临时文件
     * @param isSWL          是否是swl模式
     * @param afterId        这个 id 之后新增的才写 (0 = 全部) — 한국어판(0.93-kr17)
     * @param onGetShareLogs 回调
     */
    @SuppressLint({"DefaultLocale", "Range"})
    private void downQSLTableToFile(SQLiteDatabase db, String queryKey, int queryFilter, File adiFile
            , boolean isSWL, long afterId
            , OnShareLogEvents onGetShareLogs) {
        final int count = getCount(db, queryKey, queryFilter, afterId);
        lastExportedId = 0;

        if (onGetShareLogs != null) {
            onGetShareLogs.onShareStart(count, String.format(
                    GeneralVariables.getStringFromResource(R.string.total_logs)
                    , count));
        }
        Cursor cursor = getData(db, queryKey, queryFilter, afterId);
        FileOutputStream fileOutputStream = null;
        int position = 0;
        try {
            fileOutputStream = new FileOutputStream(adiFile, true);
            fileOutputStream.write("FT8CN ADIF Export<eoh>\n".getBytes());
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                position++;
                //한국어판(0.93-kr17): 여기까지 내보냈다는 표시(가장 큰 QSO id)를 남긴다
                if (cursor.getColumnIndex("id") != -1) {
                    long id = cursor.getLong(cursor.getColumnIndex("id"));
                    if (id > lastExportedId) {
                        lastExportedId = id;
                    }
                }
                if (onGetShareLogs != null) {
                   if (!onGetShareLogs.onShareProgress(count, position
                            , String.format(GeneralVariables.getStringFromResource(R.string.get_log_no)
                                    , position))){
                       break;
                   };
                }
                fileOutputStream.write(String.format("<call:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("call")).length()
                        , cursor.getString(cursor.getColumnIndex("call"))).getBytes());
                if (!isSWL) {
                    if (cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1) {
                        fileOutputStream.write("<QSL_RCVD:1>Y ".getBytes());
                    } else {
                        fileOutputStream.write("<QSL_RCVD:1>N ".getBytes());
                    }
                    if (cursor.getInt(cursor.getColumnIndex("isQSL")) == 1) {
                        fileOutputStream.write("<QSL_MANUAL:1>Y ".getBytes());
                    } else {
                        fileOutputStream.write("<QSL_MANUAL:1>N ".getBytes());
                    }
                } else {
                    fileOutputStream.write("<swl:1>Y ".getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("gridsquare")) != null) {
                    fileOutputStream.write(String.format("<gridsquare:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("gridsquare")).length()
                            , cursor.getString(cursor.getColumnIndex("gridsquare"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("mode")) != null) {
                    fileOutputStream.write(String.format("<mode:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("mode")).length()
                            , cursor.getString(cursor.getColumnIndex("mode"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("rst_sent")) != null) {
                    fileOutputStream.write(String.format("<rst_sent:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("rst_sent")).length()
                            , cursor.getString(cursor.getColumnIndex("rst_sent"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("rst_rcvd")) != null) {
                    fileOutputStream.write(String.format("<rst_rcvd:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("rst_rcvd")).length()
                            , cursor.getString(cursor.getColumnIndex("rst_rcvd"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("qso_date")) != null) {
                    fileOutputStream.write(String.format("<qso_date:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("qso_date")).length()
                            , cursor.getString(cursor.getColumnIndex("qso_date"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("time_on")) != null) {
                    fileOutputStream.write(String.format("<time_on:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("time_on")).length()
                            , cursor.getString(cursor.getColumnIndex("time_on"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("qso_date_off")) != null) {
                    fileOutputStream.write( String.format("<qso_date_off:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("qso_date_off")).length()
                            , cursor.getString(cursor.getColumnIndex("qso_date_off"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("time_off")) != null) {
                    fileOutputStream.write(String.format("<time_off:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("time_off")).length()
                            , cursor.getString(cursor.getColumnIndex("time_off"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("band")) != null) {
                    fileOutputStream.write(String.format("<band:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("band")).length()
                            , cursor.getString(cursor.getColumnIndex("band"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("freq")) != null) {
                    fileOutputStream.write(String.format("<freq:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("freq")).length()
                            , cursor.getString(cursor.getColumnIndex("freq"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("station_callsign")) != null) {
                    fileOutputStream.write(String.format("<station_callsign:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("station_callsign")).length()
                            , cursor.getString(cursor.getColumnIndex("station_callsign"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("my_gridsquare")) != null) {
                    fileOutputStream.write(String.format("<my_gridsquare:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("my_gridsquare")).length()
                            , cursor.getString(cursor.getColumnIndex("my_gridsquare"))).getBytes());
                }

                if (cursor.getColumnIndex("operator") != -1) {
                    if (cursor.getString(cursor.getColumnIndex("operator")) != null) {
                        fileOutputStream.write(String.format("<operator:%d>%s "
                                , cursor.getString(cursor.getColumnIndex("operator")).length()
                                , cursor.getString(cursor.getColumnIndex("operator"))).getBytes());
                    }
                }
                String comment = cursor.getString(cursor.getColumnIndex("comment"));

                //<comment:15>Distance: 99 km <eor>
                //在写库的时候，一定要加" km"
                fileOutputStream.write(String.format("<comment:%d>%s <eor>\n"
                        , comment.length()
                        , comment).getBytes());
            }


        } catch (IOException e) {
            Log.e(TAG,String.format("写文件出错：%s",e.getMessage()));
            ToastMessage.show(String.format(GeneralVariables
                    .getStringFromResource(R.string.write_file_error), e.getMessage()));
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, String.format("关闭写文件出错：%s", e.getMessage()));
                ToastMessage.show(String.format(GeneralVariables
                        .getStringFromResource(R.string.write_file_error), e.getMessage()));
            }
        }

        if (onGetShareLogs != null) {
            onGetShareLogs.afterGet(count, String.format(
                    GeneralVariables.getStringFromResource(R.string.total_logs)
                    , position));
        }
        Log.d(TAG, String.format("写入数据%d条", position));

        cursor.close();
    }

    /**
     * 分享文件
     *
     * @param context Context
     * @param file    文件对象
     * @param title   标题
     */
    public void doShareLogs(Context context, File file, String title
            , SQLiteDatabase db, String queryKey, int queryFilter, File adiFile
            , boolean isSWL
            , OnShareLogEvents onGetShareLogs) {

        isCancel=false;

        downQSLTableToFile(db, queryKey, queryFilter, adiFile, false, 0
                , wrapEvents(context, file, title, false, onGetShareLogs));


    }

    /**
     * 한국어판(0.93-kr17): 마지막으로 내보낸 뒤 새로 저장된 교신만 ADIF 로 만들어 공유한다.
     * 검색 조건(호출부호·QSL 여부)과 상관없이 새 교신을 모두 넣는다.
     *
     * @param afterId 이 id 보다 뒤에 저장된 교신만 내보낸다(마지막 내보내기 기준점)
     * @return 실제로 내보낸 가장 큰 QSO id. 내보낼 것이 없거나 사용자가 그만두었으면 0.
     */
    public long doShareNewLogs(Context context, File file, String title
            , SQLiteDatabase db, long afterId, File adiFile
            , OnShareLogEvents onGetShareLogs) {

        isCancel=false;

        downQSLTableToFile(db, "", 0, adiFile, false, afterId
                , wrapEvents(context, file, title, true, onGetShareLogs));

        if (isCancel || lastExportedId <= afterId) {
            return 0;//공유하지 않았거나(중간 취소) 새 기록이 없으면 기준점을 올리지 않는다
        }
        return lastExportedId;
    }

    /**
     * 한국어판(0.93-kr17): 파일 쓰기 진행 상황을 화면에 전하고, 끝나면 공유 창을 여는 공통 처리.
     */
    private OnShareLogEvents wrapEvents(final Context context, final File file
            , final String title, final boolean onlyIfNotEmpty
            , final OnShareLogEvents onGetShareLogs) {
        return new OnShareLogEvents() {
            @Override
            public void onPreparing(String info) {
                if (onGetShareLogs!=null){
                    onGetShareLogs.onPreparing(info);
                }
            }

            @Override
            public void onShareStart(int count, String info) {
                if (onGetShareLogs != null) {
                    onGetShareLogs.onShareStart(count, info);
                }
            }

            @Override
            public boolean onShareProgress(int count, int position, String info) {
                if (onGetShareLogs != null) {
                    boolean temp=onGetShareLogs.onShareProgress(count, position, info);
                    isCancel=!temp;
                   return temp;
                }
                return true;
            }

            @Override
            public void afterGet( int count, String info) {
                if (onGetShareLogs != null) {
                    onGetShareLogs.afterGet( count, info);
                }
                if (!isCancel && (!onlyIfNotEmpty || lastExportedId > 0)) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext()
                            , "com.bg7yoz.ft8cn.kr.fileprovider", file);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(sharingIntent, title));
                }
            }

            @Override
            public void onShareFailed(String info) {
                if (onGetShareLogs!=null){
                    onGetShareLogs.onShareFailed(info);
                }
            }
        };
    }



}
