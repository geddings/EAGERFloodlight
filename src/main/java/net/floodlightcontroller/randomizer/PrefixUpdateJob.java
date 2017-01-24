package net.floodlightcontroller.randomizer;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by geddingsbarrineau on 12/15/16.
 */
public class PrefixUpdateJob implements Job {
    Logger log = LoggerFactory.getLogger(PrefixUpdateJob.class);
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Updating prefixes!");
    }
}
