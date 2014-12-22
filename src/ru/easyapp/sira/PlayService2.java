package ru.easyapp.sira;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Main service for playing streams or music
 * @author user
 * @since dec. 2014
 * @version 1.0
 *
 */
public class PlayService2 extends Service {
	/**
	 * Intent actions for this service for controlling 
	 * playing and changing statuses
	 */
	public static final String ACTION_PLAY = "ru.easyapp.sira.ACTION_PLAY";
	public static final String ACTION_STOP = "ru.easyapp.sira.ACTION_STOP";
	public static final String ACTION_SERVICE_STATUS = "ru.easyapp.sira.ACTION_SERVICE_STATUS";
	public static final String ACTION_ASK_SERVICE_STATUS = "ru.easyapp.sira.ACTION_ASK_SERVICE_STATUS";
	public static final String INTENT_EXTRA_STATUS = "ru.easyapp.sira.INTENT_EXTRA_STATUS";
	private static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
	
	Context context;
	
	/**
	 * Our service state
	 */
	static enum State {
		Preparing,
		Plaing,
		WillBeStopped, 
		Stoped
	}
	
	static State mServiceState = State.Stoped;
	
	/**
	 * MediaPlayer instance
	 */
	MediaPlayer mMediaPlayer = null;

	Notification mNotification = null;
	NotificationManager mNotificationManager;
	AudioManager mAudioManager;
	
	/**
	 * Instance for locking wi-fi 
	 */
	WifiLock mWifiLock;
	WifiManager mWifiManager;
	/**
	 * Notification id
	 */
	final int NOTIFICATION_ID = 1;
	
	/**
	 * Radio instance
	 */
	Radio mRadio = null;
	
	/**
	 * 
	 */
	int didWeGetAudioFocuse;
	
