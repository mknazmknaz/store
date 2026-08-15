package com.avx.nfcprobe;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import java.util.Arrays;

public class ProbeApduService extends HostApduService {
    private static final byte[] SW_OK = hex("9000");
    private static final byte[] SW_NOT_FOUND = hex("6A82");
    private static final byte[] AVX_AID = hex("F0010203040506");
    private static final byte[] NDEF_AID = hex("D2760000850101");

    @Override public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        ProbeStore.incrementApdu(this);
        ProbeStore.append(this, "RX APDU: " + toHex(commandApdu));
        if (isSelectAid(commandApdu, AVX_AID)) { ProbeStore.append(this, "Matched AVX test AID -> 9000"); return SW_OK; }
        if (isSelectAid(commandApdu, NDEF_AID)) { ProbeStore.append(this, "Matched NFC Forum NDEF AID -> 9000"); return SW_OK; }
        ProbeStore.append(this, "Unrecognized routed APDU -> 6A82");
        return SW_NOT_FOUND;
    }

    @Override public void onDeactivated(int reason) {
        String text = reason == DEACTIVATION_LINK_LOSS ? "LINK_LOSS" : reason == DEACTIVATION_DESELECTED ? "DESELECTED" : String.valueOf(reason);
        ProbeStore.append(this, "HCE deactivated: " + text);
    }

    private static boolean isSelectAid(byte[] apdu, byte[] aid) {
        if (apdu == null || apdu.length < 5 + aid.length) return false;
        if ((apdu[0]&255)!=0x00 || (apdu[1]&255)!=0xA4 || (apdu[2]&255)!=0x04 || (apdu[3]&255)!=0x00) return false;
        int lc = apdu[4]&255;
        if (lc != aid.length || apdu.length < 5 + lc) return false;
        return Arrays.equals(Arrays.copyOfRange(apdu, 5, 5 + lc), aid);
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length()/2];
        for (int i=0;i<s.length();i+=2) out[i/2]=(byte)Integer.parseInt(s.substring(i,i+2),16);
        return out;
    }

    private static String toHex(byte[] data) {
        if (data == null) return "<null>";
        StringBuilder sb = new StringBuilder(data.length*2);
        for (byte b:data) sb.append(String.format("%02X", b & 255));
        return sb.toString();
    }
}
