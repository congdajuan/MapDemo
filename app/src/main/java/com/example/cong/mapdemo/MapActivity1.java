package com.example.cong.mapdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;

import java.util.ArrayList;
import java.util.List;

public class MapActivity1 extends Activity implements OnGetPoiSearchResultListener, OnGetSuggestionResultListener,SensorEventListener {

    private Context mContext;

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    private Button mCompleteButton;
    private Button mRequestLocation;
    private ListView mListView;

    // 搜索周边相关
    private PoiSearch mPoiSearch = null;

    /**
     * 定位SDK的核心类
     */
    public LocationClient mLocationClient = null;

    /**
     * 当前标志
     */
    private Marker mCurrentMarker;
    // LatLng地理坐标基本数据结构
    private LatLng latlng;
    // 定位图标描述
    private BitmapDescriptor currentMarker = null;

    public BDLocationListener myListener = new MyLocationListener();

    private List<PoiInfo> dataList;
    private ListAdapter adapter;

    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    private int locType;
    private double longitude;// 精度
    private double latitude;// 维度
    private float radius;// 定位精度半径，单位是米
    private String addrStr;// 反地理编码
    private String province;// 省份信息
    private String city;// 城市信息
    private String district;// 区县信息
    private float direction;// 手机方向信息
    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;

    private int checkPosition;
    // 类PoiSearch继承poi检索接口
    private PoiSearch mpoiSearch;
    // SuggestionSearch建议查询类
    private SuggestionSearch mSuggestionSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在使用SDK各组件之前初始化context信息，传入ApplicationContext
        // 注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mContext = this;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务

        initView();
        initEvent();
        initLocation();
        stopLocationClient();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void initView(){
        dataList = new ArrayList<PoiInfo>();
        mMapView = (MapView) findViewById(R.id.bmapView);
        mCompleteButton = (Button) findViewById(R.id.chat_publish_complete_publish);
        mRequestLocation = (Button) findViewById(R.id.request);
        mListView = (ListView) findViewById(R.id.lv_location_nearby);
        checkPosition=0;
        adapter = new ListAdapter(0);
        mListView.setAdapter(adapter);
    }

