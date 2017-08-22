package com.example.defiance.wanggps_10;

import android.util.Log;

import com.example.defiance.wanggps_10.database.DownGpsPossibilityMap;
import com.example.defiance.wanggps_10.database.DownZAccePossibilityMap;
import com.example.defiance.wanggps_10.database.PlainGpsPossibilityMap;
import com.example.defiance.wanggps_10.database.PlainZAccePossibilityMap;
import com.example.defiance.wanggps_10.database.UpGpsPossibilityMap;
import com.example.defiance.wanggps_10.database.UpZAccePossibilityMap;

import org.litepal.crud.DataSupport;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by defiance on 2017/8/13.
 */

public class Vertibi {
    GpsProcessor gpsProcessor;
    AccelerateProcessor accelerateProcessor;
    double[] downAcceMap=new double[2000];
    double[] plainAcceMap=new double[2000];
    double[] upAcceMap=new double[2000];
    double[] downGpsMap=new double[2000];
    double[] plainGpsMap=new double[2000];
    double[] upGpsMap=new double[2000];
    //double[][] gpsConfusionMatrix=new double[3][2000];
    //double[][] acceConfusionMatrix=new double[3][2000];
    //double[][] transferMatrix=new double[3][3];
    MovingStatus movingStatusGps=new MovingStatus();
    MovingStatus movingStatusAcce=new MovingStatus();
    private double K_gps,K_barometer;

