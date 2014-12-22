package ru.easyapp.sira;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by user on 02/09/14.
 */
public class RadioFragment extends Fragment {

    private Radio radio;
    private TextView tv;
    public static final String EXTRA_RADIO_OBJECT = ".radio";

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Intent i = getActivity().getIntent();
//    	radio = (Radio)i.getSerializableExtra(EXTRA_RADIO_OBJECT);
        radio = (Radio)getArguments().getSerializable(EXTRA_RADIO_OBJECT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio, container, false);
        tv = (TextView)view.findViewById(R.id.radio_name);
        if(tv != null) {
        	tv.setEnabled(false);
    		tv.setText(radio.getRadioName());
    	}
        return view;
    }
    
    public static RadioFragment newInstance(Radio radio) {
    	Bundle args = new Bundle();
    	args.putSerializable(EXTRA_RADIO_OBJECT, radio);
    	RadioFragment fragment = new RadioFragment();
    	fragment.setArguments(args);
    	return fragment;
    }
    
}
