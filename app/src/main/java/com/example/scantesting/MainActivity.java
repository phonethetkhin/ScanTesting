package com.example.scantesting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.scantesting.permission.PermissionHelper;
import com.example.scantesting.permission.PermissionInterface;

public class MainActivity extends AppCompatActivity implements PermissionInterface {
    private PermissionHelper permissionHelper;
    Context context;

    /**
     * return permission request code
     */
    @Override
    public int getPermissionsRequestCode() {
        return 1000;
    }

    /**
     * get permissions as String array[]
     * accessing camera, read external storage, write external storeage.
     */
    @Override
    public String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
    }

    /**
     * Called When All permissions success.
     */
    @Override
    public void requestPermissionsSuccess() {

    }

    /**
     * if permissions request fail, the application will close.
     */
    @Override
    public void requestPermissionFail() {
        this.finish();
    }

    /**
     * this is the return result of permissions request.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionHelper.requestPermissionsResult(requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        permissionHelper = new PermissionHelper(this, this);// instantiate the permission helper class
        permissionHelper.requestPermissions(); //calling the method from permissionhelper class.
        context = this;
        if (Pref.getScanSelfopenSupport(BaseApplication.getAppContext(), true)) {
            Intent service = new Intent(context, ScanService.class);
            context.startService(service);
        }

        Button btn = (Button) findViewById(R.id.btnScan);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, Scan.class);
                startActivity(intent);
            }
        });
    }

}
