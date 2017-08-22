package com.example.defiance.wanggps_10.util;

import android.text.TextUtils;

import com.example.defiance.wanggps_10.gson.HeWeather5;
import com.example.defiance.wanggps_10.gson.Now;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by defiance on 2017/8/12.
 */

public class UtilityJsonParsing {
    public static HeWeather5 handleWeatherResponse(String response){

            try{
                JSONObject weatherInfo=new JSONObject(response);
                JSONArray jsonArray=weatherInfo.getJSONArray("HeWeather5");
                String weatherContent=jsonArray.getJSONObject(0).toString();
                return new Gson().fromJson(weatherContent,HeWeather5.class);

            }catch(JSONException e){
                e.printStackTrace();
            }

        return null;
    }
}
