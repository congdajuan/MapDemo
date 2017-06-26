package com.example.cong.mapdemo;


import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.SupportMapFragment;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;

public class MainActivity extends FragmentActivity implements
        OnGetPoiSearchResultListener, OnGetSuggestionResultListener {
    // 定义 BaiduMap 地图对象的操作方法与接口
    private BaiduMap mbaiduMap;
    // LatLng地理坐标基本数据结构
    private LatLng latlng;
    // 类PoiSearch继承poi检索接口
    private PoiSearch mpoiSearch;
    // SuggestionSearch建议查询类
    private SuggestionSearch mSuggestionSearch;
    private int load_Index = 0;
    //  自动填充的text
    private AutoCompleteTextView keyWorldsView = null;
    private ArrayAdapter<String> sugAdapter = null;


    // 定位相关
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyBDLocationListener();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;
    private TextView locationInfoTextView = null;
    private Button startButton = null;
    //private LocationClient locationClient = null;
    private static final int UPDATE_TIME = 5000;
    private static int LOCATION_COUTNS = 0;

    //public LocationClient locationClient = null;
    //public MyBDLocationListener myBDLocationListener = new MyBDLocationListener();

    MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在使用SDK各组件之前初始化context信息，传入ApplicationContext
        // 注意该方法要再setContentView方法之前实现
        // 在SDK各功能组件使用之前都需要调用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        //mCurrentMode = LocationMode.NORMAL;
        mbaiduMap = ((SupportMapFragment) (getSupportFragmentManager()
                .findFragmentById(R.id.map))).getBaiduMap();
        // 普通地图
        mbaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        //initLocation();
        //stopLocationClient();

        // 设置一个坐标
        latlng = new LatLng(mCurrentLon, mCurrentLat);
        //latlng = new LatLng(39.963175, 116.400244);
        // 加载一个显示坐标的一个图标
        BitmapDescriptor bimp = new BitmapDescriptorFactory()
                .fromResource(R.drawable.icon_markb);
        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions opt = new MarkerOptions().position(latlng).icon(bimp);
        // 将这些添加到地图中去
        keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        keyWorldsView.setAdapter(sugAdapter);
        mbaiduMap.addOverlay(opt);
        // 实例化PoiSearch
        mpoiSearch = PoiSearch.newInstance();
        // 注册搜索事件监听
        mpoiSearch.setOnGetPoiSearchResultListener(this);
        // 实例化建议查询类
        mSuggestionSearch = SuggestionSearch.newInstance();
        // 注册建议查询事件监听
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        /**
         * 当输入关键字变化时，动态更新建议列表
         */
        keyWorldsView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int start, int before,
                                      int count) {
                if (cs.length() <= 0) {
                    return;
                }
                String city = ((EditText) findViewById(R.id.city)).getText()
                        .toString();
                /**
                 * 使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                 */
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(cs.toString()).city(city));
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

    }




    /**
     * 定位
     */
    private void initLocation(){
        //重新设置
        //checkPosition = 0;
        //adapter.setCheckposition(0);

        mbaiduMap = mMapView.getMap();
        mbaiduMap.clear();
        // 开启定位图层
        mbaiduMap.setMyLocationEnabled(true);
        mbaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(17).build()));   // 设置级别

        // 定位初始化
        mLocationClient = new LocationClient(getApplicationContext()); // 声明LocationClient类
        mLocationClient.registerLocationListener(myListener);// 注册定位监听接口

        /**
         * 设置定位参数
         */
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        //option.setScanSpan(5000);// 设置发起定位请求的间隔时间,ms
        option.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向
        option.setOpenGps(true);
        option.setAddrType("all");// 返回的定位结果包含地址信息
        option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
        option.setIsNeedAddress(true);// 返回的定位结果包含地址信息
        mLocationClient.setLocOption(option);
        mLocationClient.start(); // 调用此方法开始定位

    }


    /**
     * 设置相关参数
     */
    private void setLocationClientOption() {
        LocationClientOption locationClientOption = new LocationClientOption();

        locationClientOption.setOpenGps(true); //打开GPS
        locationClientOption.setCoorType("bd09ll"); //返回结果返回百度经纬度坐标系

        // 这个属性一定要设置 不然找不到服务就无法定位
        //locationClientOption.setServiceName("com.baidu.location.service_v2.9");
        locationClientOption.setScanSpan(3000);
        //locationClientOption.setPoiExtraInfo(true); // 设置是否需要POI的电话地址等详细信息

        // 设置是否要返回地址信息，默认为无地址信息。String 值为 all时，表示返回地址信息。
        locationClientOption.setAddrType("all");

        //locationClientOption.setPoiNumber(10); // 设置POI个数
        locationClientOption.disableCache(true); // 设置是否使用缓存定位

        // setLocOption:设置参数
        // 方法：public void setLocOption ( LocationClientOption )
        // 配置定位SDK，参数为LocationClientOption类
        mLocationClient.setLocOption(locationClientOption);
    }

    /**
     * 发起定位
     */
    public void requestLocationInfo() {
        setLocationClientOption();

        if (mLocationClient!= null && !mLocationClient.isStarted()) {
            mLocationClient.start(); // 启动定位SDK
        }

        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.requestLocation(); // 发起定位，异步获取当前位置
        }
    }

    /**
     * 发起离线定位

    public void requestLocationInfo2() {
        setLocationClientOption();

        if (locationClient != null && !locationClient.isStarted()) {
            locationClient.start(); // 启动定位SDK
        }

        if (locationClient != null && locationClient.isStarted()) {
            locationClient.requestOfflineLocation(); // 发起定位，异步获取当前位置
        }
    } */

    /**
     * 监听函数，有更新位置的时候，格式化成字符串，输出到屏幕中 BDLocationListener：获取定位结果，获取POI信息。2个方法：
     * public void onReceiveLocation ( BDLocation location ); public void
     * onReceivePoi(BDLocation poiLocation);
     *
     * poiLocation.getAddrStr()：获取文字描述的地址(反地理编码）（网络定位）
     */
    public class MyBDLocationListener implements BDLocationListener {
        // 获取定位结果的接口。用户自己实现这个接口后，监听定位结果
        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (location == null) {
                return;
            }

            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
                mCurrentLon = location.getLatitude();
                mCurrentLat = location.getLongitude();
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            Log.i("BaiduLocationApiDem", sb.toString());
            //sendBroadCast(location.getAddrStr() + sb.toString());
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

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


    public void searchButtonProcess(View v) {
        EditText editcity = (EditText) findViewById(R.id.city);
        EditText editSearchKey = (EditText) findViewById(R.id.searchkey);
        // PoiCitySearchOption（）poi城市内检索参数 city搜索城市 keyword key搜索关键字 pageNum -
        // 分页编号
        mpoiSearch.searchInCity((new PoiCitySearchOption())
                .city(editcity.getText().toString())
                .keyword(editSearchKey.getText().toString())
                .pageNum(load_Index));

    }

    public void goToNextPage(View v) {
        load_Index++;
        searchButtonProcess(null);
    }

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
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(MainActivity.this, "成功，查看详情页面", Toast.LENGTH_SHORT)
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
            mbaiduMap.clear();
            PoiOverlay overlay = new MyPoiOverlay(mbaiduMap);
            mbaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result);
            overlay.addToMap();
            overlay.zoomToSpan();
            return;
        }
        // AMBIGUOUS_KEYWORD表示 检索词有岐义
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

            // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
            String strInfo = "在";
            for (CityInfo cityInfo : result.getSuggestCityList()) {
                strInfo += cityInfo.city;
                strInfo += ",";
            }
            strInfo += "找到结果";
            Toast.makeText(MainActivity.this, strInfo, Toast.LENGTH_LONG)
                    .show();
        }

    }

    // 建议查询事件监听所要实现的方法,这个方法主要是为AutoCompleteTextView提供参数
    @Override
    public void onGetSuggestionResult(SuggestionResult res) {
        if (res == null || res.getAllSuggestions() == null) {
            return;
        }
        sugAdapter.clear();
        for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
            if (info.key != null)
                sugAdapter.add(info.key);
        }
        sugAdapter.notifyDataSetChanged();

    }



    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mpoiSearch.destroy();
        mSuggestionSearch.destroy();

    }
}