package com.example.androidfirewall;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final int VPN_REQUEST_CODE = 0x0F;
    private Button btnStart;
    private ListView lvPackets;
    private boolean isStart;
    private List<String> list;
    private ArrayAdapter<String> adapter;
    private Intent intent;
    private DemoService.EchoBinder echoBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = new ArrayList<>();
        //list.add("aaaaa");

        lvPackets = findViewById(R.id.lvPackets);
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,list);
        lvPackets.setAdapter(adapter);

        registerReceiver(vpnStateReceiver, new IntentFilter(DemoService.BROADCAST_VPN_STATE));
        btnStart = findViewById(R.id.btnStart);
        btnStart.setText("start");
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isStart) {
                    startVPN();
                }else{
                    sendBroadcast(new Intent(DemoService.BROADCAST_STOP_VPN));
                }
            }
        });
        intent = new Intent(this,DemoService.class);

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            list.add(msg.getData().getString("data"));
            adapter.notifyDataSetChanged();
            return false;
        }
    });

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            stopService(new Intent(MainActivity.this, DemoService.class));
        }
    };


    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (DemoService.BROADCAST_VPN_STATE.equals(intent.getAction()))
            {
                if (intent.getBooleanExtra("running", false))
                {
                    isStart = true;
                    btnStart.setText("stop");
                }
                else
                {
                    isStart =false;
                    btnStart.setText("start");
                    handler.postDelayed(runnable,200);
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK)
        {
            bindService(intent,MainActivity.this,Context.BIND_AUTO_CREATE);

        }
    }

    private void startVPN()
    {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
        {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        }
        else
        {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        unregisterReceiver(vpnStateReceiver);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        echoBinder = (DemoService.EchoBinder)iBinder;
        echoBinder.getService().setCallback(new DemoService.Callback() {
            @Override
            public void onDataChange(String data) {
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data",data);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

}
