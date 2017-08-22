package com.example.defiance.wanggps_10;

/**
 * Created by defiance on 2017/8/7.
 */

public class GPSMessage{
    public double time;
    public double longitude,latitude,altitude;
    public boolean valid;

    public GPSMessage(){
        time=0;
        longitude=0;
        latitude=0;
        altitude=0;
        valid=true;
    }
}
