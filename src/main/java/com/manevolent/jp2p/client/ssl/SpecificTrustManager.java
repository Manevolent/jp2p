package com.manevolent.jp2p.client.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class SpecificTrustManager implements X509TrustManager {
    private final X509Certificate[] x509Certificates;
    private final boolean serverOnly;

    SpecificTrustManager(X509Certificate[] x509Certificates, boolean serverOnly) {
        this.x509Certificates = x509Certificates;
        this.serverOnly = serverOnly;
    }

    SpecificTrustManager(X509Certificate[] x509Certificates) {
        this(x509Certificates, true);
    }

    private void checkTrusted(X509Certificate[] x509Certificates) throws CertificateException {
        for (X509Certificate remoteCertificate : x509Certificates) {
            for (X509Certificate localCertificate : this.x509Certificates) {
                if (remoteCertificate.equals(localCertificate)) {
                    remoteCertificate.checkValidity();
                    return;
                }
            }
        }

        throw new CertificateException("No specific certificates");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        if (!serverOnly)
            checkTrusted(x509Certificates);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        checkTrusted(x509Certificates);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return x509Certificates;
    }
}