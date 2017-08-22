package com.example.defiance.wanggps_10;

/**
 * Created by defiance on 2017/8/9.
 */

import java.lang.Math;

public class BarometerProcessor {

    double refAltitude,refPressure,Temperature;
    double currentAltitude;

    public BarometerProcessor(){
        refPressure=1002;//mPa
        refAltitude=4.5;
        Temperature=35;
    }

    public void setNums(double refPressure,double temperature)
    {
        this.refPressure=refPressure;
        this.Temperature=temperature;
    }

    public double exchangeToAltitude(double currentPressure)
    {
        return currentAltitude=refAltitude+18400*(1+Temperature/273)*Math.log(refPressure/currentPressure)-90;
    }

    public double getCurrentAltitude()
    {
        return currentAltitude;
    }
}
