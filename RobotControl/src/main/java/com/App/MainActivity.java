//*******************************************************************
/**
 @file   MainActivity.java
 @author Thomas Breuer
 @date   08.11.2020
 @brief
 **/

//*******************************************************************
package com.App;

//*******************************************************************
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.App.Bluetooth.BluetoothDeviceListActivity;
import com.App.ORB.ORB;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

//*******************************************************************
public class MainActivity extends AppCompatActivity
		implements CameraBridgeViewBase.CvCameraViewListener2
{
	//---------------------------------------------------------------
	private Menu        menuLocal;
	private ORB         orb;
    private Handler     msgHandler;
	private boolean		run = false;
	private int			rotationdi = 0;
	private int			finx = 0;
	private int			finy = 0;


    private final int ORB_REQUEST_CODE         = 0;
	private final int ORB_DATA_RECEIVED_MSG_ID = 999;

	//---------------------------------------------------------------
	public MainActivity()
	{
		msgHandler = new Handler()
        {
            @Override
            public void handleMessage( Message msg )
            {
                switch( msg.what )
                {
                    case ORB_DATA_RECEIVED_MSG_ID:
                        setMsg();
                        break;
                    default:
                }
                super.handleMessage( msg );
            }
        };
    }

	//---------------------------------------------------------------
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		orb     = new ORB();
		orb.init(this, msgHandler, ORB_DATA_RECEIVED_MSG_ID );
        orb.configMotor(0, 144, 50, 50, 30);
        orb.configMotor(1, 144, 50, 50, 30);
		orb.configSensor(1,4,0,0);

		// Initialize OpenCV
		JavaCameraView mCamView = (JavaCameraView)findViewById( R.id.camera_view);
		mCamView.setCameraIndex(mCamView.CAMERA_ID_ANY);
		mCamView.setCvCameraViewListener(this);
		mCamView.enableView();

		OpenCVLoader.initDebug();

	}

    //-----------------------------------------------------------------
    @Override
    public void onDestroy()
    {
        orb.close();
        super.onDestroy();
    }

    //---------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		if (menuLocal == null)
		{
			menuLocal = menu;
			super.onCreateOptionsMenu(menu);
		}
		return true;
	}

	//---------------------------------------------------------------
	@Override
	public
	boolean onOptionsItemSelected( MenuItem item )
	{
		int id = item.getItemId();
		switch (id)
		{
			case R.id.action_connect:
				BluetoothDeviceListActivity.startBluetoothDeviceSelect(this, ORB_REQUEST_CODE );
				break;
            default:
		}
		return super.onOptionsItemSelected( item );
	}

	//-----------------------------------------------------------------
	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult( requestCode, resultCode, data );
		switch( requestCode )
		{
			case ORB_REQUEST_CODE:
				if(!orb.openBluetooth(BluetoothDeviceListActivity.onActivityResult(resultCode, data)))
				{
					Toast.makeText(getApplicationContext(), "Bluetooth not connected", Toast.LENGTH_LONG).show();
				}
				break;
            default:
		}
	}

	//-----------------------------------------------------------------
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		// Get frame
		Mat mImg = inputFrame.rgba();
		Mat hsv = new Mat();

		//kann weg
		//Core.rotate(mImg, mImg, Core.ROTATE_90_CLOCKWISE);

		Imgproc.cvtColor(mImg,hsv,Imgproc.COLOR_BGR2HSV);
		Core.inRange(hsv,new Scalar(220/2, 150, 128),new Scalar(270/2, 255, 255),hsv);

		int size = (int)(hsv.total()); //4*mImg.width()*mImg.height();
		byte[]  data = new byte[hsv.channels()*size];

		hsv.get(0,0,data);

		long xsum = 0, ysum = 0, anzahl = 0;

		int cols = hsv.cols();
		for(int x = 0; x < cols;x++)
			for(int y = 0; y < size; y+=cols)
				if( ((short)data[y + x] & 0xFF) > 128) {
					xsum += x;
					ysum += y;
					anzahl++;
				}

		int xpos = (int) (xsum / (anzahl+1));
		int ypos = (int) (ysum / ((anzahl+1) * cols));
		double radius = Math.sqrt(anzahl / Math.PI);

		Log.i("Circles", "" + xpos + ", " + ypos + ", " + (int)radius);

		finx = xpos;
		finy = ypos;

		Imgproc.circle(hsv, new Point(xpos, ypos), (int) radius, new Scalar(128, 128, 128), 20);



		if(run == true) {
			if(xpos != 0 || ypos != 0){
				if(xpos > 85){
					orb.setMotor(0, ORB.Mode.SPEED, -500, 0);
					orb.setMotor(1, ORB.Mode.SPEED, +1000, 0);
					if(rotationdi != 1)
					rotationdi = 1;
				}
				else if(xpos < 85){
					orb.setMotor(0, ORB.Mode.SPEED, -1000, 0);
					orb.setMotor(1, ORB.Mode.SPEED, +500, 0);
					if(rotationdi != 0)
						rotationdi = 0;
				}
				else if(xpos == 85){
					orb.setMotor(0, ORB.Mode.SPEED, -1000, 0);
					orb.setMotor(1, ORB.Mode.SPEED, +1000, 0);
				}
			}
			else{
				if(rotationdi == 0){
					orb.setMotor(0, ORB.Mode.SPEED, -1000, 0);
					orb.setMotor(1, ORB.Mode.SPEED, -1000, 0);
				}
				else {
					orb.setMotor(0, ORB.Mode.SPEED, +1000, 0);
					orb.setMotor(1, ORB.Mode.SPEED, +1000, 0);
				}
		}}

        /*
        HoughCircles(mImg, circles, HOUGH_GRADIENT, 1, mImg.cols()/3, 100, 100, mImg.cols()/16, mImg.cols()/2);

        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            Imgproc.circle(mImg, center, 1, new Scalar(128,128,128), 3, 8, 0 );
            // circle outline
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(mImg, center, radius, new Scalar(128,128,128), 3, 8, 0 );
        }

        //cvtColor(hsv, hsv, COLOR_GRAY2RGB);
        //Core.bitwise_and(mImg, hsv, mImg);
        */

		// Insert Text
		Imgproc.putText( mImg,
				"Preview",
				new Point(50,mImg.size().height/2),
				2, 5, new Scalar(255,00,00) );

		// return image to CameraView
		return  hsv;	}

	//-----------------------------------------------------------------
	@Override
	public void onCameraViewStarted(int width, int height)
	{
	}

	//-----------------------------------------------------------------
	@Override
	public void onCameraViewStopped()
	{
	}
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    public void onClick_Start_0( View view )
    {
		run = true;
    }


    //-----------------------------------------------------------------
    public void onClick_Stop( View view )
    {
		run = false;
        orb.setMotor( 0, ORB.Mode.POWER, 0, 0);
        orb.setMotor( 1, ORB.Mode.POWER, 0, 0);
    }

    //-----------------------------------------------------------------
	//-----------------------------------------------------------------
	private void setMsg()
	{
		TextView view;

		view = (TextView) findViewById(R.id.msgVoltage2);
		view.setText("Batt:" + String.format("%.1f V", orb.getVcc()) + String.format("  US:%.1f mm", (0.17*(orb.getSensorValue(1)-30))));

        view = (TextView) findViewById(R.id.msgORB1);
        view.setText("M0:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)0),
                                                             orb.getMotorPos((byte)0),
                                                             orb.getMotorPwr((byte)0)) );

        view = (TextView) findViewById(R.id.msgORB2);
        view.setText("M1:"     + String.format("%6d,%6d,%6d",orb.getMotorSpeed((byte)1),
                                                             orb.getMotorPos((byte)1),
                                                             orb.getMotorPwr((byte)1)) );

		view = (TextView) findViewById(R.id.xylog);
		view.setText("X:"     +  finx + ", " + "Y:" + finy );
	}
}