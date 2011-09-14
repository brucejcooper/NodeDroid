package com.eightbitcloud.internode;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.AccountProvider;
import com.eightbitcloud.internode.data.Provider;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.provider.ProviderFetcher;
import com.eightbitcloud.internode.provider.WrongPasswordException;

public class AccountListActivity extends ListActivity {
    private  static final int USER_PASS_ID = 0;
    private ArrayAdapter<CharSequence> providerSpinnerAdapter;
    private Cursor accountCursor;
    private int editedRow = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.accountlist);
        
        accountCursor = getContentResolver().query(AccountProvider.ACCOUNTS_CONTENT_URI, null, null, null, Account.ID);
        
        setListAdapter(new SimpleCursorAdapter(this, R.layout.accountlistcell, accountCursor, new String[] {Account.PROVIDER, Account.USERNAME} , new int[] {R.id.providername, R.id.accountname}));
        
        findViewById(R.id.addaccountbutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (accountCursor.getCount() == 0) {
            createNewAccount();
        }
    }
    
    @Override
    protected void onStop() {
        // Tell the update service to refresh usage for the new accounts;
        startService(new Intent(this, UsageUpdateService.class));
        super.onStop();
    }
    
    private void createNewAccount() {
        editedRow = -1;
        showDialog(USER_PASS_ID);
        
    }
    
    public void deleteAccount(int index) {
        Cursor c = (Cursor) getListAdapter().getItem(index);
        int id = c.getInt(c.getColumnIndex(Account.ID));
        getContentResolver().delete(ContentUris.withAppendedId(AccountProvider.ACCOUNTS_CONTENT_URI, id), null, null);
    }
    
    
    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        Button deleteButton = (Button) dialog.findViewById(R.id.userdialog_delete);
        EditText usernameEditor= (EditText) dialog.findViewById(R.id.username_editor);
        EditText passwordEditor= (EditText) dialog.findViewById(R.id.password_editor);
        Spinner providerSpinner= (Spinner) dialog.findViewById(R.id.providerSpinner);
        if (editedRow >= 0) {
            Cursor c = (Cursor) getListAdapter().getItem(editedRow);
            Account account = new Account(c);
            usernameEditor.setText(account.getUsername());
            passwordEditor.setText(account.getPassword());
            
            providerSpinner.setSelection(providerSpinnerAdapter.getPosition(account.getProviderName().toLowerCase()));
            
            deleteButton.setEnabled(true);
        } else {
            deleteButton.setEnabled(false);
            usernameEditor.setText("");
            passwordEditor.setText("");
            
            providerSpinner.setSelection(0);
        }
        super.onPrepareDialog(id, dialog);
    }
    
    private void showConfirmDialog(String msg, final Runnable r) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccountListActivity.this);
        builder.setMessage(msg)
               .setCancelable(true)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   @Override
                public void onClick(DialogInterface confirmDialog, int id) {
                       confirmDialog.cancel();
                       r.run();
                   }
               }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                   @Override
                public void onClick(DialogInterface confirmDialog, int id) {
                       confirmDialog.cancel();
                  }
              });
        AlertDialog alert = builder.create();
        alert.show();

    }
    
    
    @Override
    public Dialog onCreateDialog(int id) {
        final Dialog dialog;
        
        switch (id) {
        case USER_PASS_ID:
            //Context mContext = getApplicationContext();
            dialog = new Dialog(this);

            dialog.setContentView(R.layout.userpass_dialog);
            dialog.setTitle(R.string.userpasstitle);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            final Spinner providerSpinner= (Spinner) dialog.findViewById(R.id.providerSpinner);
            providerSpinnerAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, ProviderStore.getInstance().getProviderNames());   
            providerSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            providerSpinner.setAdapter(providerSpinnerAdapter);

            
            Button saveButton = (Button) dialog.findViewById(R.id.userdialog_ok);
            saveButton.setOnClickListener(new View.OnClickListener() {
                
                public void performSave() {
                    EditText usernameEditor= (EditText) dialog.findViewById(R.id.username_editor);
                    EditText passwordEditor= (EditText) dialog.findViewById(R.id.password_editor);

                    String username = usernameEditor.getText().toString();
                    String password = passwordEditor.getText().toString();
                    
                    Account dummyAccount = new Account();
                    dummyAccount.setUsername(username);
                    dummyAccount.setPassword(password);
                    // TODO need separate IDs from text
                    Object sel = providerSpinner.getSelectedItem();
                    dummyAccount.setProviderName(sel.toString().toLowerCase());
                    new PasswordCheckerTask().execute(dummyAccount);

                }
                
                @Override
                public void onClick(View v) {
                    
                    String selectedProvider = (String) providerSpinner.getSelectedItem();
                    Provider provider = ProviderStore.getInstance().getProvider(selectedProvider);
                    
                    if (provider.isBeta()) {
                        showConfirmDialog("This provider is in beta, and may not work correctly.  Please only use this provider if you are willing to provide feedback to the author.  Continue?", new Runnable() {
                            @Override
                            public void run() {
                                performSave();
                            }
                        });
                    } else {
                        performSave();
                    }
                }
            });

            
            Button cancelButton = (Button) dialog.findViewById(R.id.userdialog_cancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissDialog(USER_PASS_ID);
                }
            });

            

            
            Button deleteButton = (Button) dialog.findViewById(R.id.userdialog_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showConfirmDialog("Are you sure you want to delete this account?", new Runnable() {
                        @Override
                        public void run() {
                            dismissDialog(USER_PASS_ID);
                            deleteAccount(editedRow);
                        }
                    });
                }
            });

            break;
        default:
            dialog = null;
        }
        
        return dialog;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        editedRow = position;
        showDialog(USER_PASS_ID);
    }

    
    class PasswordCheckerTask extends AsyncTask<Account, Void, Boolean> {
        ProgressDialog progressDialog;
        Account account;
        String errorMessage;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(AccountListActivity.this, "Logging in", "Please wait...", true);
            progressDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // TODO cancel the task
                }
            });
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Account... params) {
            account = params[0];
            try {
                ProviderFetcher f = account.getProvider().createFetcher(getApplicationContext());
                try {
                    f.testUsernameAndPassword(account);
                } finally {
                    f.cleanup();
                }
                
                return true;
            } catch (WrongPasswordException e) {
                errorMessage = "Username or password wrong";
                return false;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            
            progressDialog.dismiss();
            if (result) {
                // Add the account...
                if (account.getId() != 0) {
                    getContentResolver().update(account.getUri(), account.getValues(), null, null);
                } else {
                    getContentResolver().insert(AccountProvider.ACCOUNTS_CONTENT_URI, account.getValues());
                }

                String action = getIntent().getAction();
                if (action.equals(Intent.ACTION_INSERT)) {
                    AccountListActivity.this.setResult(RESULT_OK);
                    AccountListActivity.this.finish();
                }
                dismissDialog(USER_PASS_ID);
            } else {
                Toast.makeText(AccountListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        }

        
        @Override
        protected void onCancelled() {
            progressDialog.dismiss();
            super.onCancelled();
        }
        
    }
    
    
//    static class ViewHolder {
//        TextView nameView;
//        TextView providerView;
//        TextView accountView; 
//    }
}
