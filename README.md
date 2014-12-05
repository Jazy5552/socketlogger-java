socketlogger-java
=================

Will listen on a specific port for network traffic and output/record a log to a file.  A connection can be openned to another server and data can be sent through the command prompt.  Data can be sent back to a connected client (Only to the last connected client).  

The app can be started with an argument to determine the log file name (Default: logfile.txt) like so AppName.exe FILENAME 

Commands in the command prompt.
/open HOSTNAME/IPADDRESS  Used to establish a connection to a remote server.
/close  Will close the last connected client.
/restart Shutsdown the server and starts up again prompting for a new port number.
/exit Shutsdown the server and closes the app.
