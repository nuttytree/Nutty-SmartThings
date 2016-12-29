/**
 *  Nutty Master Switch
 *
 *  Copyright 2016 Chris Nussbaum
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
 */
definition(
    name: "Nutty Master Switch",
    namespace: "nuttytree",
    author: "Chris Nussbaum",
    description: "Creates a virtual switch that will be on if any of a group of physical switches are on and off if they are all off, turning off the master switch will turn off all of the switches in the group, turning on the master switch will turn on a selected subset of the group of switches.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home30-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png")


preferences {
    page(name: "settingsPage")
}

def settingsPage() {
    dynamicPage(name: "settingsPage", title: "Nutty Master Switch Settings", install: true, uninstall: true) {
        section("Master Switch Name") {
    		input("masterName", "text", title: "Name?", required: true)
	    }
        section("Switches that control the state of the master switch") {
    		input("allSwitches", "capability.switch", title: "Switches?", multiple: true, required: true, submitOnChange: true)
	    }
    	if (allSwitches) {
            section("Switches that turn on with the master switch") {
                allSwitches.each { s ->
                    input("turnOn${s.displayName}", "bool", title: s.displayName)
                }
    	    }
        }
    }
}

def installed() {
	initialize()
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	unsubscribe()
	initialize()
	log.debug "Updated with settings: ${settings}"
}

def initialize() {
	app.updateLabel("Nutty Master Switch - " + masterName)
    def childDevices = getAllChildDevices()
    def masterSwitch = null
    if (!childDevices) {
        masterSwitch = addChildDevice("smartthings", "On/Off Button Tile", app.id + masterName, null, [label: masterName])
    }
    else {
        masterSwitch = childDevices.first()
        masterSwitch.label = masterName
    }
    log.debug "Found or created master switch: ${masterSwitch}"
    subscribe(masterSwitch, "switch.on", masterOn)
    subscribe(masterSwitch, "switch.off", masterOff)
    
    subscribe(allSwitches, "switch.on", switchOn)
    subscribe(allSwitches, "switch.off", switchOff)
}
	
def masterOn(evt) {
    log.debug "Master switch was turned on"
    def on = allSwitches.find { it.currentSwitch == "on" }
    if (!on) {
	    def leaveOn = false
        allSwitches.each { s ->
    	    if (settings."turnOn${s.displayName}") {
	    		log.debug "Turning on ${s}"
    	    	s.on()
                leaveOn = true
	        }
    	}
        if (!leaveOn) {
        	evt.device.off()
        }
    }
}

def masterOff(evt) {
	log.debug "Master switch was turned off"
    allSwitches.each { s ->
    	log.debug "Turning off ${s}"
    	s.off()
    }
}

def switchOn(evt) {
	def s = evt.getDevice()
    log.debug "${s} was turned on"
    childDevices.first().on()
}

def switchOff(evt) {
    log.debug "${evt.device} was turned off"
    def on = allSwitches.find { it.currentSwitch == "on" }
    if (!on) {
    	childDevices.first().off()
    }
}