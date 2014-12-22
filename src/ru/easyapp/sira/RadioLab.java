package ru.easyapp.sira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

/**
 * Singleton for storing data (all radio object) in one place.
 * Use {@code static RadioLab get(Context)} for getting an instance of
 * this singleton object.
 * Created by user on 02/09/14.
 */
public class RadioLab {
	private static final String FILE_DB = "siraradiosdb.txt";
	public static final String LOG_TAG = "RadioLab";
    private static RadioLab sRadioLab;
    private List<Radio> mRadios;
    private Context appContext;
    private ArrayList<StorageChangeListener> storageChangeListeners = new ArrayList<StorageChangeListener>();
    private File db;
    
    private RadioLab(Context appContext) {
        this.appContext = appContext;
        mRadios = new ArrayList<Radio>();
        
        /*
         * проверить есть ли файл на диске для списка
         */
        	db = new File(appContext.getFilesDir(), FILE_DB);
        	if (!db.exists()) {
        		Log.d(LOG_TAG, "File not found on internal sd");
        		inflateListOfRadiosFromRecourceXML();
        		saveList();
        	} else {
        		Log.d(LOG_TAG, "DB file exists on sd.");
        		inflateListOfRadiosFromTxtFile();
        	}
    }

    public static RadioLab get(Context context) {
        if(sRadioLab == null) {
            sRadioLab = new RadioLab(context.getApplicationContext());
        }
        return sRadioLab;
    }

    public List<Radio> getRadios() {
        return mRadios;
    }

    public Radio getRadio(UUID id) {
    	Radio result = null;
    	for(Radio r : mRadios) {
    		if(r.getId().equals(id)) {
    			result = r;
    		}
    	}
    	return result;
    }
    
    public void addRadio(Radio r) {
    	mRadios.add(r);
    	// notify that lab changed
    	storageChanged();
    	saveList();
    }
    
    public void deleteRadio(Radio r) {
    	mRadios.remove(r);
    	storageChanged();
    	saveList();
    }
    
    public void editRadio(Radio r, String name, String url) {
    	r.setRadioName(name);
    	r.clearRadioStreams();
    	r.addRadioStream(url);
    	storageChanged();
    	saveList();
    }
    
    private void storageChanged() {
    	for(StorageChangeListener listener : storageChangeListeners) {
    		listener.onStorageChanged();
    	}
    }
    
    public void addOnStorageChangeListener(StorageChangeListener listener) {
    	storageChangeListeners.add(listener);
    }
    
    public void removeOnStorageChangeListener(StorageChangeListener listener) {
    	storageChangeListeners.remove(listener);
    }

    /**
     * This method loads all radio stations from xml file with 
     * proper tag-scheme.
     * For first start, when we don't have db-file on flash with
     * user's stations.
     */
    private void inflateListOfRadiosFromRecourceXML() {
        Resources res = appContext.getResources();
        XmlResourceParser xrp = res.getXml(R.xml.radios);
        try {
            xrp.next();
            int event = xrp.getEventType();
            String name = null;
            String radioName = null;
            List<String> streams = null;
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG :
                        name = xrp.getName();
                        switch (name) {
                            case "radio" :
                                radioName = xrp.getAttributeValue(null, "name");
                                streams = new ArrayList<String>();
                                break;
                            case "stream" :
                                streams.add(xrp.getAttributeValue(null, "url"));
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG :
                        name = xrp.getName();
                        if (name.equals("radio") && radioName != null) {
                            Radio r = new Radio(radioName);
                            r.setRadioStreams(streams);
                            mRadios.add(r);
                        }
                        break;
                }
                event = xrp.next();
            }
        } catch(Exception e){e.printStackTrace();}
    }
    
    /**
     * This method loads all radio stations from txt file with 
     * ';'-separator. 
     * name;url -- scheme
     */
    private void inflateListOfRadiosFromTxtFile() {
    	Log.d(LOG_TAG, "inflateListFromTxtFile: trying to load.");
    	// читаем из файла по строке
    	// сплитим и делаем радио-объект,
    	// который добавляем в хранилище
    	try {
    		FileReader file = new FileReader(db);
    		BufferedReader in = new BufferedReader(file);
    		String line = null;
    		String[] radioAttributes = null;
    		while((line = in.readLine()) != null) {
    			radioAttributes = line.split(";");
    			mRadios.add(new Radio(radioAttributes[0], radioAttributes[1]));
    		}
    		in.close();
        	Log.d(LOG_TAG, "inflateListFromTxtFile: loading success.");
    	} catch (FileNotFoundException e) {
    		Log.d(LOG_TAG, "inflateListFromTxtFile: FileNotFoundException. Problems with BD file.");
    	} catch (IOException e) {
    		Log.d(LOG_TAG, "inflateListFromTxtFile: IO Exception. Problems while reading db file.");
    	}
    }
    
    /**
     * Save this list to file
     */
    private void saveList() {
    	// saving list to file in another thread to
    	// avoid block UI Thread 
    	new Thread(
    			new Runnable() {
    				public void run() {
    					Log.d(LOG_TAG, "Trying to save...");
    			    	try {
    			    		db.createNewFile();
    			    		PrintWriter out =  new PrintWriter(db);
    			        	//save list to file
    			        	for (Radio r : mRadios) {
    			        		String radioString = r.getRadioName() + ";" + r.getStream();
    			        		out.println(radioString);
    			        	}
    			        	out.close();
    			    	} catch (FileNotFoundException e ) {
    			    		Log.d(LOG_TAG, "Some problems with creating db file. Didn't save a list.");
    			    	} catch (IOException e) {
    			    		Log.d(LOG_TAG, "Can't create a new file.");
    			    		e.printStackTrace();
    			    	}
    				}
    			}
    			).start();
    }
}

