package ru.easyapp.sira;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.Toast;

/**
 * Created by user on 02/09/14.
 */
public class RadioListActivity extends Activity {
	MenuItem stopButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_radio_list);
		FragmentManager fm = getFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragment_list_container);
		if(fragment == null) {
			fragment = new RadioListFragment();
			fm
			.beginTransaction()
			.add(R.id.fragment_list_container, fragment)
			.commit();
		}

		/**
		 * Bottom fragment
		 * doesn't use
		 */
//		Fragment bottomFragment = fm.findFragmentById(R.id.fragment_bottom_action_container);
//		if(null == bottomFragment) {
//			bottomFragment = BottomFragment.newInstance();
//			fm.beginTransaction()
//			.add(R.id.fragment_bottom_action_container, bottomFragment)
//			.commit();
//		}

	}
    
    /**
     * Creating option menu in activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	ActionBar bar = getActionBar();
    	bar.setSubtitle(R.string.actionbar_subtitle);
    	getMenuInflater().inflate(R.menu.radiolistactivity_actionbar_menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	stopButton = menu.findItem(R.id.actionbar_stop_btn);
    	// ask service for status
    	// what happening with service
    	Intent getStatus = new Intent(PlayService2.ACTION_ASK_SERVICE_STATUS);
    	sendBroadcast(getStatus);
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	registerReceiver(playServiceStatusReceiver, 
    			new IntentFilter(PlayService2.ACTION_SERVICE_STATUS));

    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(playServiceStatusReceiver);
    }
    
    /**
     * Handling of click on options menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int itemId = item.getItemId();
		if (itemId == (R.id.overflow_item)) {
			owerflowItemClicked(findViewById(R.id.overflow_item));
			return true;
		} else if (itemId == (android.R.id.home)) {
			Toast.makeText(getApplicationContext(), "Back", Toast.LENGTH_SHORT).show();
			return true;
		} else if (itemId == (R.id.actionbar_stop_btn)) {
			Intent actionButtonIntent = new Intent(this, PlayService2.class);
			actionButtonIntent.setAction(PlayService2.ACTION_STOP);
			startService(actionButtonIntent);
			return true;
		}
    	return super.onOptionsItemSelected(item);
    }
    
    private void addItemClicked(){
    	RadioDialogFragment.getInstance(null).show(getFragmentManager(), "add_fragment");
    }
    
    private void owerflowItemClicked(View v){
    	PopupMenu popup = new PopupMenu(this, v);
    	MenuInflater inflater = popup.getMenuInflater();
    	inflater.inflate(R.menu.radiolistactivity_actionbar_popup_menu, popup.getMenu());
    	popup.setOnMenuItemClickListener(mPopupMenuListener);
    	popup.show();

    }
    
    /**
     * Popup menu item clicked handler
     * for owerflowItemClicked
     */
    PopupMenu.OnMenuItemClickListener mPopupMenuListener = new PopupMenu.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			int itemId = item.getItemId();
			if (itemId == (R.id.popup_add_item)) {
				addItemClicked();
			} else if (itemId == (R.id.popup_aboud_item)) {
				startActivity(new Intent(getApplicationContext(), AboutActivity.class));
			}
			return true;
		}
    };
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
//    	outState.putParcelable(key, value);
    }
    
//    @Override
//    public void onBackPressed() {
//    	if(!goingOut) {
//    		Toast.makeText(this, "Press Back button once again to exit.", Toast.LENGTH_SHORT).show();
//    		goingOut=!goingOut;
//    	} else {
//    		goingOut=!goingOut;
//    		super.onBackPressed();
//    	}
//    }
    
    BroadcastReceiver playServiceStatusReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if(PlayService2.ACTION_SERVICE_STATUS.equals(action)) {
    			setProgetsInBar(intent);
    			setStopButtonVisibilityInBar(intent);
    		}
    	}
    };
    
    private void setProgetsInBar(Intent i) {
    	PlayService2.State serviceStatus = (PlayService2.State)i.getSerializableExtra(PlayService2.INTENT_EXTRA_STATUS);
    	if(PlayService2.State.Preparing.equals(serviceStatus)) {
    		setProgressBarIndeterminateVisibility(true);
    	} else {
    		setProgressBarIndeterminateVisibility(false);
    	}
    }
    
    private void setStopButtonVisibilityInBar(Intent i) {
    	if(stopButton == null) {
    		Log.e("Stop button status", "Can't find stop button");
    		return;
    	}
    	PlayService2.State serviceStatus = (PlayService2.State)i.getSerializableExtra(PlayService2.INTENT_EXTRA_STATUS);
    	if(PlayService2.State.Stoped.equals(serviceStatus)) {
    		stopButton.setVisible(false);
    	} else {
    		stopButton.setVisible(true);
    	}
    }
}
