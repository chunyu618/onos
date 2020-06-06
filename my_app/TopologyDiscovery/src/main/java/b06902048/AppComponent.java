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

import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.packet.PacketPriority;


import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;

import org.onlab.packet.EthType;
import org.onlab.packet.EthType.EtherType;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());
	private ApplicationId appID;

    /** Some configurable property. */
    private String someProperty;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;
   
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected DeviceService deviceService;

	//@Reference(cardinality = ReferenceCardinality.MANDATORY)
	//protected FlowRuleService flowRuleService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowObjectiveService flowObjectiveService;

    @Activate
    protected void activate() {
		
		
		//register app
		appID = coreService.registerApplication("org.b06902048.app");
				
		Iterable<Device> devices = deviceService.getDevices();
		
		/*
		EthType.EtherType[] tmp = EthType.EtherType.values();
		for(EthType.EtherType t : tmp){
			log.info("type {} is {}", t.toString(), t.ethType().toShort());
		}
		*/

		for(Device d : devices){
			// selector
			// match LLDP packet from any physical port
			TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
			selector.matchInPhyPort(PortNumber.ALL);
			selector.matchEthType(EthType.EtherType.LLDP.ethType().toShort());
			//log.info("LLDP ethType is {}", EthType.EtherType.LLDP.ethType().toShort());
			
			
			// treatment
			// send every LLDP paclet to controller, except the one received from controller.
			TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
			treatment.setOutput(PortNumber.CONTROLLER);

			// forwardingObjective
			DefaultForwardingObjective.Builder forwardingObjectiveBuilder = DefaultForwardingObjective.builder();
			forwardingObjectiveBuilder.withFlag(ForwardingObjective.Flag.VERSATILE);
			forwardingObjectiveBuilder.withPriority(PacketPriority.HIGH.priorityValue());
			forwardingObjectiveBuilder.withSelector(selector.build());
			forwardingObjectiveBuilder.withTreatment(treatment.build());
			forwardingObjectiveBuilder.fromApp(appID);
			ForwardingObjective forwardingObjective = forwardingObjectiveBuilder.add();

			// install the rule
			flowObjectiveService.forward(d.id(), forwardingObjective);
			log.info("[Installation]	Install rule to DeviceID {}", d.id());
		}
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

}
