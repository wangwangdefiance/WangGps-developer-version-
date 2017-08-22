package com.example.defiance.wanggps_10.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by defiance on 2017/8/12.
 */

public class Now {

    @SerializedName("hum")
    public String hum;

    @SerializedName("pres")
    public String pres;

    @SerializedName("tmp")
    public String tmp;
}
