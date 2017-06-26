package com.example.cong.mapdemo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
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

public class PoiActivity extends FragmentActivity implements
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在使用SDK各组件之前初始化context信息，传入ApplicationContext
        // 注意该方法要再setContentView方法之前实现
        // 在SDK各功能组件使用之前都需要调用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mbaiduMap = ((SupportMapFragment) (getSupportFragmentManager()
                .findFragmentById(R.id.map ))).getBaiduMap();
        // 普通地图
        mbaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        // 设置一个坐标
        latlng = new LatLng(39.963175, 116.400244);
        // 加载一个显示坐标的一个图标
        BitmapDescriptor bimp = new BitmapDescriptorFactory()
                .fromResource(R.drawable.icon_markb);
        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions().position(latlng).icon(bimp);
        // 将这些添加到地图中去
        keyWorldsView = (AutoCompleteTextView) findViewById(R.id.searchkey);
        sugAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line);
        keyWorldsView.setAdapter(sugAdapter);
        mbaiduMap.addOverlay(option);
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
            Toast.makeText(PoiActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(PoiActivity.this, "成功，查看详情页面", Toast.LENGTH_SHORT)
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
            Toast.makeText(PoiActivity.this, strInfo, Toast.LENGTH_LONG)
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