# SynologyUploader
SynologyUploader is app for Sony cameras using [OpenMemories: Framework](https://github.com/ma1co/OpenMemories-Framework). This application uses official [Synology API](https://global.download.synology.com/download/Document/DeveloperGuide/Synology_File_Station_API_Guide.pdf)

## My camera (DSC-HX60V) is currently not able to connect to any wifi probably due to HW problem. During the searching for a wifi camera returns "System error" or simply freezes :worried: but other thinks like taking photos, movies, etc. works well. Due to this I am not able to debug and test new releases so I have to stop working on this project.

Last but not released version is attached in https://github.com/LubikR/SynologyUploader/issues/5, feel free to download and test it.

### Prerequisites
Application assumed that the wifi connection is established, if not error message will be shown to the user during the connection check to the Synology server.

### Installing
Use [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) or install through [sony-pmca.appspot.com](https://sony-pmca.appspot.com/apps) or through adb.

Thanks to [ma1co](https://github.com/ma1co) for creating the framework and many other great guidelines in his code.

### Usage
After the first run you have to set connection properties to Synology server (button Settings), ie. IP address, user name, password and shared directory on Synology server. All these attributes are checked before storing to the app's preferences. Fields are:
* Address or IP - fill in Synology address or IP without 'http://'
* Port - default ports are http - 5000, https - 5001
* User - User on Synology server
* Password - User's password
* Directory - Directory where the photos will be uploaded. Program creates subdirectory in form : Entered directory / Camera Model / Date in format DDMMYYYY / Uploaded photos
