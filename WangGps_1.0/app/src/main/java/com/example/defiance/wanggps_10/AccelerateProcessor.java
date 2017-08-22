package com.example.defiance.wanggps_10;

import android.app.Activity.*;
import android.app.Notification;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.util.Log;



import com.example.defiance.wanggps_10.database.DownZAccePossibilityMap;
import com.example.defiance.wanggps_10.database.PlainZAccePossibilityMap;
import com.example.defiance.wanggps_10.database.UpZAccePossibilityMap;

import org.litepal.crud.DataSupport;

import java.util.List;


/**
 * Created by defiance on 2017/8/10.
 */

public class AccelerateProcessor{
    private static final String TAG = "AccelerateProcessor";
    AccelerateMessage tmp;
    double ZAcceMes[];
    double ZPossibilityMap[];
    private int ZAcceToken;
    int ZAcceTokenMap[];
    private int count;
    private long totalToken;
    private int mode;
    public int ZAcceTokenList[];
    public Handler handler;

    public AccelerateProcessor()
    {
        count=0;
        totalToken=0;
        ZAcceMes=new double[100];
        ZAcceTokenMap=new int[2000];
        ZPossibilityMap=new double[2000];
        ZAcceTokenList=new int[100];
        tmp=new AccelerateMessage();
        mode=0;
    }


    public void setMode(int mode)
    {
        if(mode==0||mode==1||mode==2||mode==3)
        {
            this.mode=mode;
            Log.i(TAG,"当前加速度processor模式id: "+mode);
        }
        else
            Log.i(TAG,"模式选择出错");
    }

    public int getMode()
    {
        return mode;
    }

    AccelerateMessage sub(AccelerateMessage gravity,AccelerateMessage total)
    {
        tmp.Xaxis=total.Xaxis-gravity.Xaxis;
        tmp.Yaxis=total.Yaxis-gravity.Yaxis;
        tmp.Zaxis=total.Zaxis-gravity.Zaxis;
        return tmp;
    }

    double ZgCalculate(AccelerateMessage gravity,AccelerateMessage net)
    {
        double x,y,z;
        count++;
        if(net.Xaxis>0.5 || net.Xaxis<-0.5)
        {
            x=net.Xaxis/9.8*gravity.Xaxis;
        }
        else x=0;
        if(net.Yaxis>0.5 || net.Yaxis<-0.5)
        {
            y=net.Yaxis/9.8*gravity.Yaxis;
        }
        else y=0;
        if(net.Zaxis>0.5 || net.Zaxis<-0.5)
        {
            z=net.Zaxis/9.8*gravity.Zaxis;
        }
        else z=0;
        if(count<100)
        {
            ZAcceMes[count]=x+y+z;
            ZAcceTokenList[count]=(int)((ZAcceMes[count]+10)*100);
        }
        else
        {
            calculateMap(mode);
            count=0;
            ZAcceMes[count]=x+y+z;
            ZAcceTokenList[count]=(int)((ZAcceMes[count]+10)*100);
        }
        return (x + y + z);
    }

    private void calculateMap(int mode) {
        switch (mode) {
            case 1:
            case 2:
            case 3: {
                int ZAcceToken;
                for (int i = 0; i < 100; i++) {
                    double thisZAcceMes;
                    thisZAcceMes = ZAcceMes[i];
                    ZAcceToken = (int) ((thisZAcceMes + 10.0) * 100);
                    if (ZAcceToken < 2000 && ZAcceToken > 0) {
                        ZAcceTokenMap[ZAcceToken]++;
                    }
                }
                for (int i = 0; i < 2000; i++) {
                    ZPossibilityMap[i] = (ZPossibilityMap[i] * totalToken + ZAcceTokenMap[i]) / (totalToken + 100);
                    if (ZPossibilityMap[i] != 0)
                        Log.i(TAG, "i: " + i + " data: " + ZPossibilityMap[i] + " tokens: " + totalToken);
                }
                for (int i = 0; i < 2000; i++) ZAcceTokenMap[i] = 0;
                totalToken = totalToken + 100;
            }
        }
    }

