package ru.easyapp.sira;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by user on 02/09/14.
 */
public class RadioListFragment extends ListFragment {
	private List<Radio> mRadios;
	private Intent intent;
	private ListView lw;
	private TextView mainText;
	private TextView subText;
	private View lastOnListItemClickView;
	private ActionMode mActionMode;
	private ArrayAdapter<Radio> adapter;
	private int longClickPosition;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().setTitle(R.string.list_fragment_title);
		mRadios = RadioLab.get(getActivity()).getRadios();
		intent = new Intent(getActivity(), PlayService2.class);
		intent.setAction(PlayService2.ACTION_PLAY);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		getActivity().setProgressBarIndeterminateVisibility(true);
		lastOnListItemClickView = v;
		Radio r = (Radio)(getListAdapter().getItem(position));
		intent.putExtra(RadioFragment.EXTRA_RADIO_OBJECT, r);
		getActivity().startService(intent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		lw = getListView();
		lw.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lw.setLongClickable(true);
		lw.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(mActionMode != null) {
					return false;
				}
				mActionMode = getActivity().startActionMode(mActionModeCallBack);
				parent.setSelected(true);
				longClickPosition = position;
				return true;
			}
		});

		adapter = new RadioListAdapter(mRadios);
		setListAdapter(adapter);
		RadioLab.get(getActivity()).addOnStorageChangeListener(storageChangeListener);
	}

	private class RadioListAdapter extends ArrayAdapter<Radio> {
		public RadioListAdapter(List<Radio> radios) {
			super(getActivity(), 0, radios);
		}

		@SuppressWarnings("unused")
		public RadioListAdapter(RadioLab storage) {
			this(storage.getRadios());
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView (final int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_radio_list_item, null);
			}
			Radio r = getItem(position);
			mainText = (TextView)convertView.findViewById(R.id.textView2);
			mainText.setText(r.toString());
//    		subText.setText(r.getAllBitratesString());
			return convertView;
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String msg = intent.getStringExtra(PlayService.UPDATE_TEXT);
			updateStatusOfStream(msg);
		}
	};

	private void updateStatusOfStream(String text) {
		subText = (TextView)lastOnListItemClickView.findViewById(R.id.textView3);
		subText.setText(text);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().registerReceiver(receiver, new IntentFilter(PlayService.INTENT_UPDATED_EVENT));
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(receiver);
	}

	/**
	 * Long click menu in action bar call back
	 */
	private Callback mActionModeCallBack = new Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.action_mode_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		/**
		 * In this method we process an action
		 */
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			int itemId = item.getItemId();
			Radio r = (Radio)getListAdapter().getItem(longClickPosition);
			// Clicked: delete radio from list 
			if(itemId == R.id.delete_item) {
				RadioLab.get(getActivity()).deleteRadio(r);
			}
			// Clicked: edit radio item
			else if(itemId == R.id.edit_item) {
				// Show dialog window with name and URI
				// and if OK pressed we must save text fields to 
				// radio object and update list
				RadioDialogFragment.getInstance(r).show(getFragmentManager(), "edit_fragment");
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}
	};

	/**
	 * Call-back if data in model changed
	 */
	StorageChangeListener storageChangeListener = new StorageChangeListener() {
		@Override
		public void onStorageChanged() {
			// update list
			adapter.notifyDataSetChanged();
		}
	};

}
