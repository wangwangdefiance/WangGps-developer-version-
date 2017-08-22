package com.example.defiance.wanggps_10.database;

import org.litepal.crud.DataSupport;

/**
 * Created by defiance on 2017/8/13.
 */

public class DownGpsPossibilityMap extends DataSupport {
    private int baseData;
    private double possibility;
    private long totalToken;

    public int getBaseData(){
        return baseData;
    }

    public void setBaseData(int baseData){
        this.baseData=baseData;
    }

    public double getPossibility(){
        return possibility;
    }

    public void setPossibility(double possibility){
        this.possibility=possibility;
    }

    public long getTotalToken(){
        return totalToken;
    }

    public void setTotalToken(long totalToken){
        this.totalToken=totalToken;
    }
}
