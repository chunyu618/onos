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
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;

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
	// new a user defined processor.
	private ReactivePacketProcessor processor = new ReactivePacketProcessor();
	// create a map to store MAC address and port number, which is just like ARP table.
	protected Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();

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
		
		// Need to implement process method.
		@Override
		public void process(PacketContext context){
			//To check whether this packet was handled.
			if(context.isHandled())	{
				return;
			}
			
			InboundPacket pkt = context.inPacket();
			//parse this packet as Ethernet packet
			Ethernet ethPkt = pkt.parsed();
			//add this device to table if absent
			macTables.putIfAbsent(pkt.receivedFrom().deviceId(), Maps.newConcurrentMap());
			
			// It is not Ethernet frame or there is no suitable parser.
			if(ethPkt == null){
				return;
			}

			ConnectPoint device = pkt.receivedFrom();
			MacAddress srcMAC = ethPkt.getSourceMAC();
			MacAddress dstMAC = ethPkt.getDestinationMAC();
			
			//two type of packet : IPv4 and ARP
			if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4){
				log.info("[IPv4]	DeviceID {} from {} to {}", device.deviceId(), srcMAC, dstMAC);
			}
			else if (ethPkt.getEtherType() == Ethernet.TYPE_ARP){
				ARP p = (ARP) ethPkt.getPayload();
				String type = (p.getOpCode() == ARP.OP_REPLY)? "[REPLY]": "[REQUEST]";
				log.info("[ARP]	{} DeviceID {} from {} to {}", type, device.deviceId(), srcMAC, dstMAC);
			}
			else return;

			learning(context);
			forwarding(context);

		}
	}	

	private void learning(PacketContext context){
		InboundPacket pkt = context.inPacket();
		Ethernet ethPkt = pkt.parsed();
		
		MacAddress src = ethPkt.getSourceMAC();
		MacAddress dst = ethPkt.getDestinationMAC();
		/*
		if(ethPkt.isBroadcast()){
			// Don't learn broadcast fram.
			log.info("[Broadcast]	from {} to {}", src, dst);
			return;
		}
		*/

		if(src == null || dst == null){
			log.info("[Error]	No src or dst MacAddress");
			return;
		}

		log.info("[learning]	try to record from {} to {}", src, dst);
		ConnectPoint device = pkt.receivedFrom();

		Map<MacAddress, PortNumber> macTable = macTables.get(device.deviceId());
		if(!macTable.containsKey(src)){
			macTable.put(src, device.port());
			log.info("[learning]	learned src {} maps deviceId {} port {}", src, device.deviceId(), device.port());
		}
	}

	private void forwarding(PacketContext context){
		InboundPacket pkt = context.inPacket();
		Ethernet ethPkt = pkt.parsed();
		TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
		
		MacAddress src = ethPkt.getSourceMAC();
		MacAddress dst = ethPkt.getDestinationMAC();
		PortNumber inPort = pkt.receivedFrom().port();
		
		TrafficTreatment treatment;
		
		
		if(src == null || dst == null){
			log.info("[Error]	No src or dst MacAddress");
			return;
		}

		ConnectPoint device = pkt.receivedFrom();
		log.info("[forwarding]	inPort {}, from {} to {}", inPort, src, dst);
		log.info("[forwarding]	Device {}", device);

		Map<MacAddress, PortNumber> macTable = macTables.get(device.deviceId());
		PortNumber outPort = macTable.get(dst);
		
		if(outPort == null){
			if(!inPort.equals(macTable.get(src))){
				// Different inPort and record port, so drop.
				treatment = DefaultTrafficTreatment.builder().drop().build();
				log.info("[forwarding]	different inPort {} and record port {}, drop the packet", inPort, macTable.get(src));
			}
			else{
				//no record or broadcast, so flooding
				packetOut(context, PortNumber.FLOOD);
				log.info("[forwarding]	No record outPort, flooding");
				return;
			}
		}
		else{
			
			if(inPort.equals(outPort)){
				//same port, so drop
				treatment = DefaultTrafficTreatment.builder().drop().build();
				log.info("[forwarding]	same of inPort and outPort, drop the packet");
			}
			else{
				treatment = DefaultTrafficTreatment.builder().setOutput(outPort).build();
				log.info("[forwarding]	sent to port {}", outPort);
			}
		}
		//install rule
		/*
		ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder();
		forwardingObjective.withSelector(selectorBuilder.build());
		forwardingObjective.withTreatment(treatment);
		forwardingObjective.withPriority(PacketPriority.HIGH.priorityValue());
		forwardingObjective.withFlag(ForwardingObjective.Flag.VERSATILE);
		forwardingObjective.fromApp(appId);
		forwardingObjective.makeTemporary(20);
		forwardingObjective.add();
		*/
		selectorBuilder.matchEthDst(dst);
		selectorBuilder.matchEthSrc(src);
		selectorBuilder.matchInPort(inPort);

		ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
			.withSelector(selectorBuilder.build())
			.withTreatment(treatment)
			.withPriority(PacketPriority.HIGH.priorityValue())
			.withFlag(ForwardingObjective.Flag.VERSATILE)
			.fromApp(appId)
			.makeTemporary(20) //timeout
			.add();

		flowObjectiveService.forward(device.deviceId(), forwardingObjective);
		packetOut(context, PortNumber.TABLE);

		log.info("[forwarding]	Install rule DeviceId {}\t	match src {}, dst {}, inport {}\t	to outport {}", device.deviceId(), src, dst, inPort, outPort);
	}

	/*
	private void flood(PacketContext context) {
		if (topologyService.isBroadcastPoint(topologyService.currentTopology(), 
											context.inPacket().receivedFrom())) {
			packetOut(context, PortNumber.FLOOD);
		}
		else{				
			context.block();
		}
	}
	*/


	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}
}
