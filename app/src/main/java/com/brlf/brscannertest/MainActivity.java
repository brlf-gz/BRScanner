package com.brlf.brscannertest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.brlf.brscanner.CaptureActivity;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView start_scan = new TextView(this);
        start_scan = (TextView) findViewById(R.id.start_scan);
        start_scan.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class), REQUEST_CODE);
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    if (TextUtils.isEmpty(data.getStringExtra("result"))) {
                        Toast.makeText(this, "扫码失败", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, data.getStringExtra("result"), Toast.LENGTH_LONG).show();
                    }
                    break;

                case RESULT_CANCELED:
                    Toast.makeText(this, "扫码取消", Toast.LENGTH_LONG).show();
                    break;

                case CaptureActivity.RESULT_ERROR:
                    Toast.makeText(this, data.getStringExtra("result"), Toast.LENGTH_LONG).show();
                    break;
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
