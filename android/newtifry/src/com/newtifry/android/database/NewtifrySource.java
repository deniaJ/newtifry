/**
 * Newtifry for Android.
 * 
 * Copyright 2011 Daniel Foote
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newtifry.android.database;

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class NewtifrySource extends ORM<NewtifrySource>
{
	
	public static final NewtifrySource FACTORY = new NewtifrySource();

	private String accountName = null;
	private String changeTimestamp = null;
	private String title = null;
	private Long serverId = null;
	private String sourceKey = null;
	private Boolean serverEnabled = null;
	private Boolean localEnabled = null;
	private Boolean useGlobalNotification = true;
	private Boolean vibrate = false;
	private Boolean ringtone = false;
	private String customRingtone = "";
	private Boolean ledFlash = false;
	private Boolean speakMessage = false;
	
	/**
	 * Get the notification ID.
	 * This is the local source ID as an integer.
	 * @return
	 */
	public int getNotificationId()
	{
		// Yes, this casting will potentially lose precision. But unless
		// you've created a lot of local sources, you're unlikely to run
		// into it. If you run into this in production, please let me know.
		Long sourceId = this.getId();
		int notifyId = (int)(sourceId % Integer.MAX_VALUE);
		return notifyId;
	}

	public String getAccountName()
	{
		return accountName;
	}

	public void setAccountName( String accountName )
	{
		this.accountName = accountName;
	}

	public String getChangeTimestamp()
	{
		return changeTimestamp;
	}

	public void setChangeTimestamp( String changeTimestamp )
	{
		this.changeTimestamp = changeTimestamp;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle( String title )
	{
		this.title = title;
	}

	public Long getServerId()
	{
		return serverId;
	}

	public void setServerId( Long serverId )
	{
		this.serverId = serverId;
	}

	public String getSourceKey()
	{
		return sourceKey;
	}

	public void setSourceKey( String sourceKey )
	{
		this.sourceKey = sourceKey;
	}

	public Boolean getServerEnabled()
	{
		return serverEnabled;
	}

	public void setServerEnabled( Boolean serverEnabled )
	{
		this.serverEnabled = serverEnabled;
	}

	public Boolean getLocalEnabled()
	{
		return localEnabled;
	}

	public void setLocalEnabled( Boolean localEnabled )
	{
		this.localEnabled = localEnabled;
	}

	public Boolean getUseGlobalNotification()
	{
		return useGlobalNotification;
	}

	public void setUseGlobalNotification( Boolean useGlobalNotification )
	{
		this.useGlobalNotification = useGlobalNotification;
	}

	public Boolean getVibrate()
	{
		return vibrate;
	}

	public void setVibrate( Boolean vibrate )
	{
		this.vibrate = vibrate;
	}

	public Boolean getRingtone()
	{
		return ringtone;
	}

	public void setRingtone( Boolean ringtone )
	{
		this.ringtone = ringtone;
	}

	public String getCustomRingtone()
	{
		return customRingtone;
	}

	public void setCustomRingtone( String customRingtone )
	{
		this.customRingtone = customRingtone;
	}

	public Boolean getLedFlash()
	{
		return ledFlash;
	}

	public void setLedFlash( Boolean ledFlash )
	{
		this.ledFlash = ledFlash;
	}

	public Boolean getSpeakMessage()
	{
		return speakMessage;
	}

	public void setSpeakMessage( Boolean speakMessage )
	{
		this.speakMessage = speakMessage;
	}

	public void fromJSONObject( JSONObject source ) throws JSONException
	{
		this.changeTimestamp = source.getString("updated");
		this.title = source.getString("title");
		this.serverEnabled = source.getBoolean("enabled");
		this.sourceKey = source.getString("key");
		this.serverId = source.getLong("id");
	}
	
	public ArrayList<NewtifrySource> listAll( Context context, String accountName )
	{
		return NewtifrySource.FACTORY.genericList(context, NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?", new String[] { accountName }, NewtifryDatabaseAdapter.KEY_TITLE + " ASC");
	}
	
	public int countSources( Context context, String accountName )
	{
		String query = null;
		String[] queryParams = null;
		if( accountName != null )
		{
			query = NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?";
			queryParams = new String[] { accountName };
		}
		return this.genericCount(context, query, queryParams);
	}	
	
	public NewtifrySource getByServerId( Context context, Long serverId )
	{
		return NewtifrySource.FACTORY.getOne(context, NewtifryDatabaseAdapter.KEY_SERVER_ID + "=" + serverId, null);
	}
	
	public ArrayList<NewtifrySource> syncFromJSONArray( Context context, JSONArray sourceList, String accountName ) throws JSONException
	{
		ArrayList<NewtifrySource> result = new ArrayList<NewtifrySource>();
		HashSet<Long> seenIds = new HashSet<Long>();
		
		for( int i = 0; i < sourceList.length(); i++ )
		{
			// See if we can find a local object with that ID.
			JSONObject object = sourceList.getJSONObject(i);
			Long serverId = object.getLong("id");
			
			NewtifrySource source = NewtifrySource.FACTORY.getByServerId(context, serverId);
			
			if( source == null )
			{
				// We don't have that source locally. Create it.
				source = new NewtifrySource();
				source.fromJSONObject(object);
				// It's only locally enabled if the server has it enabled.
				source.setLocalEnabled(source.getServerEnabled());
				source.setAccountName(accountName);
			}
			else
			{
				// Server already has it. Assume the server is the most up to date version.
				source.fromJSONObject(object);
			}
			
			// Save it in the database.
			source.save(context);
			
			seenIds.add(source.getId());
		}
		
		// Now, find out the IDs that exist in our database but were not in our list.
		// Those have been deleted.
		ArrayList<NewtifrySource> allSources = NewtifrySource.FACTORY.listAll(context, accountName);
		HashSet<Long> allIds = new HashSet<Long>();
		for( NewtifrySource source: allSources )
		{
			allIds.add(source.getId());
		}
		
		allIds.removeAll(seenIds);

		for( Long sourceId: allIds )
		{
			NewtifrySource source = NewtifrySource.FACTORY.get(context, sourceId);
			NewtifryMessage.FACTORY.deleteMessagesBySource(context, source, false);
			source.delete(context);
		}

		return result;
	}

	@Override
	public Uri getContentUri()
	{
		return NewtifryDatabaseAdapter.CONTENT_URI_SOURCES;
	}

	@Override
	protected ContentValues flatten()
	{
		ContentValues values = new ContentValues();
		values.put(NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME, this.getAccountName());
		values.put(NewtifryDatabaseAdapter.KEY_SERVER_ENABLED, this.getServerEnabled() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_LOCAL_ENABLED, this.getLocalEnabled() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_TITLE, this.getTitle());
		values.put(NewtifryDatabaseAdapter.KEY_SERVER_ID, this.getServerId());
		values.put(NewtifryDatabaseAdapter.KEY_CHANGE_TIMESTAMP, this.getChangeTimestamp());
		values.put(NewtifryDatabaseAdapter.KEY_SOURCE_KEY, this.getSourceKey());
		values.put(NewtifryDatabaseAdapter.KEY_USE_GLOBAL_NOTIFICATION, this.getUseGlobalNotification() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_VIBRATE, this.getVibrate() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_RINGTONE, this.getRingtone() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_CUSTOM_RINGTONE, this.getCustomRingtone());
		values.put(NewtifryDatabaseAdapter.KEY_LED_FLASH, this.getLedFlash() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_SPEAK_MESSAGE, this.getSpeakMessage() ? 1 : 0);
		return values;
	}

	@Override
	protected NewtifrySource inflate( Context context, Cursor cursor )
	{
		NewtifrySource source = new NewtifrySource();
		source.setAccountName(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME)));
		source.setId(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_ID)));
		source.setServerEnabled(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_SERVER_ENABLED)) == 0 ? false : true);
		source.setLocalEnabled(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_LOCAL_ENABLED)) == 0 ? false : true);
		source.setServerId(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_SERVER_ID)));
		source.setTitle(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_TITLE)));
		source.setChangeTimestamp(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_CHANGE_TIMESTAMP)));
		source.setSourceKey(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_SOURCE_KEY)));
		
		source.setUseGlobalNotification(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_USE_GLOBAL_NOTIFICATION)) == 0 ? false : true);
		source.setVibrate(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_VIBRATE)) == 0 ? false : true);
		source.setRingtone(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_RINGTONE)) == 0 ? false : true);
		source.setLedFlash(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_LED_FLASH)) == 0 ? false : true);
		source.setCustomRingtone(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_CUSTOM_RINGTONE)));
		source.setSpeakMessage(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_SPEAK_MESSAGE)) == 0 ? false : true);
		
		return source;
	}

	@Override
	protected String[] getProjection()
	{
		return NewtifryDatabaseAdapter.SOURCE_PROJECTION;
	}
}
