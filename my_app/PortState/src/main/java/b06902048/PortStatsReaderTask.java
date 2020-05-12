package b06902048;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

import org.onosproject.net.Device;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleExtPayLoad;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

public class PortStatsReaderTask {

	private long delay;
	private Timer timer = new Timer();
	private Logger log;
	private Device device;
	private boolean exit;
	private PortStatistics portStats;
	protected DeviceService deviceService;
	protected FlowRuleService flowRuleService;
	class Task extends TimerTask {
		public Device getDevice() {
			return device;
		}
		public DeviceService getDeviceService() {
			return deviceService;
		}
		public long getDelay() {
			return delay;
		}

		@Override
		public void run() {
			while (!isExit()) {  
				log.info("	[DeviceID] {}", getDevice().id());
				log.info("	----------Port Statistics----------");
			
				List<PortStatistics> deltaPortStatisticsList = deviceService.getPortDeltaStatistics(getDevice().id());
				for(PortStatistics deltaPortStat : deltaPortStatisticsList){
					PortNumber port = deltaPortStat.portNumber();
					PortStatistics portStat = deviceService.getStatisticsForPort(getDevice().id(), port);
					if(deltaPortStat.packetsReceived() > 0){
						log.info("	DeviceID {}	Port {}	received {} packets, {} bytes in total",   getDevice().id(), port, portStat.packetsReceived(), portStat.bytesReceived());
						log.info("	DeviceID {}	Port {}	received {} packets, {} bytes in last second",   getDevice().id(), port, deltaPortStat.packetsReceived(), deltaPortStat.bytesReceived());
					}
				}
	

				/*
				log.info("	----------Flow Statistics----------");
				
				Iterable<FlowEntry> flows = flowRuleService.getFlowEntries(getDevice().id());
				for(FlowEntry f : flows){
					log.info("	Rule ID {}, priority {},\n				matched {} packets, {} bytes\n				Selector(match) : {}\n\r				Treament(action) : {}", f.id(), f.priority(), f.packets(), f.bytes(), f.selector(), f.treatment());
				}
				*/

				try {
					// Sleeping for 1000 * dealy seconds.
					Thread.sleep((getDelay() * 1000)); 
				} catch (InterruptedException e) {
					log.error("exception!");
					e.printStackTrace();
				}
			}
		}
	}

	public void schedule() {
		this.getTimer().schedule(new Task(), 0, 1000);
	}

	public Timer getTimer() {
		return timer;
	}

	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public Logger getLog() {
		return log;
	}

	public void setLog(Logger log) {
		this.log = log;
	}

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public PortStatistics getPortStats() {
		return portStats;
	}

	public void setPortStats(PortStatistics portStats) {
		this.portStats = portStats;
	}

	public DeviceService getDeviceService() {
		return deviceService;
	}

	public void setDeviceService(DeviceService deviceService) {
		this.deviceService = deviceService;
	}

	public FlowRuleService getFlowRuleService(){
		return this.flowRuleService;
	}

	public void setFlowRuleService(FlowRuleService flowRuleService){
		this.flowRuleService = flowRuleService;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}
}
