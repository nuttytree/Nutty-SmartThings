/**
 *  Smart Garage Door
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
    name: "Smart Garage Door",
    namespace: "nuttytree",
    author: "Chris Nussbaum",
    description: "Automatically opens and closes a garage door based on presence.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn@2x.png")


preferences {
	section("Settings") {
        input "door", "capability.garageDoorControl", title: "Garage Door?", multiple: false, required: true
        input "vehicle", "capability.presenceSensor", title: "Who?", multiple: false, required: true
        input "onArrival", "bool", title: "Open on arrival?"
        input "onDeparture", "bool", title: "Close on departure?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	if (onArrival) {
  		log.debug "Subscribing to presence.present"
        subscribe(vehicle, "presence.present", openDoor)
    }
	if (onDeparture) {
  		log.debug "Subscribing to presence.not present"
  		subscribe(vehicle, "presence.not present", closeDoor)
    }
}

def openDoor(evt) {
	door.open()
}

def closeDoor(evt) {
	door.close()
}