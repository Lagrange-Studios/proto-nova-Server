Why you built it: your motivation and story.
Me and my friend who is the collaborator on this project both enjoy making games. Previously we only made games on the Roblox Engine and wanted to do something new, something tougher and with more of a challenge. So we decided to make proto nova which is a combination of all of our favorite games whith what we hope is out own new unique interesting twist. Over the course of the last nine months we have worked tirelessly on the project and got the pre alpha out to ship for Macondo.

A short description of what it does.
So the server description I put at the bottom. But the way the three repos work together is the library supplies the common classes and methods to both the server and client
who then use those classes to communicate between themselves and have gameplay. If you want the pitch of the game it is as follows, in proto nova you awake on a world with other people in the stone age. As you progress through the tech tree the planet begins to fall apart for some reason. So you are left with a choice find out what's happening and stop it or build a space ship and sail into the stars. The choice is yours.

How it fits together, with photos for hardware
I sorta feel like I answered this one in the previous question.

Welcome to the proto nova server program. Use the exe to start the server and type help to get all the commands.
It should automatically publish the server to your local network but if you want players outside of your network you will have to port forward or use another method.
To get your server put on the public server list use the website proto-nova.net to request your server be put up.
Tip: deleting the world root will reset the server just make sure to close it before you delete and then re open the server

Build Instructions:
Download the library and build with gradle.
Download the client and sever and set the required library as the library.
Build both with gradle.
It should now run!
