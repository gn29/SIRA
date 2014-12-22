package ru.easyapp.sira;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by user on 31/08/14.
 * @author easyapp.ru
 * @version 1.0
 */
public class Radio implements Serializable{

	private static final long serialVersionUID = 1L;
	private String radioName;
    private List<String> radioStreams;
    private UUID id;
    private int mCurrentStreamIndex = 0;

    public Radio() {
        id = UUID.randomUUID();
    }

    public Radio(String radioName) {
        this();
        this.radioName = radioName;
        radioStreams = new ArrayList<String>();

    }

    public Radio(String radioName, String stream) {
        this(radioName);
        radioStreams.add(stream);
    }

    public String getRadioName() {
		return radioName;
	}

	public void setRadioName(String radioName) {
		this.radioName = radioName;
	}

	public void addRadioStream(String radioStreamUri) {
        radioStreams.add(radioStreamUri);
    }

    public void setRadioStreams(List<String> radioStreams) {
        this.radioStreams = radioStreams;
    }
    
    public void clearRadioStreams() {
    	radioStreams.clear();
    }

    public List<String> getAllRadioStreams() {
        return radioStreams;
    }

    public UUID getId() {
        return id;
    }
    
    
    /**
     * get first (or ones) uri of stream
     * in second call must return next stream if exists 
     * or the same
     * @return URI - String of uri 
     */
    public String getStream() {
    	if (mCurrentStreamIndex == radioStreams.size()) {
    		mCurrentStreamIndex = 0;
    	}
    	String stream = radioStreams.get(mCurrentStreamIndex);
    	mCurrentStreamIndex++;
    	return stream;
    }

    @Override
    public String toString() {
        return radioName;
    }

}
