/*
TP-linkServer.js V3.0.
This is a node.js bridge applet supporting TP-Link Devices.  There is a companion applet, "TP-LinkBridge.js" that is OPTIONAL.  The companion applet with the associated Device Handler provides for monitoring of the bridge device, and if a PC will allow for remote PC reboot if either of the applets are running.
History:
06-01-2017 - Release of update supporting 3.0 device handlers and the HS110 energy monitoring functions.  Added an error log file.
*/
//	---------------------------------------------------------------------------
var http = require('http')
var net = require('net')
var fs = require('fs')

var server = http.createServer(onRequest)
var serverPort = '8082'  // Same is in various groovy files.
server.listen(serverPort)
console.log("TP-Link Device Bridge Application")
fs.appendFile("error.log", "\n\r\n\r" + new Date() + "TP-Link Device Bridge Error Log")
//	---------------------------------------------------------------------------
function onRequest(request, response){
	console.log(" ")
	console.log(new Date())
	var command = request.headers["command"]
	switch(command) {
		case "restartPC":
			var bridgeExec = require('child_process').exec
			console.log("Restarting PC")
			response.setHeader("cmd-response", "restartPC")
			response.end()
			bridgeExec('shutdown /r /t 005')
			break

		case "pollServer":
			console.log("Server poll response sent to SmartThings")
			response.setHeader("cmd-response", "ok")
			response.end()
			break

		case "deviceCommand":
			processDeviceCommand(request, response)
			break

		default:
			console.log("Invalid Command received from SmartThings")
			response.setHeader("cmd-response", "commError")
			fs.appendFile("error.log", "\n\r\n\r" + new Date() + "#### Invalid Command: " + command)
			response.end()
	}
}
//	---------------------------------------------------------------------------
function processDeviceCommand(request, response) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	console.log("Sending to IP address: " + deviceIP + " Command: " + command)

//	---------------------------------------------------------------------------
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(4000)  // 4 seconds timeout.
   	 socket.on('connect', () => {
  		socket.write(encrypt(command))
   	 })
//	---------------------------------------------------------------------------
	var concatData = ""
	var resp = ""
	setTimeout(mergeData, 500)
	function mergeData() {
		if (concatData != "") {
			data = decrypt(concatData.slice(4)).toString('ascii')
			console.log("Command Response sent to SmartThings!")
console.log(data)
			response.setHeader("cmd-response", data)
			response.end()
			socket.end()
		}
	}
//	---------------------------------------------------------------------------
	socket.on('data', (data) => {
		concatData += data.toString('ascii')
//	---------------------------------------------------------------------------
	}).on('timeout', () => {
		socket.end()
		response.setHeader("cmd-response", 'commError')
		console.log("##### commsError:  Communications Timeout #####")
		fs.appendFile("error.log", "\n\r\n\r" + new Date() + "#### No Comms with device: " + deviceIP)
		response.end()
	}).on('error', (err) => {
		socket.end()
		response.setHeader("cmd-response", 'commError')
		console.log("##### commsError:  Communications Data Error #####")
		fs.appendFile("error.log", "\n\r\n\r" + new Date() + "#### Comms error with device: " + deviceIP)
		response.end()
	})
//	---------------------------------------------------------------------------
	function encrypt(input) {
		var buf = Buffer.alloc(input.length)
		var key = 0xAB
		for (var i = 0; i < input.length; i++) {
			buf[i] = input.charCodeAt(i) ^ key
			key = buf[i]
		}
		var bufLength = Buffer.alloc(4)
		bufLength.writeUInt32BE(input.length, 0)
		return Buffer.concat([bufLength, buf], input.length + 4)
	}
//	---------------------------------------------------------------------------
	function decrypt(input, firstKey) {
		var buf = Buffer.from(input)
		var key = 0x2B
		var nextKey
		for (var i = 0; i < buf.length; i++) {
			nextKey = buf[i]
			buf[i] = buf[i] ^ key
			key = nextKey
		}
		return buf
	}
}
