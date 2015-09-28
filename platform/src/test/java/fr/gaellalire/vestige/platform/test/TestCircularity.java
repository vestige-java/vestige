package fr.gaellalire.vestige.platform.test;

import java.security.AccessController;
import java.security.ProtectionDomain;

import org.junit.Test;

import fr.gaellalire.vestige.platform.system.JVMVestigeSystemActionExecutor;
import fr.gaellalire.vestige.platform.system.PrivateVestigePolicy;
import fr.gaellalire.vestige.platform.system.PrivateVestigeSecurityManager;
import fr.gaellalire.vestige.platform.system.PrivateWhiteListVestigePolicy;
import fr.gaellalire.vestige.platform.system.PublicVestigeSystem;
import fr.gaellalire.vestige.platform.system.VestigeSystem;
import fr.gaellalire.vestige.platform.system.VestigeSystemAction;

/**
 * @author gaellalire
 */
public class TestCircularity {

    @Test
    // @Ignore
    public void test() throws Exception {
        LogbackUtil.giveDirectStreamAccessToLogback();
        System.out.println("quoi ?");
        new JVMVestigeSystemActionExecutor(true).execute(new VestigeSystemAction() {

            @Override
            public void vestigeSystemRun(final PublicVestigeSystem vestigeSystem) {
         //       Security.getProviders();

                vestigeSystem.createSubSystem();

                try {
                    Class.forName(ProtectionDomain.class.getName(), true, PrivateWhiteListVestigePolicy.class.getClassLoader());
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }

                PrivateVestigePolicy vestigePolicy = new PrivateVestigePolicy(null);
                vestigeSystem.setPolicy(vestigePolicy);

                PrivateWhiteListVestigePolicy whiteListVestigePolicy = new PrivateWhiteListVestigePolicy();
                // whiteListVestigePolicy.addSafeClassLoader(VestigePolicy.class.getClassLoader());
                // whiteListVestigePolicy.addSafeClassLoader(PrivateVestigeSecurityManager.class.getClassLoader());
                // whiteListVestigePolicy.addSafeClassLoader(VestigeClassLoader.class.getClassLoader());
                // //
                // whiteListVestigePolicy.addSafeClassLoader(StandardEditionVestige.class.getClassLoader());
                // whiteListVestigePolicy.addSafeClassLoader(JoranConfigurator.class.getClassLoader());
                // whiteListVestigePolicy.addSafeClassLoader(ConsoleTarget.class.getClassLoader());
                whiteListVestigePolicy.addSafeClassLoader(TestCircularity.class.getClassLoader());
                vestigeSystem.setWhiteListPolicy(whiteListVestigePolicy);

                PrivateVestigeSecurityManager vestigeSecurityManager = new PrivateVestigeSecurityManager();
                vestigeSystem.setSecurityManager(vestigeSecurityManager);
                System.out.println("Debut");
                try {
                    if (((VestigeSystem) vestigeSystem).getCurrentPolicy().implies(getClass().getProtectionDomain(), new RuntimePermission("setFactory"))) {
                        System.out.println("OK");
                    } else {
                        System.out.println("Non");
                    }
                    AccessController.checkPermission(new RuntimePermission("setFactory"));
                    System.out.println("Checked");
                } catch (Throwable e) {
                    System.out.println("Refuse");
                    e.printStackTrace();
                }
            }
        });
        System.out.println("et beh ?");
    }
}
