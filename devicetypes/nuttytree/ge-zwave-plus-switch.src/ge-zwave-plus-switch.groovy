/**
 *  GE Z-Wave Plus Switch
 *
 *  Copyright 2017 Chris Nussbaum
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Author: Chris Nussbaum
 *	Date: 03/19/2017
 *
 *	Changelog:
 *
 *  0.10 (03/19/2017) -	Initial 0.1 Beta.
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pressed
 *   Double-Tap Down   2        pressed
 *
 */
metadata {
	definition (name: "GE Z-Wave Plus Switch", namespace: "nuttytree", author: "Chris Nussbaum") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Indicator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

		attribute "inverted", "enum", ["inverted", "not inverted"]
        
        command "doubleUp"
        command "doubleDown"
        command "inverted"
        command "notInverted"
        
		fingerprint mfr:"0063", prod:"4952", model: "3036", deviceJoinName: "GE Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3037", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
	}


	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}

	tiles(scale:2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821", nextState:"turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:"Turning On", action:"switch.off", icon:"st.Home.home30", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:"Turning Off", action:"switch.on", icon:"st.Home.home30", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}
        
        standardTile("doubleUp", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▲▲", backgroundColor: "#ffffff", action: "doubleUp", icon: "st.Home.home30"
		}     
 
        standardTile("doubleDown", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Tap ▼▼", backgroundColor: "#ffffff", action: "doubleDown", icon: "st.Home.home30"
		} 

		standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
			state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
			state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		}
        
		standardTile("inverted", "device.inverted", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "not inverted", label: "Not Inverted", action:"inverted", icon:"st.Home.home30", backgroundColor: "#ffffff"
			state "inverted", label: "Inverted", action:"notInverted", icon:"st.Home.home30", backgroundColor: "#79b821"
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
        details(["switch", "doubleUp", "doubleDown", "indicator", "inverted", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
    def result = null
	if (description != "updated") {
		log.debug "parse() >> zwave.parse($description)"
		def cmd = zwave.parse(description, [0x70: 2])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	log.debug("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		log.debug("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	sendEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    log.debug "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
	if (cmd.value == 255) {
    	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	sendEvent(name: "numberOfButtons", value: 2, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
    if (cmd.parameterNumber == 3) {
    	def value = cmd.configurationValue[0] == 1 ? "when on" : cmd.configurationValue[0] == 2 ? "never" : "when off"
        sendEvent(name: "indicatorStatus", value: value, displayed: false)
    }
    else {
    	def value = cmd.configurationValue[0] == 1 ? "inverted" : "not inverted"
        sendEvent(name: "inverted", value: value, displayed: false)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    sendEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    log.debug "---MANUFACTURER SPECIFIC REPORT V2---"
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "manufacturerName: ${cmd.manufacturerName}"
    state.manufacturer=cmd.manufacturerName
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)	
    sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

// handle commands
def configure() {
	log.debug "Executing 'configure'"
    
    def cmds = []
    // Get current config parameter values
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId)
    
    delayBetween(cmds,500)
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

def inverted() {
	sendEvent(name: "inverted", value: "inverted", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()))
}

def notInverted() {
	sendEvent(name: "inverted", value: "not inverted", display: false)
	sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()))
}

def doubleUp() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "1"], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: "2"], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def poll() {
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh'"
    delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
        zwave.configurationV2.configurationGet(parameterNumber: 3).format(),
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(),
        zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	])
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	])
}