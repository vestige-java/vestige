/*
 * This file is part of Vestige.
 *
 * Vestige is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Vestige is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vestige.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.gaellalire.vestige.edition.standard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gaellalire.vestige.admin.command.VestigeCommandExecutor;
import fr.gaellalire.vestige.admin.web.VestigeServlet;
import fr.gaellalire.vestige.application.manager.ApplicationManager;
import fr.gaellalire.vestige.edition.standard.schema.Bind;
import fr.gaellalire.vestige.edition.standard.schema.Web;

/**
 * @author Gael Lalire
 */
public class WebServerFactory implements Callable<VestigeServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerFactory.class);

    private static final String DEFAULT_PASSWORD = "changeit";

    private static final char[] DEFAULT_PASSWORD_CHARARRAY = DEFAULT_PASSWORD.toCharArray();

    private Web web;

    private ApplicationManager applicationManager;

    private VestigeCommandExecutor vestigeCommandExecutor;

    @SuppressWarnings("unused")
    private File appHomeFile;

    private File webConfig;

    private File webData;

    private X509Certificate rootCert;

    private PrivateKey rootKey;

    private File caFile;

    private File browser;

    private VestigeStateListener vestigeStateListener;

    public WebServerFactory(final File webConfig, final File webData, final Web web, final ApplicationManager applicationManager, final File appHomeFile,
            final VestigeCommandExecutor vestigeCommandExecutor, final VestigeStateListener vestigeStateListener) {
        this.web = web;
        this.applicationManager = applicationManager;
        this.appHomeFile = appHomeFile;
        this.webConfig = webConfig;
        this.webData = webData;
        this.vestigeCommandExecutor = vestigeCommandExecutor;
        this.vestigeStateListener = vestigeStateListener;
        caFile = new File(webConfig, "ca.p12");
        browser = new File(webConfig, "browser");
    }

    private KeyPair generateKeyPair() throws Exception {
        RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
        BigInteger publicExponent = new BigInteger("10001", 16);
        keyGen.init(new RSAKeyGenerationParameters(publicExponent, SecureRandom.getInstance("DEFAULT", BouncyCastleProvider.PROVIDER_NAME), 2048, 80));
        AsymmetricCipherKeyPair keys = keyGen.generateKeyPair();

        RSAPrivateCrtKeyParameters rsaKeyParameters = (RSAPrivateCrtKeyParameters) keys.getPrivate();
        RSAPrivateKeySpec privateSpec = new RSAPrivateCrtKeySpec(rsaKeyParameters.getModulus(), publicExponent, rsaKeyParameters.getExponent(), rsaKeyParameters.getP(),
                rsaKeyParameters.getQ(), rsaKeyParameters.getDP(), rsaKeyParameters.getDQ(), rsaKeyParameters.getQInv());
        KeyFactory factory = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);

        PrivateKey privateKey = factory.generatePrivate(privateSpec);
        PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keys.getPublic()).getEncoded()));

        return new KeyPair(publicKey, privateKey);
    }

    private String getComputerName() throws UnknownHostException {
        String name = System.getenv("COMPUTERNAME");
        if (name != null) {
            return name;
        }
        name = System.getenv("HOSTNAME");
        if (name != null) {
            return name;
        }
        InetAddress localHost = InetAddress.getLocalHost();
        name = localHost.getHostName();
        if (name.equals(localHost.getHostAddress())) {
            name = null;
        }
        return name;
    }

    private String getUserName() throws UnknownHostException {
        return System.getProperty("user.name");
    }

    private String getFullName() throws UnknownHostException {
        String computerName = getComputerName();
        String userName = getUserName();
        if (computerName == null && userName == null) {
            return null;
        }
        StringBuilder fullName = new StringBuilder();
        if (userName != null) {
            fullName.append(userName);
        }
        if (computerName != null) {
            if (fullName.length() != 0) {
                fullName.append(" at ");
            }
            fullName.append(computerName);
        }
        return fullName.toString();
    }

    private X509Certificate generateRootCertificate(final KeyPair keys) throws Exception {

        String computerName = getFullName();
        String name;
        if (computerName == null) {
            name = "CN=Vestige CA,O=Vestige,OU=Vestige";
        } else {
            name = "CN=Vestige CA of " + computerName + ",O=Vestige,OU=Vestige";
        }

        X500Name x500nameIssuer = new X500Name(name);
        X500Name x500nameSubject = new X500Name(name);
        BigInteger serial = new BigInteger(64, new Random());
        Date notBefore = new Date();

        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(notBefore);
        tempCal.add(Calendar.DATE, 20 * 365);
        Date notAfter = tempCal.getTime();

        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keys.getPublic().getEncoded());

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(x500nameIssuer, serial, notBefore, notAfter, Locale.US, x500nameSubject, subPubKeyInfo);

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true).getEncoded());

        KeyUsage keyUsage = new KeyUsage(KeyUsage.keyCertSign);
        builder.addExtension(Extension.keyUsage, true, keyUsage.getEncoded());

        JcaX509ExtensionUtils u = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.subjectKeyIdentifier, false, u.createSubjectKeyIdentifier(subPubKeyInfo).getEncoded());

        ContentSigner sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate());

        X509CertificateHolder holder = builder.build(sigGen);
        InputStream is = new ByteArrayInputStream(holder.toASN1Structure().getEncoded());
        return (X509Certificate) CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME).generateCertificate(is);
    }

    private X509Certificate generateServerCertificate(final PublicKey publicKey, final X509Certificate issuerCertificate, final PrivateKey signKey, final Set<String> names)
            throws Exception {

        String computerName = getFullName();
        String dname;
        if (computerName == null) {
            dname = "CN=Vestige Server,O=Vestige,OU=Vestige";
        } else {
            dname = "CN=Vestige Server of " + computerName + ",O=Vestige,OU=Vestige";
        }

        X500Name x500nameSubject = new X500Name(dname);
        BigInteger serial = new BigInteger(64, new Random());
        Date notBefore = new Date();

        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(notBefore);
        tempCal.add(Calendar.DATE, 365);
        Date notAfter = tempCal.getTime();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerCertificate, serial, notBefore, notAfter, x500nameSubject, publicKey);

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false).getEncoded());

        JcaX509ExtensionUtils u = new JcaX509ExtensionUtils();
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        builder.addExtension(Extension.subjectKeyIdentifier, false, u.createSubjectKeyIdentifier(subPubKeyInfo).getEncoded());

        List<GeneralName> generalNames = new ArrayList<GeneralName>(names.size());
        for (String name : names) {
            if (name.equals(InetAddress.getByName(name).getHostAddress())) {
                int percentIndex = name.indexOf('%');
                if (percentIndex != -1) {
                    name = name.substring(0, percentIndex);
                }
                generalNames.add(new GeneralName(GeneralName.iPAddress, name));
            } else {
                generalNames.add(new GeneralName(GeneralName.dNSName, name));
            }
        }

        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames.toArray(new GeneralName[generalNames.size()])).getEncoded());

        KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment);
        builder.addExtension(Extension.keyUsage, true, keyUsage.getEncoded());

        ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth);
        builder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage.getEncoded());

        builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerCertificate).getEncoded());

        ContentSigner sigGen = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(signKey);

        X509CertificateHolder holder = builder.build(sigGen);
        InputStream is = new ByteArrayInputStream(holder.toASN1Structure().getEncoded());
        return (X509Certificate) CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME).generateCertificate(is);
    }

    private X509Certificate generateClientCertificate(final PublicKey publicKey, final X509Certificate issuerCertificate, final PrivateKey signKey) throws Exception {

        String computerName = getFullName();
        String name;
        if (computerName == null) {
            name = "CN=Vestige Client,O=Vestige,OU=Vestige";
        } else {
            name = "CN=Vestige Client of " + computerName + ",O=Vestige,OU=Vestige";
        }

        X500Name x500nameSubject = new X500Name(name);
        BigInteger serial = new BigInteger(64, new Random());
        Date notBefore = new Date();

        Date notAfter = issuerCertificate.getNotAfter();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerCertificate, serial, notBefore, notAfter, x500nameSubject, publicKey);

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false).getEncoded());

        JcaX509ExtensionUtils u = new JcaX509ExtensionUtils();
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        builder.addExtension(Extension.subjectKeyIdentifier, false, u.createSubjectKeyIdentifier(subPubKeyInfo).getEncoded());

        KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature /* | KeyUsage.keyEncipherment */);
        builder.addExtension(Extension.keyUsage, true, keyUsage.getEncoded());

        ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);
        builder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage.getEncoded());

        ContentSigner sigGen = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(signKey);

        X509CertificateHolder holder = builder.build(sigGen);
        InputStream is = new ByteArrayInputStream(holder.toASN1Structure().getEncoded());
        return (X509Certificate) CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME).generateCertificate(is);
    }

    public void loadCA() throws Exception {
        do {
            KeyStore caStore = new KeyStoreLoader() {

                @Override
                public void init(final KeyStore keyStore) throws Exception {
                    KeyPair kp = generateKeyPair();
                    X509Certificate rootCertificate = generateRootCertificate(kp);
                    keyStore.setKeyEntry("vestige_ca", kp.getPrivate(), DEFAULT_PASSWORD_CHARARRAY, new Certificate[] {rootCertificate});
                    browser.mkdirs();
                    File rootCertFile = new File(browser, "vestige_ca.crt");
                    PemWriter pw = new PemWriter(new OutputStreamWriter(new FileOutputStream(rootCertFile), "UTF-8"));
                    try {
                        pw.writeObject(new JcaMiscPEMGenerator(rootCertificate));
                    } finally {
                        pw.close();
                    }
                    vestigeStateListener.certificateAuthorityGenerated(rootCertFile);
                }

            }.load(caFile);
            rootKey = (PrivateKey) caStore.getKey("vestige_ca", DEFAULT_PASSWORD_CHARARRAY);
            rootCert = (X509Certificate) caStore.getCertificate("vestige_ca");
            if (rootCert == null) {
                caFile.delete();
            }
        } while (!caFile.exists());
    }

    /**
     * @author Gael Lalire
     */
    private static final class HostBind {
        private String host;

        private int port;

        // 3 : any
        // 2 : loopback
        // 1 : other
        // 0 : none
        private int level;

        private HostBind() {
        }
    }

    public VestigeServer call() throws Exception {
        webData.mkdirs();
        webConfig.mkdirs();
        final List<Bind> binds = web.getBind();
        final Server webServer = new Server();
        List<Connector> connectors = new ArrayList<Connector>(binds.size());
        SslContextFactory sslContextFactory = new SslContextFactory() {
            @Override
            protected KeyManager[] getKeyManagers(final KeyStore keyStore) throws Exception {
                KeyManager[] managers = null;

                if (keyStore != null) {
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
                    keyManagerFactory.init(keyStore, DEFAULT_PASSWORD.toCharArray());
                    managers = keyManagerFactory.getKeyManagers();
                }

                return managers;
            }

            protected TrustManager[] getTrustManagers(final KeyStore trustStore, final Collection<? extends CRL> crls) throws Exception {
                TrustManager[] managers = null;
                if (trustStore != null) {
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", BouncyCastleJsseProvider.PROVIDER_NAME);
                    trustManagerFactory.init(trustStore);

                    managers = trustManagerFactory.getTrustManagers();
                }

                return managers;
            }

        };
        sslContextFactory.setNeedClientAuth(true);
        sslContextFactory.setExcludeProtocols("SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1");

        File serverFile = new File(webConfig, "server.p12");
        File trustFile = new File(webConfig, "trust.p12");

        do {
            KeyStore serverStore = new KeyStoreLoader() {

                @Override
                public void init(final KeyStore keyStore) throws Exception {
                    KeyPair kp = generateKeyPair();
                    loadCA();
                    final Set<String> names = new HashSet<String>();
                    names.add("localhost");
                    for (final Bind bind : binds) {
                        String host = bind.getHost();
                        if (host == null) {
                            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                            while (e.hasMoreElements()) {
                                NetworkInterface n = e.nextElement();
                                Enumeration<InetAddress> ee = n.getInetAddresses();
                                while (ee.hasMoreElements()) {
                                    InetAddress i = ee.nextElement();
                                    names.add(i.getHostAddress());
                                    names.add(i.getHostName());
                                }
                            }
                        } else {
                            InetAddress inetAddress = InetAddress.getByName(host);
                            names.add(inetAddress.getHostName());
                            names.add(inetAddress.getHostAddress());
                        }
                    }
                    keyStore.setKeyEntry("vestige_server", kp.getPrivate(), DEFAULT_PASSWORD_CHARARRAY,
                            new Certificate[] {generateServerCertificate(kp.getPublic(), rootCert, rootKey, names), rootCert});

                }
            }.load(serverFile);

            X509Certificate x509Certificate = (X509Certificate) serverStore.getCertificate("vestige_server");
            if (x509Certificate == null) {
                serverFile.delete();
                continue;
            }
            Date notAfter = x509Certificate.getNotAfter();
            Calendar tempCal = Calendar.getInstance();
            tempCal.add(Calendar.DATE, 60);

            if (notAfter.before(tempCal.getTime())) {
                serverFile.delete();
            }

        } while (!serverFile.exists());

        final KeyStore trustStore = new KeyStoreLoader() {

            @Override
            public void init(final KeyStore keyStore) throws Exception {
                loadCA();
                browser.mkdirs();
                new KeyStoreLoader() {

                    @Override
                    public void init(final KeyStore keyStore) throws Exception {
                        KeyPair kp = generateKeyPair();
                        loadCA();
                        keyStore.setKeyEntry("vestige_client", kp.getPrivate(), DEFAULT_PASSWORD_CHARARRAY,
                                new Certificate[] {generateClientCertificate(kp.getPublic(), rootCert, rootKey), rootCert});
                    }

                    public void keyStoreWritten(final File keyStoreFile) {
                        vestigeStateListener.clientP12Generated(keyStoreFile);
                    }
                }.load(new File(browser, "vestige_client.p12"));
                keyStore.setCertificateEntry("vestige_ca", rootCert);
            }
        }.load(trustFile);

        sslContextFactory.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        sslContextFactory.setProtocol("TLSv1.2");

        // "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        // sslContextFactory.setIncludeCipherSuites("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");

        sslContextFactory.setKeyStoreProvider(BouncyCastleProvider.PROVIDER_NAME);
        sslContextFactory.setKeyStorePath(serverFile.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(DEFAULT_PASSWORD);
        // sslContextFactory.setKeyManagerPassword(DEFAULT_PASSWORD);
        sslContextFactory.setCertAlias("vestige_server");
        sslContextFactory.setKeyStoreType("PKCS12");
        sslContextFactory.setTrustStore(trustStore);

        final HostBind localHostBind = new HostBind();
        localHostBind.port = -1;
        for (final Bind bind : binds) {

            Connector connector = new SslSelectChannelConnector(sslContextFactory) {
                @Override
                public void open() throws IOException {
                    super.open();
                    final String host = bind.getHost();
                    int localPort = getLocalPort();
                    if (host == null) {
                        localHostBind.host = "localhost";
                        localHostBind.port = localPort;
                        localHostBind.level = 3;
                    } else {
                        if (localHostBind.level == 0) {
                            localHostBind.host = host;
                            localHostBind.port = localPort;
                            localHostBind.level = 1;
                        }
                        if (localHostBind.level < 3) {
                            try {
                                InetAddress inetAddress = InetAddress.getByName(host);
                                if (inetAddress.isAnyLocalAddress()) {
                                    localHostBind.host = "localhost";
                                    localHostBind.port = localPort;
                                    localHostBind.level = 3;
                                } else if (localHostBind.level < 2 && inetAddress.isLoopbackAddress()) {
                                    localHostBind.host = inetAddress.getHostName();
                                    localHostBind.port = localPort;
                                    localHostBind.level = 2;
                                }
                            } catch (UnknownHostException e) {
                                LOGGER.debug("Invalid host", e);
                            }
                        }
                    }

                    if (LOGGER.isInfoEnabled()) {
                        if (host == null) {
                            LOGGER.info("Listen on *:{} for web interface", localPort);
                        } else {
                            LOGGER.info("Listen on {}:{} for web interface", host, localPort);
                        }
                    }
                }
            };
            connector.setPort(bind.getPort());
            connector.setHost(bind.getHost());
            connectors.add(connector);
        }
        webServer.setConnectors(connectors.toArray(new Connector[connectors.size()]));
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(new ServletHolder(new VestigeServlet(applicationManager, vestigeCommandExecutor)), "/");

        // String contextPath = "webdav";
        // HttpManagerBuilder httpManagerBuilder = new HttpManagerBuilder();
        // httpManagerBuilder.setFsContextPath(contextPath);
        // httpManagerBuilder.setFsHomeDir(appHomeFile.getPath());
        // servletHandler.addServletWithMapping(new ServletHolder(new VestigeWebdavServlet(httpManagerBuilder.buildHttpManager())),
        // "/" + contextPath + "/*");

        webServer.setHandler(servletHandler);
        return new VestigeServer() {

            @Override
            public void stop() throws Exception {
                webServer.stop();
                localHostBind.host = null;
                localHostBind.port = -1;
                localHostBind.level = 0;
                File portFile = new File(webData, "port.txt");
                portFile.delete();
                LOGGER.info("Web interface stopped");
            }

            @Override
            public void start() throws Exception {
                webServer.start();
                if (localHostBind.level != 0) {
                    File portFile = new File(webData, "port.txt");
                    FileWriter fileWriter = new FileWriter(portFile);
                    try {
                        fileWriter.write(String.valueOf(localHostBind.port));
                    } finally {
                        fileWriter.close();
                    }
                }
                LOGGER.info("Web interface started");
            }

            @Override
            public String getLocalHost() {
                return localHostBind.host;
            }

            @Override
            public int getLocalPort() {
                return localHostBind.port;
            }
        };
    }

}
