package com.jp.socketserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



 public class MainActivity extends ListActivity implements OnClickListener {

		
String[] itemname={"","","","",""} ;
	Handler updateHandler;
	Thread serverThread = null;
	String IP=null, lastIP=null,  bip=null;
	DatagramSocket udpsocket=null;
	String messageStr="Hello Android!";
	private static final String LOG_TAG = "AudioRecordTest";
	 DatagramSocket localsocket ;
	 filetransfer mtransfer;
	private FileDescriptor audiofd=null;
	FileInputStream mfileInputStream =null;
	public static final int SERVERPORT = 6000;
	ArrayAdapter<String> aaclient;
	List<String> peer_list = new ArrayList<String>();

	
	
	Handler gethandler()
	{
	 return updateHandler;
	}
	
	private class PeerMsg
	{
		String IP;
		String Device;
		String Msg;
	};
	
	List<PeerMsg> devicelist;
	static boolean playing=true;
	
	MediaRecorder mRecorder;
	MediaPlayer mPlayer;
	private static String mFileRec=null, mFilePly = null, mFileTest=null;
	
	public void onTabClicked(View view) {
		new AlertDialog.Builder(this)
	    .setTitle("about")
	    .setMessage("This app is only for demo usage, author: Peng Jiang, jiangpen@gmail.com")
	    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            // continue with delete
	        }
	     })
	     
	     /*
	    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            // do nothing
	        }
	     })
	     */
	    .setIcon(android.R.drawable.ic_dialog_alert)
	     .show();
	}

    public MainActivity() {
    	String tmp=Environment.getExternalStorageDirectory().getAbsolutePath();
    	mFileRec = tmp;
    	mFileRec += "/audiorecordtest.3gp";
    	
    	mFilePly=tmp;
    	mFilePly+="/audioplaytest.3gp";
    	
    	
    	mFileTest=tmp;
    	mFileTest+="/sensear.log";
    	
    	localsocket=null;
    
    	
    	
    }
    
   
    private void showinGUI(String msg)
    {
   	 Message dMessage=new Message();
   	 dMessage.what=0xff;
   	 dMessage.obj=msg;
   	 updateHandler.sendMessage(dMessage);
    }
    
    
    public class sendthread implements Runnable 
    {
    	private String sendip;
    	public sendthread(Object parameter) {
    	       // store parameter for later user
    		sendip=(String) parameter;
    	   }
		@Override
		public void run() {
			// TODO Auto-generated method stub
			int by=mtransfer.sendfile(sendip, mFileRec);
          	 Message dMessage=new Message();
       	 dMessage.what=0xff;
       	 dMessage.obj="send file for len"+Integer.toString(by);
       	 updateHandler.sendMessage(dMessage);
		}
    
    }
    
    private void openandsent(String otherip)
    {
    	Runnable r = new sendthread(otherip);
    	new Thread(r).start();
    	/*
        new Thread(new Runnable() {  
            @Override  
            public void run() 
            { 

            int by=mtransfer.sendfile(lastIP, mFileRec);
           	 Message dMessage=new Message();
        	 dMessage.what=0xff;
        	 dMessage.obj="send file for len"+Integer.toString(by);
        	 updateHandler.sendMessage(dMessage);
            }
            }).start();
            */
    }
    
    
    
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
            
          //  openandsent();
            
           for(int i=0; i<devicelist.size();  i++)
       	 {
       		 PeerMsg device= devicelist.get(i);
       		 if(device!=null  && device.IP!=null)
       		 {
       				Log.d("IP", "send to "+device.IP);
       				lastIP=device.IP;
       			 openandsent(device.IP);
       		        
       		
       		        
       		 }
       	 }
            
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    
    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFilePly);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    
    private void startRecording() {
      

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileRec);
        
        /*
         if(audiofd==null)
         
        	return;
        mRecorder.setOutputFile(audiofd);
        */
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
       
        mRecorder = null;

        
    }

	public void getallpeer()
	{
		//Toast.makeText(getBaseContext(), IP, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {  
            @Override  
            public void run() 
            { 
            	bip=getBroadcastIP(IP);
            	Log.d("IP", "send to "+bip);
            	pingpeer(bip, null);
            	
            }
            }).start();
	}
	public void initIP()
	{
    	String s=getIpAddress();
    	if(s!=null)
    	{
    	
    	IP=s;
    	}
    	else
    		Toast.makeText(getBaseContext(), "return null", Toast.LENGTH_SHORT).show();
	
    	getallpeer();
	}
	
	public void sendtopeer()
	{
        new Thread(new Runnable() {  
            @Override  
            public void run() 
            { 
            	//String br=getBroadcastIP(IP);
            	
            	while(true)
            	{
            	 for(int i=0; i<devicelist.size();  i++)
            	 {
            		 PeerMsg device= devicelist.get(i);
            		 if(device!=null  && device.IP!=null)
            		 {
            				Log.d("IP", "send to "+device.IP);
                        	pingpeer(device.IP, "that is only from this device");
            		 }
            	 }
            	 
              try {
				Thread.sleep(10000);
				Log.d("IP", "sleep retry ");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            	}
            	
            }
            }).start();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		//get selected items
		String selectedValue = (String) getListAdapter().getItem(position);
		Toast.makeText(this, selectedValue, Toast.LENGTH_SHORT).show();

	}
	
	@SuppressLint("NewApi") ArrayAdapter createlist()
	{
	String testitem[]={"BC","DC"};	
	//aaclient=new ArrayAdapter<String>(this, R.layout.mylist,R.id.label,itemname);	
	aaclient.clear();
	aaclient.addAll(testitem);
	return aaclient;
	}
	
	   private class MyArrayAdapter<String> extends ArrayAdapter<String>
	    {
	        public MyArrayAdapter(Context context, int resource, int textViewResourceId, String[] itemname) {
	            super(context, resource, textViewResourceId, itemname);
	        }

	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View itemView = super.getView(position, convertView, parent);
	            ImageView imageView = (ImageView) itemView.findViewById(R.id.logo);
	            if(position<itemindex)
	            imageView.setImageResource(R.drawable.android_logo);
	            else
	            	imageView.setImageResource(R.drawable.asfalt_light);
	            
	            return itemView;
	        }
	    }
	   
	   
	
	 int itemindex=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(getBaseContext(), "Oncreate", Toast.LENGTH_SHORT).show();
         devicelist = new ArrayList<PeerMsg>();
        
         itemindex=0;
         mtransfer=new filetransfer();
         
         mtransfer.setinstance(this);

         
         final Button buttonPTT = (Button) findViewById(R.id.ptt);
        // buttonPTT.setBackgroundColor(0xff99ccff);
         initIP();
         
         buttonPTT.setOnTouchListener(new OnTouchListener() {
        	    @Override
        	    public boolean onTouch(View v, MotionEvent event) {
        	        if(event.getAction() == MotionEvent.ACTION_DOWN) {
        	           //pressed
        	        	Log.d("button", "press");
        	        	buttonPTT.setBackgroundColor(0xffcccc99);
        	        	onRecord(true);
        	        	
        	        } else if (event.getAction() == MotionEvent.ACTION_UP) {
        	           //relased
        	        	buttonPTT.setBackgroundColor(0xff99ccff);
        	        	Log.d("button", "relased");
        	        	onRecord(false);
        	    }
        	        return true;
        	}
         });
        	

                 
         

        
        

       
       
        
        new Thread(new Runnable() {  
            @Override  
            public void run() 
            { 
            	if(IP==null)
            		IP=getIpAddress();
            	
            	String s=pongpeer(IP);//dead loop
            	
            }
            }).start();
        
        new Thread(new Runnable() {  
            @Override  
            public void run() 
            { 
            	
            	Log.d("rec", "before receive file");
            	mtransfer.recieveFile(mFilePly);//dead loop
            	
            }
            }).start(); 
        
        
        if(itemname!=null)
        aaclient=new MyArrayAdapter<String>(this, R.layout.mylist,R.id.label,itemname);
    
        this.setListAdapter(aaclient);
        
        
        sendtopeer();
        
        updateHandler = new Handler()
        {
        	@Override
            public void handleMessage(Message inputMessage) {
        		if(inputMessage.what==1)
        		{
        			//EditText mylist = (EditText) findViewById(R.id.editText1);
        			 
        			
        			PeerMsg s=(PeerMsg)inputMessage.obj;
        			String ss="IP:"+s.IP+" from:"+s.Device;
        			//if(s.Msg!=null)
        			//	ss+=s.Msg;
        			
        			//mylist.append(ss);
        			peer_list.add(ss);
        			
        			
        		   // itemname = new String[peer_list.size()];
        			//itemname = peer_list.toArray(itemname);
        			ListView p=(ListView) findViewById(android.R.id.list);
        			//p.invalidateViews();
        			
        			//aaclient=createlist();
        			//aaclient.add("test1");
        			itemname[itemindex]=ss;
        			itemindex++;
        			if(itemindex==5)
        				itemindex=0;
        			aaclient.notifyDataSetChanged();
        			Toast.makeText(getBaseContext(),ss , Toast.LENGTH_SHORT).show();
                	
        		}
        		
        		if(inputMessage.what==2)
        		{
        			PeerMsg s=(PeerMsg)inputMessage.obj;
        			//Toast.makeText(getBaseContext(),s.Msg , Toast.LENGTH_SHORT).show();
        		}
        		
        		if(inputMessage.what==3)
        		{
        		
        			PeerMsg s=(PeerMsg)inputMessage.obj;
        			String ss="IP:"+s.IP+" from:"+s.Device;
        			//Toast.makeText(getBaseContext(),"msg3"+ss , Toast.LENGTH_SHORT).show();
        		}
        		
        		if(inputMessage.what==0x4)
        		{
        			onPlay(playing);//play that
        		}
        		
        		if(inputMessage.what==0xff)
        		{
        			String s=(String)inputMessage.obj;
        			//Toast.makeText(getBaseContext(),s, Toast.LENGTH_SHORT).show();
        		}
        		
        	}
        };
    }
    
    /*this function will return IP array of given myip, if my ip is 192.168.1.2 then return 192..168.1.1 to 192.168.1.254*/
    public String getBroadcastIP(String myip)
    {
        if(myip!=null)
        {
        	String[] iparry=myip.split("\\.");
        	
        	
        	if(iparry!=null)
        	{
        		iparry[3]="255";
        		return iparry[0]+"."+iparry[1]+"."+iparry[2]+"."+iparry[3];
        	}
        }
    	return null;
    }
    
    
    
    public String pongpeer(String IPaddr)
    {
        String text=null;
        int server_port = SERVERPORT;
       
        try {
       if(udpsocket==null)
       {
         udpsocket = new DatagramSocket(null);
         udpsocket.setReuseAddress(true);
         udpsocket.bind(new InetSocketAddress(server_port));
       }
        byte[] message = new byte[1500];
        DatagramPacket p = new DatagramPacket(message, message.length);
        while(true)
        {
        try {
        udpsocket.receive(p);
        text = new String(message, 0, p.getLength());
        String[] separated = text.split(":");
        
        
        Log.d("Udp tutorial","message:" + text);
        if(separated[0]!=null && separated[0].compareTo(messageStr)==0)
        {
        	 Log.d("Udp tutorial","you are here!");
        	
        	
        	 PeerMsg mymsg=new PeerMsg();
        	 mymsg.Device= separated[2];
        	 mymsg.IP=separated[1];
        	 if(separated.length==4)
        	 mymsg.Msg=separated[3];
        	 else
        		 mymsg.Msg=null;
        	 
        	 Message dMessage=new Message();
        	 dMessage.what=3;
        	 dMessage.obj=mymsg;
        	 updateHandler.sendMessage(dMessage);
        	 
        	 int found=0;
        	 for(int i=0; i<devicelist.size();  i++)
        	 {
        		 PeerMsg device= devicelist.get(i);
        		 if(device!=null  && device.IP!=null)
        		 {
        			 if(device.IP.compareTo(mymsg.IP)==0)//found same
        			 {
        				 found=1;
        				 break;
        			 }
        		 }
        	 }
        	 if(found==0  && mymsg.IP.compareTo(IP)!=0)//don't add duplicated and only add other IP
        	 {
        	 devicelist.add(mymsg);
        	 Message completeMessage=new Message();
        	 completeMessage.what=1;
        	 completeMessage.obj=mymsg;
        	 updateHandler.sendMessage(completeMessage);
        	 }
        	 
        	 if(mymsg.Msg!=null)
        	 {
        		 Log.d("Udp tutorial","mymsg.Msg"+mymsg.Msg);
        		 Message completeMessage=new Message();
            	 completeMessage.what=2;
            	 completeMessage.obj=mymsg;
            	 updateHandler.sendMessage(completeMessage); 
        	 }
        }
        }
        	 catch (Exception ex) {
                 Log.e(" pong peer receive", ex.toString());
                 break;
             }
        	 
        }
        
       
        
        udpsocket.close();
        }
        catch (Exception ex) {
            Log.e("pong peer create socket", ex.toString());
        }
        
        return text;
        
    }
    

    

    
    public int pingpeer(String IPaddr, String msg)
    {
        
        int server_port = SERVERPORT;
        try {
        	if(localsocket==null)
        	localsocket = new DatagramSocket();
        InetAddress local = InetAddress.getByName(IPaddr);
        String myip=getIpAddress();
        if(myip==null)
        	myip="";
        String sends;
        if(msg==null)
        sends=messageStr+":"+myip+":"+getDeviceName();
        else
        sends=messageStr+":"+myip+":"+getDeviceName()+":"+msg;
        
        int msg_length=sends.length();
        byte[] message = sends.getBytes();
        
        DatagramPacket p = new DatagramPacket(message, msg_length,local,server_port);
        localsocket.send(p);
        }
        catch (Exception ex) {
            Log.e("Socket exception ping", ex.toString());
        }
    	 
    	 
    	return 0;
    }
    
    public String getIP()
    {
    	WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
    	return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
    	        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    public static String getIpAddress() { 
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress().toString();
                        Log.d("IP address",""+ipAddress);
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
        }
        return null; 
}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public String getDeviceName() {
    	   String manufacturer = Build.MANUFACTURER;
    	   String model = Build.MODEL;
    	   if(manufacturer==null)
    		   manufacturer="";
    	   if(model==null)
    		   model="";
    	   
    	   if (model.startsWith(manufacturer)) {
    	      return capitalize(model);
    	   } else {
    	      return capitalize(manufacturer) + " " + model;
    	   }
    	}
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
}
