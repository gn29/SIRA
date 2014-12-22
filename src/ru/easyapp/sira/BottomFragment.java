package ru.easyapp.sira;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BottomFragment extends Fragment {
	Radio r;
	
	private BottomFragment(){
		
	}
	
	@Override
	public void onCreate(Bundle savedBundle) {
		super.onCreate(savedBundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.bottom_actions, container, false);
		return v;
	}
	
	public static BottomFragment newInstance() {
		return new BottomFragment();
	}
	
}
