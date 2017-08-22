package com.example.defiance.wanggps_10;


import static com.example.defiance.wanggps_10.MovingStatus.status.down;
import static com.example.defiance.wanggps_10.MovingStatus.status.plain;
import static com.example.defiance.wanggps_10.MovingStatus.status.up;

/**
 * Created by defiance on 2017/8/21.
 */

public class MovingStatus {


   public enum status
    {
        up,
        plain,
        down,
    }

    public static int[] states = new int[]{up.ordinal(), plain.ordinal(), down.ordinal()};
    public static int[] observations = new int[100];
    public static double[] start_probability = new double[]{0,1,0};
    public static double[][] transititon_probability = new double[][]{
            {0.6,0.3,0.1},
            {0.15,0.7, 0.15},
            {0.1,0.3,0.6}
    };
    public static double[][] emission_probability = new double[3][2000];
    }