    /**
     * 事件初始化
     */
    private void initEvent(){

        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                checkPosition = position;
                adapter.setCheckposition(position);
                adapter.notifyDataSetChanged();
                PoiInfo ad = (PoiInfo) adapter.getItem(position);
                MapStatusUpdate  u = MapStatusUpdateFactory.newLatLng(ad.location);
                mBaiduMap.animateMapStatus(u);
                mCurrentMarker.setPosition(ad.location);
            }
        });

        mRequestLocation.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ToastUtil.show(getApplicationContext(), "正在定位。。。");
                initLocation();
            }
        });

        mCompleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ToastUtil.show(getApplicationContext(), "名称是: " + dataList.get(checkPosition).name+" 地址是："+dataList.get(checkPosition).address);
            }
        });

    }

    /**
     * 定位
     */
    private void initLocation(){
        /**///重新设置
        checkPosition = 0;
        adapter.setCheckposition(0);

        //mBaiduMap = mMapView.getMap();
        //mBaiduMap.clear();
        // 实例化PoiSearch
        mpoiSearch = PoiSearch.newInstance();
        // 注册搜索事件监听
        mpoiSearch.setOnGetPoiSearchResultListener(this);
        // 实例化建议查询类
        mSuggestionSearch = SuggestionSearch.newInstance();
        // 注册建议查询事件监听
        mSuggestionSearch.setOnGetSuggestionResultListener(this);


        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.clear();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(17).build()));   // 设置级别

        // 定位初始化
        mLocationClient = new LocationClient(getApplicationContext()); // 声明LocationClient类
        mLocationClient.registerLocationListener(myListener);// 注册定位监听接口

        /**
         * 设置定位参数
         */
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        //option.setOpenGps(true); // 打开gps
        //option.setScanSpan(1000);// 设置发起定位请求的间隔时间,ms
        option.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向
        option.setAddrType("all");// 返回的定位结果包含地址信息
        option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
        option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
        mLocationClient.setLocOption(option);
        mLocationClient.start(); // 调用此方法开始定位
        //stopLocationClient();
    }

    @Override
    public void onGetSuggestionResult(SuggestionResult suggestionResult) {

    }

    /**
     * 定位SDK监听函数
     *
     * @author
     *
     */
    public class MyLocationListener implements BDLocationListener {
        /*
        LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());*/
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            locType = location.getLocType();
            Log.i("mybaidumap", "当前定位的返回值是："+locType);
            if (location.hasRadius()) {// 判断是否有定位精度半径
                radius = location.getRadius();
            }

            if (locType == BDLocation.TypeNetWorkLocation) {
                addrStr = location.getAddrStr();// 获取反地理编码(文字描述的地址)
                Log.i("mybaidumap", "当前定位的地址是："+addrStr);
            }

            direction = location.getDirection();// 获取手机方向，【0~360°】,手机上面正面朝北为0°
            province = location.getProvince();// 省份
            city = location.getCity();// 城市
            district = location.getDistrict();// 区县
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
            if (locType == BDLocation.TypeNetWorkLocation) {
                addrStr = location.getAddrStr();// 获取反地理编码(文字描述的地址)
                Log.i("mybaidumap", "当前定位的地址是："+addrStr);
            }

            direction = location.getDirection();// 获取手机方向，【0~360°】,手机上面正面朝北为0°
            province = location.getProvince();// 省份
            city = location.getCity();// 城市
            district = location.getDistrict();// 区县
            searchNeayBy();
            if (isFirstLoc) {
                isFirstLoc = false;
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }


            /*/将当前位置加入List里面
            PoiInfo info = new PoiInfo();
            info.address = location.getAddrStr();
            info.city = location.getCity();
            info.location = ll;
            info.name = location.getAddrStr();
            dataList.add(info);
            adapter.notifyDataSetChanged();
            Log.i("mybaidumap", "province是："+province +" city是"+city +" 区县是: "+district);*/


            /*// 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            //画标志
            CoordinateConverter converter = new CoordinateConverter();
            converter.coord(ll);
            converter.from(CoordinateConverter.CoordType.COMMON);
            LatLng convertLatLng = converter.convert();

            OverlayOptions ooA = new MarkerOptions().position(ll).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka));
            //mCurrentMarker = (Marker) mBaiduMap.addOverlay(ooA);


            MapStatusUpdate u = MapStatusUpdateFactory.newLatLngZoom(convertLatLng, 17.0f);
            mBaiduMap.animateMapStatus(u);

            //画当前定位标志
            MapStatusUpdate uc = MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(uc);

            mMapView.showZoomControls(false);*/
            /*//poi 搜索周边
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    Looper.prepare();
                    searchNeayBy();
                    Looper.loop();
                }
            }).start();*/
        }


        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }

        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null) {
                return;
            }
        }
    }
    /**
     * 停止定位
     */
    public void stopLocationClient() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            // 关闭定位SDK,调用stop之后，设置的参数LocationClientOption仍然保留
            mLocationClient.stop();
        }
    }

    /**
     * 搜索周边
     */
    private void searchNeayBy(){
        // POI初始化搜索模块，注册搜索事件监听
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(this);
        PoiNearbySearchOption poiNearbySearchOption = new PoiNearbySearchOption();

        poiNearbySearchOption.keyword("医院");
        poiNearbySearchOption.location(new LatLng(mCurrentLat, mCurrentLon));
        poiNearbySearchOption.radius(5000);  // 检索半径，单位是米
        poiNearbySearchOption.pageCapacity(10);  // 默认每页10条
        mPoiSearch.searchNearby(poiNearbySearchOption);  // 发起附近检索请求
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Log.i("----------------", "---------------------");
                    adapter.notifyDataSetChanged();
                    break;

                default:
                    break;
            }
        }
    };

    /*
     * 接受周边地理位置结果

    @Override
    public void onGetPoiResult(PoiResult result) {
        // 获取POI检索结果
        if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {// 没有找到检索结果
            Toast.makeText(MapActivity.this, "未找到结果",Toast.LENGTH_LONG).show();
            return;
        }

        if (result.error == SearchResult.ERRORNO.NO_ERROR) {// 检索结果正常返回
//			mBaiduMap.clear();
            if(result != null){
                if(result.getAllPoi()!= null && result.getAllPoi().size()>0){
                    dataList.addAll(result.getAllPoi());
//					adapter.notifyDataSetChanged();
                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);
                }
            }
        }
    }*/



    // 针对检索功能模块（POI检索、线路规划等），地图SDK还对外提供相应的覆盖物来快速展示结果信息。这些方法都是开源的，开发者可根据自己的实际去求来做个性化的定制。
    // 利用检索结果覆盖物展示POI搜索结果的方式如下：
    // 第一步，构造自定义 PoiOverlay 类；
    private class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {
            super.onPoiClick(index);
            // PoiInfo类是poi信息类
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            // 判断poi点是否有美食类详情页面，这里也可以判断其它不是餐厅页面需要自己去查找方法api
            if (poi.hasCaterDetails) {
                // 返回该 poi 详情检索参数对象
                mpoiSearch.searchPoiDetail((new PoiDetailSearchOption())
                        .poiUid(poi.uid));
            }
            return true;

        }

    }

    // 检索查询事件监听实现的方法
    @Override
    public void onGetPoiDetailResult(PoiDetailResult result) {
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MapActivity1.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(MapActivity1.this, "成功，查看详情页面", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    // 检索查询事件监听实现的方法
    @Override
    public void onGetPoiResult(PoiResult result) {
        if (result == null
                || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            return;
        }
        // 判断搜索结果状态码result.error是否等于检索结果状态码， SearchResult.ERRORNO值的没问题
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            mBaiduMap.clear();
            PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result);
            overlay.addToMap();
            overlay.zoomToSpan();
            if(result != null){
                if(result.getAllPoi()!= null && result.getAllPoi().size()>0){
                    dataList.addAll(result.getAllPoi());
                    //adapter.notifyDataSetChanged();
                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);
                }
            }
            //return;
        }


    }



    class ListAdapter extends BaseAdapter{

        private int checkPosition;

        public ListAdapter(int checkPosition){
            this.checkPosition = checkPosition;
        }

        public void setCheckposition(int checkPosition){
            this.checkPosition = checkPosition;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return dataList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = LayoutInflater.from(MapActivity1.this).inflate(R.layout.list_item, null);

                holder.textView = (TextView) convertView.findViewById(R.id.text_name);
                holder.textAddress = (TextView) convertView.findViewById(R.id.text_address);
                holder.imageLl = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);

            }else{
                holder = (ViewHolder)convertView.getTag();
            }
            Log.i("mybaidumap", "name地址是："+dataList.get(position).name);
            Log.i("mybaidumap", "address地址是："+dataList.get(position).address);

            holder.textView.setText(dataList.get(position).name);
            holder.textAddress.setText(dataList.get(position).address);
            if(checkPosition == position){
                holder.imageLl.setVisibility(View.VISIBLE);
            }else{
                holder.imageLl.setVisibility(View.GONE);
            }

            return convertView;
        }

    }

    class ViewHolder{
        TextView textView;
        TextView textAddress;
        ImageView imageLl;
    }


    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        if (mLocationClient != null) {
            mLocationClient.stop();
        }

        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mPoiSearch.destroy();
        super.onDestroy();
        // 在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

}
