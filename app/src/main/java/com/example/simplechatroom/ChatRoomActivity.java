package com.example.simplechatroom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.server.ChatBean;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText editText;
    private Button button;//send
    private Button button2;//upload
    private Socket socket;
    private ArrayList<ChatBean> loglist;
    private ChatAdapter chatAdapter;

    private String ip;
    private String port;

    private final int RESULT_LOAD_IMAGE = 1234;
    private String BASE_PATH = "/storage/emulated/0/";

    //storage/emulated/0/tieba/79D2DEF845613D82B52CB938E7C18679.jpg
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        init();
        Intent intent = getIntent();
        ip = intent.getStringExtra("ip");
        port = intent.getStringExtra("port");
        final Handler handler = new MyHandler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("ip:" + ip + " port: " + port);
                    socket = new Socket(ip, Integer.parseInt(port));

                    InputStream is = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    while (true) {
                        String data = reader.readLine();
                        Message message = Message.obtain();
                        System.out.println("data:" + data);
                        if (data.equals("FILE")) {
                            DataInputStream dis = new DataInputStream(socket.getInputStream());
                            DataOutputStream dos = null;
                            File path = new File(BASE_PATH + "ReceivedFiles");
                            if (!path.exists()) {
                                path.mkdirs();
                            }
                            System.out.println("path:" + path);
                            File file = new File(path + "/" + dis.readUTF());
                            double totleLength = dis.readLong();
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            dos = new DataOutputStream(new FileOutputStream(file));
                            int length = -1;
                            byte[] buff = new byte[10240];
                            double curLength = 0;
                            System.out.println("Start Receiving");
                            while ((length = dis.read(buff)) > 0) {
                                dos.write(buff, 0, length);
                                curLength += length;
                                //System.out.println("Transmission progress: "+(curLength/totleLength*100)+"%");
                                if (curLength >= totleLength) {
                                    dos.flush();
                                    break;
                                }
                            }
                            System.out.println("Successful reception.");

                            message.what = 0;
                            message.obj = file;

                        }
                        // 发到主线程中 收到的数据
                        else {

                            message.what = 1;
                            message.obj = data;

                        }

                        handler.sendMessage(message);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("又出啥错了老哥");
//                    Intent backintent = new Intent(ChatRoomActivity.this, MainActivity.class);
//                    startActivity(backintent);
                }
            }
        }).start();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String data = editText.getText().toString();
                if (data == null || data.isEmpty()) return;
                editText.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream outputStream = socket.getOutputStream();
                            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                            ChatBean chatBean = new ChatBean(data, String.valueOf(socket.getLocalPort()), df.format(new Date()), 0);
//                            System.out.println(">>>"+chatBean.toJsonString());
                            outputStream.write((chatBean.toJsonString() + "\r\n").getBytes("utf-8"));

                            outputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();//获得图片的绝对路径
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            System.out.println("图片地址：" + picturePath);
            final File image = new File(picturePath);

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                        ChatBean chatBean = new ChatBean("picture:" + image.getName(), String.valueOf(socket.getLocalPort()), df.format(new Date()), -1);
                        outputStream.write((chatBean.toJsonString() + "\r\n").getBytes("utf-8"));
                        outputStream.flush();
                        Thread.sleep(200);
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        DataInputStream dis = new DataInputStream(new FileInputStream(image));
                        dos.writeUTF(image.getName());// 传送文件名字
                        dos.flush();
                        dos.writeLong(image.length());// 传送长度
                        dos.flush();

                        int length = -1;// 读取到的文件长度
                        byte[] buff = new byte[10240];

                        while ((length = dis.read(buff)) > 0) { // 循环读取文件，直到结束
                            dos.write(buff, 0, length);
                            dos.flush();
                        }
//                        System.out.println("传送文件完成");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        if (resultCode != RESULT_OK) {

            return;
        }
    }

    private void init() {
        recyclerView = findViewById(R.id.recycleview);
        editText = findViewById(R.id.et);
        button = findViewById(R.id.send_button);
        button2 = findViewById(R.id.upload_button);
        loglist = new ArrayList<>();
        chatAdapter = new ChatAdapter(this);

    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                File file = (File) msg.obj;

                String hint = "You have received file " + file.getName() + ".It has been saved into " + file.getPath();
                Toast.makeText(ChatRoomActivity.this, hint, Toast.LENGTH_LONG).show();
            } else if (msg.what == 1) {
                //
                String localPort = String.valueOf(socket.getLocalPort());
                JSONObject jsb = null;
                String namePort = null;
                String content = null;
                String time = null;
                int type = 0;

                try {
                    jsb = new JSONObject((String) msg.obj);
                    namePort = jsb.getString("name");
                    content = jsb.getString("content");
                    time = jsb.getString("time");
                    type = jsb.getInt("type");
                    System.out.println("port:" + namePort);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (localPort.equals(namePort)) {
                    ChatBean bean = new ChatBean(content, "我：", time, 2);//右
                    loglist.add(bean);


                } else if (namePort != null) {
                    ChatBean bean = new ChatBean(content, ("来自：" + namePort), time, 1);//左
                    loglist.add(bean);
                }

                // 向适配器set数据
                chatAdapter.setData(loglist);
                recyclerView.setAdapter(chatAdapter);
                LinearLayoutManager manager = new LinearLayoutManager(ChatRoomActivity.this, LinearLayoutManager.VERTICAL, false);
                recyclerView.setLayoutManager(manager);
            }
        }
    }

}
