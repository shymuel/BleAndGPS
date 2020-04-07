package com.myble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.myble.adapter.BleAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG ="ble_tag" ;

    TextView BleSerStatus;
    TextView BleBindStatus;
    ListView bleListView;
    Button start_scan;
    //以下为第二个layout
    private LinearLayout operaView;
    private Button btnWrite;
    private Button btnRead;
    private EditText etWriteContent;
    private TextView tvResponse;

    private List<BluetoothDevice> mDatas;  //列举蓝牙
    private List<Integer> mRssis;
    private BleAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean isScaning=false;
    private boolean isConnecting=false;
    private BluetoothGatt mBluetoothGatt;

    //服务和特征值
    //UUID是设备唯一识别码
    private UUID write_UUID_service;
    private UUID write_UUID_chara;
    private UUID read_UUID_service;
    private UUID read_UUID_chara;
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private UUID indicate_UUID_service;
    private UUID indicate_UUID_chara;
    private String hex="7B46363941373237323532443741397D";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);
        initView();
        initData();
        mBluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();  //这个地方他好像是用来监听蓝牙状态的
        if (mBluetoothAdapter==null||!mBluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  //申请打开蓝牙
            startActivityForResult(intent,0);
        }
    }

    private void initData() {
        mDatas=new ArrayList<>();
        mRssis=new ArrayList<>();
        mAdapter=new BleAdapter(MainActivity.this,mDatas,mRssis);
        bleListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void initView(){
        BleBindStatus=findViewById(R.id.tv_ser_bind_status);  //设备连接情况
        //BleSerStatus=findViewById(R.id.tv_ser_ble_status);  //设备搜索情况
        start_scan=findViewById(R.id.start_scan); //开始搜索
        bleListView=findViewById(R.id.ble_list_view);  //蓝牙设备列表
        operaView=findViewById(R.id.opera_view);  //读写操作的位置
        btnWrite=findViewById(R.id.btnWrite);  //写操作
        btnRead=findViewById(R.id.btnRead);  //读操作
        etWriteContent=findViewById(R.id.et_write);  //写操作的文本框
        tvResponse=findViewById(R.id.tv_response);  //返回的数据的文本框
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readData();
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() { //执行写入操作
            @Override
            public void onClick(View v) { writeData(); }
        });

        start_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {    //蓝牙标志
                if (isScaning){
                    BleSerStatus.setText("停止搜索");
                    stopScanDevice();
                }else{ checkPermissions(); }

            }
        });
        bleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isScaning){ stopScanDevice(); }
                if (!isConnecting){
                    isConnecting=true;
                    BluetoothDevice bluetoothDevice= mDatas.get(position);
                    //连接设备
                    BleBindStatus.setText("连接中");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback, TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this,
                                true, gattCallback);
                    }
                }

            }
        });
    }

    private void readData() {  //这个读数据有点问题，回头改成开关
        BluetoothGattCharacteristic characteristic=mBluetoothGatt.getService(read_UUID_service)
                .getCharacteristic(read_UUID_chara);
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 开始扫描 10秒后自动停止
     * */
    private void scanDevice(){
        BleBindStatus.setText("正在搜索");
        isScaning=true;
        mBluetoothAdapter.startLeScan(scanCallback);
        new Handler().postDelayed(new Runnable() {  //10s后执行进程中的动作
            @Override
            public void run() {
                //结束扫描
                mBluetoothAdapter.stopLeScan(scanCallback);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isScaning=false;
                        BleBindStatus.setText("搜索已结束");
                    }
                });
            }
        },10000);
    }

    /**
     * 停止扫描
     * */
    private void stopScanDevice(){
        isScaning=false;
        mBluetoothAdapter.stopLeScan(scanCallback);
    }

    BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e("warning", "run: scanning...");
            if (!mDatas.contains(device)){
                mDatas.add(device);
                mRssis.add(rssi);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
        /**
         * 断开或连接 状态发生变化时调用
         * */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG,"onConnectionStateChange()");
            if (status==BluetoothGatt.GATT_SUCCESS){
                //连接成功
                if (newState== BluetoothGatt.STATE_CONNECTED){
                    Log.e(TAG,"连接成功");
                    //发现服务
                    gatt.discoverServices();
                }
            }else{
                //连接失败
                Log.e(TAG,"失败=="+status);
                mBluetoothGatt.close();
                isConnecting=false;
            }
        }
        /**
         * 发现设备（真正建立连接）
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //直到这里才是真正建立了可通信的连接
            isConnecting=false;
            Log.e(TAG,"onServicesDiscovered()---建立连接");
            //获取初始化服务和特征值
            initServiceAndChara();
            //订阅通知
            BluetoothGattCharacteristic characteristic = mBluetoothGatt
                    .getService(notify_UUID_service).getCharacteristic(notify_UUID_chara);
            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic,true);

            if (b) {
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (b1) {
                        mBluetoothGatt.writeDescriptor(descriptor);
                        Log.d(TAG, "描述 UUID :" + descriptor.getUuid().toString());
                    }
                }
                Log.d(TAG, "startRead: " + "监听接收数据开始");
            }
            Log.e(TAG,"已订阅通知");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleListView.setVisibility(View.GONE);
                    operaView.setVisibility(View.VISIBLE);
                    BleBindStatus.setText("已连接");
                }
            });
        }
        /**
         * 读操作的回调
         **/
         @Override
         public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
         super.onCharacteristicRead(gatt, characteristic, status);
         Log.e(TAG,"onCharacteristicRead()");
         }
        /**
         * 写操作的回调
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.e(TAG,"onCharacteristicWrite()  status="+status+",value="+(new String(characteristic.getValue())));
        }
        /**
         * 接收到硬件返回的数据
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e(TAG,"onCharacteristicChanged()"+characteristic.getValue());
            final byte[] data=characteristic.getValue();
            //final String string1 = new String(characteristic.getValue());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addText(tvResponse,new String(data));  //修改UI，之前显示不出来有点问题
                    BleBindStatus.setText("new data");
                }
            });
        }
    };

    /**
     * 检查权限
     */
    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            // 用户已经同意该权限
                            scanDevice();
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.e(TAG,"用户开启权限后才能使用");
                        }
                    }
                });
    }

    private void initServiceAndChara(){
        List<BluetoothGattService> bluetoothGattServices= mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService:bluetoothGattServices){
            List<BluetoothGattCharacteristic> characteristics=bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic:characteristics){
                int charaProp = characteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    read_UUID_chara=characteristic.getUuid();
                    read_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"read_chara="+read_UUID_chara+"----read_service="+read_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    write_UUID_chara=characteristic.getUuid();
                    write_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"write_chara="+write_UUID_chara+"----write_service="+write_UUID_service);

                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notify_UUID_chara=characteristic.getUuid();
                    notify_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"notify_chara="+notify_UUID_chara+"----notify_service="+notify_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    indicate_UUID_chara=characteristic.getUuid();
                    indicate_UUID_service=bluetoothGattService.getUuid();
                    Log.e(TAG,"indicate_chara="+indicate_UUID_chara+"----indicate_service="+indicate_UUID_service);

                }
            }
        }
    }

    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();  //字节数
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight()); //移动到下面的行
        }
    }

    private void writeData(){
        BluetoothGattService service=mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(write_UUID_chara);
        byte[] data;
        String content=etWriteContent.getText().toString();
        if (!TextUtils.isEmpty(content)){
            data=content.getBytes();
        }else{
            String temp_string= "empty";
            data= temp_string.getBytes();
        }
        if (data.length>20){//数据大于个字节 分批次写入
            Log.e(TAG, "writeData: length="+data.length);
            int num=0;
            if (data.length%20!=0){
                num=data.length/20+1;
            }else{
                num=data.length/20;
            }
            for (int i=0;i<num;i++){
                byte[] tempArr;
                if (i==num-1){
                    tempArr=new byte[data.length-i*20];
                    System.arraycopy(data,i*20,tempArr,0,data.length-i*20);
                }else{
                    tempArr=new byte[20];
                    System.arraycopy(data,i*20,tempArr,0,20);
                }
                charaWrite.setValue(tempArr);
                mBluetoothGatt.writeCharacteristic(charaWrite);
            }
        }else{
            charaWrite.setValue(data);
            mBluetoothGatt.writeCharacteristic(charaWrite);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }
}
