package fr.gaellalire.vestige.system.test;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import fr.gaellalire.vestige.spi.system.VestigeSystem;
import fr.gaellalire.vestige.system.JVMVestigeSystemActionExecutor;
import fr.gaellalire.vestige.system.VestigeSystemAction;

/**
 * @author Gael Lalire
 */
public class TestVestigeDriver {

    @Test
    @Ignore
    public void testname() throws Exception {
        // System.out.println("par ici" + ProxySelector.getDefault());
        LogbackUtil.giveDirectStreamAccessToLogback();
        new JVMVestigeSystemActionExecutor(false).equals(new VestigeSystemAction() {

            @Override
            public void vestigeSystemRun(final VestigeSystem vestigeSystem) {
                System.err.println("par la");
                Thread thread1 = new Thread() {

                    @Override
                    public void run() {
                        try {
                            vestigeSystem.createSubSystem(null);
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
                            vestigeSystem.createSubSystem(null);
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
