package com.jp.socketserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.os.Message;
import android.util.Log;

@SuppressLint("NewApi") public class filetransfer {
	private ServerSocket serverSocket, TCPSeverSocket;
	 Socket datasocket;
	 int done=0;
	 
	 MainActivity maininstance;
	 
	 public void setinstance(MainActivity minstance)
	 {
		 maininstance=minstance;
	 }
	 
	public static final int DATAPORT = 6001;
	public boolean recieveFile( String FilePly)
    {
    	try {
    	if(TCPSeverSocket==null)
    	{
    	TCPSeverSocket = new ServerSocket();
    	TCPSeverSocket.setReuseAddress(true);
    	TCPSeverSocket.bind(new InetSocketAddress(DATAPORT)); 
    	}
    	while(true)
    	{
    		Log.d("recieveFile", "b4 accept");
    		Socket sock = TCPSeverSocket.accept();
    		Log.d("recieveFile", "data coming");
    		
    		ByteBuffer b = ByteBuffer.allocate(4);
    		byte[] flarray = b.array();
    	    InputStream is = sock.getInputStream();
    	    FileOutputStream fos = new FileOutputStream(FilePly);
    	    BufferedOutputStream bos = new BufferedOutputStream(fos);
    	    int reclen=is.read(flarray, 0, flarray.length);
    	    ByteBuffer buf = ByteBuffer.wrap(flarray);
    	    //buf.order(ByteOrder.BIG_ENDIAN);
    	    
    	    int filelen= buf.getInt();
    	    
    	    Log.d("recieveFile", "file len"+Integer.toString(filelen));
    	    
    	    byte[] mybytearray = new byte[filelen];
    	    int bytesRead=0;
    	    while(true)
    	    {
    	    int thisRead = is.read(mybytearray, bytesRead, mybytearray.length-bytesRead);
    	    bos.write(mybytearray, bytesRead, thisRead);
    	    bytesRead+=thisRead;
    	    if(bytesRead==mybytearray.length)
    	    	break;
    	    }
    	    bos.flush();
    	   
    	    bos.close();
    	    sock.close();
	           	 Message dMessage=new Message();
	        	 dMessage.what=0x4;
	        	 dMessage.obj="receive file for len"+Integer.toString(bytesRead);
	        	 
	        	 
	        	 maininstance.gethandler().sendMessage(dMessage);
	        	  //  File filenew = new File(mFilePly);
	            //	int file_size = Integer.parseInt(String.valueOf(filenew.length()));
	            	
    	   // Log.d("recieveFile", "file receive Ok"+Integer.toString(file_size));
    		//onPlay(playing);//play that
    		Log.d("recieveFile", "file playing OK");
    	}
    	
    	}
   	 catch (Exception ex) {
            Log.e("Socket exception in receive file", ex.toString(), ex);
           
        }
    	return true;
    }
	
	
	synchronized  public int   sendfile(String IPaddr, String filename)
    {
    	int len=0;
    	
        File filenew = new File(filename);
    	int file_size = Integer.parseInt(String.valueOf(filenew.length()));
    	
    	if(done==1)
    	{
    		return -1;
    	}
    	
    	try {
         done=1;
    	datasocket = new Socket(IPaddr, DATAPORT);
    	Log.d("sendfile", IPaddr);
    	
    	if(datasocket!=null)
    	{
    		ByteBuffer b = ByteBuffer.allocate(4);
    		b.putInt(file_size);
    		byte[] filelenarray = b.array();
    		

    	byte[] mybytearray = new byte[file_size];
    	Log.d("sendfile", "send finished"+Integer.toString((int) file_size));
    	
    	
    	
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
        bis.read(mybytearray, 0, mybytearray.length);
        byte[] dst = Arrays.copyOf(mybytearray, mybytearray.length);
        
        OutputStream os = datasocket.getOutputStream();
        os.write(filelenarray, 0, filelenarray.length);//send len
        os.write(dst, 0, dst.length);//send data
        os.flush();
        bis.close();
        os.close();
        datasocket.close();
        datasocket=null;
        
        
        
        //filenew.
        len=mybytearray.length;
        Log.d("sendfile", "send finished"+Integer.toString(mybytearray.length));
        if(datasocket!=null)
        datasocket.close();
        datasocket=null;
        done=0;
    	}
    	
    	 }
         catch (Exception ex) {
        	 Log.e("MYAPP", "exception", ex);
             Log.e("Socket exception ", ex.toString());
            
                 datasocket=null;
                 done=0;
         }
    	return len;
    	
    }

}
