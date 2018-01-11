package com.example.yellow.gpssensor;
//debug-SHA1: 0C:18:37:35:DE:B1:54:97:EE:3B:DB:D7:4F:2B:74:C3:6C:45:F9:C5
//release-SHA1: 92:7E:2D:91:E3:D9:F6:59:0E:13:0F:62:25:CF:C9:B4:5C:08:A1:60
//service-id:153058
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.location.Poi;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.fence.PolylineFence;
import com.baidu.trace.api.track.DistanceRequest;
import com.baidu.trace.api.track.DistanceResponse;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPoint;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.LocationMode;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.SortType;
import com.baidu.trace.model.TraceLocation;
import com.baidu.trace.model.TransportMode;

import static java.lang.Math.sqrt;

public class MapActivity extends AppCompatActivity {
    private int tag=1;// 请求标识
    private String entityName="myTestTrace";
    private long serviceId=153058;

    private TextureMapView mMapView;
    private BaiduMap mBaiduMap;
    private BitmapDescriptor mMarker;//显示定位点
    public LocationClient mLocationClient=null;
    private MyLocationListener myListener=new MyLocationListener();
    public BDNotifyListener myNotifyListener=new MyNotifyListener();

    private long endTime;
    private long startTime;
    private double localCurrentSpeed=0;
    private LatLng lastPoint;
    private ArrayList<LatLng> trackLatLngs;
    private ArrayList<LatLng> drawLatLngs;
    public Trace mTrace;
    public LBSTraceClient mTraceClient;
    public Polyline mPolyline;

    private MYSQL sql;

    private boolean HeatMode=false;
    private boolean TrafficMode=false;
    private boolean SatelliteMode=false;
    private boolean LocateToCurrent=false;
    private boolean isFirstLoc=true;
    private boolean isTracing=false;
    private boolean isDraw=false;
    private boolean isNeedRealTimeDraw=false;

    private static final int REQUEST_ACCESS_COARSE_LOCATION=1;
    private static final int REQUEST_ACCESS_FINE_LOCATION=2;
    private static final int REQUEST_INTERNET=3;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE=4;
    private static String[] PERMISSION_LOCATION={
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        //在SDK各功能组件使用之前都需要调用SDKInitializer.initialize(getApplicationContext());
        // 因此我们建议该方法放在Application的初始化方法中
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map);
        mMapView=(TextureMapView) findViewById(R.id.bmapView);//获取地图控件引用
        mBaiduMap=mMapView.getMap();

        mLocationClient = new LocationClient(getApplicationContext());//声明LocationClient类
        mLocationClient.registerLocationListener(myListener);//注册监听函数
        mMarker=BitmapDescriptorFactory.fromResource(R.drawable.loc_current32red);

        sql=new MYSQL(this);

        initLoc();

        initTrace();

        verifyPermission(this);

