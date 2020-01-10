package com.example.scantesting;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scantesting.ScanService.MyBinder;
import com.hsm.barcode.DecodeOptions;
import com.hsm.barcode.DecodeWindowing.DecodeWindow;
import com.hsm.barcode.DecodeWindowing.DecodeWindowMode;
import com.hsm.barcode.DecoderConfigValues.LightsMode;
import com.hsm.barcode.DecoderConfigValues.OCRMode;
import com.hsm.barcode.DecoderConfigValues.OCRTemplate;
import com.hsm.barcode.DecoderConfigValues.SymbologyFlags;
import com.hsm.barcode.DecoderConfigValues.SymbologyID;
import com.hsm.barcode.DecoderException;
import com.hsm.barcode.DecoderException.ResultID;
import com.hsm.barcode.ImageAttributes;
import com.hsm.barcode.ImagerProperties;
import com.hsm.barcode.SymbologyConfig;
import com.hsm.barcode.DecoderException;




import androidx.appcompat.app.AppCompatActivity;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Scan extends AppCompatActivity implements ScanListener {
	
	private static final String TAG = "OemScanDemo";
	private static final String OEM_SCAN_DEMO_VERSION = "$LastChangedRevision: 57$";

	private static final int SCAN_KEY = 0;		// Scan key default

	private static final int  AUS_POST = 1;
	private static final int  JAPAN_POST = 3;
	private static final int  KIX = 4;
	private static final int  PLANETCODE = 5;
	private static final int  POSTNET = 6;
	private static final int  ROYAL_MAIL = 7;
	private static final int  UPU_4_STATE = 9;
	private static final int  USPS_4_STATE = 10;
	private static final int  US_POSTALS = 29;
	private static final int  CANADIAN = 30;
	private long clicknowTime = 0;
	private long clicklastTime = 0;

	private static boolean bOkToScan = false;			// Flag to start scanning
	private static boolean bDecoding = false;			// Flag to start decoding
	private static boolean bRunThread = false;			// Flag to run thread
	private static boolean bThreadDone = true;			// Flag to signal thread done
	private TextView m_DecodeResultsView;				// ResultsView object
	private static long decodeTime = 0;					// Time for decode
	private ScanListAdapter adapter;

	private static int g_nDecodeTimeout = 10000; 		// Decode timeout 10 seconds
	private static int g_ScanKey = 0; 						// Scan Key value // FIXME: make -1?
	public static final String PREFS_NAME = "MyPrefsFile"; 	// Preference file to store scan key
	private static boolean bAppRetainsPreferences = false;  // Retain preference settings when changing activities
	
	Button m_ScanButton;
	private Intent service;

	private static boolean g_bContinuousScanEnabled = false;		// Continuous scan option
	private static boolean g_bContinuousScanStarted = false;  // Continuous scan started (TODO: test me)
	
	private static String g_strFileSaveType = "pgm";		// File save type extension

	private static int g_nTotalDecodeTime = 0;	// Used to capture total decode time (for averaging)
	private static int g_nNumberOfDecodes = 0;	// Used to capture total decodes
	
	public int g_nImageWidth = 0;			// Global image width
	public int g_nImageHeight = 0;			// Global image height
	
	public static Scan instance = null;	// For accessing MainActivity from another activity
	
	private static boolean bWaitMultiple = false;	// flag for single or multiple decode
	private int g_nMultiReadResultCount = 0;		// For tracking # of multiread results
	private int g_nMaxMultiReadCount = 0;		// Maximum multiread count
	
	public boolean g_bKeepGoing = true; 		// for trigger callback
	public boolean bTriggerReleased = true;
	protected boolean bind = false;
	protected ScanService scanService;
	private ListView lv;

	/** 
	  * Application create?
	  * 
	  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_CONTEXT_MENU);
        setContentView(R.layout.activity_scan);
        // Create instance
        instance = this;
                
        // Scan key       
        g_ScanKey = SCAN_KEY;
        Log. d(TAG, "g_ScanKey=" + g_ScanKey);
        if(g_ScanKey == -1) // not initialized
        	Log. d(TAG, "Please define SCAN_KEY");
       
        //scanService.m_decResult = new DecodeResult();
		service = new Intent(this, ScanService.class);
		startService(service);
		bindService(service, serviceConnection, BIND_AUTO_CREATE);
		adapter = new ScanListAdapter(this);
		lv = (ListView) findViewById(R.id.scan_lv);
		lv.setAdapter(adapter);

//		ActionBar actionBar = getActionBar();
//        actionBar.show();
    }
    
	ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			bind = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			bind = true;
			//calling mybinder class from scanservice
			MyBinder myBinder = (MyBinder) service;
			scanService = myBinder.getService();//getting getservice method from mybinder
			scanService.setOnScanListener(Scan.this);
			scanService.setScanEnbale(true);
		}
	};


    /** 
	  * Called when key is down
	  * 
	  */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    { 
    	Log. d(TAG, "onKeyDown" + "keyCode=" + keyCode + "(g_ScanKey=" + g_ScanKey + ")");
    	   	
    	if (keyCode == g_ScanKey)
    	{ 
    		if(g_bContinuousScanEnabled)
    		{
    			// Start scanning only if it has not started (or was stopped)
    			if(!g_bContinuousScanStarted)
    			{
    				Toast.makeText(getApplicationContext(), "Press scan key to stop continous scanning.", Toast.LENGTH_LONG).show();
        			
    				g_bContinuousScanStarted = true;
    				processScanButtonPress();
    			}
    			else
    			{
    				g_bContinuousScanStarted = false;
    				StopScanning();
    			}
    		}
    		else
    		{
	    		// Process normally...    		
	    		processScanButtonPress();
    		}
    	}
    	else
    	{
    		return super.onKeyDown(keyCode, event);
    	}
    	
    	return false;
    }
    
    /** 
	  * Called when key is up
	  * 
	  */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    { 
    	
    	Log. d(TAG, "onKeyUp" + "keyCode=" + keyCode + "(g_ScanKey=" + g_ScanKey + ")");
    	
    	if (keyCode == g_ScanKey) 
    	{
    		if(!g_bContinuousScanEnabled)
    			StopScanning();
    	}
    	else
    	{
    		return super.onKeyUp(keyCode, event);
    	}
    	
    	return false;
    }
        
    /** 
     * Called when the user clicks the image
     * 
     * */
    //no need
  /*  public void onClickImage(View view)
    {
    	Button SaveButton = (Button) findViewById(R.id.buttonSave);
    	
    	// Make invisible
    	view.setVisibility(View.GONE);
    	SaveButton.setVisibility(Button.GONE);
    }
    
    *//**
     * Called when the user clicks the save button
     * 
     * *//*
    public void onClickSaveImage(View view)
    {   	
    	//Log. d(TAG, "onClickSaveImage++");

    	saveLastImage();
    	
    	//Log. d(TAG, "onClickSaveImage--");
    }
    
    *//**
     * Called when the user clicks the Scan button 
     * 
     * */
	public void onClickScan(View view) {
		scanService.onClickScan(view);		
	}
     
    /** 
	  * Called when screen tap (TODO: future enhancements)
	  * 
	  */
    //no need
   /* @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();
        
        int pointerCount = event.getPointerCount();
        
        //Log. d(TAG, "number of touches = " + pointerCount);
        if(pointerCount==2) // TODO: detect a swipe to bring up setting menu?
        {
        	switch (eventaction) {
	            case MotionEvent.ACTION_DOWN:
	                // finger touches the screen
	            	Log. d(TAG, "finger touched screen");
	                break;
	
	            case MotionEvent.ACTION_MOVE:
	                // finger moves on the screen
	            	//Log. d(TAG, "finger moves on screen");
	                break;
	
	            case MotionEvent.ACTION_UP:
	                // finger leaves the screen
	            	Log. d(TAG, "finger leaves screen");
	                break;
	            case MotionEvent.ACTION_POINTER_DOWN:
	                // finger leaves the screen
	            	Log. d(TAG, "pointer down action");
	                break;
	        }
        }
        
        *//*switch (eventaction) {
	            case MotionEvent.ACTION_DOWN: 
	                // finger touches the screen
	            	Log. d(TAG, "finger touched screen");
	                break;
	
	            case MotionEvent.ACTION_MOVE:
	                // finger moves on the screen
	            	Log. d(TAG, "finger moves on screen");
	                break;
	
	            case MotionEvent.ACTION_UP:   
	                // finger leaves the screen
	            	Log. d(TAG, "finger leaves screen");
	                break;
	                }
         * *//*

        // tell the system that we handled the event and no further processing is required
        return true; 
    }
    */
    /** 
	  * Called when application gets focus
	  * 
	  */
    //no need
   /* @Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
		Log. d(TAG, "onResume");
	}

    *//**
	  * Called when application loses focus
	  * 
	  *//*
	@Override
	public void onPause() {
	    super.onPause();  // Always call the superclass method first
		Log. d(TAG, "onPause");
	}
*/
	@Override
	protected void onDestroy(){
		super.onDestroy();
//		if(null != f4Receiver){
//			this.unregisterReceiver(f4Receiver);
//		}

		unbindService(serviceConnection);
	}
	//no need
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * 
     * *//*
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
         
        switch (item.getItemId())
        {
	        // Settings
	        case R.id.menu_settings:
	        	// Start configuration settings activity...
	    		Intent i = new Intent(this, ConfigurationSettingsActivity.class);
	            startActivityForResult(i, 1);  
	            break;
	        // Disable All
	        case R.id.disable_all_symbologies:
	        	try 
	        	{
	        		disableAllSymbologies();
	        		*//* This will read HSMDecoderAPI settings (after disable all)
	        		 * and re-configure Configuration Settings SetSymbologySettings 
	        		 * will be called by onResume *//*
	        		SetSymbologyPreferences(false);
	        	} 
	        	catch (DecoderException e) 
	        	{
	        		HandleDecoderException(e);
	        	}
	    		break;
	    	// Enable All
	        case R.id.enable_all_symbologies:
				try 
				{
					enableAllSymbologies();
					*//* This will read HSMDecoderAPI settings (after enable all)
	        		 * and re-configure Configuration Settings SetSymbologySettings 
	        		 * will be called by onResume *//*
					SetSymbologyPreferences(false);
				} 
				catch (DecoderException e) 
				{
					HandleDecoderException(e);
				}
	        	break;
	        // Image Mode
//	        case R.id.mode:
//	        	// Using a new way (image capture activity)
//	        	Log.d(TAG, "Start new ImageCaptureActivity!! :)");
//	    		Intent i1 = new Intent(this, ImageCaptureActivity.class);
//	            startActivityForResult(i1, 1);
//	            Log.d(TAG, "Returned ImageCaptureActivity");
//	        	break;
//	        // Get Last Image
//	        case R.id.get_last_image:
//	        	GetLastImage();
//	        	break;
	        // About
	        case R.id.about:
	        	try
	        	{
	        		ShowAbout();
	        	}
	        	catch (DecoderException e) 
				{
					HandleDecoderException(e);
				}
	        	break;
	        // Reset
	        case R.id.reset_settings:
	        	ResetSettings();
	        	break;
	        // Default
	        default:
	            return super.onOptionsItemSelected(item);
        }
        
        return true;
    }    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case 1:
            //configureUserDecoderSettings();
            break;
 
        }
 
    }
    */
	//no need
	/**
	 *  Displays results when reading mulitple barcodes
	 *  
	 */
    public void DisplayMultireadResults()
    {
    	runOnUiThread(new Runnable() {

            @Override
            public void run() {

		    	m_DecodeResultsView = (TextView) findViewById(R.id.textViewDataResults);
		    	try
				{		    		
		    		if(g_nMultiReadResultCount == 0)
	    				 m_DecodeResultsView.setText(""); // clear the results
		    		
		    		g_nMultiReadResultCount++;
		    		// pull the data manually:
		    		if (scanService.m_Decoder.getBarcodeLength() > 0) {

						 m_DecodeResultsView.append( g_nMultiReadResultCount + ": " + scanService.m_Decoder.getBarcodeData() + "\n" );
						 Log. d(TAG, "Additional Multiread Results");
						 Log. d(TAG, "  AimID:" + scanService.m_Decoder.getBarcodeAimID() );
						 Log. d(TAG, "  AimModifier:" + scanService.m_Decoder.getBarcodeAimModifier() );
						 Log. d(TAG, "  CodeID:" + scanService.m_Decoder.getBarcodeCodeID() );
						 Log. d(TAG, "  Length:" + scanService.m_Decoder.getBarcodeLength() );
		        	} else {
		        		 m_DecodeResultsView.append( g_nMultiReadResultCount + ": " + "!! No Data !!" + "\n" );
		        	}
				} catch(DecoderException e) {
					HandleDecoderException(e);
				}

		     }
    	});
    }
    
    /** 
	  * Displays the decoded results (note: called from thread)
	  * 
	  */
    @Override
    public void DisplayDecodeResults() 
    {
    	runOnUiThread(new Runnable() {

            @Override
            public void run() {

            	m_DecodeResultsView = (TextView) findViewById(R.id.textViewDataResults);
            	
            	if (scanService.m_decResult.length > 0) {
            		try {
            			
						if(scanService.m_decResult.barcodeData.length()!=0)
						{
	            			m_DecodeResultsView.setText("Data : " + scanService.m_decResult.barcodeData +
								"\nLength: " + scanService.m_decResult.length +
								"\nAimID: " + String.format("]%c%c (0x%02x%02x)", scanService.m_decResult.aimId, scanService.m_decResult.aimModifier, scanService.m_decResult.aimId, scanService.m_decResult.aimModifier) +
								"\nCodeID: " + String.format("%c (0x%x)", scanService.m_decResult.codeId, scanService.m_decResult.codeId) +
								"\nTTR (ms): " + scanService.decodeTime + " (" + scanService.m_Decoder.getLastDecodeTime() + ")");
						 
	        				if (adapter != null) {
		            			adapter.addStr(scanService.m_decResult.barcodeData);
	        					adapter.notifyDataSetChanged();
	        					lv.setSelection(adapter.getCount() - 1);
	        				}
	        				
						}
						else
						{
							String m_strDecodedData ="";
							scanService.m_decResult.byteBarcodeData = scanService.m_Decoder.getBarcodeByteData();

							int BytesInLabel =  0;
							do
							{
								m_strDecodedData+= String.format("%02x",scanService.m_decResult.byteBarcodeData[BytesInLabel]&0xff  );
								BytesInLabel++;
							} while(BytesInLabel < scanService.m_decResult.byteBarcodeData.length);


							m_DecodeResultsView.setText("Data: " + m_strDecodedData +
									"\nLength: " + scanService.m_decResult.byteBarcodeData.length +
									"\nAimID: " + String.format("]%c%c (0x%02x%02x)", scanService.m_decResult.aimId, scanService.m_decResult.aimModifier, scanService.m_decResult.aimId, scanService.m_decResult.aimModifier) +
									"\nCodeID: " + String.format("%c (0x%x)", scanService.m_decResult.codeId, scanService.m_decResult.codeId) +
									"\nTTR (ms): " + scanService.decodeTime + " (" + scanService.m_Decoder.getLastDecodeTime() + ")");
	        				
							if (adapter != null) {
		            			adapter.addStr(m_strDecodedData);
	        					adapter.notifyDataSetChanged();
	        					lv.setSelection(adapter.getCount() - 1);
	        				}
							

						}
						
						 Log. d(TAG, "TTR " + scanService.decodeTime + " ms [" + scanService.m_Decoder.getLastDecodeTime() + "ms]");
            		} catch(DecoderException e) {
            			HandleDecoderException(e);
            		}
					 
					 if(g_bContinuousScanEnabled)
					 {
						 g_nNumberOfDecodes++;
						 g_nTotalDecodeTime += scanService.decodeTime;
						 m_DecodeResultsView.append("\n\nAverage TTR: " + g_nTotalDecodeTime/g_nNumberOfDecodes);
					 }
					 
            	} else {
            		m_DecodeResultsView.setText("No Read");
            	}
            }
        });
    }

	/** 
	  * Processes the scan button press
	  * 
	  */
	void processScanButtonPress()
	{
		StartScanning();
	}
	
	/** 
	  * Starts scanning - enables flag to start scanning (decoding)
	  * 
	  */
	void StartScanning()
	{
		scanService.StartScanning();
	}
	
	/** 
	  *  Stops scanning - disables flag to stop scanning / cancel decode (decoding)
	  * 
	  */
	void StopScanning()
	{
		scanService.StopScanning();
	}
	
	/** 
	  * Enables all symbologies
	 * @throws DecoderException 
	  * 
	  */
	// no need
	/*void enableAllSymbologies() throws DecoderException
	{
		scanService.m_Decoder.enableSymbology(SymbologyID.SYM_ALL);
	}
	
	*//**//**
	  * Disables all symbologies
	 * @throws DecoderException 
	  * 
	  *//**//*
	void disableAllSymbologies() throws DecoderException
	{
		scanService.m_Decoder.disableSymbology(SymbologyID.SYM_ALL);
	}
	
	*//**//**
	  * Sets default preferences based on "HSMDecoderAPI" settings
	 * @throws DecoderException 
	  * 
	  *//*
	*/@SuppressWarnings("deprecation")
	void SetSymbologyPreferences(boolean bDefault)// throws DecoderException
	{
		Log. d(TAG, "SetSymbologyPreferences++");
		
		SymbologyConfig symConfig = new SymbologyConfig(0);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Editor editor = prefs.edit();
		for(int i = 0; i < SymbologyID.SYM_ALL; i++)
		{
			symConfig.symID = i; // TODO: move me?
			try 
			{
				if(bDefault)
					scanService.m_Decoder.getSymbologyConfigDefaults(symConfig);
				else
					scanService.m_Decoder.getSymbologyConfig(symConfig);
			} 
			catch (DecoderException e) {
				// Exceptions are OK here since we are only "getting"
				Log.d(TAG, "SymId " + i + " " + e.getMessage());
			}
			
			switch(i)
			{	
				case SymbologyID.SYM_AZTEC:
					// enable, min, max
					editor.putBoolean("sym_aztec_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_aztec_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_aztec_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODABAR:
					// enable, check enable, start/stop transmit, codabar concatenate, min, max
					editor.putBoolean("sym_codabar_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_codabar_check_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_codabar_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_codabar_start_stop_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_START_STOP_XMIT) > 0 ? true : false);
					editor.putBoolean("sym_codabar_concatenate_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CODABAR_CONCATENATE) > 0 ? true : false);
					editor.putString("sym_codabar_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_codabar_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODE11:
					// enable, check enable, min, max
					editor.putBoolean("sym_code11_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_code11_check_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_ENABLE) > 0 ? true : false);
					editor.putString("sym_code11_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_code11_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODE128:
					// enable, min, max
					editor.putBoolean("sym_code128_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_code128_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_code128_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_GS1_128:
					// enable, min, max
					editor.putBoolean("sym_gs1_128_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_gs1_128_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_gs1_128_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODE39:
					// enable, check enable, start/stop transmit, append, fullascii
					editor.putBoolean("sym_code39_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_code39_check_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_code39_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_code39_start_stop_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_START_STOP_XMIT) > 0 ? true : false);
					editor.putBoolean("sym_code39_append_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE_APPEND_MODE) > 0 ? true : false);
					editor.putBoolean("sym_code39_fullascii_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE_FULLASCII) > 0 ? true : false);
					editor.putString("sym_code39_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_code39_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODE49:
				case SymbologyID.SYM_PLESSEY:
		        case SymbologyID.SYM_CODE16K:
		        case SymbologyID.SYM_POSICODE:
				case SymbologyID.SYM_LABEL:
					// not supported
					break;
				case SymbologyID.SYM_GRIDMATRIX:
					editor.putBoolean("sym_gridmatrix_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_gridmatrix_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_gridmatrix_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_CODE93:
					// enable, min, max
					editor.putBoolean("sym_code93_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_code93_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_code93_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_COMPOSITE:
					// enable, composite upc, min, max
					editor.putBoolean("sym_composite_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_composite_upc_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_COMPOSITE_UPC) > 0 ? true : false);
					editor.putString("sym_composite_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_composite_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_DATAMATRIX:
					// enable, min, max
					editor.putBoolean("sym_datamatrix_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_datamatrix_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_datamatrix_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_EAN8:
					// enable, check transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
					editor.putBoolean("sym_ean8_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_ean8_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_ean8_addenda_separator_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR) > 0 ? true : false);
					editor.putBoolean("sym_ean8_2_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_ean8_5_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_ean8_addenda_required_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED) > 0 ? true : false);
					break;
				case SymbologyID.SYM_EAN13:
					// enable, check transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
					editor.putBoolean("sym_ean13_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_ean13_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_ean13_addenda_separator_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR) > 0 ? true : false);
					editor.putBoolean("sym_ean13_2_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_ean13_5_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_ean13_addenda_required_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED) > 0 ? true : false);
					break;
				case SymbologyID.SYM_INT25:
					// enable, check enable, check transmit enable, min, max
					editor.putBoolean("sym_int25_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_int25_check_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_int25_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putString("sym_int25_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_int25_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_MAXICODE:
					// enable, min, max
					editor.putBoolean("sym_maxicode_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_maxicode_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_maxicode_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_MICROPDF:
					// enable, min, max
					editor.putBoolean("sym_micropdf_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_micropdf_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_micropdf_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_PDF417:
					// enable, min, max
					editor.putBoolean("sym_pdf417_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_pdf417_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_pdf417_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_QR:
					// enable, min, max
					editor.putBoolean("sym_qr_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_qr_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_qr_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_HANXIN:
					// enable, min, max
					editor.putBoolean("sym_hanxin_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_hanxin_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_hanxin_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_RSS:
					// rss enable, rsl enable, rse enable, min, max 
					editor.putBoolean("sym_rss_rss_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_RSS_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_rss_rsl_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_RSL_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_rss_rse_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_RSE_ENABLE) > 0 ? true : false);
					editor.putString("sym_rss_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_rss_max", Integer.toString(symConfig.MaxLength));
					break;
				case SymbologyID.SYM_UPCA:
					// enable, check transmit, sys num transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
					editor.putBoolean("sym_upca_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_upca_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_upca_sys_num_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_NUM_SYS_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_upca_addenda_separator_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR) > 0 ? true : false);
					editor.putBoolean("sym_upca_2_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_upca_5_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_upca_addenda_required_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED) > 0 ? true : false);
					editor.putBoolean("sym_translate_upca_to_ean13_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_UPCA_TRANSLATE_TO_EAN13) > 0 ? true : false);
					break;
				case SymbologyID.SYM_UPCE1:
					// upce1 enable
					editor.putBoolean("sym_upce1_upce1_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_UPCE1_ENABLE) > 0 ? true : false);
					break;
				case SymbologyID.SYM_UPCE0:
					// enable, upce expanded, char char transmit, num sys transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
					editor.putBoolean("sym_upce0_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_upce0_upce_expanded_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_EXPANDED_UPCE) > 0 ? true : false);
					editor.putBoolean("sym_upce0_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_upce0_sys_num_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_NUM_SYS_TRANSMIT) > 0 ? true : false);
					editor.putBoolean("sym_upce0_addenda_separator_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR) > 0 ? true : false);
					editor.putBoolean("sym_upce0_2_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_upce0_5_digit_addenda_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA) > 0 ? true : false);
					editor.putBoolean("sym_upce0_addenda_required_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED) > 0 ? true : false);
					break;
				case SymbologyID.SYM_ISBT:
					// enable
					editor.putBoolean("sym_isbt_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					break;
				case SymbologyID.SYM_IATA25:
					// enable, min, max
					editor.putBoolean("sym_iata25_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_iata25_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_iata25_max", Integer.toString(symConfig.MaxLength));
				case SymbologyID.SYM_CODABLOCK:
					// enable, min, max
					editor.putBoolean("sym_codablock_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_codablock_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_codablock_max", Integer.toString(symConfig.MaxLength));
					break;
				
				/* Post Symbology Config */
				case SymbologyID.SYM_POSTNET:
					// check transmit
					editor.putBoolean("sym_postnet_check_transmit_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT) > 0 ? true : false);
		        case SymbologyID.SYM_JAPOST:
		        case SymbologyID.SYM_PLANET:
		        case SymbologyID.SYM_DUTCHPOST:
		        case SymbologyID.SYM_US_POSTALS1:
		        case SymbologyID.SYM_USPS4CB:
		        case SymbologyID.SYM_IDTAG:
		        case SymbologyID.SYM_BPO:
		        case SymbologyID.SYM_CANPOST:
		        case SymbologyID.SYM_AUSPOST:
		        	// enable (config)
		        	editor.putString("sym_post_config", "0"); // i know this is disabled (no_postals) by default - another way?
		        	
		        	if(i == SymbologyID.SYM_AUSPOST)
		        	{
		        		// Default Bar Width & Interpret Mode (both off)
		        		editor.putBoolean("sym_auspost_bar_output_enable", false);
		        		editor.putString("sym_aus_interpret_mode", "0");
		        	}
		        	
					break;
				/* ===================== */	
					
		        case SymbologyID.SYM_MSI:
		        	// enable, check enable, min, max
					editor.putBoolean("sym_msi_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putBoolean("sym_msi_check_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_CHECK_ENABLE) > 0 ? true : false);
					editor.putString("sym_msi_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_msi_max", Integer.toString(symConfig.MaxLength));
		        case SymbologyID.SYM_TLCODE39:
		        	// enable
		        	editor.putBoolean("sym_tlcode39_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
		        	break;
		        case SymbologyID.SYM_MATRIX25:
		        	// enable, min, max
					editor.putBoolean("sym_matrix25_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_matrix25_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_matrix25_max", Integer.toString(symConfig.MaxLength));
		        	break;
		        case SymbologyID.SYM_KOREAPOST:
		        	// enable, min, max
					editor.putBoolean("sym_koreapost_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_koreapost_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_koreapost_max", Integer.toString(symConfig.MaxLength));
		        	break;
		        case SymbologyID.SYM_TRIOPTIC:
		        	// enable
					editor.putBoolean("sym_trioptic_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
		        	break;
		        case SymbologyID.SYM_CODE32:
		        	// enable
					editor.putBoolean("sym_code32_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
		        	break;
		        case SymbologyID.SYM_STRT25:
		        	// enable, min, max
					editor.putBoolean("sym_strt25_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_strt25_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_strt25_max", Integer.toString(symConfig.MaxLength));
		        	break;
		        case SymbologyID.SYM_CHINAPOST:
		        	// enable, min, max
		        	editor.putBoolean("sym_chinapost_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
					editor.putString("sym_chinapost_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_chinapost_max", Integer.toString(symConfig.MaxLength));
		        case SymbologyID.SYM_TELEPEN:
		        	// enable, telepen old style, min, max
		        	editor.putBoolean("sym_telepen_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
		        	editor.putBoolean("sym_telepen_telepen_old_style_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_TELEPEN_OLD_STYLE) > 0 ? true : false);
					editor.putString("sym_telepen_min", Integer.toString(symConfig.MinLength));
					editor.putString("sym_telepen_max", Integer.toString(symConfig.MaxLength));
		        	break;
		        case SymbologyID.SYM_COUPONCODE:
		        	// enable
		        	editor.putBoolean("sym_couponcode_enable", (symConfig.Flags & SymbologyFlags.SYMBOLOGY_ENABLE) > 0 ? true : false);
		        	break;
				default:
					break;
			}
			
			editor.commit();
		}
		
		// OCR Config (disabled, user, "1,3,7,7,7,7,7,7,7,7,0")
		editor.putBoolean("sym_ocr_enable", false);
		editor.putString("sym_ocr_mode_config", Integer.toString(OCRMode.OCR_OFF));
		editor.putString("sym_ocr_template_config", Integer.toString(OCRTemplate.USER));
		editor.putString("sym_ocr_user_template", "1,3,7,7,7,7,7,7,7,7,0");
		editor.commit();
		
		Log. d(TAG, "SetSymbologyPreferences--");
	}
	
	/** 
	  * Sets default OCR preferences based on "HSMDecoderAPI" settings
	 * @throws  
	  * 
	  */
	void SetOcrPreferences(boolean bDefault)
	{
		Log. d(TAG, "SetOcrPreferences++");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Editor editor = prefs.edit();
		
		boolean bOk = true;
		int default_ocr_mode = -1;
		int default_template = -1;
		byte[] default_ocr_user_template = null;
		String default_ocr_user_template_string = null;
		try
		{
			default_ocr_mode = scanService.m_Decoder.getOCRMode();
			default_template = scanService.m_Decoder.getOCRTemplates();		
			default_ocr_user_template = scanService.m_Decoder.getOCRUserTemplate();
			
			for(int i = 0; i < default_ocr_user_template.length; i++)
				Log. d(TAG, "default_ocr_user_template[" + i + "] = " + default_ocr_user_template[i]);
			
			Log. d(TAG, "default_ocr_mode = " + default_ocr_mode);
			Log. d(TAG, "default_template = " + default_template);
				
			// Convert 'default_ocr_user_template_string' to printable string...
			StringBuilder sb = new StringBuilder();
		    for(byte b : default_ocr_user_template)
		    {
		    	sb.append(String.format("%x,", b&0xff));
		    }
		    sb.deleteCharAt(sb.length()-1);
		    Log. d(TAG, "sb = " + sb);
		    default_ocr_user_template_string = sb.toString();
		   
		    Log. d(TAG, "default_ocr_user_template_string = " + default_ocr_user_template_string);
		}
		catch(DecoderException e)
		{
			bOk = false;
			HandleDecoderException(e);
		}
		//catch(UnsupportedEncodingException e)
		//{
		//	e.printStackTrace();
		//}
		
		if(bOk)
		{
			editor.putBoolean("sym_ocr_enable", false);
			editor.putString("sym_ocr_mode_config", Integer.toString(default_ocr_mode));
			editor.putString("sym_ocr_template_config", Integer.toString(default_template));
			editor.putString("sym_ocr_user_template", default_ocr_user_template_string);
		}
		else
		{
			Log. d(TAG, "!! FAILED TO GET OCR SETTINGS !!");
			
			// OCR Config (disabled, user, "1,3,7,7,7,7,7,7,7,7,0")
			editor.putBoolean("sym_ocr_enable", false);
			editor.putString("sym_ocr_mode_config", Integer.toString(OCRMode.OCR_OFF));
			editor.putString("sym_ocr_template_config", Integer.toString(OCRTemplate.USER));
			editor.putString("sym_ocr_user_template", "1,3,7,7,7,7,7,7,7,7,0");
		}
		editor.commit();
		
		//Log. d(TAG, "SetOcrPreferences--");
	}
	
	/** 
	  * Sets default Decoder preferences based on "HSMDecoderAPI" settings
	  * 
	  */
	void SetDecodingPreferences(boolean bDefault)
	{
		Log. d(TAG, "SetDecodingPreferences++");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Editor editor = prefs.edit();
		
		/* Decode Timeout */
		editor.putString("decoding_pref_decode_timeout", Integer.toString(10));
		
		int nMode = DecodeWindowMode.DECODE_WINDOW_MODE_DISABLED;
		int nDebugWindow = 0;
		DecodeWindow myWindow = new DecodeWindow();
		boolean bOk = true;
		try
		{
			Log. d(TAG, "getDecodeWindow");
			scanService.m_Decoder.getDecodeWindow(myWindow);
			Log. d(TAG, "getDecodeWindowMode");
			nMode = scanService.m_Decoder.getDecodeWindowMode();
			Log. d(TAG, "getShowDecodeWindow");
			nDebugWindow = scanService.m_Decoder.getShowDecodeWindow();
			//bOk = false;
		}
		catch(DecoderException e)
		{
			HandleDecoderException(e);
			bOk = false;
		}
		
		if(!bOk) // safety
		{
			// Window
			editor.putString("decode_window_upper_left_x", Integer.toString(386)); 	// #define IT6000_DEFAULT_DECODE_WINDOW_ULX       386
			editor.putString("decode_window_upper_left_y", Integer.toString(290));	// #define IT6000_DEFAULT_DECODE_WINDOW_ULY       290
			editor.putString("decode_window_lower_right_x", Integer.toString(446));	// #define IT6000_DEFAULT_DECODE_WINDOW_LRX       446
			editor.putString("decode_window_lower_right_y", Integer.toString(350)); // #define IT6000_DEFAULT_DECODE_WINDOW_LRY       350
			// App Enable
			editor.putBoolean("decode_centering_enable", false);
			// Mode
			editor.putString("decode_centering_mode", Integer.toString(0)); // default is actually off
			// Debug Window
			editor.putBoolean("decode_debug_window_enable", false);
		}
		else
		{
			// Window
			editor.putString("decode_window_upper_left_x", Integer.toString(myWindow.UpperLeftX));
			editor.putString("decode_window_upper_left_y", Integer.toString(myWindow.UpperLeftY));
			editor.putString("decode_window_lower_right_x", Integer.toString(myWindow.LowerRightX));
			editor.putString("decode_window_lower_right_y", Integer.toString(myWindow.LowerRightY));
			// App Enable
			editor.putBoolean("decode_centering_enable", (nMode > 0) ? true : false); // if nMode > 0, centering is enabled
			// Mode
			editor.putString("decode_centering_mode", Integer.toString(nMode)); // default is actually off, but 2 is typical if enabled
			// Debug Window
			editor.putBoolean("decode_debug_window_enable", (nDebugWindow > 0) ? true : false); // only support off and white
		}
		
		/* Decode Search Limit */
		editor.putString("decode_search_limit", Integer.toString(800));
		
		/* WaitForDecode */
		editor.putString("decode_wait_for_decode_config", Integer.toString(0)); // wait for single by default
		
		/* Multiread Count */
		editor.putString("decode_multiread_count", Integer.toString(2));
		

		editor.commit();
		
		//Log. d(TAG, "SetDefaultDecoderPreferences--");
	}
	
	/** 
	  * Sets default Decoder preferences based on "HSMDecoderAPI" settings
	  * 
	  */
	void SetScanningPreferences(boolean bDefault)
	{
		Log. d(TAG, "SetScanningPreferences++");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Editor editor = prefs.edit();
		
		/* Lights Mode */
		editor.putString("lightsConfig", Integer.toString(3));
		editor.commit();
		
		/* Continuous Scan */
		editor.putBoolean("continous_scan_enable", false);
		editor.commit();
		
		//Log. d(TAG, "SetDefaultScanningPreferences--");
	}
	
	/** 
	  * Sets Application preferences based on settings
	  * 
	  */
	void SetApplicationPreferences(boolean bDefault)
	{
		Log. d(TAG, "SetApplicationPreferences++");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		prefs.edit();
		
		/* Scan Button */
		// ignored ... this is retained
		
		/* File Save Type */
		// ignored ... this is retained
	}

	
	/** 
	  * Sets the symbology settings based on user preferences
	 * @throws DecoderException 
	  * 
	  */
	@SuppressWarnings("deprecation")
	void SetSymbologySettings() //throws DecoderException
	{
		Log. d(TAG, "SetSymbologySettings++");
		
		int flags = 0;											// flags config
		int min = 0;											// minimum length config
		int max = 0;											// maximum length config
		int postal_config = 0;									// postal config
		String temp;											// temp string for converting string to int
		SymbologyConfig symConfig = new SymbologyConfig(0);		// symbology config
		int min_default, max_default;
		String strMinDefault = null;
		String strMaxDefault = null;
		boolean bNotSupported = false;
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		for(int i = 0; i < SymbologyID.SYM_ALL; i++)
		{
			
			symConfig.symID = i;								// symID
			//if( i != SymbologyID.SYM_OCR &&
			//	i != SymbologyID.SYM_POSTALS )
			//scanService.m_Decoder.getSymbologyConfig(symConfig,false); // gets the current symConfig
			flags = 0;											// reset the flags
					
			// Set appropriate sym config mask...
			switch(i)
			{
				// Flag & Range:
				case SymbologyID.SYM_AZTEC:
		        case SymbologyID.SYM_CODABAR:
		        case SymbologyID.SYM_CODE11:
		        case SymbologyID.SYM_CODE128:
				case SymbologyID.SYM_GS1_128:
		        case SymbologyID.SYM_CODE39:
		        //case SymbologyID.SYM_CODE49: 		// not supported
		        case SymbologyID.SYM_CODE93:
		        case SymbologyID.SYM_COMPOSITE:
		        case SymbologyID.SYM_DATAMATRIX:
		        case SymbologyID.SYM_INT25:
		        case SymbologyID.SYM_MAXICODE:
		        case SymbologyID.SYM_MICROPDF:
		        case SymbologyID.SYM_PDF417:
		        case SymbologyID.SYM_QR:
		        case SymbologyID.SYM_RSS:
		        case SymbologyID.SYM_IATA25:
		        case SymbologyID.SYM_CODABLOCK:
		        case SymbologyID.SYM_MSI:
		        case SymbologyID.SYM_MATRIX25:
		        case SymbologyID.SYM_KOREAPOST:
		        case SymbologyID.SYM_STRT25:
		        //case SymbologyID.SYM_PLESSEY: 	// not supported
		        case SymbologyID.SYM_CHINAPOST:
		        case SymbologyID.SYM_TELEPEN:
		        //case SymbologyID.SYM_CODE16K: 	// not supported
		        //case SymbologyID.SYM_POSICODE:	// not supported
				case SymbologyID.SYM_HANXIN:
				//case SymbologyID.SYM_GRIDMATRIX:	// not supported
					try
					{
						scanService.m_Decoder.getSymbologyConfig(symConfig); // gets the current symConfig
						min_default = scanService.m_Decoder.getSymbologyMinRange(i); strMinDefault = Integer.toString(min_default);
						max_default = scanService.m_Decoder.getSymbologyMaxRange(i); strMaxDefault = Integer.toString(max_default);
					}
					catch(DecoderException e)
					{
						HandleDecoderException(e);
					}
					symConfig.Mask = SymbologyFlags.SYM_MASK_FLAGS | SymbologyFlags.SYM_MASK_MIN_LEN | SymbologyFlags.SYM_MASK_MAX_LEN;
					break;
				// Flags Only:
				case SymbologyID.SYM_EAN8:
		        case SymbologyID.SYM_EAN13:
		        case SymbologyID.SYM_POSTNET:
		        case SymbologyID.SYM_UPCA:
		        case SymbologyID.SYM_UPCE0:
		        case SymbologyID.SYM_UPCE1:
		        case SymbologyID.SYM_ISBT:
		        case SymbologyID.SYM_BPO:
		        case SymbologyID.SYM_CANPOST:
		        case SymbologyID.SYM_AUSPOST:
		        case SymbologyID.SYM_JAPOST:
		        case SymbologyID.SYM_PLANET:
		        case SymbologyID.SYM_DUTCHPOST:
		        case SymbologyID.SYM_TLCODE39:
		        case SymbologyID.SYM_TRIOPTIC:
		        case SymbologyID.SYM_CODE32:
		        case SymbologyID.SYM_COUPONCODE:
				case SymbologyID.SYM_USPS4CB:
				case SymbologyID.SYM_IDTAG:
				//case SymbologyID.SYM_LABEL:		// not supported
				case SymbologyID.SYM_US_POSTALS1:
					try
					{
						scanService.m_Decoder.getSymbologyConfig(symConfig); // gets the current symConfig
					}
					catch(DecoderException e)
					{
						HandleDecoderException(e);
					}
					symConfig.Mask = SymbologyFlags.SYM_MASK_FLAGS;
					break;
				// default:
				default:
					// invalid / not supported
					bNotSupported = true;
					break;
			}
			
			// Set symbology config...
			switch(i)
			{
				case SymbologyID.SYM_AZTEC:
					// enable
					flags |= sharedPrefs.getBoolean("sym_aztec_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_aztec_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_aztec_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODABAR:
		        	// enable, check char, start/stop transmit, codabar concatenate
		        	flags |= sharedPrefs.getBoolean("sym_codabar_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_codabar_check_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_codabar_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_codabar_start_stop_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_START_STOP_XMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_codabar_concatenate_enable", false) ? SymbologyFlags.SYMBOLOGY_CODABAR_CONCATENATE : 0;	
		        	// min, max
		        	temp = sharedPrefs.getString("sym_codabar_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_codabar_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODE11:
					// enable, check char
		        	flags |= sharedPrefs.getBoolean("sym_code11_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code11_check_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_code11_min", strMinDefault); min = Integer.parseInt(temp);
		        	temp = sharedPrefs.getString("sym_code11_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODE128:
		        	// enable
					flags |= sharedPrefs.getBoolean("sym_code128_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_code128_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_code128_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
				case SymbologyID.SYM_GS1_128:
		        	// enable
					flags |= sharedPrefs.getBoolean("sym_gs1_128_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_gs1_128_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_gs1_128_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODE39:
		        	// enable, check char, start/stop transmit, append, full ascii
		        	flags |= sharedPrefs.getBoolean("sym_code39_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code39_check_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code39_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code39_start_stop_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_START_STOP_XMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code39_append_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE_APPEND_MODE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_code39_fullascii_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE_FULLASCII : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_code39_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_code39_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODE49:
				case SymbologyID.SYM_PLESSEY:
		        case SymbologyID.SYM_CODE16K:
		        case SymbologyID.SYM_POSICODE:
				case SymbologyID.SYM_LABEL:
					// not supported
					break;			
				case SymbologyID.SYM_GRIDMATRIX:
					flags |= sharedPrefs.getBoolean("sym_gridmatrix_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_gridmatrix_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_gridmatrix_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODE93:
		        	// enable
					flags |= sharedPrefs.getBoolean("sym_code93_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_code93_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_code93_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_COMPOSITE:
		        	// enable, composit upc
		        	flags |= sharedPrefs.getBoolean("sym_composite_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_composite_upc_enable", false) ? SymbologyFlags.SYMBOLOGY_COMPOSITE_UPC : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_composite_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_composite_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_DATAMATRIX:
					// enable
					flags |= sharedPrefs.getBoolean("sym_datamatrix_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					// min, max
					temp = sharedPrefs.getString("sym_datamatrix_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_datamatrix_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
				case SymbologyID.SYM_EAN8:
					// enable, check char transmit, addenda separator, 2 digit addena, 5 digit addena, addena required
					flags |= sharedPrefs.getBoolean("sym_ean8_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					flags |= sharedPrefs.getBoolean("sym_ean8_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
					flags |= sharedPrefs.getBoolean("sym_ean8_addenda_separator_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR : 0;
					flags |= sharedPrefs.getBoolean("sym_ean8_2_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA: 0;
					flags |= sharedPrefs.getBoolean("sym_ean8_5_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA : 0;
					flags |= sharedPrefs.getBoolean("sym_ean8_addenda_required_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED : 0;
					break;
		        case SymbologyID.SYM_EAN13:
		        	// enable, check char transmit, addenda separator, 2 digit addena, 5 digit addena, addena required
					flags |= sharedPrefs.getBoolean("sym_ean13_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					flags |= sharedPrefs.getBoolean("sym_ean13_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
					flags |= sharedPrefs.getBoolean("sym_ean13_addenda_separator_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR : 0;
					flags |= sharedPrefs.getBoolean("sym_ean13_2_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA: 0;
					flags |= sharedPrefs.getBoolean("sym_ean13_5_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA : 0;
					flags |= sharedPrefs.getBoolean("sym_ean13_addenda_required_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED : 0;
					break;
		        case SymbologyID.SYM_INT25:
					// enable, check enable, check transmit
		        	flags |= sharedPrefs.getBoolean("sym_int25_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_int25_check_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_ENABLE : 0;
					flags |= sharedPrefs.getBoolean("sym_int25_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
					// min, max
					temp = sharedPrefs.getString("sym_int25_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_int25_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_MAXICODE:
					// enable
		        	flags |= sharedPrefs.getBoolean("sym_maxicode_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_maxicode_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_maxicode_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_MICROPDF:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_micropdf_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_micropdf_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_micropdf_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_PDF417:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_pdf417_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_pdf417_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_pdf417_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_QR:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_qr_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_qr_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_qr_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_HANXIN:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_hanxin_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_hanxin_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_hanxin_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_RSS:
		        	// rss enable, rsl enable, rse enable
		        	flags |= sharedPrefs.getBoolean("sym_rss_rss_enable", false) ? SymbologyFlags.SYMBOLOGY_RSS_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_rss_rsl_enable", false) ? SymbologyFlags.SYMBOLOGY_RSL_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_rss_rse_enable", false) ? SymbologyFlags.SYMBOLOGY_RSE_ENABLE : 0;
		        	// min, max
					temp = sharedPrefs.getString("sym_rss_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_rss_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_UPCA:
					// enable, check transmit, sys num transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
		        	flags |= sharedPrefs.getBoolean("sym_upca_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_sys_num_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_NUM_SYS_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_addenda_separator_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_2_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_5_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upca_addenda_required_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED : 0;
		        	flags |= sharedPrefs.getBoolean("sym_translate_upca_to_ean13_enable", false) ? SymbologyFlags.SYMBOLOGY_UPCA_TRANSLATE_TO_EAN13 : 0;		        
		        	break;
		        case SymbologyID.SYM_UPCE1:
		        	// upce1 enable
		        	flags |= sharedPrefs.getBoolean("sym_upce1_upce1_enable", false) ? SymbologyFlags.SYMBOLOGY_UPCE1_ENABLE : 0;
					break;
		        case SymbologyID.SYM_UPCE0:
		        	// enable, upce expanded, char char transmit, num sys transmit, addenda separator, 2 digit addenda, 5 digit addenda, addenda required
		        	flags |= sharedPrefs.getBoolean("sym_upce0_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_upce_expanded_enable", false) ? SymbologyFlags.SYMBOLOGY_EXPANDED_UPCE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_sys_num_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_NUM_SYS_TRANSMIT : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_addenda_separator_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_SEPARATOR : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_2_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_2_DIGIT_ADDENDA : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_5_digit_addenda_enable", false) ? SymbologyFlags.SYMBOLOGY_5_DIGIT_ADDENDA : 0;
		        	flags |= sharedPrefs.getBoolean("sym_upce0_addenda_required_enable", false) ? SymbologyFlags.SYMBOLOGY_ADDENDA_REQUIRED : 0;
					break;
		        case SymbologyID.SYM_ISBT:
					// enable
		        	flags |= sharedPrefs.getBoolean("sym_isbt_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					break;
		        case SymbologyID.SYM_IATA25:
					// enable
		        	flags |= sharedPrefs.getBoolean("sym_iata25_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_iata25_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_iata25_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CODABLOCK:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_codablock_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_codablock_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_codablock_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
				
				/* Post Symbology Config */					
				case SymbologyID.SYM_POSTNET:
					Log. d(TAG, "Configure SYM_POSTNET");
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == POSTNET) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	Log. d(TAG, "SYM_POSTNET postal_config = " + postal_config);
		        	Log. d(TAG, "SYM_POSTNET flags = " + flags);
		        	// check transmit
		        	flags |= sharedPrefs.getBoolean("sym_postnet_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	break;
				case SymbologyID.SYM_JAPOST:
					// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == JAPAN_POST) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_PLANET:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == PLANETCODE) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_DUTCHPOST:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == KIX) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_US_POSTALS1:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == US_POSTALS) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_USPS4CB:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == USPS_4_STATE) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_IDTAG:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == UPU_4_STATE) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_BPO:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == ROYAL_MAIL) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_CANPOST:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == CANADIAN) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	break;
		        case SymbologyID.SYM_AUSPOST:
		        	// enable
		        	temp = sharedPrefs.getString("sym_post_config", "0"); postal_config = Integer.parseInt(temp);
		        	flags |= (postal_config == AUS_POST) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;	        	
		        	// Bar output
		        	sharedPrefs.getBoolean("sym_auspost_bar_output_enable", false);
		        	// Interpret Mode
		        	temp = sharedPrefs.getString("sym_aus_interpret_mode","0"); postal_config = Integer.parseInt(temp);
		        	switch(postal_config)
		        	{
		        		// Numeric N Table:
		        		case 1:
		        			flags |= SymbologyFlags.SYMBOLOGY_AUS_POST_NUMERIC_N_TABLE;
		        			break;
		        		// Alphanumeric C Table:
		        		case 2:
		        			flags |= SymbologyFlags.SYMBOLOGY_AUS_POST_ALPHANUMERIC_C_TABLE;
		        			break;
		        		// Combination N & C Tables:
		        		case 3:
		        			flags |= SymbologyFlags.SYMBOLOGY_AUS_POST_COMBINATION_N_AND_C_TABLES;
		        			break;
		        		default:
		        			break;
		        	}
		        	break;
				/* ===================== */	
					
		        case SymbologyID.SYM_MSI:
					// enable, check transmit
		        	flags |= sharedPrefs.getBoolean("sym_msi_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_msi_check_transmit_enable", false) ? SymbologyFlags.SYMBOLOGY_CHECK_TRANSMIT : 0;
		        	Log.d(TAG, "sym msi flags = " + flags);
		        	// min, max
		        	temp = sharedPrefs.getString("sym_msi_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_msi_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_TLCODE39:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_tlcode39_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					break;
		        case SymbologyID.SYM_MATRIX25:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_matrix25_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_matrix25_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_matrix25_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_KOREAPOST:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_koreapost_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_koreapost_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_koreapost_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_TRIOPTIC:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_trioptic_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					break;
		        case SymbologyID.SYM_CODE32:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_code32_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					break;
		        case SymbologyID.SYM_STRT25:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_strt25_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_strt25_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_strt25_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_CHINAPOST:
		        	// enable
		        	flags |= sharedPrefs.getBoolean("sym_chinapost_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_chinapost_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_chinapost_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_TELEPEN:
					// enable, telepen old style
		        	flags |= sharedPrefs.getBoolean("sym_telepen_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
		        	flags |= sharedPrefs.getBoolean("sym_telepen_telepen_old_style_enable", false) ? SymbologyFlags.SYMBOLOGY_TELEPEN_OLD_STYLE : 0;
		        	// min, max
		        	temp = sharedPrefs.getString("sym_telepen_min", strMinDefault); min = Integer.parseInt(temp);
					temp = sharedPrefs.getString("sym_telepen_max", strMaxDefault); max = Integer.parseInt(temp);
					break;
		        case SymbologyID.SYM_COUPONCODE:
					// enable
		        	flags |= sharedPrefs.getBoolean("sym_couponcode_enable", false) ? SymbologyFlags.SYMBOLOGY_ENABLE : 0;
					break;
				
				default:
					symConfig.Mask = 0; // will not setSymbologyConfig
					break;
			}

			if(bNotSupported)
			{
				bNotSupported = false; // // do nothing, but reset flag
			}
			if(symConfig.Mask == (SymbologyFlags.SYM_MASK_FLAGS | SymbologyFlags.SYM_MASK_MIN_LEN | SymbologyFlags.SYM_MASK_MAX_LEN) ) // Flags & Range
			{
				symConfig.Flags = flags;
				symConfig.MinLength = min;
				symConfig.MaxLength = max;
				try 
				{
					scanService.m_Decoder.setSymbologyConfig(symConfig);
				} 
				catch (DecoderException e) 
				{
					Log. d(TAG, "1 EXCEPTION SYMID = " + i);
					HandleDecoderException(e);
				}
			}
			else if(symConfig.Mask == (SymbologyFlags.SYM_MASK_FLAGS)) // Flag Only
			{
				symConfig.Flags = flags;
				try
				{
					scanService.m_Decoder.setSymbologyConfig(symConfig);	
				}
				catch (DecoderException e) 
				{
					Log. d(TAG, "2 EXCEPTION SYMID = " + i);
					HandleDecoderException(e);
				}
			}
			else
			{
				// invalid
			}
		}
		
		Log. d(TAG, "SetSymbologySettings--");
	}
	
	/** 
	  * Sets the OCR settings based on user preferences
	 * @throws DecoderException 
	  * 
	  */
	void SetOcrSettings() throws DecoderException
	{	
		Log. d(TAG, "SetOcrSettings++");
		int ocr_mode = 0;
		int ocr_template = 0;
		
		String temp;
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// mode (enable)
		temp = sharedPrefs.getString("sym_ocr_mode_config", "0"); ocr_mode = Integer.parseInt(temp);
		// ocr template
		temp = sharedPrefs.getString("sym_ocr_template_config", "0"); ocr_template = Integer.parseInt(temp);
		// user defined template
		temp = sharedPrefs.getString("sym_ocr_user_template", "1,3,7,7,7,7,7,7,7,7,0");
		String[] separated = temp.split(",");
		byte[] ocr_user_defined_template = new byte[separated.length];
		
		int i = 0;
		do
		{
			ocr_user_defined_template[i]= Byte.parseByte(separated[i]);
			i++;
		}
		while(i != separated.length);
		
		Log. d(TAG, "ocr mode = " + ocr_mode);
		Log. d(TAG, "ocr template config = " + ocr_template);
		Log. d(TAG, "ocr user template string = " + temp);
		for( i = 0; i < ocr_user_defined_template.length; i++)
			Log. d(TAG, "ocr user template bytes[" + i + "] = " + ocr_user_defined_template[i]);
		
		scanService.m_Decoder.setOCRMode(ocr_mode);
		scanService.m_Decoder.setOCRTemplates(ocr_template);
		scanService.m_Decoder.setOCRUserTemplate(ocr_user_defined_template);
		
		Log. d(TAG, "SetOcrSettings--");
	}
	
	/** 
	  * Sets the Decoder settings based on user preferences
	 * @throws DecoderException 
	  * 
	  */
	void SetDecodingSettings() throws DecoderException
	{
		Log. d(TAG, "SetDecodingSettings++");
		
		String temp;
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Decode Timeout
		temp = sharedPrefs.getString("decoding_pref_decode_timeout", Integer.toString(10));
		g_nDecodeTimeout = Integer.parseInt(temp) * 1000; // sec to ms
		
		// Windowing
		DecodeWindow myWindow = new DecodeWindow();
		boolean enable_windowing = false;
		boolean bDebugWindowMode = false;
		int nMode = DecodeWindowMode.DECODE_WINDOW_MODE_DISABLED;
		
		Log. d(TAG, "nMode = " + nMode);
		
		enable_windowing = sharedPrefs.getBoolean("decode_centering_enable", false);
		
		temp = sharedPrefs.getString("decode_centering_mode", "2"); nMode = Integer.parseInt(temp);
		temp = sharedPrefs.getString("decode_window_upper_left_x", "0"); myWindow.UpperLeftX = Integer.parseInt(temp);
		temp = sharedPrefs.getString("decode_window_upper_left_y", "0"); myWindow.UpperLeftY = Integer.parseInt(temp);
		temp = sharedPrefs.getString("decode_window_lower_right_x", "0"); myWindow.LowerRightX = Integer.parseInt(temp);
		temp = sharedPrefs.getString("decode_window_lower_right_y", "0"); myWindow.LowerRightY = Integer.parseInt(temp);
		bDebugWindowMode = sharedPrefs.getBoolean("decode_debug_window_enable", false);
		
		if(enable_windowing)
		{
			Log. d(TAG, "Centering is enabled");
			
			Log. d(TAG, "enable the mode... nMode = " + nMode);
			// enable the mode
			scanService.m_Decoder.setDecodeWindowMode(nMode);
						
			Log. d(TAG, "set the window... myWindow.UpperLeftX = " + myWindow.UpperLeftX);
			// set the window
			scanService.m_Decoder.setDecodeWindow(myWindow);		
			
			Log. d(TAG, "set the debug window");
			// set the debug window
			//if(bDebugWindowMode) nMode = DecodeWindowShowWindow.DECODE_WINDOW_SHOW_WINDOW_WHITE; // white
			//	scanService.m_Decoder.setShowDecodeWindow(nMode);
			
		}
		else
		{
			// disable windowing
			scanService.m_Decoder.setDecodeWindowMode(nMode);	
		}
		
		
		// Decode Search Limit
		temp = sharedPrefs.getString("decode_time_limit", "800"); scanService.m_Decoder.setDecodeAttemptLimit(Integer.parseInt(temp));
		
		// WaitForDecode timeout only
		temp = sharedPrefs.getString("decode_wait_for_decode_config", "0"); bWaitMultiple = (Integer.parseInt(temp) == 1) ? true : false;
		
		// Multiread count
		temp = sharedPrefs.getString("decode_multiread_count", "1"); g_nMaxMultiReadCount = Integer.parseInt(temp);
		DecodeOptions decOpt = new DecodeOptions();
		decOpt.DecAttemptLimit = -1; // ignore
		decOpt.VideoReverse = -1; // ignore
		decOpt.MultiReadCount = g_nMaxMultiReadCount;
		scanService.m_Decoder.setDecodeOptions(decOpt);
		
		
		//Log. d(TAG, "SetDecodingSettings--");
	}
	
	/** 
	  * Sets the Scanning settings based on user preferences
	 * @throws DecoderException 
	 * @throws NumberFormatException
	  * 
	  */
	void SetScanningSettings() throws NumberFormatException, DecoderException
	{
		Log. d(TAG, "SetScanningSettings++");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int myLightsMode = LightsMode.ILLUM_AIM_ON;
		
		Log. d(TAG, "myLightsMode = " + myLightsMode);

		/* Lights Mode */
		String lightsModeString = prefs.getString("lightsConfig", "3");
		myLightsMode = Integer.parseInt(lightsModeString);
		scanService.m_Decoder.setLightsMode(myLightsMode);
		
		g_bContinuousScanEnabled = prefs.getBoolean("continous_scan_enable", false);
		
		Log. d(TAG, "SetScanningSettings--");
	}
	
	/** 
	  * Sets the Application settings based on user preferences
	  * 
	  */
	void SetApplicationSettings()
	{
		Log. d(TAG, "SetApplicationSettings++");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		/* Scan Button */
		boolean bEnableScanButton = prefs.getBoolean("app_pref_scan_button", false);
		Button m_ScanButton = (Button) findViewById(R.id.buttonScan);
		if(bEnableScanButton)
		{
			// Make scan button visible
			m_ScanButton.setVisibility(View.VISIBLE);
		}
		else
		{
			// Make scan button gone
			//m_ScanButton.setVisibility(View.GONE);
		}
		
		/* File Save Type */
		g_strFileSaveType = prefs.getString("app_pref_file_save_type", "pgm");
		Log. d(TAG, "filesavetype = " + g_strFileSaveType);
		
		Log. d(TAG, "SetApplicationSettings--");
	}
	
	/** 
	  * Displays the passed in Bitmap and makes save image button visible
	  * 
	  */

	//no need
	/*void DisplayImage(Bitmap bmp) // TODO: consider changing name (since it displays save button too)
	{
		ImageView imageView = (ImageView) findViewById(R.id.imageView1);
		Button SaveButton = (Button) findViewById(R.id.buttonSave);

		imageView.setImageBitmap(bmp);				// Set the image
		imageView.setVisibility(View.VISIBLE);	// Makes it visible
		imageView.bringToFront();					// Brings to the front
		SaveButton.setVisibility(Button.VISIBLE);	// Make button visible
		
		// Give some notification to the user image can be clicked to close
		Toast.makeText(getApplicationContext(), "Click image to close it.", Toast.LENGTH_LONG).show();
	}
	*/
	/** 
	  * Dummy test used to use inheritance from other activities
	  * 
	  */
	//no need
	/*public void NewTest()
	{
		
		Toast.makeText(getApplicationContext(), "NEW TEST", Toast.LENGTH_LONG).show();
	}
	*/
	/** 
	  * Handles the DecoderException by displaying error in log and printing the stack trace
	  * 
	  */
	public void HandleDecoderException(final DecoderException e)
	{
		runOnUiThread(new Runnable() {

            @Override
            public void run() {
				Log. d(TAG, "HandleDecoderException++");
				
				if(true)
				{
				//	Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				else // more user friendly?
				{
					switch(e.getErrorCode())
					{
					
						case ResultID.RESULT_ERR_NOTCONNECTED:
							Toast.makeText(getApplicationContext(), "Error: Engine not connected to perform this operation (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
							break;
						case ResultID.RESULT_ERR_NOIMAGE:
							Toast.makeText(getApplicationContext(), "Error: No image (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
							break;
						default:
							Toast.makeText(getApplicationContext(), "Unknown Error (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
					}
				}
				
				Log. d(TAG, "HandleDecoderException--");
        }
      });
	}
	
	/** 
	  * Gets the last decoded image and calls a function to display it
	  * 
	  */
	//no need
	/*void GetLastImage()
	{
		//Log. d(TAG, "GetLastImage++");

		Bitmap bmp = Bitmap.createBitmap(scanService.g_nImageWidth, scanService.g_nImageHeight, Bitmap.Config.RGB_565);
		ImageAttributes attr = new ImageAttributes();	
		byte[] buffer = new byte[scanService.g_nImageWidth * scanService.g_nImageHeight]; // it 8-bit RAW
		
		// Get last image
		try 
		{
			buffer = scanService.m_Decoder.getLastImage(attr);
		} 
		catch (DecoderException e) {
			HandleDecoderException(e);
			return;
		}

		// Ensure it's valid
		if(buffer.equals(null))
		{
			Log. d(TAG, "no image");
			return;
		}
		
		// Convert from 8-bit RAW to 16-bit color
		int width = scanService.g_nImageWidth;
		int height = scanService.g_nImageHeight;
		int[] array = new int[width*height*2]; // 2 bytes per pixel (RGB_565)
		
		for(int h = 0; h < height; h++)
		{
			for(int w = 0; w < width; w++)
			{
				array[width*h + w] = buffer[width*h + w] * 0x00010101;		
			}
		}
		
		// Set the pixels
		bmp.setPixels(array, 0, width, 0, 0, width, height);

		// Display image (TODO: make this a method like ShowImage)
		DisplayImage(bmp);

		//Log. d(TAG, "GetLastImage--");
	}

	*//**
	  * Shows the About screen with Engine Details and Version Info
	 * @throws DecoderException 
	  * 
	  */
	/*void ShowAbout() throws DecoderException
	{
		Log. d(TAG, "ShowAbout++");
		
		// Shorten scan demo version
		String scandemo_version = "r" + OEM_SCAN_DEMO_VERSION.replace("$LastChangedRevision:", "").replace("$", "").trim();
		
		Log. d(TAG, "Get decoder revs...");
		// Decoder Rev's are very long, so we'll shorten them (to year.month.rev):
		String strDecoderRevFullString = scanService.m_Decoder.getDecoderRevision();
		String strDclRevFullString = scanService.m_Decoder.getControlLogicRevision();
		
		String strDecoderRevSubString = strDecoderRevFullString.substring(strDecoderRevFullString.indexOf(":") + 1, strDecoderRevFullString.length() - 1);
		String strDclRevSubString = strDclRevFullString.substring(strDclRevFullString.indexOf(":") + 1, strDclRevFullString.length() - 1);;
		
		Log. d(TAG, "Get imager props...");
		// Get Imager properties:
		ImagerProperties imgProp = new ImagerProperties();
		scanService.m_Decoder.getImagerProperties(imgProp);
		Log. d(TAG, "...Return from imager props");

		final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.revision_info_dialog);
        dialog.setTitle("Engine/Application Information");

        final TextView textview = (TextView)dialog.findViewById(R.id.textViewRevisionInfo);
        
        textview.setText(
        		"== Engine Information ==" +
        		"\nEngineID: 0x" + Integer.toHexString(imgProp.FirmwareEngineID) + " (" + scanService.m_Decoder.getEngineID() + ")" +
        		"\nS/N: " + scanService.m_Decoder.getEngineSerialNumber() +
        		"\nPSoC Rev: " + scanService.m_Decoder.getPSOCMajorRev() + "." + scanService.m_Decoder.getPSOCMinorRev() + 
        		"\nCols: " + imgProp.Columns + " Rows: " + imgProp.Rows + 
        		"\nAimerType: " + imgProp.AimerType + " Optics: " + imgProp.Optics +
        		
        		"\n\n== Revision Information ==" +
        		"\nOemScanDemo: " + scandemo_version +
        		"\nAPI: " + scanService.m_Decoder.getAPIRevision() +
        		"\nDecoder: " + strDecoderRevSubString +
        		"\nDCL: " + strDclRevSubString +
        		"\nScan Driver: " + scanService.m_Decoder.getScanDriverRevision() //+
        		
        		// Additional API function testing (not supported yet):
        		//"\n\nEngine Focus: " + scanService.m_Decoder.getEngineFocus() + 
        		//"\nEngine Illum: " + scanService.m_Decoder.getEngineIlluminationType() +
        		//"\nEngine AimerType: " + scanService.m_Decoder.getEngineAimerType() +
        		//"\nEngine ImagerType: " + scanService.m_Decoder.getEngineImagerType()
        		
        		);
        
        dialog.show();
		
		Log. d(TAG, "ShowAbout--");
	}
	*/
	/** 
	  * Reset scan_key setting if it mistakenly sets to system key (like home key)
	  * 
	  */
	void ResetSettings()
	{
		// Reset Scan key in case it's self configures!
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		Editor editor = settings.edit();
		editor.putInt("scan_key", -1);
		editor.commit();
		
		// Scan key preferences      
        int scankey = settings.getInt("scan_key", 0);        
        g_ScanKey = scankey; 
        Log. d(TAG, "g_ScanKey=" + g_ScanKey);
        
		try 
		{
			scanService.m_Decoder.setSymbologyDefaults(SymbologyID.SYM_ALL);
		} 
		catch (DecoderException e) {
			HandleDecoderException(e);
			return;
		}
        Log. d(TAG, "setSymbologyDefaults");
        
		/* This will read HSMDecoderAPI settings (after default all) 
		 * and re-configure Configuration Settings SetSymbologySettings 
		 * will be called by onResume */ 
		SetSymbologyPreferences(false);
	}
	
	/** 
	  * Backdoor Test
	  * 
	  */
	void BackdoorTest()
	{	
		Log. d(TAG, "DoBackdoorTest++");
		// Removed
		Log. d(TAG, "DoBackdoorTest--");
	}

	/** 
	  * Gets the last image and calls to store it
	  * 
	  */
	public void saveLastImage()
    {
		Log. d(TAG, "saveLastImage filetype = " + g_strFileSaveType);
       ImageAttributes attr = new ImageAttributes();
       byte[] image;
           try {
			image = scanService.m_Decoder.getLastImage(attr);
		} catch (DecoderException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "getLastImage error " + e.getErrorCode() + " - " + e.getMessage());
			return;
		}
           StoreByteImage(image, 100, g_strFileSaveType);
    }
	
	/** 
	  * Stores the last image (FIXME: cleanup!)
	  * 
	  */
	public boolean StoreByteImage(byte[] imageData, int quality, String expName) {

	   Log. d(TAG, "StoreByteImage++ expName = " + expName);
	   
       File sdImageMainDirectory = new File("/sdcard/Test"); // FIXME: Don't hardcode a path
       File imageFilePath =  new File(sdImageMainDirectory.getPath() + "/Images/");
       String strDateTimeStamp = (DateFormat.format("ddMMyyyy_hhmmss", new java.util.Date()).toString());
       Log. d(TAG, "timeStamp = " + strDateTimeStamp);
       
       	imageFilePath.mkdirs();
          FileOutputStream fileOutputStream1 = null;
          BufferedOutputStream bos = null;
          String nameFile = "";
          String shortNameFile = "lastImage_" + strDateTimeStamp;
          try {
                 BitmapFactory.Options options=new BitmapFactory.Options();
                 options.inSampleSize = 1;

                 int width = 0;
                 int height = 0;
                 if(expName.equals("raw")){//if(expName == "raw"){
                       width = scanService.g_nImageWidth;
                       height = scanService.g_nImageHeight;
                       //Deal with the Delivery task if it is running
                       //nameFile = imageFilePath.toString() +"/" + "RawImage_"+rawPicCount + ".raw";
                       nameFile = imageFilePath.toString() +"/" + shortNameFile + ".raw";
                       fileOutputStream1 = new FileOutputStream(nameFile);
                       bos = new BufferedOutputStream(fileOutputStream1);
                       bos.write(imageData);
                       Log.d(TAG, "RAW file save success");
                       Toast.makeText(getApplicationContext(), shortNameFile + ".raw saved", Toast.LENGTH_LONG).show();
                 }
                 else if(expName.equals("pgm")){//else if(expName == "pgm"){
                     width = scanService.g_nImageWidth;
                     height = scanService.g_nImageHeight;
                     //Deal with the Delivery task if it is running
                     //nameFile = imageFilePath.toString() +"/" + "PgmImage_"+pgmPicCount+".pgm";
                     nameFile = imageFilePath.toString() +"/" + shortNameFile + ".pgm";
                     
                     fileOutputStream1 = new FileOutputStream(nameFile);
                     bos = new BufferedOutputStream(fileOutputStream1);
                     //bufferedOutput.write("Line one".getBytes());
                     
                     bos.write("P5".getBytes()); 						// P5
                     bos.write("\n".getBytes());						// [CR]
                     bos.write("# last image sent to decoder".getBytes());		// comment
                     bos.write("\n".getBytes());						// [CR]
                     bos.write(Integer.toString(width).getBytes()); 	// width
                     bos.write(" ".getBytes());							// (space)
                     bos.write(Integer.toString(height).getBytes()); 	// height
                     bos.write("\n".getBytes());						// [CR]
                     bos.write("255".getBytes()); 		// 255
                     bos.write("\n".getBytes());						// [CR]
                     
                     // Data:
                     bos.write(imageData);
                     Log.d(TAG, "PGM file save success");
                     Toast.makeText(getApplicationContext(), shortNameFile + ".pgm saved", Toast.LENGTH_LONG).show();
                 }
                 else if(expName.equals("png")){//else if(expName == "png"){
                       width = scanService.g_nImageWidth;
                       height = scanService.g_nImageHeight;
                       
                       Bitmap myBitMap;
                  myBitMap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                  byte[] buffer = new byte[scanService.m_Decoder.getImageHeight() * scanService.m_Decoder.getImageWidth()];
                  ImageAttributes attr = new ImageAttributes();
                  buffer = scanService.m_Decoder.getLastImage(attr);
                   // FIXME:
              		int[] array = new int[width*height*2];
              		int cnt = 0;
              		for(int h = 0; h < height; h++)
              		{
              			for(int w = 0; w < width; w++)
              			{
              				array[cnt] = buffer[width*h + w] * 0x00010101;
              				cnt += 1;
              				
              			}
              		}
              		Log.d(TAG, "setPixels to myBitMap");
                  myBitMap.setPixels(array, 0, width, 0, 0, width, height);
                  

                       //myImage = renderCroppedGreyscaleBitmap(imageData, 0, 0, width, height, width);
                       Log.d(TAG, "imageData length " + imageData.length);
                       Log.d(TAG, "Preview width: " + width +
                                            " Preview height: " + height);    
                       //nameFile = "Image_";
                       
//                         if(myImage.isMutable())
//                                Log.d(TAG, "Image is mutable");

                       //String myNameFile = imageFilePath.toString() +"/" + nameFile + pngPicCount + ".png";
                       String myNameFile = imageFilePath.toString() +"/" + shortNameFile + ".png";
                       fileOutputStream1 = new FileOutputStream(myNameFile);
                       bos = new BufferedOutputStream(fileOutputStream1);
                       myBitMap.compress(Bitmap.CompressFormat.PNG, quality, bos);
                       Log.d(TAG, "PNG file save success");
                       //Toast.makeText(getApplicationContext(), "Image_"+pngPicCount+".png saved", Toast.LENGTH_LONG).show();
                       Toast.makeText(getApplicationContext(), shortNameFile + ".png saved", Toast.LENGTH_LONG).show();
                 }
                 else
                 {
                	 Log.d(TAG, "!! invalid image save type !!");
                	 return false; 
                 }
                 bos.flush();
                 bos.close();

          } catch (FileNotFoundException e) {
                 e.printStackTrace();
          } catch (IOException e) {
                 Log.d(TAG,"Image save was unsuccessful");
                 e.printStackTrace();
          } catch (DecoderException e) {
        	  Log.d(TAG, "getLastImage error " + e.getErrorCode() + " - " + e.getMessage());
  			return false;
		}
          return true;
   }
	/**
     * Function:ChineseHandle
     * @param arraydata
     * @return
     */
    private static String ChineseHandle(int[] arraydata){
    	String str01 = "";
	    if(!Isutf8orgb2312(arraydata)){
	    	str01 =	Utf8toString(arraydata);;
	    	
	    }else{
	    	
	    	str01 =	DecodetoString(1,arraydata);
	    	
	    }
	    return str01;
    }

	private static boolean Isutf8orgb2312(int[] value){
    	  boolean bool = true;//GB2312
    	  int len=value.length;
    	  boolean flag = false;
    	  
    		  for(int i=0;i<len;i++){
    			  if(value[i]>=128){
    				  if((i+2)<len){
    	  				  if((value[i]>=0xE0) && (value[i+1]>=0x80) && (value[i+2]>=0x80)){//Judge TF-8
                            i=i+2;
     				  }else{
     					  flag = true;
     					  bool = true;
     					  break;
     				  }
    				}
    	  			else{
    	  				flag = true;
    	  			    break; 
    	  			}
    			  } 			  
    		  }
    		  if(!flag){
    			  bool = false;
    		  }
    	  return bool;
          
      }
     
     /**
      * function:DecodetoString
      * id:=0,reservd,=1,Chinese ,=2,Japanese
      * @return
      */
     private static String DecodetoString(int id, int[] value){
  	   int len = value.length; 
  	   String str=null;
  	   byte[] bt = new byte[len];
  	   for(int i = 0;i<len;i++){
  		   bt[i]=(byte)value[i];
  	   }
  	   try{
  		   switch(id){
  		   case 1:
  			 Log.d("GB2312","GB2312 in Java dectected");
  			   str = new String(bt,"gb2312");
  			   break;
  		   case 2:
  			 Log.d("SHIFT-JIS","SHIFT-JIS in Java dectected");
  			   str = new String(bt,"SHIFT-JIS");
  			   break;
  		   default:
  			   str = new String(bt,"gb2312");
  			   break;			   		   
  		   }
  		   
  	   }catch(Exception e){
  		   e.printStackTrace();
  	   }
  	   return str;  	   
     }
     
     /**
      * function:UTF8  to String
      * @param value
      * @return
      */
     private static String Utf8toString(int[] value){
  	   int len = value.length; 
  	   String str=null;
  	   byte[] bt = new byte[len];
  	   for(int i = 0;i<len;i++){
  		   bt[i]=(byte)value[i];
  	   }
  	   try{
  		   str = new String(bt,"utf-8");
  	   }catch(Exception e){
  		   e.printStackTrace();
  	   }
  	   return str;   
     }
     
     private BroadcastReceiver f4Receiver = new BroadcastReceiver() {

 		@Override
 		public void onReceive(Context context, Intent intent) {
 			// Bundle bundle = intent.getExtras();
 			if (intent.hasExtra("F4key")) {
 				if (intent.getStringExtra("F4key").equals("down")) {
 					Log.v(TAG,"f4Receiver");
 						clicknowTime = System.currentTimeMillis();
 						if (clicknowTime - clicklastTime > 200) {
 							Log.e("ScanActivity:", "F4key down");
 							clicklastTime = clicknowTime;
 							if(!bOkToScan)
 							{
 								// Let the user know they can stop scanning by pressing scan button again
 								if(g_bContinuousScanEnabled)
 									Toast.makeText(getApplicationContext(), "Press scan button to stop continuous scanning.", Toast.LENGTH_LONG).show();

 								processScanButtonPress();

 								if(bWaitMultiple)
 									bTriggerReleased = true; // release trigger so it can restart
 							}
 							else
 								StopScanning();
 						}
 					
 				} else if (intent.getStringExtra("F4key").equals("up")) {
 					
 				}
 			}
 		}
 	};

	@Override
	public void result(String content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void henResult(String codeType, String context) {
		// TODO Auto-generated method stub
		
	}
}
