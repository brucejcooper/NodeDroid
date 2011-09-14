package com.eightbitcloud.internode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class NoAccountsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.noaccounts);
        
        Button addButton = (Button) findViewById(R.id.addaccountbutton);
        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addFirstAccount();
            }
        });

    }
    
    public void addFirstAccount() {
        Intent intent = new Intent(NoAccountsActivity.this, AccountListActivity.class).setAction(Intent.ACTION_INSERT);
        startActivityForResult(intent, 0);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the resultCode on to the caller.
        setResult(resultCode);
        finish();
    }

}
