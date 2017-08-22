package com.example.defiance.wanggps_10;

import android.util.Log;

import com.example.defiance.wanggps_10.database.DownGpsPossibilityMap;
import com.example.defiance.wanggps_10.database.PlainGpsPossibilityMap;
import com.example.defiance.wanggps_10.database.UpGpsPossibilityMap;

import org.litepal.crud.DataSupport;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by defiance on 2017/8/7.
 */

public class GpsProcessor{
    double velocityPossibilityMap[];
    int velocityTokenMap[];
    long totalToken;
    private int count;
    GPSMessage mesList[];
    public double altitude;
    private int mode;
    public boolean isValid;
    public int gpsTokenList[];

    public GpsProcessor(){
        mesList=new GPSMessage[100];
        for(int i=0;i<100;i++)
            mesList[i]=new GPSMessage();
        velocityTokenMap=new int[2000];
        velocityPossibilityMap=new double[2000];
        gpsTokenList=new int[100];
        totalToken=0;
        count=0;
        mode=0;
        altitude=0.0;
    }

    public void setMode(int mode)
    {
        if(mode==0||mode==1||mode==2||mode==3)
        {
            this.mode=mode;
            Log.i(TAG,"当前gpsProcessor模式id: "+mode);
        }
        else
            Log.i(TAG,"模式选择出错");
    }

    public int getMode()
    {
        return mode;
    }

    public void readIn(GPSMessage newMessage)
    {
        Log.i(TAG,"记录新数据");
        if(count<100&&newMessage.valid==true)
        {
            mesList[count].time=newMessage.time;
            mesList[count].altitude=newMessage.altitude;
            altitude=newMessage.altitude;
            mesList[count].longitude=newMessage.longitude;
            mesList[count].latitude=newMessage.latitude;
            isValid=true;
            gpsTokenList[count]=(int)((mesList[count].altitude+10)*100);
        }
        else if(newMessage.valid==false)
        {
            isValid=false;
        }
        else
        {
            calculateMap();
            count=0;
        }
        count++;
    }

    private void calculateMap(){
        GPSMessage lastMessage;
        lastMessage=new GPSMessage();
        double velocityZ;
        int velocityZToken;
        for(int i=0;i<100;i++)
        {
            GPSMessage thisMessage;
            if (i!=0&&isValid==true);
            {
                thisMessage=mesList[i];
                velocityZ=(thisMessage.altitude-lastMessage.altitude)/((thisMessage.time-lastMessage.time)/(1.0E3));
                velocityZToken=(int)((velocityZ+10.0)*100);
                Log.i(TAG,"this is the "+i+" message");
                Log.i(TAG,thisMessage.altitude+"      "+lastMessage.altitude);
                Log.i(TAG,"time1: "+(thisMessage.time-lastMessage.time)+" velocity: "+velocityZ+ " token: "+velocityZToken);
                if(velocityZToken<2000&&velocityZToken>0)
                {
                    velocityTokenMap[velocityZToken]++;
                }
            }
            lastMessage=thisMessage;
        }
        for(int i=0;i<2000;i++)
        {
            velocityPossibilityMap[i]=(velocityPossibilityMap[i]*totalToken+velocityTokenMap[i])/(totalToken+100);
            if(velocityPossibilityMap[i]!=0)
                Log.i(TAG,"i: "+i+" data: "+velocityPossibilityMap[i]+" tokens: "+totalToken);
        }
        for(int i=0;i<2000;i++)velocityTokenMap[i]=0;
        totalToken=totalToken+100;
    }