	/**
	 * Service life-circle method
	 */
	@Override
	public void onCreate(){
		super.onCreate();
		mNotificationManager = (NotificationManager)getSystemService(ContextWrapper.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(ContextWrapper.AUDIO_SERVICE);
		mWifiManager = (WifiManager)getSystemService(ContextWrapper.WIFI_SERVICE);
		mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Sira_wifi_lock");
		context = getApplicationContext();
		
	}
	
	/**
	 * Service life-circle method calls when service returns from 
	 * pause or re-called by intent
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		registerReceiver(serviceAskedForItsStatus, new IntentFilter(ACTION_ASK_SERVICE_STATUS));
		String action = intent.getAction();
		if(action.equals(ACTION_PLAY)) {
			mRadio = (Radio)intent.getSerializableExtra(RadioFragment.EXTRA_RADIO_OBJECT);
			intent.removeExtra(RadioFragment.EXTRA_RADIO_OBJECT);
			playStream();
		} else if (action.equals(ACTION_STOP)){
			stopPlayingStream();
		}
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(serviceAskedForItsStatus);
	}
	
	/**
	 * Create if need and prepare media player
	 */
	void prepareOrCreateMediaPlayerIfNeed(){
		if(mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		} else {
			mMediaPlayer.reset();
		}
		mMediaPlayer.setAudioStreamType(STREAM_TYPE);
		mMediaPlayer.setOnPreparedListener(onPreparedListener);
		mMediaPlayer.setOnErrorListener(mErrorListener);
		mMediaPlayer.setOnInfoListener(mInfoListener);
		mMediaPlayer.setOnCompletionListener(mCompletitionListener);
	}
	
	/**
	 * Release all resources
	 */
	void releaseResources() {
		// going out from foreground service
		stopForeground(true);
		
		//releasing player
		if(mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
		
		//To-do: clear other things like wi-fi lock or 3g
		if (mWifiLock.isHeld()) {
			mWifiLock.release();
		}
		
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
	}
	
	/**
	 * Start playing stream.
	 * 1. prepare player
	 * 2. put stream uri and prepare asynch
	 * 3. go foreground
	 * 4. update notification
	 * 5. start playing 
	 * 
	 * @param uri
	 */
	void playStream() {
		prepareOrCreateMediaPlayerIfNeed();
		String uri = mRadio.getStream();
		try {
			mMediaPlayer.setDataSource(uri);
		} catch (IllegalStateException e) {
		} catch (IOException e) {
		}
		mServiceState = State.Preparing;
		mMediaPlayer.prepareAsync();
		setServiceForeground();
	}

	MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener(){
	@Override
	public void onPrepared(MediaPlayer mp) {
			if(mServiceState == State.WillBeStopped) {
				stopPlayingStream();
				return;
			}
			//=== locking ===
			// Wi-Fi
			mWifiLock.acquire();
			// CPU
			mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
			// audio focus
			do {
			didWeGetAudioFocuse = mAudioManager.requestAudioFocus(mAudioFocusChangeListener, 
					STREAM_TYPE, 
					AudioManager.AUDIOFOCUS_GAIN);
			} while (didWeGetAudioFocuse == AudioManager.AUDIOFOCUS_REQUEST_FAILED);
			// start playing
			mp.start();
			// change service state
			mServiceState = State.Plaing;
			broadcastServiceStatus();
		}
	};
	
	void stopPlayingStream() {
		if(mServiceState == State.Plaing) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}
			mServiceState = State.Stoped;
			releaseResources();
		} else if (mServiceState == State.Preparing) {
			mServiceState = State.WillBeStopped;
		} else if (mServiceState == State.WillBeStopped) {
			mServiceState = State.Stoped;
			releaseResources();
		}
		broadcastServiceStatus();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	void createNotification() {
		String title = context.getString(R.string.app_name);
		if(mRadio != null) {
			title = mRadio.getRadioName();
		}
		Intent actionButtonIntent = new Intent(this, PlayService2.class);
		actionButtonIntent.setAction(ACTION_STOP);
		PendingIntent pi = PendingIntent.getService(this, 1, actionButtonIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		RemoteViews rViews = new RemoteViews(getPackageName(), R.layout.service_notification);
		rViews.setOnClickPendingIntent(R.id.stop_btn, pi);
		rViews.setTextViewText(R.id.n_title, title);
		rViews.setTextViewText(R.id.n_line2, "On air");
		
		Intent openActivityIntent = new Intent(this, RadioListActivity.class);
		PendingIntent openActivityPi = PendingIntent
				.getActivity(getApplicationContext(), 2, openActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		Notification.Builder builder = new Notification.Builder(getApplicationContext());
		builder
			.setSmallIcon(R.drawable.ic_stat_notify_radio)
			.setOngoing(true)
			.setContentIntent(openActivityPi)
			.setContent(rViews);
			
		int sdk_int = android.os.Build.VERSION.SDK_INT;
		if(sdk_int >= 11 && sdk_int < 16) {
			mNotification = builder.getNotification();
		} else if (sdk_int >= 16) {
//			builder.addAction(R.drawable.ic_action_stop, 
//					context.getString(R.string.action_stop), 
//					pi);
			mNotification = builder.build();
		}
		startForeground(NOTIFICATION_ID, mNotification);
	}

	void updateNotification(String text) {
		if(mNotification == null) {
			createNotification();
		}
	}
	
	void setServiceForeground() {
		createNotification();
		
	}
	
	
	AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener =
			new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch(focusChange) {
			case(AudioManager.AUDIOFOCUS_GAIN) :
				// go loud!
				// unMute
				mAudioManager.setStreamMute(STREAM_TYPE, false);
				Log.d("AudioFocus", "au gain");
			break;
			case(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) :
				// pause playing 
				Log.d("AudioFocus", "au GAIN_TRANSIENT");
			break;
			case(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) :
				// pause playing 
				Log.d("AudioFocus", "au GAIN_TRANSIENT_EXCLUSIVE");
			break;
			case(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) :
				// pause playing 
				Log.d("AudioFocus", "au GAIN_TRANSIENT_MAY_DUCK");
			break;	

			case(AudioManager.AUDIOFOCUS_LOSS) :
				stopPlayingStream();
			// надо здесь сделать нотификацию об останове с
			// перезапуском проигрывания по желанию пользователя
				Log.d("AudioFocus", "au loss");
			break;
			case(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) :
				// reduce volume to 0.1f
				mAudioManager.setStreamMute(STREAM_TYPE, true);
				Log.d("AudioFocus", "au loss_TRANSIENT");
			break;
			case(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) :
				// pause playing 
				Log.d("AudioFocus", "au loss_TRANSIENT_CAN_DUCK)");
			break;
			}

		}
	};
	
	MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener(){
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.d("io_error", String.valueOf(what + " + " + extra));
			return true;
		}
	};
	
	MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener(){
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.d("buffer listener", String.valueOf(what));
			return true;
		}
	};
	
	MediaPlayer.OnCompletionListener mCompletitionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			
		}
	};
	
	private void broadcastServiceStatus() {
		Intent i = new Intent(ACTION_SERVICE_STATUS);
		i.putExtra(INTENT_EXTRA_STATUS, mServiceState);
		sendBroadcast(i);
	}
	
	BroadcastReceiver serviceAskedForItsStatus = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(ACTION_ASK_SERVICE_STATUS.equals(action)) {
				broadcastServiceStatus();
			}
		}
	};

}
