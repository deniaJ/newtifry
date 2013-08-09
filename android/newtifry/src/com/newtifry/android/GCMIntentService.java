package com.newtifry.android;

import org.apache.http.ParseException;

//import com.newtifry.android.app.database.NewtifryAccount;
import com.newtifry.android.database.NewtifryAccount;
import com.newtifry.android.database.NewtifryMessage;
import static com.newtifry.android.CommonUtilities.SENDER_ID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
		// Dispatch this to the updater service.
		Intent intentData = new Intent(getBaseContext(), UpdaterService.class);
		intentData.putExtra("type", "registration");
		intentData.putExtra("registration", registrationId);
		startService(intentData);

		// Clear any errors we had.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = settings.edit();
		editor.putString("dm_register_error", "");
		editor.commit();

		// Update the home screen.
		Intent updateUIIntent = new Intent(Newtifry.UPDATE_INTENT);
		context.sendBroadcast(updateUIIntent);        
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");
		Bundle extras = intent.getExtras();
		
		// The server would have sent a message type.
		String type = extras.getString("type");
        Log.i(TAG, "message type " + type);
		
		if( type.equals("message") )
		{
			// Fetch the message out into a NewtifryMessage object.
			try
			{
				NewtifryMessage message = NewtifryMessage.fromGCM(context, extras);
				
				Log.d(TAG, "We've been newtifried! " + message.getMessage());
				
				// Persist this message to the database.
				message.save(context);
				
				// Send a notification to the notification service, which will then
				// dispatch and handle everything else.
				Intent intentData = new Intent(getBaseContext(), NewtificationService.class);
				intentData.putExtra("messageId", message.getId());
				intentData.putExtra("operation", "newtifry");
				startService(intentData);
			}
			catch( ParseException ex )
			{
				// Failed to parse a Long.
				Log.e(TAG, "Failed to parse a long - malformed message from server: " + ex.getMessage());
			}
			catch( NewtifryMessage.UnsourceableMessage ex )
			{
				// Hmm... a message there was no way to find a source for.
				// Don't do anything - but do log it.
				Long accountId = Long.parseLong(extras.getString("device_id"));
				Long sourceId = Long.parseLong(extras.getString("source_id"));
				Log.d(TAG, "Unsourceable message: source ID " + sourceId + " device ID " + accountId);
			}
		}
		else if( type.equals("refreshall") )
		{
			// Server says to refresh our list when we can. Typically means that
			// a source has been deleted. Make a note of it.

 			Long serverAccountId = Long.parseLong(extras.getString("device_id"));

 			NewtifryAccount account = NewtifryAccount.FACTORY.getByServerId(context, serverAccountId);
			
			// Assuming it was found...
			if( account != null )
			{
				account.setRequiresSync(true);
				account.save(context);
			}
			
			Log.d(TAG, "Server just asked us to refresh sources list - usually due to deletion.");
		}
		else if( type.equals("sourcechange") )
		{
			// Server says that a source has been created or updated.
			// We should pull a copy of it locally.
			Long serverSourceId = Long.parseLong(extras.getString("id"));
			Long serverDeviceId = Long.parseLong(extras.getString("device_id"));
			
			Intent intentData = new Intent(getBaseContext(), UpdaterService.class);
			intentData.putExtra("type", "sourcechange");
			intentData.putExtra("sourceId", serverSourceId);
			intentData.putExtra("deviceId", serverDeviceId);
			startService(intentData);
			
			Log.d(TAG, "Server just asked us to update/create server source ID " + serverSourceId + " for server account ID " + serverDeviceId);
		}
		else if( type.equals("devicedelete") )
		{
			// Server says we've been deregistered. We should now clear our registration.
			Long deviceId = Long.parseLong(extras.getString("device_id"));
			NewtifryAccount account = NewtifryAccount.FACTORY.getByServerId(context, deviceId);
			
			// Check if it's NULL - it's possible we have a desync!
			if( account != null )
			{
				// Disable it, and clear the registration ID.
				account.setEnabled(false);
				account.setServerRegistrationId(null);
				account.setRequiresSync(true);
				
				// Save it back to the database.
				account.save(context);
			}
			
			Log.d(TAG, "Server just asked us to deregister! And should be done now.");
		}
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
    }

    @Override
    public void onError(Context context, String errorId) {
		Log.e("Newtifry", "Error with registration: " + errorId);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = settings.edit();
		editor.putString("dm_register_error", errorId);
		editor.commit();
		// Notify the user.
		// TODO: Do this.
		// Update the home screen.
		Intent updateUIIntent = new Intent(Newtifry.UPDATE_INTENT);
		context.sendBroadcast(updateUIIntent);        
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        return super.onRecoverableError(context, errorId);
    }
}



