package com.example.jolin.afinal;

import android.os.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatClientThread extends Thread {

    private static Socket socket = null;
    private static Client client = null;
    private static InputStream is = null;
    // private FileOutputStream fos = null;
    private DataInputStream dis = null;
    private byte[] buffer;
    // private File file = null;
    // private RandomAccessFile rand = null;
    // private int photoCount = 0;
    // public static boolean threadStatus = true;
    private double muscleData;
    private int byteLenData = 0;
    private static byte[] readBuffer = null;
    public static Lock lock = new ReentrantLock();
    public static Condition condition = lock.newCondition();

    public ChatClientThread(Client _client, Socket _socket) {
        client = _client;
        socket = _socket;
        open();
        start();
        // main thread 用另個buffer存收到的影像
        // 此thread 收到影像後，看main thread的lock是否正在收。
    }

    public void open() {
        try {
            is = socket.getInputStream();
            dis = new DataInputStream(is);


        } catch (IOException e) {
            System.out.println("Error getting input stream : " + e.getMessage());
            client.stop();
        }
    }

    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            System.out.println("Error closing input stream : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.print("----------------ChatClientThread----------------");
        /*從Server傳入影像byte，再輸出成file*/
        try {
            // while(threadStatus){
            while(true){
                /*if(Client.allowReceive == false){
                    // 照片還沒被拍下，還沒有影像要傳
                    continue;
                }*/

                System.out.println("ChatClientThread Lock!");
                lock.lock();
                try{
                    // 鎖空byte，有東西才解
                    System.out.println("ChatClientThread Await!");
                    condition.await();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                    System.out.println("ChatClientThread UnLock!");
                }

                System.out.println("Ready to Receive");

                // 用lock 鎖住，放到另個地方存(buffer) main thread較順
                // 收送都要

                boolean type = dis.readBoolean();
                if(type == true){ // true 表示收到muscleData
                    muscleData = dis.readDouble();
                }
                else{ // false 表示收到影像array
                    byteLenData = dis.readInt();
                    System.out.println("Receive image file length: " + byteLenData);
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Error : " + e.getMessage());
                    }

                    buffer = new byte[byteLenData];
                    int count = 0;
                    lock.lock();
                    while(count < byteLenData){
                        count += is.read(buffer, count, byteLenData - count);
                    }
                    lock.unlock();
                    // rand.write(buffer); //Writes bytes to output stream
                    System.out.println("Receive image from Server..." + count);

                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Error : " + e.getMessage());
                    }

                    // rand.close();
                    System.out.println("Receive image FINISH.");
                }

                if(lock.tryLock()){
                    try{
                        readBuffer = buffer;
                    }finally {
                        lock.unlock();
                    }
                }

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateReceiveTask();
                    }

                }).start();

                // Client.allowReceive = false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error" + e.getMessage());
            // threadStatus = false;
            client.stop();
        }
    }

    public static byte[] getReadBuffer(){
        return readBuffer;
    }

    private void updateReceiveTask()
    {
        Message msg = new Message();
        msg.what = StartGameActivity.DO_UPDATE_Receive;
        StartGameActivity.mUpdateHandler.sendMessage(msg);
    }
}