/*
HS110 Prototype Bridge Server.
This is a node.js server supporting TP-Link Devices.  This node server will:
a.  receive raw TP-Link device commands from SmartThings.
b.  encrypt and then send the command to the TP-Link device.
c.  return the response raw data to the SmartThings from the TP-Link Device.
d.  provides an interface to the TP-LinkBridge.groovy file which monitors this process and provides the ability to restart the PC.  Installation of the TP-LinkBridge.groovy DH is NOT REQUIRED for this program to work properly.

History:
03-13-2017 - Initial release with name TP-LinkServerLite.js
04-02-2017 - Ver 1.2.  Changed TCP timeout to 2 secs. Added error message that will be sent to SmartThings if TCP timeout is attained indicating a bulb off-line. Integrated TCP functions into onRequest. 
04-07-2017	- Ver 2.0.  Added TP-LinkBridge device handler interface.  Updated file name to 'TP-LinkServer.js'.
04-13-2017 - Ver 2.1.  Updated file to have switch. Added responses.
*/
//-------------------------------------------------------------------------------
var resp
var http = require('http')
var net = require('net')
var server = http.createServer(onRequest)
var serverPort = '8085'  // Same is in various groovy files.
server.listen(serverPort)
console.log("Prototype HS110 DH Bridge App")
//-- For each request received from the SmartThings. ----------------------------
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
			resp = ""
			processDeviceCommand(request)
			setTimeout(setCommandHeader, 500)
			function setCommandHeader() {
				var respData = decrypt(resp.slice(4))
console.log("Response Data:     " + respData)
				response.setHeader("cmd-response", respData)
				console.log("Command Response sent to SmartThings!")
				response.end()
			}
			break

		default:
			console.log("Invalid Command received from SmartThings")
			response.setHeader("cmd-response", "invalidServerCmd")
			response.end()
	}
}
//-- Response to TP-Link Device Commands ---------------------------------------
function processDeviceCommand(request) {
	var command = request.headers["tplink-command"]
	var deviceIP = request.headers["tplink-iot-ip"]
	console.log("Sending to IP address: " + deviceIP + " Command: " + command)
	var socket = net.connect(9999, deviceIP)
	socket.setKeepAlive(false)
	socket.setTimeout(600)
   	 socket.on('connect', () => {
  		socket.write(encrypt(command))
   	 })
	socket.on('data', (data) => {
		resp = resp + data.toString('ascii')
		socket.end()
	}).on('timeout', () => {
		resp = encrypt("TcpTimeout").toString('ascii')
		socket.end()
	}).on('error', (err) => {
		resp = encrypt("TcpError").toString('ascii')
		socket.end()
	})
}
//-- Encrypt the command including a 4 byte TCP header. -----------------------
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
//--- Decrypt the response. ---------------------------------------------------
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
