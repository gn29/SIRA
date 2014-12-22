package ru.easyapp.sira;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Dialog view for add and edit radios.
 * @author user
 *
 */
public class RadioDialogFragment extends DialogFragment {
	private Radio r;
	private EditText radioName;
	private EditText radioUrl;

	private RadioDialogFragment(Radio r) {
		this.r = r;
	}

	/**
	 * Static constructor with parameters
	 * @param r - instance of Radio for editing or null for creating
	 * @return
	 */
	public static RadioDialogFragment getInstance(Radio r) {
		return new RadioDialogFragment(r);
	}

	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_radio, null, false);
		radioName  = (EditText)view.findViewById(R.id.radio_name);
		radioUrl = (EditText)view.findViewById(R.id.radio_uri);
		builder.setTitle(getResources().getString(R.string.popup_add_item));
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok, positiveButtonOnClickListener);
		if (r != null) {
			radioName.setText(r.getRadioName());
			radioUrl.setText(r.getStream());
		}
		return builder.create();
	}

	DialogInterface.OnClickListener positiveButtonOnClickListener = 
			new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			saveRadio();
		}
	};

	private void saveRadio() {
		// проверить не пустые ли поля
		String name = radioName.getText().toString();
		if (name.isEmpty()) {
			name = getResources().getString(R.string.no_name_radio);
		}

		// проверить правильность ввода адреса
		String url = radioUrl.getText().toString();
		if(url.isEmpty()) {
			Toast.makeText(getActivity(), getResources().getString(R.string.no_url_radio), Toast.LENGTH_LONG).show();
			return;
		}
		if(!(url.startsWith("http://") || url.startsWith("https://"))) {
			url = "http://" + url;
		}

		// сохранить в RadioLab
		// если новое
		if (r == null) {
			RadioLab.get(getActivity()).addRadio(new Radio(name, url));
		} 
		// если после изменения, то надо поставить у старого радио эти поля 
		// отдаем все храннилищу, пусть там все меняет, что бы мы в
		// View об этом не думали
		else {
			RadioLab.get(getActivity()).editRadio(r, name, url);
		}
	}


}
