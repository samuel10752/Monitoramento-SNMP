package com.example.printercounters.controllers;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public abstract class PrinterModel {
    protected String ip;
    protected TextField macField;
    protected TextField serialField;
    protected TextField nameprinterField;
    protected TextArea webInfoArea;

    public PrinterModel(String ip, TextField macField, TextField serialField, TextField nameprinterField,
            TextArea webInfoArea) {
        this.ip = ip;
        this.macField = macField;
        this.serialField = serialField;
        this.nameprinterField = nameprinterField;
        this.webInfoArea = webInfoArea;
    }

    // Abstract method for subclasses to override
    public abstract void fetchOidData(String oid);

    // Getter for IP
    public String getIp() {
        return ip;
    }

    public abstract String getMacAddress();

    public abstract String getSerialNumber();

    public abstract String getWebCounters();

    public abstract void fetchPrinterInfo();

    public abstract void fetchWebPageData();

    public void disableSSLCertificateChecking() {
        try {
            TrustManager[] trustAllCertificates = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCertificates, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
