package com.example.server;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientManager {
    private static final int PORT = 50000;
    private static List<Socket> clientList = new ArrayList<>();
    private static ServerSocket server = null;
    private static ExecutorService clientES = null;//thread pool


    public static class ServerRunnable implements Runnable{
        private Socket socket;
        private String MyPortName;
        private File file;
        public ServerRunnable(Socket socket){
            this.socket = socket;
        }

        public void sendMessageAll(String message,int type){
            //System.out.println(message);
            for(Socket sk:clientList){

                PrintWriter pout = null;
                OutputStream os = null;
                int length;
                byte[] buff = new byte[10240];
                try{
                    os = sk.getOutputStream();
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)),true);
                    pout.println(message);
                    pout.flush();
                    Thread.sleep(200);
                    if(type==-1){//还要发送一次图片
//                        if(String.valueOf(sk.getPort()) == MyPortName){//不用发给自己了
//                            continue;
//                        }
                        DataInputStream dis = new DataInputStream(new FileInputStream(file));
                        DataOutputStream dos = new DataOutputStream(os);
                        pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)),true);
                        pout.println("FILE\r\n");
                        pout.flush();
                        System.out.println("Start sending files");
                        dos.writeUTF(file.getName());// 传送文件名字
                        dos.flush();
                        dos.writeLong(file.length());// 传送长度
                        dos.flush();
                        length = -1;
                        while((length=dis.read(buff))>0){
                            dos.write(buff,0,length);
                            dos.flush();
                        }
                        System.out.println("Sent successfully");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        public void sendFileAllExceptSelf(File file){//因某种原因弃用
            int length;
            byte[] buff = new byte[10240];
            PrintWriter pout = null;
            for(Socket sk:clientList){
//                if(String.valueOf(sk.getPort()) == MyPortName){//不用发给自己了
//                    continue;
//                }
                try{
                    OutputStream os = sk.getOutputStream();
                    DataInputStream dis = new DataInputStream(new FileInputStream(file));
                    DataOutputStream dos = new DataOutputStream(os);
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)),true);
                    pout.println("FILE\r\n");
                    pout.flush();

                    dos.writeUTF(file.getName());// 传送文件名字
                    dos.flush();
                    dos.writeLong(file.length());// 传送长度
                    dos.flush();
                    length = -1;
                    while((length=dis.read(buff))>0){
                        dos.write(buff,0,length);
                        dos.flush();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        @Override
        public void run() {

            BufferedReader reader = null;
            BufferedWriter writer = null;
            try{
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
                while(true){

                    String message = reader.readLine();
                    System.out.println(message);

                    ChatBean chatBean = new Gson().fromJson(message,ChatBean.class);
                    MyPortName = chatBean.getName();
                    if(chatBean.getType()==-1){//发送的是图片

                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        DataOutputStream dos = null;
                        file = new File("F:\\Socket\\"+dis.readUTF());//保存到PC端F盘Socket目录下
                        System.out.println("File Path: "+file.getPath());
                        //获取服务器传过来的文件大小
                        double totleLength = dis.readLong();
                        System.out.println("File Length: "+totleLength);
                        dos = new DataOutputStream(new FileOutputStream(file));
                        //开始接收文件
                        System.out.println("Start receiving:");
                        int length=-1;
                        byte[] buff= new byte[10240];
                        double curLength = 0;
                        while((length=dis.read(buff))>0){
                            dos.write(buff, 0, length);
                            curLength+=length;
                            System.out.println("Transmission progress: "+(curLength/totleLength*100)+"%");
                            if(curLength==totleLength){
                                dos.flush();break;
                            }
                        }
                        System.out.println("Successful reception.");
                        this.sendMessageAll(message,-1);

                    }
                    else if(chatBean.getContent().equals("exit")){
                        System.out.println("User "+chatBean.getName()+" got off line.");
                        clientList.remove(socket);
                        this.sendMessageAll(message,0);
                        reader.close();
                        socket.close();
                        break;
                    }else{
                        this.sendMessageAll(message,0);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        try{
            server = new ServerSocket(PORT);
            clientES = Executors.newCachedThreadPool();
            System.out.println("server is running");
            while (true){
                Socket client = server.accept();
                System.out.println("Accept new connection from "+client.getPort());
//                OutputStream os = client.getOutputStream();
//                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));
//                writer.write("login successfully\r\n");
//                writer.flush();
                clientList.add(client);
                clientES.execute(new ServerRunnable(client));
            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
