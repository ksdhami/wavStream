# WAV File Music Streaming Client and Server

Simple multi-threaded client server audio streaming project that operates over TCP/IP


## Deployement

__To Compile:__
1. First terminal: ```javac Server.java```
2. Second terminal: ```javac Client.java```


__To Run:__
1. First terminal: ```java Server <Port Number>```
2. Second terminal: ```java Client <Server IP> <Port Number>```
3. Log in with appropriate account
   1. As a User
      1. username: user
      2. password: pass
   2. As an Admin
      1.  username: admin
      2.  password: pass
4.  Enter Commands
    1.  Admin Only
        - `add <song name>`
        - `delete <song name>`
        - `create <username> <pass>`
        - `remove <username>`
    2. All Users
       - `play <song name>`
       - `pause`
       - `resume`
       - `stop`
       - `library` 

___

Future work is to add ability to use playlists
