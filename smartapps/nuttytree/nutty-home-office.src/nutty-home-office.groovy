/**
 *  Nutty Home Office
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
    name: "Nutty Home Office",
    namespace: "nuttytree",
    author: "Chris Nussbaum",
    description: "Sets home mode to \"Work from Home\" when one specific person is home during specific times/days and no other listed people are home.  It will check if the staus needs to change at the selected start time and when any of the presence sensors arrive or leave so it can account for someone leaving late (after the normal start time) or if you leave to run a quick errand (will return to \"Work from Home\" mode when you get back).",
    category: "Mode Magic",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office22-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office22-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office22-icn@2x.png")


preferences {
    page (name:"configActions")
}

def configActions() {
    dynamicPage(name: "configActions", title: "Configure Actions", uninstall: true, install: true) {
        section ("When this person") {
            input "person", "capability.presenceSensor", title: "Who?", multiple: false, required: true
        }
        section ("Is home after") {
            input "startTime", "time", title: "What time?", required: true
        }
        section ("And before") {
            input "endTime", "time", title: "What time?", required: true
        }
        section ("On these days") {
        	input "days", "enum", title: "Set for specific day(s) of the week?", multiple: true, required: true,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
        }
        section ("And none of these people are home") {
        	input "others", "capability.presenceSensor", title: "Who?", multiple: true, required: true
        }

        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
            phrases.sort()
            section("Perform this action") {
                input "wfhPhrase", "enum", title: "\"Working from Home\" action?", required: true, options: phrases
            }
            section("And at the end of the work day perform this action") {
                input "donePhrase", "enum", title: "\"Done with Work\" action?", required: true, options: phrases
            }
        }

        section (title: "More options") {
            input "wfhDelay", "number", title: "Delay action on arrival by this many minutes?", required: false
            input "sendPushMessage", "bool", title: "Send a push notification?"
            input "phone", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
    schedule(timeToday(startTime, location?.timeZone), "checkPresence")
	subscribe(person, "presence.present", presenceChange)
	subscribe(others, "presence.not present", presenceChange)
    if (customName) {
      app.setTitle(customName)
    }
}

def presenceChange(evt) {
	if (wfhDelay && (evt.value == "present")) {
    	runIn((wfhDelay * 60), checkPresence)
    } else {
    	checkPresence()
    }
}

def checkPresence() {
    if (timeOk && daysOk && othersGone && modeOk) {
        if (person.latestValue("presence") == "present") {
            log.debug "${person} is present, triggering WFH action."
            location.helloHome.execute(settings.wfhPhrase)
            def message = "${location.name} executed '${settings.wfhPhrase}' because ${person} is home."
            send(message)
            state.wfhMode = location.mode
        }
    }
}

private getTimeOk() {
	def curTime = now()
    def result = ((curTime >= timeToday(startTime).time) && (curTime < timeToday(endTime).time))
    log.debug "Time Ok: $result"
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    log.debug "Days Ok: $result"
    result
}

private getOthersGone() {
	def result = true
    if (others) {
    	others.each { other ->
        	if (other.latestValue("presence") == "present") {
            	result = false
            }
        }
    }
    log.debug "Others Gone: $result"
    result
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    log.debug "Mode Ok: $result"
    result
}

private send(msg) {
    if (sendPushMessage) {
        sendPush(msg)
    }

    if (phone) {
        sendSms(phone, msg)
    }

    log.debug msg
}

def doneWorking() {
	if (location.mode == state.wfhMode) {
    	location.helloHome.execute(settings.donePhrase)
    }
}
