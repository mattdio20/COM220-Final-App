package com.example.android.com220finalapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;

public class EmergencyContact extends Activity {
    private static final int RESULT_PICK_CONTACT = 85500;
    private TextView textView1;
    private TextView textView2;
    private EditText textInput1;

    public boolean hasSMSPerms = false;
    public boolean hasLocPerms = false;
    public boolean hasContactPerms = false;

    LocationManager locationManager;
    private Location currentLocation;
    private double currentLat;
    private double currentLon;
    static String beginning = "http://maps.google.com/";

    // Stuff that needs to be stored in app data

    Friend savedContact = new Friend();
    String defaultMessage = "Could you come get me? I'm too drunk to drive. Here is my location: ";
    String savedMessage = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        // info popup about this part of the app
        // possibly skip this if this activity has been visited before in a session?
        infoAlertDialog();

        //create objs pointing to items on the UI
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textInput1 = (EditText) findViewById(R.id.message);

        // if savedmessage is empty, set default message
        if(savedMessage.equals(""))
            textInput1.setText(defaultMessage);
        else
            textInput1.setText(savedMessage);

        // general asking for permissions for each of the emergency contact features
        reqPermissions();

        // get current location to send to contact
        if(this.hasLocPerms)
        {
            FindLocation();
        }

        // eventlistener for 'pick contact' button
        Button buttonContact = (Button) findViewById(R.id.button1);
        buttonContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // just needs contact permission to work
                if(hasContactPerms)
                    pickContact(v);
                else {
                    ActivityCompat.requestPermissions(EmergencyContact.this, new String[]{Manifest.permission.READ_CONTACTS}, 7);
                    if(hasContactPerms)
                        pickContact(v);
                }
            }
        });

        // event listener for 'send sms' button
        Button buttonSMS= (Button) findViewById(R.id.button2);
        buttonSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // will need loc and sms permissions (and contacts permission indirectly)
                if(hasSMSPerms && hasLocPerms) {
                    BACTriggersSMS();
                }
                else {
                    reqPermissions();
                    if(hasSMSPerms && hasLocPerms) {
                        BACTriggersSMS();
                    }
                }
            }
        });

        // if newly input message isn't empty or the default message, save it!
        Button buttonSaveMsg = (Button) findViewById(R.id.button3);
        buttonSaveMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newMessage = textInput1.getText().toString();
                savedMessage = newMessage;
                Toast.makeText(EmergencyContact.this, "Message Saved!", Toast.LENGTH_SHORT).show();
                Log.d("msg", savedMessage);
            }
        });

        Button infoButton= (Button) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoAlertDialog();
            }
        });

    }

    // ************************************************************ //

    // METHODS TO BE CALLED BY OTHER ACTIVITIES

    //CHECKS FOR PERMISSIONS, THEN CALLS METHOD TO SEND SMS
    public void BACTriggersSMS()
    {
        if(hasSMSPerms && hasLocPerms) {
            sendEmergencySMS();
            //sendSMSToContact();
        }
        else {
            reqPermissions();
            if(hasSMSPerms && hasLocPerms) {
                sendEmergencySMS();
                //sendSMSToContact();
            }
        }
    }

    // ************************************************************ //

    // METHODS STRICTLY FOR THIS ACTIVITY

    public void pickContact(View v) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }


    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null;
            String name = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();

            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            // Set the value to the textviews

            String str = ""; // reformat phone number for Smsmanager
            for(int i = 0; i < phoneNo.length(); i++)
            {
                if(phoneNo.charAt(i) >='0' && phoneNo.charAt(i) <='9')
                {
                    str += phoneNo.charAt(i);
                }
            }

            textView1.setText(name);
            textView2.setText(str);

            Friend ec1 = new Friend(name,str);
            savedContact = ec1;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //USES SMS INTENT -- this works! use this!
    public void sendEmergencySMS()
    {
        String message = textInput1.getText().toString();
        message = message + "\n" + formURL("dir", currentLat, currentLon, 9 );
        String phoneNo = textView2.getText().toString();

        Log.d("msg", message+phoneNo);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNo));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }

    //USES SMSMANAGER -- doesn' want to work! ugh!
    private void sendSMSToContact()
    {
        SmsManager smsManager = SmsManager.getDefault();

        PendingIntent sentPI;
        String SENT = "SMS_SENT";

        sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);

        String message = textInput1.getText().toString();

        smsManager.sendTextMessage("+" + savedContact.getNum(),
                null,  message + " I'm at this location: " + formURL("dir", currentLat, currentLon, 9 ), sentPI, null);


    }



    private void reqPermissions()
    {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else
        {
            this.hasLocPerms = true;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        else
        {
            this.hasLocPerms = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 8 /*TODO get a real request code.  any number may not work*/);
        }
        else
        {
            this.hasSMSPerms = true;
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 7 /*TODO get a real request code.  any number may not work*/);
        }
        else
        {
            this.hasContactPerms = true;
        }
    }


    //Functionality Disabling Parameters
    // Send Message Feature: disabled w/o location and sms permissions
    // dependant on contact permission as well to store a phone number
    // Choose Contact Feature: disabled w/o contact permissions

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 8: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "SMS permission granted");
                    this.hasSMSPerms = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // will ask for permission when before sending message iff permission is false
                }
                return;
            }
            case 7:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Contact Read permission granted");
                    this.hasContactPerms = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Location permission granted");
                    this.hasLocPerms = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    // will ask for permission when before sending message iff permission is false

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }

    private String formURL(String rType, double lat, double lon, int zoom)
    {
        //new Schema
        //return beginning +"/" + rType + "/" + "My+Location/" + lat + "," + lon + "/@" + lat + "," + lon + "," + zoom + "z";
        //Old Schema
        return beginning + "?q=" + "@" + lat + "," + lon + "&zoom=9";
    }

    public void FindLocation() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener()
        {
            public void onLocationChanged(Location location)
            {
                Log.d("SiggiLoc", "Location Changed");
                currentLocation = location;
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
            }


            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };



        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Log.d("SiggiLoc", "Didn't have Location permissions");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }

    private void infoAlertDialog()
    {
        new AlertDialog.Builder(this)
                .setTitle("Set an Emergency Contact")
                .setMessage("Here you can set an message to be sent to one of your contacts " +
                        "in case you aren't fit a to drive. \n \n Thank you for being responsible!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(null,null)
                .setIcon(null)
                .show();

    }

    public class Friend implements Serializable
    {
        private String name = "";
        /**
         * Should be in the format "12225551234", as that's what the SMSManager takes
         */
        private String num = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNum() {
            return num;
        }

        public void setNum(String num) {
            this.num = num;
        }

        private Friend()
        {

        }

        private Friend(String name, String num)
        {
            this.name = name;
            this.num = num;
        }

        @Override
        public String toString() {
            return this.name + ": " + this.num;
        }

    }


}