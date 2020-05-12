/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package b06902048;
// import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostService;import org.onosproject.net.topology.TopologyService;

import java.lang.System;
import java.util.Optional;
import java.util.Map;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Some configurable property. */
	// private String someProperty;
	private ApplicationId appId;
	private ReactivePacketProcessor processor = new ReactivePacketProcessor();

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected ComponentConfigService cfgService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected PacketService packetService;
						
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowObjectiveService flowObjectiveService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected TopologyService topologyService;

	@Activate
	protected void activate() {

		//register app
		appId = coreService.registerApplication("org.b06902048.app");

		//Add a processor with priority 2
		packetService.addProcessor(processor, PacketProcessor.director(2));
		
		//build a processing selector as default selector
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

		//only process IPV4 packets
		selector.matchEthType(Ethernet.TYPE_IPV4);
		
		//register the selector
		packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
		
		log.info("Started");
	}

	@Deactivate
	protected void deactivate() {
		//remove processor
		packetService.removeProcessor(processor);
		processor = null;
		
		log.info("Stopped");
	}

	private class ReactivePacketProcessor implements PacketProcessor{
		@Override
		public void process(PacketContext context){
			if(context.isHandled())	{
				return;
			}
			InboundPacket pkt = context.inPacket();
			Ethernet ethPkt = pkt.parsed();

			if(ethPkt == null){
				return;
			}

			HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
			HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());
			System.out.println("--------------------");
			System.out.println("Switch is " + context.inPacket().receivedFrom().deviceId());
			System.out.println("src is" + srcId);
			System.out.println("dst is" + dstId);
			System.out.println("--------------------");
			flood(context);

		}
	}	
	private void flood(PacketContext context) {
		if (topologyService.isBroadcastPoint(topologyService.currentTopology(), 
											context.inPacket().receivedFrom())) {
			packetOut(context, PortNumber.FLOOD);
		}
		else{				
			context.block();
		}
	}
	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}
}
