package com.silent.telebot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListActivity extends Activity {
    private static final int REQUEST_VPN = 100;
    private Switch switchVpn;
    private RecyclerView recyclerApps;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        switchVpn = findViewById(R.id.switch_vpn);
        recyclerApps = findViewById(R.id.recycler_apps);
        recyclerApps.setLayoutManager(new LinearLayoutManager(this));

        // تحميل قائمة التطبيقات
        loadApps();

        // تفعيل VPN
        switchVpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startVpn();
            } else {
                stopVpn();
            }
        });
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        List<AppItem> appList = new ArrayList<>();

        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            // تجاهل تطبيقات النظام
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            String name = pm.getApplicationLabel(app).toString();
            String packageName = app.packageName;
            boolean blocked = false; // اقرأ من SharedPreferences لاحقاً

            appList.add(new AppItem(name, packageName, blocked, app.loadIcon(pm)));
        }

        adapter = new AppAdapter(this, appList);
        recyclerApps.setAdapter(adapter);
    }

    private void startVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN);
        } else {
            startVpnService();
        }
    }

    private void stopVpn() {
        stopService(new Intent(this, FirewallVpnService.class));
        Toast.makeText(this, "❌ تم إيقاف جدار الحماية", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN) {
            if (resultCode == RESULT_OK) {
                startVpnService();
            } else {
                switchVpn.setChecked(false);
                Toast.makeText(this, "❌ تم رفض صلاحية VPN", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startVpnService() {
        startService(new Intent(this, FirewallVpnService.class));
        Toast.makeText(this, "✅ جدار الحماية مفعل", Toast.LENGTH_SHORT).show();
    }
}
