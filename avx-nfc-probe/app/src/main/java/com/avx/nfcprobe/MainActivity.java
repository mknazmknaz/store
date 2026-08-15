package com.avx.nfcprobe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Arrays;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
    private TextView status, count, modeText, cardInfo, log;
    private NfcAdapter nfcAdapter;
    private boolean cardScanMode = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        setContentView(buildUi());
        ProbeStore.append(this, "App opened");
        refresh();
    }

    @Override protected void onResume() { super.onResume(); applyMode(); }

    @Override protected void onPause() {
        disableReaderMode();
        unsetPreferredHce();
        super.onPause();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(ProbeStore.ACTION_UPDATED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
    }

    @Override protected void onStop() { super.onStop(); unregisterReceiver(receiver); }

    @Override public void onTagDiscovered(Tag tag) {
        String uid = toHex(tag.getId());
        String techs = Arrays.toString(tag.getTechList());
        ProbeStore.setCardInfo(this, "UID: " + uid + "\nTech: " + techs);
        ProbeStore.append(this, "CARD SCANNED UID=" + uid + " TECH=" + techs);
    }

    private void setCardScanMode(boolean enabled) {
        cardScanMode = enabled;
        ProbeStore.append(this, enabled ? "Mode -> CARD INSPECTOR" : "Mode -> HCE READER PROBE");
        applyMode();
        refresh();
    }

    private void applyMode() {
        if (cardScanMode) {
            unsetPreferredHce();
            enableReaderMode();
        } else {
            disableReaderMode();
            setPreferredHce();
        }
    }

    private void enableReaderMode() {
        if (nfcAdapter == null) return;
        try {
            int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
            nfcAdapter.enableReaderMode(this, this, flags, null);
        } catch (Exception e) {
            ProbeStore.append(this, "Reader mode error: " + e.getClass().getSimpleName());
        }
    }

    private void disableReaderMode() {
        if (nfcAdapter == null) return;
        try { nfcAdapter.disableReaderMode(this); } catch (Exception ignored) {}
    }

    private void setPreferredHce() {
        try {
            if (nfcAdapter != null) {
                CardEmulation ce = CardEmulation.getInstance(nfcAdapter);
                boolean ok = ce.setPreferredService(this, new ComponentName(this, ProbeApduService.class));
                ProbeStore.append(this, "Foreground HCE preference: " + (ok ? "ACTIVE" : "NOT SET"));
            }
        } catch (Exception e) {
            ProbeStore.append(this, "Foreground HCE preference error: " + e.getClass().getSimpleName());
        }
    }

    private void unsetPreferredHce() {
        try { if (nfcAdapter != null) CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this); }
        catch (Exception ignored) {}
    }

    private View buildUi() {
        int pad = dp(18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        root.addView(text("AVX NFC Probe", 26, true));
        TextView subtitle = text("Card inspector + HCE / ISO-DEP APDU detector", 14, false);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        status = text("", 16, true); root.addView(status);
        modeText = text("", 15, true); modeText.setPadding(0, dp(4), 0, dp(4)); root.addView(modeText);
        count = text("", 15, false); count.setPadding(0, 0, 0, dp(10)); root.addView(count);

        LinearLayout modeButtons = new LinearLayout(this);
        modeButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button inspect = new Button(this); inspect.setText("Scan Card"); inspect.setOnClickListener(v -> setCardScanMode(true));
        modeButtons.addView(inspect, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button probe = new Button(this); probe.setText("Probe Reader"); probe.setOnClickListener(v -> setCardScanMode(false));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(52), 1); pp.setMarginStart(dp(8)); modeButtons.addView(probe, pp);
        root.addView(modeButtons);

        cardInfo = text("No physical card scanned in this app yet.", 13, false);
        cardInfo.setTypeface(Typeface.MONOSPACE); cardInfo.setTextIsSelectable(true); cardInfo.setPadding(dp(10), dp(10), dp(10), dp(10)); cardInfo.setBackgroundColor(0xFFF5F5F5);
        LinearLayout.LayoutParams ci = new LinearLayout.LayoutParams(-1, -2); ci.setMargins(0, dp(10), 0, dp(10)); root.addView(cardInfo, ci);

        TextView instructions = text("ONE-APP TEST\n1. NFC ON. Tap Scan Card and scan your authorised physical card.\n2. Tap Probe Reader. Keep phone unlocked.\n3. Tap Falco, then repeat on Micro ID.\n4. If APDU count increases, screenshot/copy the event log.\n\nThis app only inspects card metadata and probes HCE/APDU compatibility. It does not duplicate an access credential or unlock a door/lift.", 14, false);
        instructions.setPadding(0, 0, 0, dp(10)); root.addView(instructions);

        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button nfcSettings = new Button(this); nfcSettings.setText("NFC Settings");
        nfcSettings.setOnClickListener(v -> { try { startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); } });
        buttons.addView(nfcSettings, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button clear = new Button(this); clear.setText("Clear Log"); clear.setOnClickListener(v -> ProbeStore.clearLog(this));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(52), 1); cp.setMarginStart(dp(8)); buttons.addView(clear, cp); root.addView(buttons);

        TextView logTitle = text("Event log", 16, true); logTitle.setPadding(0, dp(14), 0, dp(6)); root.addView(logTitle);
        log = text("", 13, false); log.setTypeface(Typeface.MONOSPACE); log.setTextIsSelectable(true); log.setMovementMethod(new ScrollingMovementMethod()); log.setPadding(dp(10), dp(10), dp(10), dp(10)); log.setBackgroundColor(0xFFF1F1F1);
        ScrollView scroll = new ScrollView(this); scroll.addView(log); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private void refresh() {
        boolean nfc = nfcAdapter != null;
        boolean enabled = nfc && nfcAdapter.isEnabled();
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        status.setText("NFC: " + (enabled ? "ON" : (nfc ? "OFF" : "NOT AVAILABLE")) + "   |   HCE: " + (hce ? "SUPPORTED" : "NOT SUPPORTED"));
        modeText.setText("MODE: " + (cardScanMode ? "CARD INSPECTOR" : "HCE READER PROBE"));
        int c = ProbeStore.getCount(this);
        count.setText("APDU received: " + c + (c > 0 ? "  ✓ READER REACHED HCE SERVICE" : ""));
        cardInfo.setText(ProbeStore.getCardInfo(this));
        log.setText(ProbeStore.getLog(this));
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(0xFF111111);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setGravity(Gravity.START); return t;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static String toHex(byte[] data) {
        if (data == null) return "<null>";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
