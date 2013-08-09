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
import java.util.HashMap;
import java.util.HashSet;

import com.newtifry.android.remote.BackendRequest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

public class NewtifryAccount extends ORM<NewtifryAccount>
{
	public final static NewtifryAccount FACTORY = new NewtifryAccount();
	
	private String accountName = null;
	private Long serverRegistrationId = null;
	private Boolean enabled = null;
	private Boolean requiresSync = true;
	private String lastC2DMId = null;

	public String getAccountName()
	{
		return accountName;
	}

	public void setAccountName( String accountName )
	{
		this.accountName = accountName;
	}

	public Boolean getEnabled()
	{
		return enabled;
	}

	public void setEnabled( Boolean enabled )
	{
		this.enabled = enabled;
	}

	public void setServerRegistrationId( Long serverRegistrationId )
	{
		this.serverRegistrationId = serverRegistrationId;
	}

	public Long getServerRegistrationId()
	{
		return serverRegistrationId;
	}
	
	public Boolean getRequiresSync()
	{
		return requiresSync;
	}

	public void setRequiresSync( Boolean requiresSync )
	{
		this.requiresSync = requiresSync;
	}

	public String getLastC2DMId()
	{
		return lastC2DMId;
	}

	public void setLastC2DMId( String lastC2DMId )
	{
		this.lastC2DMId = lastC2DMId;
	}

	/**
	 * Register the device with the server.
	 * @param context
	 * @param key
	 * @param showStatus
	 */
	public void registerWithBackend( Context context, String key, boolean register, String statusMessage, Handler handler, HashMap<String, Object> metadata )
	{
		// Register the device with the server.
		BackendRequest request;
		if( register )
		{
			request = new BackendRequest("/devices/register");
		}
		else
		{
			request = new BackendRequest("/devices/deregister");
		}
		request.add("devicekey", key);
		request.add("devicetype", "android");
		try
		{
			request.add("deviceversion", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
		}
		catch( NameNotFoundException e )
		{
			request.add("deviceversion", "Unknown");
		}
		
		// Send something so we know what the device is.
		request.add("nickname", Build.MODEL);

		// If already registered, update the same entry.
		if( this.getServerRegistrationId() != null )
		{
			request.add("id", this.getServerRegistrationId().toString());
		}

		if( register )
		{
			request.add("operation", "add");
		}
		else
		{
			request.add("operation", "remove");
		}

		
		// And the callback handler, if required.
		request.setHandler(handler);
		
		// Add any metadata if required.
		if( metadata != null )
		{
			for( String metaKey: metadata.keySet() )
			{
				request.addMeta(metaKey, metadata.get(metaKey));
			}
		}		
		
		// For debugging, dump the request data.
		request.dumpRequest();
		// Start a thread to make the request.
		request.startInThread(context, statusMessage, this.getAccountName());
	}
	
	public ArrayList<NewtifryAccount> listAll( Context context )
	{
		return NewtifryAccount.FACTORY.genericList(context, null, null, NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME + " ASC");
	}	
	
	public NewtifryAccount getByAccountName( Context context, String accountName )
	{
		return this.getOne(context, NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME + "=?", new String[] { accountName });
	}
	
	public NewtifryAccount getByServerId( Context context, Long serverId )
	{
		return this.getOne(context, NewtifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID + "=" + serverId, null);
	}
	
	public void deleteByAccountName( Context context, String accountName )
	{
		this.genericDelete(context, NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?", new String[] { accountName });
	}
	
	/**
	 * Sync the account list with our own copy, adding new ones as needed.
	 * @param accountManager
	 */
	public void syncAccountList( Context context, AccountManager accountManager )
	{
		Account[] accounts = accountManager.getAccountsByType("com.google");
		
		HashSet<String> seenAccounts = new HashSet<String>();
		
		for( int i = 0; i < accounts.length; i++ )
		{
			NewtifryAccount account = NewtifryAccount.FACTORY.getByAccountName(context, accounts[i].name);
			
			if( account == null )
			{
				// Can't find it. Create one.
				account = new NewtifryAccount();
				account.setEnabled(false); // Disabled by default.
				account.setAccountName(accounts[i].name);
				account.save(context);
			}
			
			seenAccounts.add(accounts[i].name);
		}
		
		// List all accounts, and add them to a list of accounts in the database.
		ArrayList<NewtifryAccount> allAccounts = NewtifryAccount.FACTORY.listAll(context);
		HashSet<String> localAccounts = new HashSet<String>();
		
		for( NewtifryAccount account: allAccounts )
		{
			localAccounts.add(account.getAccountName());
		}
		
		// Intersect the sets, and remove any accounts as appropriate.
		localAccounts.removeAll(seenAccounts);
		
		// Now remove anything in local accounts that should not be there.
		for( String accountName: localAccounts )
		{
			NewtifryAccount.FACTORY.deleteByAccountName(context, accountName);
		}
		
		// And we're finally complete!
	}	

	@Override
	public Uri getContentUri()
	{
		return NewtifryDatabaseAdapter.CONTENT_URI_ACCOUNTS;
	}

	@Override
	protected ContentValues flatten()
	{
		ContentValues values = new ContentValues();
		values.put(NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME, this.getAccountName());
		values.put(NewtifryDatabaseAdapter.KEY_ENABLED, this.getEnabled() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID, this.getServerRegistrationId());
		values.put(NewtifryDatabaseAdapter.KEY_REQUIRES_SYNC, this.getRequiresSync() ? 1 : 0);
		values.put(NewtifryDatabaseAdapter.KEY_LAST_C2DM_ID, this.getLastC2DMId());

		return values;
	}

	@Override
	protected NewtifryAccount inflate( Context context, Cursor cursor )
	{
		NewtifryAccount account = new NewtifryAccount();
		account = new NewtifryAccount();
		account.setAccountName(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_ACCOUNT_NAME)));
		account.setId(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_ID)));
		account.setEnabled(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_ENABLED)) == 0 ? false : true);
		account.setServerRegistrationId(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID)));
		account.setRequiresSync(cursor.getLong(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_REQUIRES_SYNC)) == 0 ? false : true);
		account.setLastC2DMId(cursor.getString(cursor.getColumnIndex(NewtifryDatabaseAdapter.KEY_LAST_C2DM_ID)));
		
		if( account.getServerRegistrationId() == 0 )
		{
			account.setServerRegistrationId(null);
		}
		return account;
	}

	@Override
	protected String[] getProjection()
	{
		return NewtifryDatabaseAdapter.ACCOUNT_PROJECTION;
	}
}
