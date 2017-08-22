package com.example.defiance.wanggps_10;

//安卓核心类
import java.io.IOException;
import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
//sensor相关类，用作本机气压计，温度计等数据读取
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//GPS相关类，用作本机原生GPS数据读取
import android.location.LocationManager;
import android.widget.TextView;
import android.widget.Toast;

//import com.baidu.location.BDLocation;
//import com.baidu.location.BDLocationListener;
//import com.baidu.location.LocationClient;
//import com.baidu.location.LocationClientOption;

import com.example.defiance.wanggps_10.gson.HeWeather5;
import com.example.defiance.wanggps_10.util.HttpUtil;
import com.example.defiance.wanggps_10.util.UtilityJsonParsing;

import org.litepal.LitePal;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.defiance.wanggps_10.MovingStatus.status.down;
import static com.example.defiance.wanggps_10.MovingStatus.status.plain;
import static com.example.defiance.wanggps_10.MovingStatus.status.up;
//


public class MainActivity extends Activity implements View.OnClickListener{
    //天气数据获取-常量，变量声明
    private final static String weatherId="minhang";//由于测试环境只在上海闵行，因此这里只获取闵行的天气数据
    private final static String heWeatherApiKey="9263953c18634e21b2376f80226917ff";//个人申请的免费和风天气api端口秘钥
    private final static String heWeatherUrl="https://free-api.heweather.com/v5/";//和风天气url地址
    private HeWeather5 heWeather5;

    //UI面板声明
    private TextView textGps;
    private TextView textBaro;
    private TextView textGravity;
    private TextView textAcce;
    private TextView textWeather;
    private TextView textFinal;
    private TextView textStatus;
    //    private EditText editTextCell;

    //SensorManager类别声明
    //   private LocationListenerCell locationListenerCell;
    //    private LocationManager locationManagerCell;
    private SensorManager baroSensorManager;
    private SensorManager gravitySensorManager;
    private SensorManager acceSensorManager;

    //TAG常量声明
    private static final String TAG = "MainActivity";

    //Sensor类声明
    private Sensor baroSensor;
    private Sensor gravitySensor;
    private Sensor acceSensor;

    //数据记录状态量声明
    public GPSMessage gpsMessage;
    private float pressure;
    private float calibrate=0;//0海拔的偏移值将会通过气象台的预报接口数据进行修正
    private double ZaxisAcce;
    private AccelerateMessage tmpGravity;
    private AccelerateMessage tmpAcce;
    private AccelerateMessage tmp;

    private Vertibi algorithmGps;
    private  Vertibi algorithmAcce;

    //    public LocationClient locationClient;

    //Processor类自定义处理类声明
    private BarometerProcessor barometerProcessor;
    private AccelerateProcessor accelerateProcessor;

    private ZAxisAdjuster zAxisAdjuster;