    public void renewMap(final int mode)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (mode) {
                    case 1: {
                        List<UpGpsPossibilityMap> tmp = DataSupport.findAll(UpGpsPossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=1) {
                            Log.i(TAG, "上行表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                UpGpsPossibilityMap upGpsPossibilityMap = new UpGpsPossibilityMap();
                                upGpsPossibilityMap.setBaseData(i);
                                upGpsPossibilityMap.setPossibility(velocityPossibilityMap[i]);
                                upGpsPossibilityMap.setTotalToken(totalToken);
                                upGpsPossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "上行表数据受损，重新建标表");
                                DataSupport.deleteAll(UpGpsPossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(1);
                            } else {
                                Log.i(TAG, "上行表数据完整，更新数据");
                                for (UpGpsPossibilityMap mapData : tmp) {
                                    mapData.setPossibility(velocityPossibilityMap[i]);
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
                        List<PlainGpsPossibilityMap> tmp = DataSupport.findAll(PlainGpsPossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=1) {
                            Log.i(TAG, "平行表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                PlainGpsPossibilityMap plainGpsPossibilityMap = new PlainGpsPossibilityMap();
                                plainGpsPossibilityMap.setBaseData(i);
                                plainGpsPossibilityMap.setPossibility(velocityPossibilityMap[i]);
                                plainGpsPossibilityMap.setTotalToken(totalToken);
                                plainGpsPossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "平行表数据受损，重新建标表");
                                DataSupport.deleteAll(PlainGpsPossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(2);
                            } else {
                                Log.i(TAG, "平行表数据完整，更新数据");
                                for (PlainGpsPossibilityMap mapData : tmp) {
                                    mapData.setPossibility(velocityPossibilityMap[i]);
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
                        List<DownGpsPossibilityMap> tmp = DataSupport.findAll(DownGpsPossibilityMap.class);
                        int i = 0;
                        Log.i(TAG, "thread running");
                        if (tmp.size()<=1) {
                            Log.i(TAG, "下行表空，创建新表");
                            for (i = 0; i < 2000; i++) {
                                DownGpsPossibilityMap downGpsPossibilityMap = new DownGpsPossibilityMap();
                                downGpsPossibilityMap.setBaseData(i);
                                downGpsPossibilityMap.setPossibility(velocityPossibilityMap[i]);
                                downGpsPossibilityMap.setTotalToken(totalToken);
                                downGpsPossibilityMap.save();
                            }
                        } else {
                            if (tmp.size() != 2000) {
                                Log.i(TAG, "下行表数据受损，重新建标表");
                                DataSupport.deleteAll(DownGpsPossibilityMap.class);
                                Log.i(TAG,"清空后大小 "+tmp.size());
                                renewMap(3);
                            } else {
                                Log.i(TAG, "下行表数据完整，更新数据");
                                for (DownGpsPossibilityMap mapData : tmp) {
                                    mapData.setPossibility(velocityPossibilityMap[i]);
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

    public void readMap(int flag) {
        if(flag!=0) {
            switch (flag) {
                case 1:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<UpGpsPossibilityMap> tmp = DataSupport.findAll(UpGpsPossibilityMap.class);
                            int i = 0;
                            for (UpGpsPossibilityMap mapData : tmp) {
                                if (i == mapData.getBaseData())
                                    velocityPossibilityMap[i] = mapData.getPossibility();
                                else {
                                    Log.i(TAG, "上行GPS数据存储参数有误");
                                }
                                i++;
                            }
                        }

                    });
                case 2:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<PlainGpsPossibilityMap> tmp = DataSupport.findAll(PlainGpsPossibilityMap.class);
                            int i = 0;
                            for (PlainGpsPossibilityMap mapData : tmp) {
                                if (i == mapData.getBaseData())
                                    velocityPossibilityMap[i] = mapData.getPossibility();
                                else {
                                    Log.i(TAG, "平行GPS数据存储参数有误");
                                }
                                i++;
                            }
                        }

                    });
                case 3:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<DownGpsPossibilityMap> tmp = DataSupport.findAll(DownGpsPossibilityMap.class);
                            int i = 0;
                            for (DownGpsPossibilityMap mapData : tmp) {
                                if (i == mapData.getBaseData())
                                    velocityPossibilityMap[i] = mapData.getPossibility();
                                else {
                                    Log.i(TAG, "下行GPS数据存储参数有误");
                                }
                                i++;
                            }
                        }

                    });
            }
        }
    }
}
