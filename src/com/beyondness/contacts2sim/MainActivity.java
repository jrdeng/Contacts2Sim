package com.beyondness.contacts2sim;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity {
    private static final String TAG = "C2S";
    private ListView mListView;
    private Uri mSimUri = Uri.parse("content://icc/adn/");
    private Uri mContactUri = ContactsContract.Contacts.CONTENT_URI;
    private ContentResolver mCR;

    private ArrayList<String> mPhoneContacts = new ArrayList<String>();

    private static final int MSG_READ_CONTACTS = 0;
    private static final int MSG_CLEAR_SIM_CONTACTS = 1;
    private static final int MSG_EXPORT_TO_SIM = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_READ_CONTACTS:
                readPhoneContacts();
                break;

            case MSG_CLEAR_SIM_CONTACTS:
                clearSimContacts();
                readSimContacts();
                break;

            case MSG_EXPORT_TO_SIM:
                exportContactsToSim();
                readSimContacts();
                break;

            default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCR = getContentResolver();

        mListView = (ListView) findViewById(R.id.list);
        
        Button seeButton = (Button) findViewById(R.id.see);
        seeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                readSimContacts();
            }
        });

        
        
        Button clearButton = (Button) findViewById(R.id.clear);
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mHandler.sendEmptyMessage(MSG_CLEAR_SIM_CONTACTS);
            }
        });

        Button insertButton = (Button) findViewById(R.id.insert);
        insertButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mHandler.sendEmptyMessage(MSG_EXPORT_TO_SIM);
            }
        });

        mHandler.sendEmptyMessageDelayed(MSG_READ_CONTACTS, 500);
    }

    protected void exportContactsToSim() {
        for (String contact : mPhoneContacts) {
            String[] pair = contact.split(":", 2);
            String name = pair[0];
            String number = pair[1];
            insertToSim(name, number);
        }
    }

    private void insertToSim(String name, String number) {
        Log.d(TAG, "inserting..." + name + ":" + number);
        ContentValues values = new ContentValues();
        values.put("tag", name);
        values.put("number", number);
        mCR.insert(mSimUri, values);
    }

    private void clearSimContacts() {
        Cursor c = this.getContentResolver().query(mSimUri, null, null, null, null);
        List<String> contacts = new ArrayList<String>();
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndex("name"));
            String number = c.getString(c.getColumnIndex("number"));
            contacts.add(name + ":" + number);
        }
        c.close();
        for (String contact : contacts) {
            Log.d(TAG, "deleting..." + contact);
            String[] pair = contact.split(":", 2);
            String name = pair[0].trim();
            String number = pair[1].trim();
            mCR.delete(mSimUri, "number=" + number, null);
        }
    }

    private void readSimContacts() {
        Cursor c = this.getContentResolver().query(mSimUri, null, null, null, null);
        String b = "";
        while (c.moveToNext()) {
            b += (c.getString(c.getColumnIndex("_id"))) + "\n";
            b += (c.getString(c.getColumnIndex("name"))) + "\n";
            b += (c.getString(c.getColumnIndex("number"))) + "\n";
        }
        c.close();

        Log.d(TAG, "----------------------\n" + b);
    }

    private void readPhoneContacts() {
        mPhoneContacts.clear();
        ArrayList<String> tmpNameList = new ArrayList<String>();
        Cursor c = this.getContentResolver().query(mContactUri, null, null, null, null);
        while (c.moveToNext()) {
            String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));

            if (Integer.parseInt(c.getString(c
                    .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                // has numbers
                Cursor pCur = mCR.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[] { id }, null);
                while (pCur.moveToNext()) {
                    /* specify account name....
                    int anid = pCur.getColumnIndex("account_name");
                    if (anid >= 0) {
                        String accountName = pCur.getString(anid);
                        if (!accountName.equals("junren.deng@gmail.com")) {
                            continue;
                        }
                    }
                    */
                    String name = pCur.getString(pCur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = pCur.getString(pCur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    name = name.trim();
                    String tmpName = name;
                    int nIndex = 2;
                    while (tmpNameList.contains(name)) {
                        name = tmpName + nIndex; // append 2/3...to avoid same name
                        nIndex++;
                    }
                    tmpNameList.add(name);
                    number = number.trim();
                    number = number.replace("-", "");
                    number = number.replace(" ", "");
                    if (number.startsWith("+86")) {
                        number = number.substring(3);
                    }
                    if (number.isEmpty()) {
                        continue;
                    }
                    Log.d(TAG, "CONTACT: " + name + " (" + number + ")");
                    mPhoneContacts.add(name + ":" + number);
                }
                pCur.close();
            }
        }
        c.close();

        Log.d(TAG, "CONTACT: count=" + mPhoneContacts.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
