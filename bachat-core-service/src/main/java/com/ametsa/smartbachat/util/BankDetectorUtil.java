package com.ametsa.smartbachat.util;

public class BankDetectorUtil {

    public static String detectBank(String text) {
        if (text == null) return null;
        String t = text.toLowerCase();
        if (t.contains("hdfc bank")) return "HDFC";
        if (t.contains("icici bank")) return "ICICI";
        if (t.contains("state bank of india") || t.contains("sbi")) return "SBI";
        if (t.contains("axis bank")) return "AXIS";
        if (t.contains("kotak mahindra")) return "KOTAK";
        return null;
    }
}
