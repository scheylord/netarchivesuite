#!/bin/bash
export CLASSPATH=/home/test/UNITTEST/lib/dk.netarkivet.harvester.jar:/home/test/UNITTEST/lib/dk.netarkivet.archive.jar:/home/test/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/test/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/test/UNITTEST
java -Xmx1536m -Ddk.netarkivet.settings.file=/home/test/UNITTEST/conf/settings_harvester_8082.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/test/UNITTEST/conf/log_sidekick.prop -Dsettings.common.jmx.port=8101 -Dsettings.common.jmx.rmiPort=8201 -Dsettings.common.jmx.passwordFile=/home/test/UNITTEST/conf/jmxremote.password -Djava.security.manager -Djava.security.policy=conf/security.policy  dk.netarkivet.harvester.sidekick.SideKick dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook ./conf/start_harvester_8082.sh  < /dev/null > start_sidekick_8082.sh.log 2>&1 &
