package net.floodlightcontroller.randomizer;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 12/15/16.
 */
public class AddressUpdateJob implements Job {
    Logger log = LoggerFactory.getLogger(AddressUpdateJob.class);
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Time to update some addresses!");
    }
}