    public void getMaps(){
        new Thread(new Runnable() {
            @Override
            public void run() {
        int i = 0;
        boolean flag = true;
        Log.i(TAG, "算法读Gps表");
        List<DownZAccePossibilityMap> tmp1 = DataSupport.findAll(DownZAccePossibilityMap.class);
        for (DownZAccePossibilityMap mapData : tmp1) {
            if (i == mapData.getBaseData()) {

                downAcceMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }
        i=0;
        List<PlainZAccePossibilityMap> tmp2 = DataSupport.findAll(PlainZAccePossibilityMap.class);
        for (PlainZAccePossibilityMap mapData : tmp2) {
            if (i == mapData.getBaseData()) {

                plainAcceMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }
        i=0;
        List<UpZAccePossibilityMap> tmp3 = DataSupport.findAll(UpZAccePossibilityMap.class);
        for (UpZAccePossibilityMap mapData : tmp3) {
            if (i == mapData.getBaseData()) {

                upAcceMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }
        i=0;
        Log.i(TAG, "算法读加速度表");
        List<DownGpsPossibilityMap> tmp4 = DataSupport.findAll(DownGpsPossibilityMap.class);
        for (DownGpsPossibilityMap mapData : tmp4) {
            if (i == mapData.getBaseData()) {

                downGpsMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }
        i=0;
        List<PlainGpsPossibilityMap> tmp5 = DataSupport.findAll(PlainGpsPossibilityMap.class);
        for (PlainGpsPossibilityMap mapData : tmp5) {
            if (i == mapData.getBaseData()) {

                plainGpsMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }
        i=0;
        List<UpGpsPossibilityMap> tmp6 = DataSupport.findAll(UpGpsPossibilityMap.class);
        for (UpGpsPossibilityMap mapData : tmp6) {
            if (i == mapData.getBaseData()) {

                upGpsMap[i] = mapData.getPossibility();
            } else {
                Log.i(TAG, "database " + mapData.getBaseData() + "  possi: " + mapData.getPossibility() + "  token: " + mapData.getTotalToken());
                flag = false;
            }
            i++;
        }

        if(flag)
        {
            for(i=0;i<2000;i++)
            {
                movingStatusGps.emission_probability[0][i]=upGpsMap[i];
            }
            for(i=0;i<2000;i++)
            {
                movingStatusGps.emission_probability[1][i]=plainGpsMap[i];
            }
            for(i=0;i<2000;i++)
            {
                movingStatusGps.emission_probability[2][i]=downGpsMap[i];
            }
            for(i=0;i<2000;i++)
            {
                movingStatusAcce.emission_probability[0][i]=upAcceMap[i];
            }
            for(i=0;i<2000;i++)
            {
                movingStatusAcce.emission_probability[1][i]=plainAcceMap[i];
            }
            for(i=0;i<2000;i++)
            {
                movingStatusAcce.emission_probability[2][i]=downAcceMap[i];
            }
        }
            }
        }).start();
    }
    //getMaps算法从6张数据表中获取了数据并且构建了两个矩阵，作为表观矩阵，之后将会用数据的数据结合表观矩阵调整隐式转移矩阵的值

    public Vertibi(GpsProcessor gpsProcessor, AccelerateProcessor accelerateProcessor)
    {
        getMaps();
        this.gpsProcessor=gpsProcessor;
        this.accelerateProcessor = accelerateProcessor;
    }

    public void readData()
    {
        int i=0;
        for(int mes:accelerateProcessor.ZAcceTokenList) {
        movingStatusAcce.observations[i]=mes;
        }
        for(int j=0;j<100;j++)
        {
            movingStatusGps.observations[i]=gpsProcessor.gpsTokenList[i];
        }
    }

    /**
     * 求解HMM模型
     * @return 最可能的序列
     */
    public int[] computeAcce()
    {
        double[][] V = new double[movingStatusAcce.observations.length][movingStatusAcce.states.length];
        int[][] path = new int[movingStatusAcce.states.length][movingStatusAcce.observations.length];

        for (int y : movingStatusAcce.states)
        {
            V[0][y] = movingStatusAcce.start_probability[y] * movingStatusAcce.emission_probability[y][movingStatusAcce.observations[0]];
            path[y][0] = y;
        }

        for (int t = 1; t < movingStatusAcce.observations.length; ++t)
        {
            int[][] newpath = new int[movingStatusAcce.states.length][movingStatusAcce.observations.length];

            for (int y : movingStatusAcce.states)
            {
                double prob = -1;
                int state;
                for (int y0 : movingStatusAcce.states)
                {
                    double nprob = V[t - 1][y0] * movingStatusAcce.transititon_probability[y0][y] * movingStatusAcce.emission_probability[y][movingStatusAcce.observations[t]];
                    if (nprob > prob)
                    {
                        prob = nprob;
                        state = y0;
                        // 记录最大概率
                        V[t][y] = prob;
                        // 记录路径
                        System.arraycopy(path[state], 0, newpath[y], 0, t);
                        newpath[y][t] = y;
                    }
                }
            }

            path = newpath;
        }

        double prob = -1;
        int state = 0;
        for (int y : movingStatusAcce.states)
        {
            if (V[movingStatusAcce.observations.length - 1][y] > prob)
            {
                prob = V[movingStatusAcce.observations.length - 1][y];
                state = y;
            }
        }

        return path[state];
    }

    public int[] computeGps()
    {
        double[][] V = new double[movingStatusGps.observations.length][movingStatusGps.states.length];
        int[][] path = new int[movingStatusGps.states.length][movingStatusGps.observations.length];

        for (int y : movingStatusGps.states)
        {
            V[0][y] = movingStatusGps.start_probability[y] * movingStatusGps.emission_probability[y][movingStatusGps.observations[0]];
            path[y][0] = y;
        }

        for (int t = 1; t < movingStatusGps.observations.length; ++t)
        {
            int[][] newpath = new int[movingStatusGps.states.length][movingStatusGps.observations.length];

            for (int y : movingStatusGps.states)
            {
                double prob = -1;
                int state;
                for (int y0 : movingStatusGps.states)
                {
                    double nprob = V[t - 1][y0] * movingStatusGps.transititon_probability[y0][y] * movingStatusGps.emission_probability[y][movingStatusGps.observations[t]];
                    if (nprob > prob)
                    {
                        prob = nprob;
                        state = y0;
                        // 记录最大概率
                        V[t][y] = prob;
                        // 记录路径
                        System.arraycopy(path[state], 0, newpath[y], 0, t);
                        newpath[y][t] = y;
                    }
                }
            }

            path = newpath;
        }

        double prob = -1;
        int state = 0;
        for (int y : movingStatusGps.states)
        {
            if (V[movingStatusGps.observations.length - 1][y] > prob)
            {
                prob = V[movingStatusGps.observations.length - 1][y];
                state = y;
            }
        }

        return path[state];
    }
}
