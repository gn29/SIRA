package ru.easyapp.sira;

import ru.easyapp.sira.R;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Better to use new version of service - PlayService2.java
 * @author user
 * @see PlayService2
 */
public class PlayService extends Service {
	
	public static final String SERVICE_NAME = "ru.easyapp.SIRO.PlayService";
	public static final String INTENT_STOP =  "ru.easyapp.SIRO.intent_stop";
	public static final String INTENT_UPDATED_EVENT = "ru.easyapp.SIRO.buffer_update_event";
	public static final String UPDATE_TEXT = "ru.easyapp.SIRO.update_text";
	
	private MediaPlayer player = null;
	private int NOTIFICATION_ID = 1;
	private Notification notification;
	private Intent updateIntent;
	private Radio r;
	
	@Override
	public void onCreate() {
		super.onCreate();
		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		updateIntent = new Intent(INTENT_UPDATED_EVENT);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(getApplicationContext(), "onStartCommand", Toast.LENGTH_SHORT).show();

		if (intent != null) {

			// if user clicked another station on activity
			// than stop and reset current play
			if (player.isPlaying()){
				player.stop();
				player.reset();
				stopForeground(true);
			}

			// if it's from click on notification 
			// player must stop playing and release resources
			if (intent.getBooleanExtra(INTENT_STOP, false)) {
				player.release();
				stopForeground(true);
				stopSelf();
				return 0;
			}

			r = (Radio)intent.getSerializableExtra(RadioFragment.EXTRA_RADIO_OBJECT);
			intent.removeExtra(RadioFragment.EXTRA_RADIO_OBJECT);
			String url = r.getStream();

			player.setOnPreparedListener(onPreparedListener);
			player.setOnInfoListener(onInfoListener);
			try {
				player.setDataSource(url);
				player.prepareAsync();
			} catch (Exception e) {}
			putServiceForeground();
		}
		return Service.START_STICKY;
	}
	
	/**
	 * MediaPlayer
	 * Listener implementation for MediaPlayer.prepareAsync() call-back
	 * 
	 * @return MediaPlayer.OnPreparedListener interface implementation object
	 */
	MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener(){
		@Override
		public void onPrepared(final MediaPlayer mp) {
			mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mp.start();
		}
	};
	
	/**
	 * MediaPlayer
	 * Listener for errors while playing
	 */
	MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}
	};
	
	/**
	 * MediaPlayer
	 * Listener for updating info about track or stream
	 */
	private MediaPlayer.OnInfoListener onInfoListener = new MediaPlayer.OnInfoListener() {
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			String msg = null;
			switch(what) {
			case(MediaPlayer.MEDIA_INFO_UNKNOWN) :
				msg = "Unknown info";
				break;
			case(MediaPlayer.MEDIA_INFO_BUFFERING_START) :
				msg = "Buffering";
				break;
			case(MediaPlayer.MEDIA_INFO_BUFFERING_END) :
				msg = "Buffer is OK";
				break;
			case(MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) :
				msg = "Live";
				break;
			}
			updateIntent.putExtra(UPDATE_TEXT, msg);
			sendBroadcast(updateIntent);
			return true;
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Remove service from foreground 
	 * and clear memory from player.
	 */
	@Override
	public void onDestroy() {
		stopForeground(true);
		player.release();
	}
	
	/**
	 * Put service foreground
	 */
	@SuppressWarnings({"deprecation"})
	@SuppressLint("NewApi")
	private void putServiceForeground() {
		Intent intentStop = new Intent(this, PlayService.class);
			intentStop.putExtra(INTENT_STOP, true);
			intentStop.putExtra(RadioFragment.EXTRA_RADIO_OBJECT, r);
		PendingIntent cancelledPendingIntent = PendingIntent.getService(this, 1, intentStop, 0);
		
		RemoteViews rViews = new RemoteViews(getPackageName(), R.layout.bottom_actions);
		
		Notification.Builder builder =	new Notification.Builder(getApplicationContext())
			.setSmallIcon(R.drawable.ic_stat_notify_radio)
//			.setContentTitle(r.getRadioName())
//			.setContentText(getApplicationContext().getResources().getString(R.string.notification_service_comment))
			.setOngoing(true)
			.setContentIntent(cancelledPendingIntent)
			.setContent(rViews);
		
		int versionSdk = VERSION.SDK_INT;
		if(versionSdk >= 14 && versionSdk < 16) {
			notification = builder.getNotification();
		}
		if(versionSdk >= 16) {
			notification = builder.build();
		}
		startForeground(NOTIFICATION_ID, notification);
	}
}
