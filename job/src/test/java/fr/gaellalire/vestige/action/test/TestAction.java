package fr.gaellalire.vestige.action.test;

import org.junit.Test;

import fr.gaellalire.vestige.job.DefaultJobManager;
import fr.gaellalire.vestige.job.Job;
import fr.gaellalire.vestige.spi.job.JobHelper;

/**
 * @author Gael Lalire
 */
public class TestAction {

    @Test
    public void testName() throws Exception {
        DefaultJobManager defaultActionManager = new DefaultJobManager();
        defaultActionManager.submitJob("a", "action 1", new Job() {

            @Override
            public void run(final JobHelper actionHelper) throws Exception {
                System.out.println("action inside");
                synchronized (this) {
                    wait();
                }
                System.out.println("action outside");
            }
        }, null);
    }

}
