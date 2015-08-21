package com.manevolent.jp2p.extensible;

import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.extensible.socket.client.NativeSocketClient;
import sun.security.x509.*;

import javax.net.ssl.*;
import java.io.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

/**
 * SSL extensions for JP2P. Used to wrap NetworkClient objects in SSL contexts.
 */
public final class SSL {

    /**
     * Creates an SSL client socket factory using the specified trust manager.
     * @return SSLSocketFactory instance.
     */
    public static SSLSocketFactory createFactory(X509Certificate certificate)
            throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("server", certificate);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, null);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(
                kmf.getKeyManagers(),
                new TrustManager[] { new SpecificTrustManager(new X509Certificate[] { certificate }) },
                new SecureRandom()
        )
        ;
        return context.getSocketFactory();
    }

    /**
     * Creates an SSL server socket factory using the specified trust manager.
     * @param trustManagers Trust managers to use when creating the SSL socket factory.
     * @return SSLServerSocketFactory instance.
     */
    public static final SSLServerSocketFactory createServerFactory(
            TrustManager[] trustManagers,
            Certificate certificate,
            PrivateKey key)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", key, "intlPassword".toCharArray(), new Certificate[]{certificate});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "intlPassword".toCharArray());

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(
                kmf.getKeyManagers(),
                trustManagers,
                new SecureRandom()
        );
        return context.getServerSocketFactory();
    }

    /**
     * Connects to a host using SSL/TCP
     * @param host SSL host the client is connected to.
     * @param port SSL port the client is connected to.
     * @param socketFactory SSL socket factory to use.
     * @return NetworkClient instance, wrapped in an SSL context.
     * @throws IOException
     */
    public static NativeSocketClient connect(
            String host, int port,
            SSLSocketFactory socketFactory) throws IOException {

        if (host == null || host.length() <= 0)
            throw new IllegalArgumentException("host not defined");

        SSLSocket socket = ((SSLSocket) socketFactory.createSocket());
        socket.connect(new InetSocketAddress(host, port));
        socket.setSoTimeout(15000);

        socket.setUseClientMode(true);
        socket.startHandshake(); //Start SSL handshake.

        return new NativeSocketClient(socket, true);
    }

    /**
     * Connects to a host using SSL/TCP
     * @param host SSL host the client is connected to.
     * @param port SSL port the client is connected to.
     * @return NetworkClient instance, wrapped in an SSL context.
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static NetworkClient connect(String host, int port,
                                        X509Certificate certificate)
            throws KeyManagementException, NoSuchAlgorithmException,
            IOException, CertificateException, KeyStoreException, UnrecoverableKeyException {
        return connect(
                host, port,
                createFactory(certificate)
        );
    }

    public static X509Certificate loadX509Certificate(File file)
            throws CertificateException, FileNotFoundException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new FileInputStream(file));
    }

    public static PrivateKey loadPKCS8Key(File file)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        byte[] buf = new byte[(int)raf.length()];
        raf.readFully(buf);
        raf.close();

        PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(buf);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(kspec);
    }

    public static X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException
    {
        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * 86400000l);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
        info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        return cert;
    }
}
