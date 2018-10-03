package com.app.task;

import com.app.DatabaseService;
import com.app.TomcatStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshDBTask  implements Runnable {
    private static Logger log = LoggerFactory.getLogger(RefreshDBTask.class);
    @Override
    public void run() {

        //System.out.print("\n\n" + Thread.currentThread().getName()+" START: "+new Date());
        DatabaseService.initDB();
        //System.out.print(" ...... " + Thread.currentThread().getName()+" DONE: "+new Date());
        if (Thread.interrupted()){
            log.warn("Refresh DB Task Interupted");
            return;
        }

    }

}
