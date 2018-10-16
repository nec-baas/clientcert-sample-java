package com.nec.baas.client_cert;

import com.nec.baas.core.NbCallback;
import com.nec.baas.core.NbErrorInfo;
import com.nec.baas.core.NbResultCallback;
import com.nec.baas.core.NbService;
import com.nec.baas.generic.NbGenericServiceBuilder;
import com.nec.baas.object.NbClause;
import com.nec.baas.object.NbObject;
import com.nec.baas.object.NbObjectBucket;
import com.nec.baas.object.NbObjectBucketManager;
import com.nec.baas.object.NbQuery;
import com.nec.baas.user.NbUser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main
 */
public class Main {
    private final String NAME = getClass().getName();
    private final Logger logger = Logger.getLogger(NAME);

    private NbObjectBucket mNbObjectBucket;
    private NbObject mNbObject;

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private Main() {
    }

    private void start() throws Exception {
        logger.info(NAME + " : START");

        // モバイルバックエンド基盤 SDK の初期化
        NbService nbService = new NbGenericServiceBuilder()
                                      .tenantId(Config.TENANT_ID)
                                      .appId(Config.APP_ID)
                                      .appKey(Config.APP_KEY)
                                      .endPointUri(Config.ENDPOINT_URI)
                                      .build();

        if (Config.PROXY_HOST != null) {
            NbService.setProxy(Config.PROXY_HOST, Config.PROXY_PORT);
        }

        // クライアント証明書・CA証明書　読み込み.
        try{
            InputStream inputStreamClientCert = new FileInputStream(Config.CLIENT_CERT);
            InputStream inputStreamTrustedCaCert = new FileInputStream(Config.TRUSTED_CA_CERT);
            NbService.setClientCertificate(inputStreamClientCert,
                    Config.CLIENT_CERT_PASSWORD,
                    inputStreamTrustedCaCert);
        }catch (IOException | GeneralSecurityException e){
            logger.log(Level.SEVERE, "Exception : ", e);
            System.exit(1);
        }

        logger.info("isClientCertSet : " + NbService.isClientCertSet());

        NbObjectBucketManager obm = NbService.getInstance().objectBucketManager();
        mNbObjectBucket = obm.getBucket(Config.OBJECT_BUCKET_NAME);

        getCurrentUser();

        createObject();
        readObject();
        updateObject();
        deleteObject();

        NbService.disableClientCertificate();

        logger.info("isClientCertSet : " + NbService.isClientCertSet());
        logger.info(NAME + " : END");
    }

    private void getCurrentUser() throws Exception{
        logger.info("Get Current User: Start");
        CountDownLatch countDownLatch = new CountDownLatch(1);

        NbUser.refreshCurrentUser(new NbCallback<NbUser>() {
            @Override
            public void onSuccess(NbUser nbUser) {
                logger.info("Get Current User: Success: " + nbUser.toJsonObject());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int i, NbErrorInfo nbErrorInfo) {
                logger.info("Get Current User: Failure: " + nbErrorInfo.toString());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    private void createObject() throws Exception{
        logger.info("Create Object: Start");
        CountDownLatch countDownLatch = new CountDownLatch(1);

        NbObject obj = mNbObjectBucket.newObject();
        obj.put("name", NAME);
        obj.put("operation","CREATE");
        obj.save(new NbCallback<NbObject>() {
            @Override
            public void onSuccess(NbObject nbObject) {
                mNbObject = nbObject;
                logger.info("Create Object: Success: " + nbObject.toJSONString());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int i, NbErrorInfo nbErrorInfo) {
                logger.info("Create Object: Failure: " + nbErrorInfo.toString());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    private void readObject() throws Exception{
        logger.info("Read Object: Start");
        CountDownLatch countDownLatch = new CountDownLatch(1);

        NbClause clause = new NbClause()
                                  .equals("name", NAME)
                                  .equals("operation", "CREATE");
        NbQuery query = new NbQuery();
        query.setClause(clause);

        mNbObjectBucket.query(query, new NbCallback<List<NbObject>>() {
            @Override
            public void onSuccess(List<NbObject> nbObjects) {
                mNbObject = nbObjects.get(0);
                logger.info("Read Object: Success: " + mNbObject.toJSONString());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int i, NbErrorInfo nbErrorInfo) {
                logger.info("Read Object: Failure: " + nbErrorInfo.toString());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    private void updateObject() throws Exception{
        logger.info("Update Object: Start");

        if(mNbObject == null){
            logger.info("Update Object: Failure: Object is NULL");
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);

        mNbObject.put("operation", "UPDATE");
        mNbObject.save(new NbCallback<NbObject>() {
            @Override
            public void onSuccess(NbObject nbObject) {
                mNbObject = nbObject;
                logger.info("Update Object: Success: " + nbObject.toJSONString());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int i, NbErrorInfo nbErrorInfo) {
                logger.info("Update Object: Failure: " + nbErrorInfo.toString());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    private void deleteObject() throws Exception{
        logger.info("Delete Object: Start");

        if(mNbObject == null){
            logger.info("Delete Object: Failure: Object is NULL");
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);

        mNbObject.deleteObject(new NbResultCallback() {
            @Override
            public void onSuccess() {
                logger.info("Delete Object: Success");
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int i, NbErrorInfo nbErrorInfo) {
                logger.info("Delete Object: Failure: " + nbErrorInfo.toString());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

}
