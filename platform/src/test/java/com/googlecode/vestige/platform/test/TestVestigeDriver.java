package com.googlecode.vestige.platform.test;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.googlecode.vestige.platform.system.PublicVestigeSystem;
import com.googlecode.vestige.platform.system.JVMVestigeSystemActionExecutor;
import com.googlecode.vestige.platform.system.VestigeSystemAction;

/**
 * @author gaellalire
 */
public class TestVestigeDriver {

    @Test
    @Ignore
    public void testname() throws Exception {
        // System.out.println("par ici" + ProxySelector.getDefault());
        LogbackUtil.giveDirectStreamAccessToLogback();
        new JVMVestigeSystemActionExecutor(false).equals(new VestigeSystemAction() {

            @Override
            public void vestigeSystemRun(final PublicVestigeSystem vestigeSystem) {
                System.err.println("par la");
                Thread thread1 = new Thread() {

                    @Override
                    public void run() {
                        try {
                            vestigeSystem.createSubSystem();
                            MockDriver driver = new MockDriver();
                            Enumeration<Driver> drivers = DriverManager.getDrivers();
                            Assert.assertFalse(drivers.hasMoreElements());
                            DriverManager.registerDriver(driver);
                            drivers = DriverManager.getDrivers();
                            Assert.assertEquals(driver, drivers.nextElement());
                            Assert.assertFalse(drivers.hasMoreElements());
                            System.out.println("OK T1");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                };
                thread1.start();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                Thread thread2 = new Thread() {

                    @Override
                    public void run() {
                        try {
                            vestigeSystem.createSubSystem();
                            MockDriver driver = new MockDriver();
                            Enumeration<Driver> drivers = DriverManager.getDrivers();
                            Assert.assertFalse(drivers.hasMoreElements());
                            DriverManager.registerDriver(driver);
                            drivers = DriverManager.getDrivers();
                            Assert.assertEquals(driver, drivers.nextElement());
                            Assert.assertFalse(drivers.hasMoreElements());
                            System.out.println("OK T2");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                };
                thread2.start();
                try {
                    thread1.join();
                    thread2.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("pre fin");
            }
        });
        System.out.println("fin");
    }

}
