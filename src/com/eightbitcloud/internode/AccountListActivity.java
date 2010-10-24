package com.eightbitcloud.internode;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;

import com.eightbitcloud.internode.data.Account;
import com.eightbitcloud.internode.data.ProviderStore;
import com.eightbitcloud.internode.data.Service;
import com.eightbitcloud.internode.provider.WrongPasswordException;

public class AccountListActivity extends ListActivity {
    private LayoutInflater mInflater;
    private  static final int USER_PASS_ID = 0;
    private List<Account> accounts = new ArrayList<Account>();
    
    private int editedRow = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(this);
        
        setContentView(R.layout.accountlist);
        
        setListAdapter(new BaseAdapter() {
            public int getCount() {
                return accounts.size();
            }

            public Account getItem(int position) {
                return accounts.get(position);
            }

            public long getItemId(int position) {
                return position;
            }
            
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.accountlistcell, null);

                    // Creates a ViewHolder and store references to the two children views
                    // we want to bind data to.
                    holder = new ViewHolder();
                    holder.nameView = (TextView) convertView.findViewById(R.id.accountname);
                    holder.accountView = (TextView) convertView.findViewById(R.id.accountnumber);
                    convertView.setTag(holder);
                } else {
                    // Get the ViewHolder back to get fast access to the TextView
                    // and the ImageView.
                    holder = (ViewHolder) convertView.getTag();
                }
                Account account = getItem(position);
                holder.nameView.setText(account.getUsername());
                StringBuilder sb = new StringBuilder();
                for (Service service: account.getAllServices()) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(service.getIdentifier());
                }
                holder.accountView.setText(sb.toString());
                
                return convertView;
            }
        });
        
        findViewById(R.id.addaccountbutton).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                createNewAccount();
            }
        });
        
        
        

    }
    
    
    private SharedPreferences getAccountPreferences() {
        return getApplication().getSharedPreferences(PreferencesSerialiser.PREFS_FILE, Application.MODE_PRIVATE);
    }
    

    
    @Override
    public void onStart() {
        super.onStart();
        PreferencesSerialiser.deserialise(getAccountPreferences(), accounts);
        Log.d(NodeUsage.TAG, "Starting accountlist, accts is " + accounts + ", prefs = " + getAccountPreferences().getAll());
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        if (accounts.isEmpty()) {
            createNewAccount();
        }
    }
    
    private void createNewAccount() {
        editedRow = -1;
        showDialog(USER_PASS_ID);
        
    }
    
    public void deleteAccount(int index) {
        accounts.remove(index);
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();

    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        PreferencesSerialiser.serialise(accounts, getAccountPreferences());
        Log.d(NodeUsage.TAG, "Pausing accountlist, accts is " + accounts + ", prefs = " + getAccountPreferences().getAll());
    }
    
    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        Button deleteButton = (Button) dialog.findViewById(R.id.userdialog_delete);
        if (editedRow >= 0) {
            Account account = (Account) getListAdapter().getItem(editedRow);
            EditText usernameEditor= (EditText) dialog.findViewById(R.id.username_editor);
            EditText passwordEditor= (EditText) dialog.findViewById(R.id.password_editor);
            usernameEditor.setText(account.getUsername());
            passwordEditor.setText(account.getPassword());
            
            deleteButton.setEnabled(true);
        } else {
            deleteButton.setEnabled(false);
        }
        super.onPrepareDialog(id, dialog);
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
            ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.providers, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            providerSpinner.setAdapter(adapter);

            
            Button saveButton = (Button) dialog.findViewById(R.id.userdialog_ok);
            saveButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    EditText usernameEditor= (EditText) dialog.findViewById(R.id.username_editor);
                    EditText passwordEditor= (EditText) dialog.findViewById(R.id.password_editor);

                    String username = usernameEditor.getText().toString();
                    String password = passwordEditor.getText().toString();
                    
                    Account dummyAccount = new Account();
                    dummyAccount.setUsername(username);
                    dummyAccount.setPassword(password);
                    // TODO need separate IDs from text
                    Object sel = providerSpinner.getSelectedItem();
                    dummyAccount.setProvider(ProviderStore.getInstance().getProvider(sel.toString().toLowerCase()));
                    new PasswordCheckerTask().execute(dummyAccount);
                    
                }
            });

            
            Button cancelButton = (Button) dialog.findViewById(R.id.userdialog_cancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.cancel();
                }
            });

            

            
            Button deleteButton = (Button) dialog.findViewById(R.id.userdialog_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AccountListActivity.this);
                    builder.setMessage("Are you sure you want to delete this account?")
                           .setCancelable(true)
                           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface confirmDialog, int id) {
                                   confirmDialog.cancel();
                                   dialog.cancel();
                                   deleteAccount(editedRow);
                               }
                           }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface confirmDialog, int id) {
                                   confirmDialog.cancel();
                              }
                          });
                    AlertDialog alert = builder.create();
                    alert.show();
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
                account.getProvider().createFetcher().testUsernameAndPassword(account);
                return true;
            } catch (WrongPasswordException e) {
                errorMessage = "Username or password wrong";
                return false;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                errorMessage = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Log.d(NodeUsage.TAG, "Result is " + result);
            
            progressDialog.dismiss();
            if (result) {
                // Add the account...
                if (editedRow != -1) {
                    accounts.set(editedRow, account);
                } else {
                    accounts.add(account);
                }
                ((BaseAdapter)getListAdapter()).notifyDataSetChanged();


                String action = getIntent().getAction();
                if (action.equals(Intent.ACTION_INSERT)) {
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
    
    
    static class ViewHolder {
        TextView nameView;
        TextView accountView; 
    }
}
