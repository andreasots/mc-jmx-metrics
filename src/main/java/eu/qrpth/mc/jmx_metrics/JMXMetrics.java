package eu.qrpth.mc.jmx_metrics;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMXMetrics implements DedicatedServerModInitializer {
	public static final String MOD_ID = "jmx-metrics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeServer() {
	}
}
