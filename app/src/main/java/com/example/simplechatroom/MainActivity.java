package com.example.simplechatroom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private EditText ipEditText, portEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        ipEditText.setText("192.168.1.4");//设置默认值
        portEditText.setText("50000");//设置默认值
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipEditText.getText().toString().trim();
                String port = portEditText.getText().toString().trim();
                if (ip.equals(null) || port.equals(null) || ip.isEmpty() || port.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please Input ip and port", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
                intent.putExtra("ip", ip);
                intent.putExtra("port", port);
                startActivity(intent);
            }
        });
    }


    private void initView() {
        button = findViewById(R.id.login_button);
        ipEditText = findViewById(R.id.ip_edit_text);
        portEditText = findViewById(R.id.port_edit_text);
    }
}