    public void renewMap(final int mode)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (mode) {
                    case 1: {
                        List<UpZAccePossibilityMap> tmp = DataSupport.findAll(UpZAccePossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=10) {
                            Log.i(TAG, "表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                UpZAccePossibilityMap upZAccePossibilityMap = new UpZAccePossibilityMap();
                                //if(ZPossibilityMap[i]-0<-0.0005||ZPossibilityMap[i]-0>0.0005);
                                //Log.i(TAG,ZPossibilityMap[i]+"      "+i);
                                upZAccePossibilityMap.setBaseData(i);
                                upZAccePossibilityMap.setPossibility(ZPossibilityMap[i]);
                                upZAccePossibilityMap.setTotalToken(totalToken);
                                upZAccePossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "表数据受损，重新建标表");
                                DataSupport.deleteAll(UpZAccePossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(1);
                            } else {

                                Log.i(TAG, "表数据完整，更新数据");
                                for (UpZAccePossibilityMap mapData : tmp) {
                                    //if(ZPossibilityMap[i]-0<-0.0005||ZPossibilityMap[i]-0>0.0005);
                                    //Log.i(TAG,ZPossibilityMap[i]+"      "+i);
                                    mapData.setPossibility(ZPossibilityMap[i]);
                                    mapData.setBaseData(i);
                                    mapData.setTotalToken(totalToken);
                                    mapData.save();
                                    i++;
                                }
                            }
                        }
                        Log.i(TAG, "数据更新完成");
                        break;
                    }

                    case 2:
                    {
                        List<PlainZAccePossibilityMap> tmp = DataSupport.findAll(PlainZAccePossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=10) {
                            Log.i(TAG, "表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                PlainZAccePossibilityMap plainZAccePossibilityMap = new PlainZAccePossibilityMap();
                                plainZAccePossibilityMap.setBaseData(i);
                                plainZAccePossibilityMap.setPossibility(ZPossibilityMap[i]);
                                plainZAccePossibilityMap.setTotalToken(totalToken);
                                plainZAccePossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "表数据受损，重新建标表");
                                DataSupport.deleteAll(PlainZAccePossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(2);
                            } else {
                                Log.i(TAG, "表数据完整，更新数据");
                                for (PlainZAccePossibilityMap mapData : tmp) {
                                    mapData.setPossibility(ZPossibilityMap[i]);
                                    mapData.setBaseData(i);
                                    mapData.setTotalToken(totalToken);
                                    mapData.save();
                                    i++;
                                }
                            }
                        }
                        Log.i(TAG, "数据更新完成");
                        break;
                    }

                    case 3:
                    {
                        List<DownZAccePossibilityMap> tmp = DataSupport.findAll(DownZAccePossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=10) {
                            Log.i(TAG, "表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                DownZAccePossibilityMap downZAccePossibilityMap = new DownZAccePossibilityMap();
                                downZAccePossibilityMap.setBaseData(i);
                                downZAccePossibilityMap.setPossibility(ZPossibilityMap[i]);
                                downZAccePossibilityMap.setTotalToken(totalToken);
                                downZAccePossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "表数据受损，重新建标表 "+tmp.size());
                                DataSupport.deleteAll(DownZAccePossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(3);
                            } else {
                                Log.i(TAG, "表数据完整，更新数据");
                                for (DownZAccePossibilityMap mapData : tmp) {
                                    mapData.setPossibility(ZPossibilityMap[i]);
                                    mapData.setBaseData(i);
                                    mapData.setTotalToken(totalToken);
                                    mapData.save();
                                    i++;
                                }
                            }
                        }
                        Log.i(TAG, "数据更新完成");
                        break;
                    }

                }
            }
        }).start();

    }

    public void readMap(int mode) {
        switch (mode) {
            case 1: {
                Log.i(TAG, "读表");
                List<UpZAccePossibilityMap> tmp = DataSupport.findAll(UpZAccePossibilityMap.class);
                int i = 0;
                boolean flag = true;
                for (UpZAccePossibilityMap mapData : tmp) {
                    if (i == mapData.getBaseData()) {
                        //if(mapData.getPossibility()-0>0.0005||mapData.getPossibility()-0<-0.0005);
                        //Log.i(TAG,mapData.getPossibility()+"      "+i);
                        ZPossibilityMap[i] = mapData.getPossibility();
                        totalToken = mapData.getTotalToken();
                    } else {
                        Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                        flag = false;
                    }
                    i++;
                }
                if (!flag) {
                    DataSupport.deleteAll(UpZAccePossibilityMap.class);
                    renewMap(1);
                }
                break;
            }

            case 2: {
                Log.i(TAG, "读表");
                List<PlainZAccePossibilityMap> tmp = DataSupport.findAll(PlainZAccePossibilityMap.class);
                int i = 0;
                boolean flag = true;
                for (PlainZAccePossibilityMap mapData : tmp) {
                    if (i == mapData.getBaseData()) {

                        ZPossibilityMap[i] = mapData.getPossibility();
                        totalToken = mapData.getTotalToken();
                    } else {
                        Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                        flag = false;
                    }
                    i++;
                }
                if (!flag) {
                    DataSupport.deleteAll(PlainZAccePossibilityMap.class);
                    renewMap(2);
                }
                break;
            }

            case 3: {
                Log.i(TAG, "读表");
                List<DownZAccePossibilityMap> tmp = DataSupport.findAll(DownZAccePossibilityMap.class);
                int i = 0;
                boolean flag = true;
                for (DownZAccePossibilityMap mapData : tmp) {
                    if (i == mapData.getBaseData()) {

                        ZPossibilityMap[i] = mapData.getPossibility();
                        totalToken = mapData.getTotalToken();
                    } else {
                        Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                        flag = false;
                    }
                    i++;
                }
                if (!flag) {
                    DataSupport.deleteAll(DownZAccePossibilityMap.class);
                    renewMap(3);
                }
                break;
            }
        }


    }

}