        locateToCurrent((ImageButton)findViewById(R.id.locating_button));
    }
    public void verifyPermission(Activity activity){
        try{
            int permission= ActivityCompat.checkSelfPermission(activity,PERMISSION_LOCATION[3]);
            if(permission!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,PERMISSION_LOCATION,1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String permission[],int[] grantResults){
        if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //permission granted
        }
        else{
            //permission not set
        }
    }

    public void initLoc(){
        //开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        //定位相关参数设置
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系
        //共有三种坐标可选
        //1. gcj02：国测局坐标；
        //2. bd09：百度墨卡托坐标；
        //3. bd09ll：百度经纬度坐标；

        int span = 1000;
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setScanSpan(span);
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationDescribe(true);
        option.setIsNeedLocationPoiList(false);//可选，默认false，设置是否需要POI 结果，可以在BDLocation.getPoiList里得到
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(true);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);//加载设置
    }

    public void initTrace(){
        //entityName= Build.MODEL;
        serviceId=153058;
        lastPoint=new LatLng(113.394514,23.066745);
        trackLatLngs=new ArrayList<>();
        boolean isNeedObjectStorage=false;// 是否需要对象存储服务
        int gatherInterval=3;// 定位周期(单位:秒)
        int packInterval=10;// 打包回传周期(单位:秒)

        mTrace=new Trace(serviceId,entityName,isNeedObjectStorage);
        mTraceClient=new LBSTraceClient(getApplicationContext());// 初始化轨迹服务客户端
        mTraceClient.setInterval(gatherInterval,packInterval);
        mTraceClient.setLocationMode(LocationMode.High_Accuracy);
        mTraceClient.setOnTraceListener(mTraceListener);
    }
    public void startTrace(){
        mTraceClient.startTrace(mTrace,mTraceListener);// 开启服务
        mTraceClient.startGather(mTraceListener);// 开启采集
        startTime=System.currentTimeMillis()/1000;// 开始时间(单位：秒)
        endTime=0;
        isNeedRealTimeDraw=true;
        //mLatestPoint();//实时更新、画出轨迹
    }
    public void stopTrace(){
        mTraceClient.stopTrace(mTrace,mTraceListener);// 停止服务
        mTraceClient.stopGather(mTraceListener);// 停止采集
        endTime=System.currentTimeMillis()/1000;// 结束时间(单位：秒)
        isNeedRealTimeDraw=false;
        //setVisibilityInsideTraceInfo(1);//hide counting and show share btns
        //mHistoryTrack();
    }
    //以上四个listener必须相同,或者在一开始mTraceClient.setOnTraceListener(mTraceListener)设置在此不用传入
    OnTraceListener mTraceListener=new OnTraceListener() {
        @Override
        public void onBindServiceCallback(int i, String s) {}
        @Override
        public void onStartTraceCallback(int i, String s) {}
        @Override
        public void onStopTraceCallback(int i, String s) {}
        @Override
        public void onStartGatherCallback(int i, String s) {}
        @Override
        public void onStopGatherCallback(int i, String s) {}
        @Override
        public void onPushCallback(byte b, PushMessage pushMessage) {}
        @Override
        public void onInitBOSCallback(int i, String s) {}
    };

    public void locateToCurrent(View target){
        isFirstLoc=true;
        mBaiduMap.clear();
        mLocationClient.start();// 构造定位数据
    }

    public class MyLocationListener implements BDLocationListener {
        //mCurrentMarker=BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);

        @Override
        public void onReceiveLocation(BDLocation location){
            localCurrentSpeed=location.getSpeed();
            // 构造定位数据
            MyLocationData locData=new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(100)// 此处设置开发者获取到的方向信息，顺时针0-360,100
                    .latitude(location.getLatitude())
                    .longitude(location.getLatitude())
                    .build();
            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);

            // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
            MyLocationConfiguration config=new MyLocationConfiguration(
                    MyLocationConfiguration.LocationMode.NORMAL,false,null);//mMarker
            mBaiduMap.setMyLocationConfiguration(config);

            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
            //第一次定位需要更新地图显示状态
            if (isFirstLoc) {
                isFirstLoc = false;
                MapStatus.Builder builder = new MapStatus.Builder()
                        .target(ll)//地图缩放中心点
                        .zoom(18f);//缩放倍数 百度地图支持缩放21级 部分特殊图层为20级
                int errorCode = location.getLocType();
                if (errorCode==61||errorCode==161||errorCode==66||errorCode==65) {
                    //61-GPS，161-网络，66-离线，65-缓存，改变地图状态
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                    //mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                    //构建MarkerOption，用于在地图上添加Marker
                    OverlayOptions option=new MarkerOptions().position(ll).icon(mMarker);
                    mBaiduMap.addOverlay(option);
                }
                else{
                    /*TextView tv=(TextView)findViewById(R.id.loc_text);
                    String txt=""+tv;
                    tv.setText(txt);*/
                    Toast.makeText(MapActivity.this,errorCode+"  Something Wrong!",Toast.LENGTH_SHORT).show();
                }
            }

            if(isNeedRealTimeDraw) {
                trackLatLngs.add(ll);
                if (trackLatLngs.size() >= 2) {
                    PolylineOptions ooPolyline = new PolylineOptions().width(10).color(0xAAFF0000).points(trackLatLngs);
                    mBaiduMap.clear();
                    mPolyline = (Polyline) mBaiduMap.addOverlay(ooPolyline);//显示当前位置，并时时动态的画出运动轨
                }
                //distanceCount();//0-request,1-caculate with trackLatLngs
                updateTraceInformation(disLatLng(trackLatLngs));
            }

            /*
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            double latitude=location.getLatitude();//维度
            double longitute=location.getLongitude();//经度
            float radius=location.getRadius();//定位精度，默认0.0f
            ////获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
            String coorType = location.getCoorType();
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
            int errorCode = location.getLocType();
            */
            /*
            String addr=location.getAddrStr();//获取详细地址信息
            String country=location.getCountry();//获取国家
            String province=location.getProvince();    //获取省份
            String city = location.getCity();    //获取城市
            String district = location.getDistrict();    //获取区县
            String street = location.getStreet();    //获取街道信息
            */
            //String locationDescribe = location.getLocationDescribe();    //获取位置描述信息
            //List<Poi> poiList = location.getPoiList();//获取周边POI信息,POI信息包括POI ID、名称等
            /*
            mBaiduMap.setMyLocationEnabled(true);// 开启定位图层

            // 当不需要定位图层时关闭定位图层
            mBaiduMap.setMyLocationEnabled(false);
            */
        }
    }

    public class MyNotifyListener extends BDNotifyListener{
        public void onNotify(BDLocation mlocation, float distance){
            //已到达设置监听位置附近
        }

        /*public void removeNotify(){
            myListener.removeNotifyEvent(myListener);
        }*/

    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView=null;
    }
    @Override
    protected void onResume(){
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    public void heatModeMap(View view){
        HeatMode=!HeatMode;
        mBaiduMap.setBaiduHeatMapEnabled(HeatMode);
        ImageButton ibtn=(ImageButton)findViewById(R.id.heat_mode_btn);
        if(HeatMode) ibtn.setBackgroundResource(R.drawable.heat_on);
        else ibtn.setBackgroundResource(R.drawable.heat_off);
    }
    public void trafficModeMap(View view){
        TrafficMode=!TrafficMode;
        mBaiduMap.setTrafficEnabled(TrafficMode);
        ImageButton ibtn=(ImageButton)findViewById(R.id.traffic_mode_btn);
        if(TrafficMode) ibtn.setBackgroundResource(R.drawable.traffic_on);
        else ibtn.setBackgroundResource(R.drawable.traffic_off);
    }
    public void satelliteModeMap(View view){
        SatelliteMode=!SatelliteMode;
        if(SatelliteMode) mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        else mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        ImageButton ibtn=(ImageButton)findViewById(R.id.satellite_mode_btn);
        if(SatelliteMode) ibtn.setBackgroundResource(R.drawable.satellite_on);
        else ibtn.setBackgroundResource(R.drawable.satellite_off);
    }
    public void traceSwitch(View view){
        ImageButton ibtn=(ImageButton)findViewById(R.id.record_button);
        TextView tv=(TextView)findViewById(R.id.record_text);
        if(isTracing) {
            stopTrace();
            isTracing=false;
            tv.setText("开始录制");
            ibtn.setBackgroundResource(R.drawable.trace_start);
            setVisibilityInsideTraceInfo(1);//1-show share btns
            //setTopAndRightComponentsVisibility(1);//1-normal
        }
        else {
            startTrace();
            isTracing=true;
            tv.setText("停止录制");
            ibtn.setBackgroundResource(R.drawable.trace_stop);
            setTopAndRightComponentsVisibility(0);//0-hide menu
        }
    }
    public void setLocalDeviceInformation(View view){
        EditText et=(EditText)findViewById(R.id.search_edit_text);
        if(!et.getText().toString().equals("")){
            entityName=et.getText().toString();
            initTrace();
        }
    }

    public OnEntityListener entityListener=new OnEntityListener() {
        @Override
        public void onReceiveLocation(TraceLocation traceLocation) {
            super.onReceiveLocation(traceLocation);
            //将回调的当前位置location显示在地图MapView上
            //这里位置点的返回间隔时间为Handler.postDelayed的延时时间
        }
    };
    public OnTrackListener trackListener=new OnTrackListener() {
        @Override
        public void onLatestPointCallback(LatestPointResponse response) {
            super.onLatestPointCallback(response);
            //将纠偏后实时位置显示在地图MapView上
            //这里位置点的返回间隔时间为数据打包上传的频率；数据发送到服务端，才会更新最新的纠偏位置
            /*
            if(response.getLatestPoint().getLocation().getLatitude()==lastPoint.latitude
                    && response.getLatestPoint().getLocation().getLongitude()==lastPoint.longitude){
                return;
            }//返回的第一个点是上一次采集的最后一个点，可能和当前位置距离很大，应该弃用
            //位置点的返回间隔时间为数据打包上传的频率
            */
            LatestPoint point = response.getLatestPoint();
            LatLng currentLatLng;
            currentLatLng = new LatLng(point.getLocation().getLatitude(),point.getLocation().getLongitude());
            lastPoint=currentLatLng;
            trackLatLngs.add(currentLatLng);
            //mapUtil.drawHistoryTrack(trackPoints,false,mCurrentDirection);//显示当前位置，并时时动态的画出运动轨
            OverlayOptions ooPolyline=new PolylineOptions().width(10).color(0xAAFF0000).points(trackLatLngs);
            mBaiduMap.clear();
            mPolyline=(Polyline)mBaiduMap.addOverlay(ooPolyline);
            //updateTraceInformation("5","100");//test
        }
        @Override
        public void onHistoryTrackCallback(HistoryTrackResponse response){
            super.onHistoryTrackCallback(response);
            List<TrackPoint> points=response.getTrackPoints();//获取轨迹点
            List<LatLng> trackLatLngsOverall=new ArrayList<>();//轨迹点转到地图的经纬点
            for(TrackPoint trackPoint:points){
                //将轨迹点转化为地图画图层的LatLng类
                LatLng latlng=new LatLng(trackPoint.getLocation().getLatitude(),
                        trackPoint.getLocation().getLongitude());
                trackLatLngsOverall.add(latlng);
            }
            if(trackLatLngsOverall.size()>=2){
                OverlayOptions ooPolyline=new PolylineOptions().width(10).color(0xAAFF0000).points(trackLatLngsOverall);
                mBaiduMap.clear();
                mPolyline=(Polyline)mBaiduMap.addOverlay(ooPolyline);
            }
        }
        @Override
        public void onDistanceCallback(DistanceResponse response){
            super.onDistanceCallback(response);
            double distance=response.getDistance();//里程，单位：米
            double avgspeed=distance/(System.currentTimeMillis()/1000-startTime);////速度：m/s
            //updateTraceInformation(distance);
        }
    };

    public void mLatestPoint(){
        LocRequest locRequest=new LocRequest(serviceId);//定位请求参数类
        //时时定位设备当前位置，定位信息不会存储在轨迹服务端，即不会形成轨迹信息,只用于在MapView显示当前位置
        mTraceClient.queryRealTimeLoc(locRequest,entityListener);//这里只会一次定位
        //当轨迹服务开启，且采集数据开启之后，显示在地图上的位置点可以用服务端纠偏后的最新点
        //因为通过mClient.queryRealTimeLoc获取的点可能不精确，出现漂移等情况。
        //查询服务端纠偏后的最新轨迹点请求参数类
        LatestPointRequest request=new LatestPointRequest(tag,serviceId,entityName);
        ProcessOption processOption=new ProcessOption();//纠偏选项
        processOption.setRadiusThreshold(50);//设置精度过滤，0为不需要；精度大于50米的位置点过滤掉
        processOption.setTransportMode(TransportMode.walking);
        processOption.setNeedDenoise(true);//去噪处理
        processOption.setNeedMapMatch(true);//绑路处理
        request.setProcessOption(processOption);//设置参数
        mTraceClient.queryLatestPoint(request,trackListener);
    }
    public void mHistoryTrack(View view){
        //这个函数最好是结束后调用，回调函数中得到整个过程的
        HistoryTrackRequest historyTrackRequest=new HistoryTrackRequest();
        ProcessOption processOption=new ProcessOption();//纠偏选项
        processOption.setRadiusThreshold(50);//设置精度过滤，0为不需要；精度大于50米的位置点过滤掉
        processOption.setTransportMode(TransportMode.walking);
        processOption.setNeedDenoise(true);//去噪处理
        processOption.setNeedVacuate(true);//抽稀
        processOption.setNeedMapMatch(true);//绑路处理
        historyTrackRequest.setProcessOption(processOption);//设置参数
        /**
         * 设置里程补偿方式，当轨迹中断5分钟以上，会被认为是一段中断轨迹，默认不补充
         * 比如某些原因造成两点之间的距离过大，相距100米，那么在这两点之间的轨迹如何补偿
         SupplementMode.driving：补偿轨迹为两点之间最短驾车路线
         SupplementMode.riding：补偿轨迹为两点之间最短骑车路线
         SupplementMode.walking：补偿轨迹为两点之间最短步行路线
         SupplementMode.straight：补偿轨迹为两点之间直线
         */
        historyTrackRequest.setSupplementMode(SupplementMode.no_supplement);
        historyTrackRequest.setSortType(SortType.asc);//设置返回结果的排序规则，默认升序排序；升序：集合中index=0代表起始点；降序：结合中index=0代表终点。
        historyTrackRequest.setCoordTypeOutput(CoordType.bd09ll);//设置返回结果的坐标类型，默认为百度经纬度

        /**
         *设置是否返回纠偏后轨迹，默认不纠偏
         true：打开轨迹纠偏，返回纠偏后轨迹;
         false：关闭轨迹纠偏，返回原始轨迹。
         打开纠偏时，请求时间段内轨迹点数量不能超过2万，否则将返回错误。
         */
        historyTrackRequest.setProcessed(true);

        //请求历史轨迹
        historyTrackRequest.setTag(tag);//设置请求标识，用于唯一标记本次请求，在响应结果中会返回该标识
        historyTrackRequest.setServiceId(serviceId);//设置轨迹服务id
        historyTrackRequest.setEntityName(entityName);//查找的轨迹名称

        /**
         * 设置startTime和endTime，会请求这段时间内的轨迹数据;
         * 这里查询采集开始到采集结束之间的轨迹数据
         */
        if(endTime==0) endTime=System.currentTimeMillis()/1000;//结束轨迹录制前调用的endTime
        historyTrackRequest.setStartTime(startTime);
        historyTrackRequest.setEndTime(endTime);

        mTraceClient.queryHistoryTrack(historyTrackRequest,trackListener);
    }
    public void distanceCount(){
        DistanceRequest distanceRequest = new DistanceRequest(tag, serviceId, entityName);
        distanceRequest.setStartTime(startTime);// 设置开始时间
        distanceRequest.setEndTime(System.currentTimeMillis()/1000);// 设置结束时间
        distanceRequest.setProcessed(true);// 纠偏
        ProcessOption processOption = new ProcessOption();// 创建纠偏选项实例
        processOption.setNeedDenoise(true);// 去噪
        processOption.setNeedMapMatch(true);// 绑路
        processOption.setTransportMode(TransportMode.walking);// 交通方式为步行
        distanceRequest.setProcessOption(processOption);// 设置纠偏选项
        distanceRequest.setSupplementMode(SupplementMode.no_supplement);// 里程填充方式为无

        mTraceClient.queryDistance(distanceRequest, trackListener);// 查询里程
    }
    public double disLatLng(List<LatLng> plist){
        double sum=0;
        for(int i=1;i<plist.size();i++){
            /*double lat=trackLatLngs.get(i).latitude-trackLatLngs.get(i-1).latitude;
            double lng=trackLatLngs.get(i).longitude-trackLatLngs.get(i-1).longitude;
            sum+=Math.sqrt(lat*lat+lng*lng);*/
            sum+=DistanceUtil.getDistance(plist.get(i),plist.get(i-1));
        }
        return sum;
    }

    public void goToHome(View view){
        Intent intent=new Intent(this,home_page.class);
        startActivity(intent);
    }
    public void setTopAndRightComponentsVisibility(int mode){
        ConstraintLayout MenuCL=(ConstraintLayout)findViewById(R.id.search_bar_layout);
        ConstraintLayout TraceCL=(ConstraintLayout)findViewById(R.id.trace_information_container);
        ConstraintLayout DrawCL=(ConstraintLayout)findViewById(R.id.draw_information_container);
        LinearLayout LL=(LinearLayout)findViewById(R.id.map_menus_right);
        if(mode==1){//normal
            MenuCL.setVisibility(View.VISIBLE);
            LL.setVisibility(View.VISIBLE);
            TraceCL.setVisibility(View.GONE);
            DrawCL.setVisibility(View.GONE);
        }
        else if(mode==0){//trace
            MenuCL.setVisibility(View.GONE);
            LL.setVisibility(View.GONE);
            TraceCL.setVisibility(View.VISIBLE);
        }
        else if(mode==2){//draw
            MenuCL.setVisibility(View.GONE);
            LL.setVisibility(View.GONE);
            DrawCL.setVisibility(View.VISIBLE);
        }
    }
    public void updateTraceInformation(double distance){
        TextView tv1=(TextView)findViewById(R.id.current_speed_num);
        TextView tv2=(TextView)findViewById(R.id.distance_trace_num_information);
        TextView tv3=(TextView)findViewById(R.id.time_trace_num_information);
        TextView tv4=(TextView)findViewById(R.id.average_speed_num);
        DecimalFormat f=new DecimalFormat("##.0");
        double avgs=distance/(System.currentTimeMillis()/1000-startTime);
        tv1.setText(localCurrentSpeed+"");
        tv2.setText(f.format(distance));
        tv3.setText(getTimeHMSFormat(System.currentTimeMillis()/1000-startTime));
        tv4.setText(f.format(avgs));
    }
    public void setVisibilityInsideTraceInfo(int mode){
        ConstraintLayout btnCL=(ConstraintLayout)findViewById(R.id.trace_finish_btns_container);
        ConstraintLayout infoCL=(ConstraintLayout)findViewById(R.id.current_speed_container);
        if(mode==0){//normal
            btnCL.setVisibility(View.GONE);
            infoCL.setVisibility(View.VISIBLE);
        }
        else if(mode==1){//finish
            btnCL.setVisibility(View.VISIBLE);
            infoCL.setVisibility(View.GONE);
        }
    }
    public void cancelTrace(View view){
        stopTrace();

        setVisibilityInsideTraceInfo(0);//show countings and hide btns
        setTopAndRightComponentsVisibility(1);
        trackLatLngs.clear();
        mBaiduMap.clear();
        //no need to save in case of cancel
    }
    public void finishTrace(View view){
        stopTrace();

        setVisibilityInsideTraceInfo(0);
        setTopAndRightComponentsVisibility(1);

        String stime=snapShot();
        DataShare ds=((DataShare)getApplicationContext());
        sql.save_drawLatLngs(ds.getUserid(),trackLatLngs,stime);

        Toast.makeText(this, "轨迹已经保存到个人中心啦", Toast.LENGTH_SHORT).show();
        trackLatLngs.clear();
        mBaiduMap.clear();
    }
    public void shareTrace(View view){
        stopTrace();

        setVisibilityInsideTraceInfo(0);
        setTopAndRightComponentsVisibility(1);

        String stime=snapShot();
        DataShare ds=((DataShare)getApplicationContext());
        sql.save_drawLatLngs(ds.getUserid(),trackLatLngs,stime);

        goToShare("trace");
        trackLatLngs.clear();
        mBaiduMap.clear();
    }
    public String getTimeHMSFormat(long t){
        String h="";
        String m="";
        String s="";
        long ts=t%60;
        long tm=t/60;
        long th=t/3600;
        if(ts<10) s+="0";
        if(tm<10) m+="0";
        if(th<10) h+="0";
        return h+th+":"+m+tm+":"+s+ts;
    }
    public void drawTraceInit(){
        drawLatLngs=new ArrayList<>();
        //final BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.red_dot);
        BaiduMap.OnMapClickListener listener=new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(isDraw){
                    drawLatLngs.add(latLng);
                    showDraw();
                }
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        };
        mBaiduMap.setOnMapClickListener(listener);
    }
    public void showDraw(){
        BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.red_dot);
        if (drawLatLngs.size() >= 2) {
            PolylineOptions ooPolyline = new PolylineOptions().width(10).color(0xAAFF0000).points(drawLatLngs);
            mBaiduMap.clear();
            mPolyline = (Polyline) mBaiduMap.addOverlay(ooPolyline);//显示当前位置，并时时动态的画出运动轨
        }
        for(int i=0;i<drawLatLngs.size();i++){
            OverlayOptions poptions=new MarkerOptions().position(drawLatLngs.get(i)).icon(bitmap);
            mBaiduMap.addOverlay(poptions);
        }
        updateDrawInfo();
    }

    public void updateDrawInfo(){
        TextView drawDis=(TextView)findViewById(R.id.draw_distance_num);
        DecimalFormat f=new DecimalFormat("#.0");
        drawDis.setText(f.format(disLatLng(drawLatLngs)));
    }
    public void startDraw(View view){
        isDraw=true;
        mBaiduMap.clear();
        drawTraceInit();
        setTopAndRightComponentsVisibility(2);
        setTraceButtonVisibility(2);
    }
    public void finishDraw(View view){
        isDraw=false;

        String stime=snapShot();
        DataShare ds=((DataShare)getApplicationContext());
        sql.save_drawLatLngs(ds.getUserid(),trackLatLngs,stime);

        Toast.makeText(this,"绘制的轨迹已经保存在个人中心啦",Toast.LENGTH_SHORT).show();
        setTopAndRightComponentsVisibility(1);
        setTraceButtonVisibility(1);
        drawLatLngs.clear();
        mBaiduMap.clear();
        mBaiduMap.setOnMapClickListener(null);

        //save drawLatLngs
    }
    public void shareDraw(View view){
        isDraw=false;
        setTopAndRightComponentsVisibility(1);
        setTraceButtonVisibility(1);

        String stime=snapShot();
        DataShare ds=((DataShare)getApplicationContext());
        sql.save_drawLatLngs(ds.getUserid(),trackLatLngs,stime);

        goToShare("draw");
        drawLatLngs.clear();
        mBaiduMap.clear();
    }
    public void undoADraw(View view){
        if(drawLatLngs.size()>=2){
            drawLatLngs.remove(drawLatLngs.size()-1);
            showDraw();
        }
    }
    public void cancelDraw(View view){
        isDraw=false;
        drawLatLngs.clear();
        setTopAndRightComponentsVisibility(1);
        mBaiduMap.clear();
        mBaiduMap.setOnMapClickListener(null);
        setTraceButtonVisibility(1);
    }
    public void setTraceButtonVisibility(int mode){//1-visible,normal,2-gone-draw
        LinearLayout layout=(LinearLayout)findViewById(R.id.record_trace_btn_layout);
        if(mode==1){
            layout.setVisibility(View.VISIBLE);
        }
        else if(mode==2){
            layout.setVisibility(View.GONE);
        }
    }

    public String snapShot(){
        //mBaiduMap.setMapStatus(MapStatusUpdateFactory. newLatLng(trackLatLngs.get(trackLatLngs.size()-1)));
        final SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        final Date date=new Date(System.currentTimeMillis());
        BaiduMap.SnapshotReadyCallback callback=new BaiduMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                DataShare ds=((DataShare) getApplicationContext());
                ds.setSnapShot(bitmap);
                String path=Environment.getExternalStorageDirectory().getPath();
                //File f=new File(Environment.getExternalStorageDirectory(),"/JiQu/traceShot");
                path=path+"/JiQu/traceShot";
                File file=new File(path);
                Toast.makeText(MapActivity.this,"--生成截图--",Toast.LENGTH_SHORT).show();
                try{
                    boolean b;
                    if(!file.exists()) b=file.mkdirs();//不要用mkdir，被坑了一晚上
                    File filepng=new File(format.format(date)+".png");
                    FileOutputStream out=new FileOutputStream(file+"/"+filepng);//FileOutputStream
                    if(bitmap.compress(Bitmap.CompressFormat.PNG,50,out)){
                        out.flush();
                        out.close();
                        //Toast.makeText(MapActivity.this,"截图成功",Toast.LENGTH_SHORT).show();
                    }
                    ds.setPath(path);

                }catch (Exception e){
                    Toast.makeText(MapActivity.this,"SnapShot Fail\n"+path+"\n"+e.getMessage(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        };
        mBaiduMap.snapshot(callback);
        return format.format(date);
    }


    public void goToShareBtn(View view){
        goToShare("none");
    }
    public void goToShare(String type){
        Intent intent=new Intent(MapActivity.this,ShareActivity.class);
        intent.putExtra("type",type);
        intent.putExtra("last","Map");
        startActivity(intent);
    }
    public void goToGroup(View view){
        Intent intent=new Intent(MapActivity.this,GroupActivity.class);
        Intent this_intent = getIntent();
        intent.putExtra("user",this_intent.getStringExtra("user"));
        startActivity(intent);
    }
    public void goToSetting(View view){
        Intent intent=new Intent(this,SettingActivity.class);
        startActivity(intent);
    }
    public void goToI(View view){
        Intent intent=new Intent(this,I_Activity.class);
        DataShare ds=((DataShare)getApplicationContext());
        intent.putExtra("user",ds.getUsername());
        startActivity(intent);
        this.finish();
    }
}


