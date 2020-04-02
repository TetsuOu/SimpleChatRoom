简书链接：https://www.jianshu.com/p/8c83da946a1a

## 4.写一个简单的 chat 程序，并能互传文件，编程语言不限

本例采用PC端做服务器端，android端做客户端，进行Socket通信；能进行群聊，并能上传android端的本地文件（图片）到PC端，PC端又将收到的文件转发给所有用户。

### 4.1 服务器端

#### 4.1.1 新建Module

新建一个AndroidStudio工程，命名为SimpleChatRoom。为了方便，新建一个Module命名为Server，即服务器端。

![image-20200331162539086](http://q7oeubsc9.bkt.clouddn.com/image-20200331162539086.png)

#### 4.1.2 新建Java类ChatBean

将一条聊天内容表示为一个Java对象ChatBean，主要包含：

```java
private String content;//聊天内容
private String name;//姓名，后面使用端口号作为姓名
private String time;//发送消息的时间
private int type;//消息类型，有两处用到。为0表示文字消息，为-1表示图片信息。为1表示他人发的消息，为2表示自己发的消息
```

生成其相应的get、set函数还有构造函数后，还需实现一个将一个ChatBean对象转化为JSON类型的字符串的方法。方便Socket通信时存取数据。

```java
public String toJsonString(){
        String res = "{\"content\":\""+content+"\",\"name\":\""+name+
                     "\",\"time\":\""+time+"\",\"type\":"+type+"}";
        return res;
    }
```

转换如形如：`{"content":"这是内容","name":"50000","time":"16:38:00","type":0}`

#### 4.1.3 新建Java类ClientManager

该类运行为服务器端。设置了下列4个全局变量：

```java
private static final int PORT = 50000;//服务端要监听的端口
private static List<Socket> clientList = new ArrayList<>();//为实现群聊功能而保存客户端列表
private static ServerSocket server = null;//服务器端
private static ExecutorService clientES = null;//线程池，一个连接请求开始一个线程
```

主函数：

```java
public static void main(String[] args){
        try{
            server = new ServerSocket(PORT);//服务端绑定端口PORT
            clientES = Executors.newCachedThreadPool();//创建一个可缓存线程池
            System.out.println("server is running");
            while (true){
                Socket client = server.accept();//等待客户端的连接。这是一个阻塞函数。
                System.out.println("Accept new connection from "+client.getPort());
                clientList.add(client);//加入客户端列表
                clientES.execute(new ServerRunnable(client));//连接后开启线程，进行socket通信
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
```

#### 4.1.4 聊天信息

在子线程ServerRunnable中进行

重写的run函数部分代码：

```java
public void run() {
            BufferedReader reader = null;
            try{
                InputStream is = socket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
                while(true){
                    String message = reader.readLine();
                    System.out.println(message);
                    ChatBean chatBean = new Gson().fromJson(message,ChatBean.class);
                    //...
                    this.sendMessageAll(message);
                    //...
```

通过getInputStream获取socket的输入流，服务端的Socket对象上的getInputStream方法得到的输入流其实就是从客户端发送给服务器端的数据流。利用InputStreamReader类将得到的`字节流`转化为`字符流`，按"UTF-8"编码格式进行解码。然后创建字符串缓冲流对象reader，利用其readline()方法读取在构造函数中传入的字符流并缓冲字符，以便有效地读取字符。注意，readline()方法是一个阻塞函数，读取一个文本行。故发送数据方要在末尾加上字符'\r'或'\n'等。

此时获取到的message就是前文中提到过的JSON类型字符串，为了得到其中的某些数据，又需要将其转换为JAVA对象，这里用过导入一个第三方jar包`com.google.gson`，利用里面的fromJson()方法将一个JSON类型的字符串转化为Java对象chatBean。再利用chatBean的get方法就能得到相应的内容了，比如可以通过getContent()获取content字段，判断是否是`exit`，若是则关闭Socket连接等。

得到消息后，将其对客户端列表中的所有客户端进行消息转发以实现群聊功能。

sendMessageAll()函数：

```java
public void sendMessageAll(String message){
            for(Socket sk:clientList){
                PrintWriter pout = null;
                try{
					pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sk.getOutputStream())),true);
                    pout.println(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
```

for循环遍历客户端列表，将前面得到的message发送一遍给各个客户端。方法和接收数据的类似，只不过反了一遍。

#### 4.1.5 文件信息

同样的是在子线程中进行，为了简便，和文字消息的处理在同一个线程，通过对message的type字段进行判断，若其属于文件（图片），则进行文件传输。

重写的部分run()方法：

```java
public void run() {
            BufferedReader reader = null;
            BufferedWriter writer = null;
            try{
                InputStream is = socket.getInputStream();
                DataInputStream dis = new DataInputStream(is);
                DataOutputStream dos = null;
                while(true){
                    //...
                    if(chatBean.getType()==-1){//发送的是图片
                        //保存到PC端F盘Socket目录下
                        File file = new File("F:\\Socket\\"+dis.readUTF());
                        System.out.println("File Path: "+file.getPath());
                        //获取服务器传过来的文件大小
                        double totleLength = dis.readLong();
                        System.out.println("File Length: "+totleLength);
                        dos = new DataOutputStream(new FileOutputStream(file));
                        //开始接收文件
                        System.out.println("Start receiving:");
                        int length=-1;
                        byte[] buff= new byte[10240];//一次接收10240个字节
                        double curLength = 0;
                        while((length=dis.read(buff))>0){
                            dos.write(buff, 0, length);
                            curLength+=length;
                            System.out.println("Transmission progress: "+(curLength/totleLength*100)+"%");
                            if(curLength==totleLength){//传输完成
                                dos.flush();break;
                            }
                        }
                        System.out.println("Successful reception.");
                    }
```

接收到文件保存后，同样可以转发

sendFileAllExceptSelf函数：

```java
 public void sendFileAllExceptSelf(File file){
            int length;
            byte[] buff = new byte[10240];
            PrintWriter pout = null;
            System.out.println(clientList.size());
            for(Socket sk:clientList){
                if(String.valueOf(sk.getPort()) == MyPortName){//不用发给自己了
                    continue;
                }
                try{
                    OutputStream os = sk.getOutputStream();
                    pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)),true);
                    pout.println("FILE");//发给客户端时标记开始发送文件了
                    pout.flush();

                    DataInputStream dis = new DataInputStream(new FileInputStream(file));
                    DataOutputStream dos = new DataOutputStream(os);
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
```

### 4.2 客户端

主要有两个页面，登录页面和聊天室

#### 4.2.1 登录页面

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200331205529283.png" alt="image-20200331205529283" style="zoom: 20%;" />

是由两个EditText分别用于输入IP地址（服务器端）和端口号，还有一个Button，绑定点击事件用于进入聊天室组成的。

#### 4.2.2 聊天室页面

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200331205853655.png" alt="image-20200331205853655" style="zoom:20%;" />

是由一个EditText用于输入要发送的信息，两个Button分别用来确定发送消息和打开本地相册选择要上传的图片，以及一个RecyclerView用于展现聊天记录（上图中未显示）组成的。

#### 4.2.3  聊天信息

创建一个Handler对象，用来配合Message处理异步消息：

```java
final Handler handler = new MyHandler();
```

开启子线程进行Socket的连接及发送与接收信息：

```java
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
                if(data.equals("FILE")){
                    //...接收文件，和服务器端的一样
                    System.out.println("Successful reception.");
                    message.what = 0;
                    message.obj = file;
                }
                else{
                    message.what = 1;
                    message.obj = data;
                }
                handler.sendMessage(message);// 发到主线程中 收到的数据
            }

        } catch (Exception e) {
           //...
        }
    }
}).start();
```

点击`Send`按钮后发送消息，其绑定的点击事件为;

```java
button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String data = editText.getText().toString();//获取输入的信息
                if (data == null || data.isEmpty()) return;//信息为空时不发送
                editText.setText("");//发送后编辑框清空
                new Thread(new Runnable() {//开启子线程
                    @Override
                    public void run() {
                        try {
           OutputStream outputStream = socket.getOutputStream();
           SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//时间的格式
           ChatBean chatBean = new ChatBean(data, String.valueOf(socket.getLocalPort()), df.format(new Date()), 0);//将一条聊天记录封装为一个Java对象
           //将转为Java对象的聊天记录转换为JSON型字符串后，再将其以字节流的形式发送给服务器端  
           outputStream.write((chatBean.toJsonString() + "\r\n").getBytes("utf-8"));
		   outputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
```

#### 4.2.4 文件信息

点击`img`按钮后打开本地相册，选择要发送的照片：

```java
button2.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }
});
```

```java
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
        final File image=new File(picturePath);
        //...
```

此时选中了要发送的图片`image`

开启子线程发送文件：(和服务器端发送文件给客户端的大致一样)

```java
new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        //...
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
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
```

#### 4.2.5 处理返回结果

```java
private class MyHandler extends Handler {
    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if(msg.what == 0){//收到文件
            File file = (File) msg.obj;
            String hint = "You have received file "+file.getName()+".It has been saved into "+file.getPath();//提示收到了文件
            Toast.makeText(ChatRoomActivity.this,hint,Toast.LENGTH_LONG).show();
        }
        else if (msg.what == 1) {//聊天信息
            String localPort = String.valueOf(socket.getLocalPort());
            //...利用org.json包中的JSONObject对JSON型字符串进行解析得到相应的值
            if (localPort.equals(namePort)) {//自己发送的信息
                ChatBean bean = new ChatBean(content, "我：", time, 2);//右
                loglist.add(bean);
            } else {//其他人发送的消息
                ChatBean bean = new ChatBean(content, ("来自：" + namePort), time, 1);//左
                loglist.add(bean);
            }
            // 向适配器set数据，更新RecyclerView聊天界面
            chatAdapter.setData(loglist);
            recyclerView.setAdapter(chatAdapter);
            LinearLayoutManager manager = new LinearLayoutManager(ChatRoomActivity.this, LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(manager);
        }
    }
}
```

### 4.3 实现效果

客户端运行在PC端上，一个android真机和一个AndroidStudio上的虚拟机（这个虚拟机上系统都是英文的，时区为零时区，故隔了八个小时）作为两个客户端

#### 4.3.1 聊天 

<img src="http://q7oeubsc9.bkt.clouddn.com/截屏_20200401_222644.jpg"  width="20%" />  <img src="http://q7oeubsc9.bkt.clouddn.com/]WH[KUECZC@]AIOXKU1@91X.png" width="18%"  />

服务器端消息：

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200402100617518.png" width="70%" />

#### 4.3.2 传文件

将android真机上的图片上传到服务器，然后服务器将图片转发给用户（这里为了更好的体现效果，这里也将图片转发给原上传者）

android真机选择照片：

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200402101042418.png" width="30%"  />

上传到PC端：

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200402101117732.png" width="40%" />

聊天界面：（由于虚拟机的根目录不清楚，故未将图片转发给虚拟机）

<img src="http://q7oeubsc9.bkt.clouddn.com/截屏_20200402_100001.jpg" width="18%"/> <img src="http://q7oeubsc9.bkt.clouddn.com/截屏_20200402_100151.jpg" width="18%" /> <img src="http://q7oeubsc9.bkt.clouddn.com/image-20200402101349771.png" width="18%" />



服务器端消息：

<img src="http://q7oeubsc9.bkt.clouddn.com/image-20200402102214423.png" width="80%" />