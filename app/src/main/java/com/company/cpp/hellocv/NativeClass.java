package com.company.cpp.hellocv;

public class NativeClass {
    public native static int convertGray(long matAddrRgba, long matAddrGray);
    public native static void lightDetection(long addrRgba);
    public native static String getJniStringBytes();
}
