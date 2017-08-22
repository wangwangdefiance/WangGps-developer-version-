package com.example.defiance.wanggps_10;

import android.util.Log;

/**
 * Created by defiance on 2017/8/13.
 */

public class ZAxisAdjuster {
    private static final String TAG = "ZAxisAdjuster";
    //这个方法提供了一个参数可调的卡尔曼滤波静态方法
    //其中的预估量的参数将会由模式判断的结构决定
    //但是方法本身的想法是卡尔曼滤波
    //当某一种数据不能被获得时（比如在楼道内不能使用GPS 时）该方法直接退化为简单的数据测量卡尔曼滤波
    //其中模式判断的影响将会在控制R的取值中中得到体现

    private BarometerProcessor barometerProcessor;
    private GpsProcessor gpsProcessor;

    private double altitude,lastAltitude,K,P,Q,R,baroOnlyK,baroOnlyLastAltitude,baroOnlyLastP,baroOnlyQ,baroOnlyR;
    public int status;
    private static ZAxisAdjuster zAxisAdjuster;

    private ZAxisAdjuster(BarometerProcessor barometerProcessor, GpsProcessor gpsProcessor)
    {
        this.barometerProcessor=barometerProcessor;
        this.gpsProcessor=gpsProcessor;
        baroOnlyK=0.5;
        K=0.5;
        baroOnlyLastAltitude=barometerProcessor.getCurrentAltitude();
        lastAltitude=baroOnlyLastAltitude;
        altitude=lastAltitude;
        baroOnlyQ=0.00005;
        Q=0.00005;
        baroOnlyR=0.00005;
        R=0.00005;
        status=1;//0代表上行，1代表平路，2代表下行
    }

    public static ZAxisAdjuster getZAxisAdjuster(BarometerProcessor barometerProcessor, GpsProcessor gpsProcessor)
    {
        if(zAxisAdjuster==null)
        {zAxisAdjuster=new ZAxisAdjuster(barometerProcessor,gpsProcessor);}
        return zAxisAdjuster;
    }

    public void setStatus(int newStatus)
    {
        this.status=newStatus;
    }

    public double KalmanFilter()
    {
        if(gpsProcessor.isValid)
        {

            double weightGps=0.5,weightBaro=0.5;
            K=P/(P+R);
            lastAltitude=lastAltitude+K*(weightGps*gpsProcessor.altitude+weightBaro*barometerProcessor.getCurrentAltitude()-lastAltitude);
            P=(1-K)*P+Q;
            return lastAltitude;
        }
        else
        {

            baroOnlyK=baroOnlyLastP/(baroOnlyLastP+baroOnlyR);
            baroOnlyLastAltitude=baroOnlyLastAltitude+baroOnlyK*(barometerProcessor.getCurrentAltitude()-baroOnlyLastAltitude);
            baroOnlyLastP=(1-baroOnlyK)*baroOnlyLastP+baroOnlyQ;
            return baroOnlyLastAltitude;
        }
    }
}