    @Override
    //###########################在onCreate方法中启动GPS，气压计，并且将初始值显示出来######################################
    public void onCreate(Bundle savedInstanceState) {
        //UI实例化
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textGps = (TextView) findViewById(R.id.gpsText);
        textBaro=(TextView) findViewById(R.id.baroText);
        textAcce=(TextView) findViewById(R.id.acceText);
        textGravity=(TextView) findViewById(R.id.gravityText);
        textWeather=(TextView) findViewById(R.id.weatherText);
        textFinal=(TextView) findViewById(R.id.finalAltitude);
        textStatus=(TextView)findViewById(R.id.status);
        Button GoUpStartButton =(Button)findViewById(R.id.upStartButton);
        Button GoUpStopButton =(Button)findViewById(R.id.upStopButton);
        Button GoPlainlyStartButton =(Button)findViewById(R.id.plainStartButton);
        Button GoPlainStopButton =(Button)findViewById(R.id.plainStopButton);
        Button GoDownStartButton =(Button)findViewById(R.id.downStartButton);
        Button GoDownStopButton =(Button)findViewById(R.id.downStopButton);
        Button RefreshMap=(Button)findViewById(R.id.refresh);
        GoUpStartButton.setOnClickListener(this);
        GoUpStopButton.setOnClickListener(this);
        GoPlainlyStartButton.setOnClickListener(this);
        GoPlainStopButton.setOnClickListener(this);
        GoDownStartButton.setOnClickListener(this);
        GoDownStopButton.setOnClickListener(this);
        RefreshMap.setOnClickListener(this);
//        editTextCell=(EditText)findViewById(R.id.cellText);
 //       locationManagerCell = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        locationListenerCell=new LocationListenerCell();

        //Manager类实例化
        baroSensorManager=(SensorManager) getSystemService(this.SENSOR_SERVICE);
        baroSensor = baroSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        gravitySensorManager=(SensorManager) getSystemService(this.SENSOR_SERVICE);
        gravitySensor=gravitySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        acceSensorManager=(SensorManager) getSystemService(this.SENSOR_SERVICE);
        acceSensor=acceSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        locationManagerGps=(LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Processor类实例化
        barometerProcessor=new BarometerProcessor();
        accelerateProcessor=new AccelerateProcessor();
        gpsProcessor=new GpsProcessor();

        zAxisAdjuster=ZAxisAdjuster.getZAxisAdjuster(barometerProcessor,gpsProcessor);//单件模式，因为各个processor并没有被重复实例化的危险，但是卡尔曼滤波器有肯能被多次实例化

        //数据类实例化
        tmpGravity=new AccelerateMessage();
        tmpAcce=new AccelerateMessage();
        gpsMessage=new GPSMessage();
        heWeather5=new HeWeather5();

        algorithmAcce=new Vertibi(gpsProcessor,accelerateProcessor);
        algorithmGps=new Vertibi(gpsProcessor,accelerateProcessor);

        //数据库操作
        LitePal.getDatabase();

/* *************此段是百度网络地位的初始化模块，由于使用后发现网络定位模块 已经不再返回海拔数据，因此放弃使用网络定位功能***************************
        //百度网络定位登记
        locationClient=new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(new cellLocationListener());

        //百度网络定位启动
        cellRequestLocation();

        //原生网络定位登记
        try {
            Log.i(TAG,"原生网络登记");
            Location location = locationManagerCell.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locationManagerCell.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 1,
                    locationListenerCell);
        }catch(SecurityException e){}

*****************************************************************************************************************************************************/

        //获取现在（闵行区）天气信息（只获取温度，湿度和大气压）
        String weatherRequest=heWeatherUrl+"/weather?city="+weatherId+"&key="+heWeatherApiKey;
        HttpUtil.sendOkHttpRequest(weatherRequest, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"无法获取天气信息001",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            final String responseText=response.body().string();
                heWeather5= UtilityJsonParsing.handleWeatherResponse(responseText);
                Log.i(TAG,"已经得到天气数据");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(heWeather5!=null)
                        {
                            Log.i(TAG,"准备UI");
                            barometerProcessor.setNums(Double.parseDouble(heWeather5.now.pres),Double.parseDouble(heWeather5.now.tmp));
                            showOnlineWeather(heWeather5);
                        }
                        else{
                            Toast.makeText(MainActivity.this,"无法获取天气信息002",Toast.LENGTH_SHORT).show();
                        }
                    }

                });
            }
        });




        //gps的数据库在GPS服务中进行更新

        //判断各个模块是否正常读入数据
        if(baroSensor == null){
            textBaro.setText("气压计模块异常");
        }
        if(gravitySensor==null){
            textGravity.setText("重力传感器异常");
        }
        if(acceSensor==null){
            textAcce.setText("加速度传感器异常");
        }


        //注册气压计监听
        baroSensorManager.registerListener(pressureListener, baroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        //GPS初始化，进行监听登记
        Log.i(TAG,"GPS初始化开始执行");


        if (!locationManagerGps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // 返回开启GPS导航设置界面
                    Intent gpsTurningOnIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(gpsTurningOnIntent);
        }
        // 为GPS注册监听并且获取GPS数据
        Log.i(TAG,"GPS初始化-完成MAP读入和权限判定");
        try {
                    //Location gpsLocation = locationManagerGps.getLastKnownLocation(locationManagerGps.GPS_PROVIDER);
                    //updateGpsView(gpsLocation);
                    // 监听状态
                    //locationManagerGps.addGpsStatusListener(listener);//这里可以得到Gps的状态监听;
                    // 绑定监听，有4个参数
                    // 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
                    // 参数2，位置信息更新周期，单位毫秒
                    // 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
                    // 参数4，监听
                    // 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
                    // 1秒更新一次，或最小位移变化超过1米更新一次；
                    // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
            locationManagerGps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, gpsLocationListener);
        }catch(SecurityException e){}//editTextGps.append("gps权限未打开");}
        Log.i(TAG,"GPS初始化-已登记监听");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(500);
                    }catch(InterruptedException e){//do nothing
                         }
                    final double finalAltitude=zAxisAdjuster.KalmanFilter();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textFinal.setText("处理后海拔高度为： "+finalAltitude);
                        }
                    });
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    //Log.i(TAG,"加速度计算法运行");
                    try {
                        Thread.sleep(1000);
                    }catch(InterruptedException e){//do nothing
                    }
                    int[] result = algorithmAcce.computeAcce();
                    int upToken=0,plainToken=0,downToken=0;
                    int max=1;
                    if(gpsProcessor.isValid)
                    {
                        // Log.i(TAG, "Gps算法运行");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {//do nothing
                        }
                        result = algorithmGps.computeGps();
                        for (int r : result) {
                            // System.out.print(MovingStatus.status.values()[r] + " ");
                            if(MovingStatus.status.values()[r]==up)
                            {
                                upToken++;
                            }
                            if(MovingStatus.status.values()[r]==plain)
                            {
                                plainToken++;
                            }
                            if(MovingStatus.status.values()[r]==down)
                            {
                                downToken++;
                            }
                        }
                    }

                    result=algorithmAcce.computeAcce();
                    for (int r : result)
                    {
                       // System.out.print(MovingStatus.status.values()[r] + " ");
                        if(MovingStatus.status.values()[r]==up)
                        {
                            upToken++;
                        }
                        if(MovingStatus.status.values()[r]==plain)
                        {
                            plainToken++;
                        }
                        if(MovingStatus.status.values()[r]==down)
                        {
                            downToken++;
                        }
                    }

                    if(plainToken>upToken) {
                        max = 2;
                        if (downToken > plainToken)
                            max=3;
                    }
                    else
                    {
                        if(downToken>upToken)
                            max=3;
                    }
                    zAxisAdjuster.setStatus(max);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(zAxisAdjuster.status==0)
                            textStatus.setText("上行");
                            if(zAxisAdjuster.status==1)
                                textStatus.setText("平地");
                            if(zAxisAdjuster.status==2)
                                textStatus.setText("下行");
                        }
                    });
                   // System.out.println();


                }
            }
        }).start();


    }
    //######################################################  OnCreate()函数结束  ##########################################################

    //######################################################  onResume()函数开始  ##########################################################
    @Override
    protected void onResume(){
        super.onResume();
        //判断各个模块是否正常读入数据
        if(baroSensor == null){
            textBaro.setText("气压计模块异常");
        }
        if(gravitySensor==null){
            textGravity.setText("重力传感器异常");
        }
        if(acceSensor==null){
            textAcce.setText("加速度传感器异常");
        }
        //重新读入各个MAP
        accelerateProcessor.readMap(accelerateProcessor.getMode());
        //重新注册各项传感器监听
        baroSensorManager.registerListener(pressureListener, baroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        gravitySensorManager.registerListener(gravityListener,gravitySensor,SensorManager.SENSOR_DELAY_NORMAL);
        acceSensorManager.registerListener(acceListener,acceSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    //################################################ onResume()结束 #########################################################################

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //保存地图到本地数据库
        accelerateProcessor.renewMap(accelerateProcessor.getMode());
        gpsProcessor.renewMap(gpsProcessor.getMode());

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        //更新地图并且保存到本地数据库
            accelerateProcessor.renewMap(accelerateProcessor.getMode());
 //           locationClient.stop();
            gpsProcessor.renewMap(gpsProcessor.getMode());

        //活动结束时取消监听
        if(pressureListener!=null){
            baroSensorManager.unregisterListener(pressureListener);
        }
        if(gravityListener!=null){
            gravitySensorManager.unregisterListener(gravityListener);
        }
        if(acceListener!=null){
            acceSensorManager.unregisterListener(acceListener);
        }
    }

    //################################################ 按钮控制集成 ###########################################################################
    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.upStartButton:
                Toast.makeText(MainActivity.this,"加速度读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(1);
                Toast.makeText(MainActivity.this,"加速度读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(1);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.readMap(1);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.readMap(1);
                break;
            case R.id.plainStartButton:
                Toast.makeText(MainActivity.this,"加速度读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(2);
                Toast.makeText(MainActivity.this,"加速度读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(2);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.readMap(2);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.readMap(2);
                break;
            case R.id.downStartButton:
                Toast.makeText(MainActivity.this,"加速度读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(3);
                Toast.makeText(MainActivity.this,"加速度读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(3);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库",Toast.LENGTH_SHORT).show();
                accelerateProcessor.readMap(3);
                Toast.makeText(MainActivity.this,"Gps读入本地数据库完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.readMap(3);
                break;
            case R.id.downStopButton:
                Toast.makeText(MainActivity.this,"加速度计数据开始更新",Toast.LENGTH_SHORT).show();
                accelerateProcessor.renewMap(3);
                Toast.makeText(MainActivity.this,"加速度计数据更新完成",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(0);
                Toast.makeText(MainActivity.this,"Gps计数据开始更新",Toast.LENGTH_SHORT).show();
                gpsProcessor.renewMap(3);
                Toast.makeText(MainActivity.this,"Gps计数据更新完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(0);
                break;
            case R.id.plainStopButton:
                Toast.makeText(MainActivity.this,"加速度计数据开始更新",Toast.LENGTH_SHORT).show();
                accelerateProcessor.renewMap(2);
                Toast.makeText(MainActivity.this,"加速度计数据更新完成",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(0);
                Toast.makeText(MainActivity.this,"Gps计数据开始更新",Toast.LENGTH_SHORT).show();
                gpsProcessor.renewMap(2);
                Toast.makeText(MainActivity.this,"Gps计数据更新完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(0);
                break;
            case R.id.upStopButton:
                Toast.makeText(MainActivity.this,"加速度计数据开始更新",Toast.LENGTH_SHORT).show();
                accelerateProcessor.renewMap(1);
                Toast.makeText(MainActivity.this,"加速度计数据更新完成",Toast.LENGTH_SHORT).show();
                accelerateProcessor.setMode(0);
                Toast.makeText(MainActivity.this,"Gps计数据开始更新",Toast.LENGTH_SHORT).show();
                gpsProcessor.renewMap(1);
                Toast.makeText(MainActivity.this,"Gps计数据更新完成",Toast.LENGTH_SHORT).show();
                gpsProcessor.setMode(0);
                break;
            case R.id.refresh:
                algorithmAcce.getMaps();
                algorithmGps.getMaps();
                break;
        }
    }
    //################################################################################################################################################3


    /*------------------------------------以下是气压计传感器实现块--------------------------------*/
    //气压计监听
     private SensorEventListener  pressureListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                DecimalFormat df = new DecimalFormat("0.00");
                df.getRoundingMode();
                pressure = Float.parseFloat(df.format(event.values[0]-calibrate));
                textBaro.setText(df.format(event.values[0]-calibrate)+" mbar");
                double baroAltitude=barometerProcessor.exchangeToAltitude(pressure);
                textBaro.append("\n"+df.format(baroAltitude)+" m");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
            //虚函数，本身功能不会用到这一个功能
        }
    };

    private SensorEventListener  gravityListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                tmpGravity.Xaxis=event.values[0];
                tmpGravity.Yaxis=event.values[1];
                tmpGravity.Zaxis=event.values[2];
                //textGravity.setText("\nX轴（重力加速度: "+event.values[0]);
                //textGravity.append("\nY轴（重力加速度: "+event.values[1]);
                //textGravity.append("\nZ轴（重力加速度: "+event.values[2]);
                tmp=accelerateProcessor.sub(tmpGravity,tmpAcce);
                //textGravity.append("\nX轴（重力加速度: "+tmp.Xaxis);
                //textGravity.append("\nY轴（重力加速度: "+tmp.Yaxis);
                //textGravity.append("\nZ轴（重力加速度: "+tmp.Zaxis);
                ZaxisAcce=accelerateProcessor.ZgCalculate(tmpGravity,tmp);
                textGravity.setText("加速度计绝对坐标系: "+ZaxisAcce);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
            //虚函数，本身功能不会用到这一个功能
        }
    };

    private SensorEventListener  acceListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                tmpAcce.Xaxis=event.values[0];
                tmpAcce.Yaxis=event.values[1];
                tmpAcce.Zaxis=event.values[2];
                //textAcce.setText("\nX轴（加速度: "+event.values[0]);
                //textAcce.append("\nY轴（加速度: "+event.values[1]);
                //textAcce.append("\nZ轴（加速度: "+event.values[2]);

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
            //虚函数，本身功能不会用到这一个功能
        }
    };

    private void showOnlineWeather(HeWeather5 heWeather5)
    {
        textWeather.append("\n网络数据-闵行区标准大气压： "+heWeather5.now.pres);
        textWeather.append("\n网络数据-闵行区标准温度： "+heWeather5.now.tmp);
        textWeather.append("\n网络数据-闵行区标准适度: "+heWeather5.now.tmp);
    }
    /*---------------------------------------以上是气压计传感器实现块---------------------------*/




    /*---------------------------------------以下是GPS实现块------------------------------------*/


    //这里是代码迁移区
    private LocationManager locationManagerGps;
    public GpsLocationListener gpsLocationListener=new GpsLocationListener();
    private GpsProcessor gpsProcessor;


    private class GpsLocationListener implements LocationListener {

        GPSMessage gpsMessage;
        public GpsLocationListener(){
            gpsMessage=new GPSMessage();
            Log.i(TAG,"新生listener");
        }

        private void renew(double time,double longitude,double latitude,double altitude,boolean valid)
        {
            Log.i(TAG,"更新");


            gpsMessage.time=time;

            if(gpsMessage.longitude!=longitude)
            {
                gpsMessage.longitude=longitude;
            }
            if(gpsMessage.latitude!=latitude)
            {
                gpsMessage.latitude=latitude;
            }
            if(gpsMessage.altitude!=altitude)
            {
                gpsMessage.altitude=altitude;
            }
            if(gpsMessage.valid!=valid)
            {
                gpsMessage.valid=valid;
            }
            Log.i(TAG, "new时间：" + time%100000);
            Log.i(TAG, "new经度：" + latitude);
            Log.i(TAG, "new纬度：" + longitude);
            Log.i(TAG, "new海拔：" + altitude);
            gpsProcessor.readIn(this.gpsMessage);
            Log.i(TAG,"当前状况"+gpsMessage.valid);
        }

        public void onLocationChanged(Location location) {
            //updateGpsView(location);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (gpsMessage.valid) {
                        textGps.setText("设备位置信息(GPS)\n\n经度：");
                        textGps.append(String.valueOf(gpsMessage.longitude));
                        textGps.append("\n纬度：");
                        textGps.append(String.valueOf(gpsMessage.latitude));
                        textGps.append("\n海拔：");
                        textGps.append(String.valueOf(gpsMessage.altitude));
                    }
                    else
                        textGps.setText("GPS异常");
                }
            });

            renew(location.getTime(),location.getLongitude(),location.getLatitude(),location.getAltitude(),true);
        }

        /**
         * GPS状态变化时触发
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    // editTextGps.append( "当前GPS状态正常");
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG,"内部状态异常");
                    renew(0,0,0,0,false);
                    //textGps.setText( "GPS状态异常");
                    break;
            }
        }

        //GPS开启时触发
        //此时由于传入的字符串一定是GPS常数，因此一定是GPS作为provider
        public void onProviderEnabled(String provider) {
            try {
                Location location = locationManagerGps.getLastKnownLocation(provider);
                Log.i(TAG, "时间：" + location.getTime());
                Log.i(TAG, "经度：" + location.getLongitude());
                Log.i(TAG, "纬度：" + location.getLatitude());
                Log.i(TAG, "海拔：" + location.getAltitude());
                renew(location.getTime(),location.getLongitude(),location.getLatitude(),location.getAltitude(),true);
            }catch(SecurityException e){ };
        }


        //GPS禁用时触发
        public void onProviderDisabled(String provider) {
            //textGps.setText("gps未打开");
        }

    };



    /*GpsStatus.Listener listener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                // 第一次定位
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(TAG,"第一次定位");
                    break;
                // 卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //editTextGps.append( "卫星状态改变");
                    // 获取当前状态
                    try {
                        GpsStatus gpsStatus = locationManagerGps.getGpsStatus(null);
                        // 获取卫星颗数的默认最大值
                        int maxSatellites = gpsStatus.getMaxSatellites();
                        // 创建一个迭代器保存所有卫星
                        Iterator<GpsSatellite> iters = gpsStatus.getSatellites()
                                .iterator();
                        int count = 0;
                        while (iters.hasNext() && count <= maxSatellites) {
                            GpsSatellite s = iters.next();
                            count++;
                        }
                        //editTextGps.append("搜索到：" + count + "颗卫星");
                        break;
                    }catch(SecurityException e){   }
                    // 定位启动
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(TAG, "定位启动");
                    break;
                // 定位结束
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(TAG, "定位结束");
                    break;
            }
        };
    };*/

    /*-----------------------------------------以上是GPS实现块------------------------------------*/




    /*-----------------------------------------以下是网络定位实现块-------------------------------*/
   /* public class LocationListenerCell implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "网络时间：" + location.getTime());
            Log.i(TAG, "网络经度：" + location.getLongitude());
            Log.i(TAG, "网络纬度：" + location.getLatitude());
            Log.i(TAG, "网络海拔：" + location.getAltitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            try {
                Location location = locationManagerCell.getLastKnownLocation(provider);
                Log.i(TAG, "网络时间：" + location.getTime());
                Log.i(TAG, "网络经度：" + location.getLongitude());
                Log.i(TAG, "网络纬度：" + location.getLatitude());
                Log.i(TAG, "网络海拔：" + location.getAltitude());
            }catch(SecurityException e){ };
        }
    }


    public class cellLocationListener implements BDLocationListener{
        @Override
        public void onConnectHotSpotMessage(String string,int input)
        {

        }

        @Override
        public void onReceiveLocation(final BDLocation location){

            MainActivity.this.runOnUiThread(new Runnable() {

                @Override

                public void run() {

                    StringBuilder currentPosi=new StringBuilder();
                    currentPosi.append("海拔").append(location.getAltitude());
                    currentPosi.append("\n经度：").append(location.getLatitude());
                    currentPosi.append("\n定位方式:");
                    if(location.getLocType()==BDLocation.TypeNetWorkLocation)
                        currentPosi.append("百度网络");
                    else
                        currentPosi.append("网络失效");
                    editTextCell.setText(currentPosi);
                }

            });

        }

    }

    private void cellRequestLocation(){
        initLocation();
        Log.i(TAG,"唤醒网络定位");
        locationClient.start();
    }

    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(100);
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        locationClient.setLocOption(option);
    }
                                                                          */
}
