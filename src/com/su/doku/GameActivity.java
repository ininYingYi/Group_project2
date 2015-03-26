package com.su.doku;


import java.util.Random;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends Activity
{
	//timer variables
	private Timer gameTimer;
	private TextView timeTextView;
	private int tsec=0,csec=0,cmin=0;
	//private boolean startFlag = true;
	private static final String FileName = "record.txt";
	private String player_name = "";
	private EditText finish_text;
	private TextView finish_name_text;
	private File fileDir;
	
	private Button finishButton;
	
	private GridLayout gridLayout;
	private LinearLayout linearLayout;
	private SudokuUnit[][] sudokuUnits;
	private DragUnit dragUnits[];
	private Button resetButton;
	private Button backButton;
	
	private int answer[][] = new int[9][9];
	private int givenNumber[][] = new int[9][9];
	private int userMatrix[][] = new int[9][9];
	private int saveQueue[] = new int[5];
	private int time = 0;
	private DragUnit isDraging;
	
	private final int sudokuSize = 9;	
	private SaveReadState saveObject = new SaveReadState();
	private int level;
	private int totalBlock;
	private int remainBlock;
	private int preferSize;
	private final int queueSize = 5;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		produce sudokuProduce = new produce();
		answer = sudokuProduce.generatePuzzleMatrix();
		level = 1;
		totalBlock = (level+1) * 9;
		remainBlock = totalBlock;
		dragUnits = new DragUnit[totalBlock];
		givenNumber = sudokuProduce.generatePuzzleQuestion(level);
		
		receiveData();
		initializeViews();
		
	}
	
	/*
	@Override
	protected void onPause()
	{
		super.onPause();
		
	}*/
	
	private void initializeViews()
	{
		timeTextView = (TextView) findViewById(R.id.textView_time);
		timeTextView.setText(String.valueOf(time));
		
		//用來取得螢幕大小
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		preferSize = displayMetrics.widthPixels/sudokuSize;
		
		gridLayout = (GridLayout) findViewById(R.id.GridLayout1);
		sudokuUnits = new SudokuUnit[sudokuSize][sudokuSize];
		int dragUnitCount = 0;
		for(int i = 0; i < sudokuSize; i++)
		{
			for(int j = 0; j < sudokuSize; j++)
			{
				if ( givenNumber[i][j] == 1 ) {
					sudokuUnits[i][j] = new SudokuUnit(GameActivity.this, i, j, answer[i][j]);
					userMatrix[i][j] = answer[i][j];
				}
				else {
					sudokuUnits[i][j] = new SudokuUnit(GameActivity.this, i, j, 0);
					userMatrix[i][j] = 0;
					dragUnits[dragUnitCount] = new DragUnit(GameActivity.this,answer[i][j],dragUnitCount);
					dragUnitCount++;
				}
				sudokuUnits[i][j].setOnDragListener(dragListener);
				gridLayout.addView(sudokuUnits[i][j], i*sudokuSize+j);
				//����e
				sudokuUnits[i][j].getLayoutParams().height = preferSize;
				sudokuUnits[i][j].getLayoutParams().width = preferSize;
			}
		}
		
		resetButton = (Button) findViewById(R.id.button_reset);
		resetButton.setOnClickListener(resetButtonListener);
		backButton = (Button) findViewById(R.id.button_back);
		backButton.setOnClickListener(backButtonListener);
		
		
		finishButton = (Button) findViewById(R.id.button_finish);
		finishButton.setOnClickListener(finishButtonListener);
		
		gameTimer = new Timer();
		gameTimer.schedule(timer_task, 0, 1000);
		
		linearLayout = (LinearLayout) findViewById(R.id.LinearLayout1);
	}
	
	private void receiveData()
	{
		Bundle bundle = getIntent().getExtras();
		if(bundle.getBoolean("isStart"))
		{
			// TODO 生成新遊戲
		}
		else
		{
			// TODO 讀取未完成遊戲資訊
			saveObject.ReadState();
			answer = saveObject.getAnswer();
			userMatrix = saveObject.getUserMatrix();
			for ( int i = 0; i < 9; i++ ) {
				for ( int j = 0; j < 9; j++ ) {
					if ( userMatrix[i][j] == 0 ) {
						givenNumber[i][j] = 0;
					}
					else {
						givenNumber[i][j] = 1;
					}
					
				}
			}
			
			
			/*int readQueue[] = saveObject.getQueue();
			for ( int i = 0; i < readQueue.length; i++ ) {
				int index = readQueue[i];
				dragUnits[index].showInQueue();
				linearLayout.addView(dragUnits[index]);
				dragUnits[index].getLayoutParams().height = preferSize;
				dragUnits[index].getLayoutParams().width = preferSize;
				dragUnits[index].setOnTouchListener(touchListener);
			}*/
		}
	}
	
	private OnTouchListener touchListener = new OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			//前者我亂填的,也沒用到; 後者是夾帶的資料
			DragUnit temp = (DragUnit)v;
			ClipData clipData = ClipData.newPlainText("number", temp.getNumber()+"");
			//創一個跟imageView1長得一樣的shadow
			View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
			v.startDrag(clipData, shadowBuilder, null, 0);
			isDraging = temp;
			linearLayout.removeView(v);
			temp.removeFromQueue();
			return true;
		}
	};
	
	private OnDragListener dragListener = new OnDragListener()
	{
		@Override
		public boolean onDrag(View v, DragEvent event)
		{
			switch(event.getAction())
			{
			case DragEvent.ACTION_DRAG_STARTED:
				break;
			case DragEvent.ACTION_DRAG_ENTERED:
				break;
			case DragEvent.ACTION_DRAG_LOCATION:
				break;
			case DragEvent.ACTION_DRAG_EXITED:
				break;
			case DragEvent.ACTION_DROP:	//當被拖曳的物件移動到此view的範圍並放下時
				SudokuUnit sudokuUnit = (SudokuUnit) v;
				//取得夾帶資料
				int data = Integer.parseInt(event.getClipData().getItemAt(0).getText().toString());
				if(data == answer[sudokuUnit.getIndexX()][sudokuUnit.getIndexY()])
				{
					sudokuUnit.setNumber(data);
					userMatrix[sudokuUnit.getIndexX()][sudokuUnit.getIndexY()] = data;
					remainBlock--;
					isDraging.correct();
				}
				isDraging = null;
				//判斷結束沒
				if (remainBlock == 0 ) {
					
				}
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				break;
			default:
				return false;
			}
			return true;
		}
	};
	
	private Button.OnClickListener resetButtonListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			showResetDialog();
		}
	};
	
	private Button.OnClickListener backButtonListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			showBackDialog();
		}
	};
	
	private Button.OnClickListener finishButtonListener = new Button.OnClickListener(){
		@Override
		public void onClick(View v)
		{
			showfinishDialog();
			//writeRecord(FileName);
			//showBackDialog();
			//restart
			//tsec = 0;
			//timeTextView.setTag("00:00");
		}
	};
	
	private TimerTask timer_task = new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			//if(startFlag = true){
			tsec++;
			Message msg = new Message();
			msg.what = 1;
			timer_handler.sendMessage(msg);
			//}
		}
	};
	
	private Handler timer_handler = new Handler(){
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:
				time++;
				csec = tsec % 60;
				cmin = tsec / 60;
				String s = "";
				if(cmin < 10){
					s = "0" + cmin;
				}
				else {
					s = "" + cmin;
				}
				if (csec < 10) {
					s = s + ":0" + csec;
				} else {
					s = s + ":" + csec;
				}
				timeTextView.setText(s);
				//if(startFlag = false){
				//	gameTimer.cancel();
				//}
				if ( time % 5 == 0 ) {
					generateDargUnit();
				}
				
				break;
			default:
				break;
			}
		}
	};
	
	//結束存紀錄
	private boolean FindDirectoryPath() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_REMOVED)) {
			Toast.makeText(GameActivity.this, "沒有SDDDD", Toast.LENGTH_SHORT)
					.show();
			return false;
		}
		else{
			fileDir = new File(Environment.getExternalStorageDirectory().getPath() + "/SuDoKu");
			if (!fileDir.exists())
				fileDir.mkdirs();
			return true;
		}
	}
    
    private void writeRecord(String filename){//這write會蓋掉每次的資料 write之前要先read也許
    	if(FindDirectoryPath()){
    		BufferedWriter writer = null;
			File file = null;
			//Scanner reader = null;
			
			//抓時間日期
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			Date curDate = new Date(System.currentTimeMillis());
			String str = formatter.format(curDate);
    		try {
    			file = new File(fileDir.getAbsolutePath() + "/" + filename);
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(file, true), "UTF-8"));
				writer.append(String.valueOf(tsec) + ",");
				writer.append(str + ",");
				writer.append("肉餅臉");
				writer.newLine();
				writer.flush();
				Toast.makeText(GameActivity.this, "Saved", Toast.LENGTH_SHORT)
				.show();
				
			} catch (Exception e) {
				// TODO: handle exception
				Toast.makeText(GameActivity.this, "Saving Failed",
						Toast.LENGTH_SHORT).show();
			}
    		finally{
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
    		}
    	}
    }  
	//end
	
	private void showResetDialog()
	{
		new AlertDialog.Builder(GameActivity.this)
		.setTitle(R.string.choice_reset)
		.setMessage(R.string.confirm_reset)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// TODO 重置遊戲
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				//nothing
			}
		})
		.show();
	}
	
	private void showBackDialog()
	{
		new AlertDialog.Builder(GameActivity.this)
		.setTitle(R.string.choice_back)
		.setMessage(R.string.confirm_back)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				// TODO 回傳遊戲資料
				/*Bundle bundle = new Bundle();
				
				Intent intent = new Intent();
				intent.putExtras(bundle);
				setResult(RESULT_OK, intent);*/
				showNotification(linearLayout.getChildCount());
				for ( int i = 0; i < linearLayout.getChildCount(); i++ ) {
					DragUnit temp = (DragUnit)linearLayout.getChildAt(i);
					saveQueue[i] = temp.getIndex();
				}
				saveObject.SaveState(answer, userMatrix, saveQueue);
				//back to title
				GameActivity.this.finish();
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				//nothing
			}
		})
		.show();
	}
	

	private void generateDargUnit() {
		int queueNumber = linearLayout.getChildCount();
		if (remainBlock == 0 || queueNumber >= queueSize) {
			return;
		}
		int index = getRandomInt(0,totalBlock-1);
		while(dragUnits[index].isCorrect() || dragUnits[index].isInQueue() || dragUnits[index] == isDraging) {
			index = getRandomInt(0,totalBlock-1);
		}
		dragUnits[index].showInQueue();
		linearLayout.addView(dragUnits[index]);
		dragUnits[index].getLayoutParams().height = preferSize;
		dragUnits[index].getLayoutParams().width = preferSize;
		dragUnits[index].setOnTouchListener(touchListener);
		
		Animation am = new TranslateAnimation( (6-queueNumber)*preferSize, 0, 0, 0 );
	    
	    // 動畫開始到結束的執行時間 (1000 = 1 秒)
	    am.setDuration( 1000 );
	    
	    // 動畫重複次數 (-1 表示一直重複)
	    am.setRepeatCount(0);
	    dragUnits[index].setAnimation(am);
	    am.startNow();
	}
	
	private int getRandomInt(int min, int max) {
		Random r = new Random();
		return r.nextInt(max - min + 1) + min;
	}
	
	protected void showNotification(double BMI)
    {
    	NotificationManager barManager = 
    	(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    
   	 Notification barMsg = new Notification(
   	 R.drawable.ic_launcher,
    	String.valueOf(BMI),
    	System.currentTimeMillis()
   	 );
 	PendingIntent contentIntent = PendingIntent.getActivity(   this,   0,  
                        new  Intent(this,GameActivity.class),
  	PendingIntent.FLAG_UPDATE_CURRENT);
    
   	barMsg.setLatestEventInfo(
    	GameActivity.this, 
   	 "您的BMI值過高", 
   	 "你該節食了",
    	contentIntent
   	 );
  	  barManager.notify(0,barMsg);    
    }

	private void showfinishDialog(){
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog, (ViewGroup)findViewById(R.id.dialog));
		
		
		AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
		MyAlertDialog.setTitle(R.string.finish_dialoag_title);
		MyAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		MyAlertDialog.setView(layout);
		
		DialogInterface.OnClickListener Neg_ClickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				finish_text = (EditText)findViewById(R.id.etname);
				finish_name_text = (TextView)findViewById(R.id.tvname);
				//player_name = finish_text.getText().toString();//這句必當
				Toast.makeText(GameActivity.this, "你选择的id为" + which, Toast.LENGTH_SHORT).show();
				writeRecord(FileName);
				
			}
		};
		DialogInterface.OnClickListener Pos_ClickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Toast.makeText(GameActivity.this, "你选择的id为" + which, Toast.LENGTH_SHORT).show();
			}
		};
		
		MyAlertDialog.setNegativeButton("確定", Neg_ClickListener);
		MyAlertDialog.setPositiveButton("重新一局", Pos_ClickListener);
		MyAlertDialog.create().show();			
	}

}
